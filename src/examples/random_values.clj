(ns examples.random-values
  "raylib [core] example - random values
   
   Generating random values with raylib's RNG.
   
   Difficulty: ⭐☆☆☆ (1/4)
   Based on: core/core_random_values.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.text.drawing :as rtd]
   [raylib.utils :as ru]
   [raylib.colors :as colors]))

(def screen-width 800)
(def screen-height 450)

(defn -main [& _args]
  (rcw/init-window! screen-width screen-height "raylib [core] example - random values")
  (rct/set-target-fps! 60)

  ;; Get initial random value between -8 and 5
  (let [initial-rand-value (ru/get-random-value -8 5)]
    (loop [rand-value initial-rand-value
           frames-counter 0]
      (if (rcw/window-should-close?)
        (rcw/close-window!)
        (let [;; Every 2 seconds (120 frames) generate new random value
              new-counter (inc frames-counter)
              should-update? (and (== 1 (mod (quot new-counter 120) 2))
                                  (zero? (mod new-counter 120)))
              new-rand-value (if should-update?
                               (ru/get-random-value -8 5)
                               rand-value)
              reset-counter (if should-update? 0 new-counter)]

          ;; Draw
          (rcd/begin-drawing!)
          (rcd/clear-background! colors/raywhite)

          (rtd/draw-text! "Every 2 seconds a new random value is generated:" 130 100 20 colors/maroon)
          (rtd/draw-text! (str new-rand-value) 360 180 80 colors/lightgray)

          (rcd/end-drawing!)
          (recur new-rand-value reset-counter))))))
