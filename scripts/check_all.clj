(ns check-all
  "Headless compile-check of every namespace under src/.

  `bb check` used to require exactly one namespace (examples.asteroids), which
  proved the FFI layer loads but said nothing about the other 77 examples. A
  broken require or a typo'd binding in any of them shipped green. This walks
  every .clj file under src/, derives its namespace from the path, and requires
  it — the same thing `clj -M:<alias>` does at startup, minus `-main`, so no
  window opens and nothing needs a display.

  Exits 1 with a per-namespace report if any fail."
  (:require [clojure.string :as str]))

(defn- path->ns
  "src/examples/models/dna_helix.clj -> examples.models.dna-helix"
  [path]
  (-> path
      (subs (count "src/"))
      (str/replace #"\.clj$" "")
      (str/replace "/" ".")
      (str/replace "_" "-")
      symbol))

(defn -main [& _args]
  (let [nses (->> (file-seq (java.io.File. "src"))
                  (filter #(.isFile ^java.io.File %))
                  (map #(.getPath ^java.io.File %))
                  (filter #(str/ends-with? % ".clj"))
                  (map path->ns)
                  sort)
        failures (reduce (fn [acc n]
                           (try (require n) acc
                                (catch Throwable t
                                  (conj acc [n (or (.getMessage t) (str t))]))))
                         []
                         nses)]
    (println)
    (if (seq failures)
      (do (println (format "✗ %d of %d namespaces failed to compile:"
                           (count failures) (count nses)))
          (doseq [[n msg] failures] (println " " n "-" msg))
          (System/exit 1))
      (println (format "✓ all %d namespaces compile" (count nses))))))
