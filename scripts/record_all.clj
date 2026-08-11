#!/usr/bin/env bb
;; record_all.clj — batch-record a demo GIF for one or more examples via
;; cgevent (macOS input synthesis + window capture, ~/.local/bin/cgevent).
;;
;;   bb record --only hello-world      ; record just one example
;;   bb record                         ; record everything not up to date
;;   bb record --only bounce,starfield ; subset (comma-separated ids)
;;   bb record --force                 ; ignore the ledger
;;   bb record --dry-run               ; show the plan
;;
;; bb record (in bb.edn) wraps: bb scripts/record_all.clj
;;
;; Adapted from the same pattern used by ~/dev/b12n-rljlt/scripts/record_all.clj,
;; simplified: that project's script drives its own screenshot-loop +
;; gifski/ffmpeg encode because it predates cgevent's built-in `record-gif`
;; subcommand. Here `cgevent record-gif --pid N --duration D -o FILE` does
;; the fixed-fps capture AND the GIF encode in one call, so this script only
;; needs to launch the example, wait for its window, optionally synthesize
;; an input timeline, and hand the pid to cgevent.

(ns scripts.record-all
  (:require [babashka.cli :as cli]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [bb.helpers :as h]
            [clojure.edn :as edn]
            [clojure.string :as str])
  (:import [java.security MessageDigest]))

(def spec
  {:manifest {:coerce :string
              :default "scripts/demo_manifest.edn"}
   :out-dir  {:coerce :string
              :default "docs/demos"}
   :ledger   {:coerce :string
              :default "docs/demos/ledger.edn"}
   :only     {:coerce :string
              :default nil
              :desc "Comma-separated example ids"}
   :force    {:coerce :boolean
              :default false}
   :dry-run  {:coerce :boolean
              :default false}
   :readme   {:coerce :string
              :default "docs/demos/README.md"}})

;; ---------------------------------------------------------------- helpers

(defn sha256 [path]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (->> (.digest md (fs/read-all-bytes path))
         (map #(format "%02x" %))
         (apply str))))

(def mac? (str/starts-with? (System/getProperty "os.name") "Mac"))

;; ---------------------------------------------------------------- window

(defn focus!
  "Bring the process's window frontmost so the capture isn't of an occluded window."
  [pid]
  (cond
    mac? (p/shell {:continue true
                   :out nil
                   :err nil}
                  "osascript" "-e"
                  (format "tell application \"System Events\" to set frontmost of (first process whose unix id is %d) to true" pid))
    (fs/which "xdotool") (p/shell {:continue true
                                   :out nil
                                   :err nil}
                                  "xdotool" "search" "--pid" (str pid) "windowactivate")
    :else nil))

