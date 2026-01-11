(ns debug-stats
  "Debug stats overlay plugin.
   
   Usage:
   1. Require this namespace in your game ns
   2. Call (debug-stats/enable!) once at startup
   3. Call (debug-stats/update!) in your game tick function
   4. Call (debug-stats/draw!) at the end of your draw function (inside begin/end-drawing)
   5. Press F1 to toggle the stats overlay
   
   Example:
   (ns my-game
     (:require [debug-stats]))
   
   (defn init []
     (debug-stats/enable!))
   
   (defn tick [game]
     (debug-stats/update!)
     ;; ... your game logic
     )
   
   (defn draw [game]
     (rcd/begin-drawing!)
     ;; ... your drawing code
     (debug-stats/draw!)
     (rcd/end-drawing!))"
  (:require
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib-ext :as ext])
  (:import [java.lang Runtime]))

;; State
(def ^:private state (atom {:enabled false
                            :visible false
                            :fps 0
                            :frame-time 0
                            :frame-count 0
                            :last-update 0
                            :custom-stats {}}))

;; Memory helpers
(defn- bytes->mb [bytes]
  (/ bytes 1024.0 1024.0))

(defn- get-memory-stats []
  (let [runtime (Runtime/getRuntime)
        max-mem (bytes->mb (.maxMemory runtime))
        total-mem (bytes->mb (.totalMemory runtime))
        free-mem (bytes->mb (.freeMemory runtime))
        used-mem (- total-mem free-mem)]
    {:max-mb max-mem
     :total-mb total-mem
     :free-mb free-mem
     :used-mb used-mem
     :usage-pct (* 100 (/ used-mem max-mem))}))

;; FPS calculation
(def ^:private fps-samples (atom []))
(def ^:private last-frame-time (atom (System/nanoTime)))

(defn- calculate-fps []
  (let [now (System/nanoTime)
        frame-time-ns (- now @last-frame-time)
        frame-time-ms (/ frame-time-ns 1000000.0)]
    (reset! last-frame-time now)
    (swap! fps-samples (fn [samples]
                         (vec (take-last 60 (conj samples frame-time-ns)))))
    (let [samples @fps-samples
          avg-frame-time (if (empty? samples)
                           16666666
                           (/ (reduce + samples) (count samples)))
          fps (/ 1000000000.0 avg-frame-time)]
      {:fps (int fps)
       :frame-time-ms frame-time-ms
       :avg-frame-time-ms (/ avg-frame-time 1000000.0)})))

;; Public API

(defn enable!
  "Enable the debug stats system. Call once at startup."
  []
  (swap! state assoc :enabled true)
  (reset! fps-samples [])
  (reset! last-frame-time (System/nanoTime)))

(defn disable!
  "Disable the debug stats system."
  []
  (swap! state assoc :enabled false :visible false))

(defn toggle!
  "Toggle visibility of the stats overlay."
  []
  (swap! state update :visible not))

(defn visible?
  "Check if stats overlay is currently visible."
  []
  (:visible @state))

(defn set-custom-stat!
  "Add a custom stat to display. 
   Example: (set-custom-stat! :enemies 42)"
  [key value]
  (swap! state assoc-in [:custom-stats key] value))

(defn remove-custom-stat!
  "Remove a custom stat."
  [key]
  (swap! state update :custom-stats dissoc key))

(defn update!
  "Update stats. Call this in your game tick/update function."
  []
  (when (:enabled @state)
    ;; Toggle on F1
    (when (rck/is-key-pressed? (:f1 enums/keyboard-key))
      (toggle!))
    ;; Update frame count
    (swap! state update :frame-count inc)))

(defn- format-stat [label value & [unit]]
  (str label ": " value (when unit (str " " unit))))

(defn draw!
  "Draw the stats overlay. Call this inside your drawing code (after begin-drawing!)."
  []
  (when (and (:enabled @state) (:visible @state))
    (let [{:keys [fps frame-time-ms avg-frame-time-ms]} (calculate-fps)
          {:keys [used-mb max-mb usage-pct]} (get-memory-stats)
          custom-stats (:custom-stats @state)
          frame-count (:frame-count @state)

          ;; Layout
          padding 8
          line-height 18
          font-size 14
          x 10
          y 10

          ;; Build stats lines
          lines [(format-stat "FPS" fps)
                 (format-stat "Frame" (format "%.2f" frame-time-ms) "ms")
                 (format-stat "Avg Frame" (format "%.2f" avg-frame-time-ms) "ms")
                 ""
                 (format-stat "Memory" (format "%.1f / %.1f" used-mb max-mb) "MB")
                 (format-stat "Mem Usage" (format "%.1f" usage-pct) "%")
                 ""
                 (format-stat "Frame #" frame-count)]

          ;; Add custom stats
          custom-lines (when (seq custom-stats)
                         (concat ["" "-- Custom --"]
                                 (map (fn [[k v]] (format-stat (name k) v)) custom-stats)))

          all-lines (concat lines custom-lines)

          ;; Calculate background size
          max-width (apply max (map #(if (empty? %) 0 (ext/measure-text % font-size)) all-lines))
          bg-width (+ max-width (* padding 2))
          bg-height (+ (* (count all-lines) line-height) (* padding 2))]

      ;; Draw semi-transparent background
      (rsb/draw-rectangle! x y bg-width bg-height {:r 0
                                                   :g 0
                                                   :b 0
                                                   :a 180})

      ;; Draw border
      (rsb/draw-rectangle! x y bg-width 2 colors/green)
      (rsb/draw-rectangle! x (+ y bg-height -2) bg-width 2 colors/green)
      (rsb/draw-rectangle! x y 2 bg-height colors/green)
      (rsb/draw-rectangle! (+ x bg-width -2) y 2 bg-height colors/green)

      ;; Draw title
      (rtd/draw-text! "[F1] Debug Stats" (+ x padding) (+ y padding) font-size colors/green)

      ;; Draw stats
      (doseq [[idx line] (map-indexed vector all-lines)]
        (when-not (empty? line)
          (let [line-y (+ y padding (* (inc idx) line-height))
                color (cond
                        (clojure.string/starts-with? line "FPS") colors/lime
                        (clojure.string/starts-with? line "Mem") colors/skyblue
                        (clojure.string/starts-with? line "--") colors/yellow
                        :else colors/white)]
            (rtd/draw-text! line (+ x padding) line-y font-size color)))))))

;; Convenience macro for automatic stats in game loops
(defmacro with-stats
  "Wrap your game loop body to automatically update and draw stats.
   
   Example:
   (with-stats
     (rcd/begin-drawing!)
     ;; your draw code
     (rcd/end-drawing!))"
  [& body]
  `(do
     (update!)
     ~@body))