(defn wait-for-window
  "Poll `cgevent windows --pid N` until it reports at least one on-screen
   window for the process, or `timeout-ms` elapses. Beats a fixed sleep —
   JVM+GLFW startup varies with classpath cache state."
  [{:keys [pid timeout-ms]}]
  (let [end (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [{:keys [out]} (p/shell {:out :string
                                    :err nil
                                    :continue true}
                                   "cgevent" "windows" "--pid" (str pid))
            n (some-> (re-find #"\((\d+)\)" (or out "")) second parse-long)]
        (cond
          (and n (pos? n)) true
          (> (System/currentTimeMillis) end) false
          :else (do (Thread/sleep 150) (recur)))))))

;; ---------------------------------------------------------------- input

(defn synth-key!
  "Post a key chord to `pid` via cgevent."
  [pid k]
  (p/shell {:continue true
            :out nil
            :err nil} "cgevent" "key" "--pid" (str pid) k))

(defn synth-click!
  "Click at (x, y) relative to `pid`'s frontmost window (--window), matching
   the manifest's window-relative coordinates rather than global screen
   coordinates — the target window is not guaranteed to sit at (0,0)."
  [pid x y]
  (p/shell {:continue true
            :out nil
            :err nil} "cgevent" "click" "--pid" (str pid) "--window" (str x) (str y)))

(defn play-input!
  "Fire a timeline of [at-seconds :key \"left\"] / [at-seconds :click x y]
   events against `pid` on a background thread."
  [pid timeline]
  (future
    (let [t0 (System/currentTimeMillis)]
      (doseq [[at action & args] (sort-by first timeline)]
        (let [wait (- (+ t0 (long (* 1000 at))) (System/currentTimeMillis))]
          (when (pos? wait) (Thread/sleep wait))
          (case action
            :key   (synth-key! pid (first args))
            :click (let [[x y] args] (synth-click! pid x y))
            nil))))))

;; ---------------------------------------------------------------- one example

(defn record-one!
  [{:keys [id src run duration fps width warmup input out-dir]}]
  (let [gif (str (fs/file out-dir (str id ".gif")))]
    (fs/create-dirs out-dir)
    (println (format "\n▶ %s  (%s)" id src))
    (let [proc (p/process {:out :string
                           :err :string
                           :shutdown p/destroy-tree} run)
          pid  (.pid (:proc proc))]
      (try
        (if-not (wait-for-window {:pid pid
                                  :timeout-ms (long (* 1000 (or warmup 10)))})
          (do (println "  ✗ window never appeared")
              {:id id
               :status :no-window
               ;; :err is a future that only resolves at process EOF; bound
               ;; the deref so a still-alive-but-slow process can't hang the run.
               :stderr (when-let [s (some-> (deref (:err proc) 2000 nil) str/join)]
                         (subs s 0 (min 400 (count s))))})
          (do
            (focus! pid)
            (Thread/sleep 250)
            (let [input-fut (when (seq input) (play-input! pid input))
                  {:keys [exit err]} (p/shell {:continue true
                                               :out nil
                                               :err :string}
                                              "cgevent" "record-gif"
                                              "--pid" (str pid)
                                              "--duration" (str duration)
                                              "-o" gif
                                              "--fps" (str fps)
                                              "--width" (str width))]
              ;; Recording is done — no more input needed; bounds the
              ;; background thread's lifetime instead of leaking it.
              (when input-fut (future-cancel input-fut))
              (if (and (zero? exit) (fs/exists? gif) (pos? (fs/size gif)))
                {:id id
                 :status :done
                 :gif gif
                 :bytes (fs/size gif)
                 :sha (sha256 src)
                 :duration duration
                 :fps fps
                 :width width}
                (do (fs/delete-if-exists gif)
                    {:id id
                     :status :record-failed
                     :stderr err})))))
        (finally
          (p/destroy-tree proc)
          (deref proc 3000 nil))))))

;; ---------------------------------------------------------------- manifest

(def ^:private deps-aliases
  (:aliases (edn/read-string (slurp "deps.edn"))))

(defn- ns->src-path [ns-str]
  (str "src/" (-> ns-str (str/replace "." "/") (str/replace "-" "_")) ".clj"))

(defn- src-path
  "The example's source file path, derived from deps.edn's :main-opts for
   this alias — e.g. \"bouncing-ball\" -> \"src/examples/bouncing_ball.clj\",
   \"waving-cubes\" -> \"src/examples/models/waving_cubes.clj\"."
  [alias]
  (let [main-opts (:main-opts (get deps-aliases (keyword alias)))
        ns-str    (str (second (drop-while #(not= "-m" %) main-opts)))]
    (ns->src-path ns-str)))

(defn load-manifest
  "Build the full per-example spec list from bb.helpers/examples — the
   single source of truth bb.edn's own tasks and `bb examples` are already
   generated from — merged with demo_manifest.edn's :defaults and any
   per-id :overrides."
  [manifest-path]
  (let [{:keys [defaults overrides]} (edn/read-string (slurp manifest-path))]
    (for [{:keys [alias category desc]} h/examples]
      (merge defaults
             {:id alias
              :group category
              :desc desc
              :src (src-path alias)
              :run (str "clojure -M:" alias)}
             (get overrides alias)))))

(defn up-to-date? [ledger {:keys [id src out-dir duration fps width]}]
  (let [prev (get ledger id)
        gif  (fs/file out-dir (str id ".gif"))]
    (and prev (fs/exists? gif)
         (= (:sha prev) (sha256 src))
         (= (:settings prev) [duration fps width]))))

;; Category display order + emoji/title mirror bb.helpers/print-examples-help
;; (bb.helpers/example-categories is a small map literal — iteration order
;; isn't a documented guarantee, so the README grouping pins its own order).
(def ^:private category-order
  [:games :core :shapes :textures :audio :shaders :models :text])

(defn write-readme!
  "Grouped catalog (mirrors `bb examples`'s category ordering), one heading
   + GIF per example present in `ledger` — the full cumulative set of
   everything ever successfully recorded, not just this run's newly-recorded
   subset. A run where everything was already up to date has an empty
   `results`; keying off `ledger` instead means the README doesn't get
   wiped down to nothing on such a run."
  [path ledger out-dir]
  (fs/create-dirs (fs/parent path))
  (let [done-ids (keys ledger)
        group-of (fn [id] (:category (h/find-example id)))]
    (spit path
          (str "# Demos\n\n"
               "Animated GIF previews, recorded via cgevent — see "
               "[`scripts/record_all.clj`](../../scripts/record_all.clj). "
               "Regenerate with `bb record`.\n\n"
               (str/join "\n"
                         (for [cat category-order
                               :let [{:keys [emoji title]} (get h/example-categories cat)
                                     ids (filter #(= cat (group-of %)) done-ids)]
                               :when (seq ids)]
                           (str (format "## %s %s\n\n" emoji title)
                                (str/join "\n"
                                          (for [id ids]
                                            (format "### %s\n\n![%s](%s)\n" id id
                                                    (str (fs/file-name (fs/file out-dir (str id ".gif"))))))))))
               "\n")))
  (println "\nWrote" path))

;; ---------------------------------------------------------------- main

(defn -main [& args]
  (let [{:keys [manifest out-dir ledger only force dry-run readme]}
        (cli/parse-opts args {:spec spec})
        ledger-data (if (fs/exists? ledger) (edn/read-string (slurp ledger)) {})
        wanted      (when only (set (str/split only #",")))
        examples    (cond->> (map #(assoc % :out-dir out-dir) (load-manifest manifest))
                      wanted (filter (comp wanted :id)))
        todo        (if force examples (remove #(up-to-date? ledger-data %) examples))]
    (println (format "%d examples, %d to record, %d up to date."
                     (count examples) (count todo) (- (count examples) (count todo))))
    (when dry-run
      (doseq [e todo] (println "  -" (:id e)))
      (System/exit 0))
    ;; Capture is strictly serial — only one window can be frontmost, and
    ;; `cgevent record-gif` already blocks for its own duration, so there's
    ;; no separate encode stage left to run concurrently with the next capture.
    (let [results (mapv record-one! todo)
          updated (reduce (fn [m {:keys [id status] :as r}]
                            (if (= :done status)
                              (assoc m id {:sha (:sha r)
                                           :settings [(:duration r) (:fps r) (:width r)]
                                           :bytes (:bytes r)
                                           :at (str (java.time.Instant/now))})
                              m))
                          ledger-data results)]
      (fs/create-dirs (fs/parent ledger))
      (spit ledger (pr-str updated))
      (write-readme! readme updated out-dir)
      (let [failed (remove #(= :done (:status %)) results)]
        (when (seq failed)
          (println "\nFailed:")
          (doseq [f failed] (println "  " (:id f) (:status f))))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
