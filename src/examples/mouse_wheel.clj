(ns examples.mouse-wheel
  "raylib [core] example - input mouse wheel
   
   Using mouse wheel to move objects.
   
   Difficulty: ⭐☆☆☆ (1/4)
   Based on: core/core_input_mouse_wheel.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.mouse :as rcm]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]))

(def screen-width 800)
(def screen-height 450)
(def scroll-speed 4)

(defn -main [& _args]
  (rcw/init-window! screen-width screen-height "raylib [core] example - input mouse wheel")
  (rct/set-target-fps! 60)

  (loop [box-y (- (/ screen-height 2) 40)]
    (if (rcw/window-should-close?)
      (rcw/close-window!)
      (let [;; Update - mouse wheel moves box up/down
            wheel-move (rcm/get-mouse-wheel-move)
            new-box-y (- box-y (* wheel-move scroll-speed))]

        ;; Draw
        (rcd/begin-drawing!)
        (rcd/clear-background! colors/raywhite)

        (rsb/draw-rectangle! (- (/ screen-width 2) 40) (int new-box-y) 80 80 colors/maroon)

        (rtd/draw-text! "Use mouse wheel to move the cube up and down!" 10 10 20 colors/gray)
        (rtd/draw-text! (format "Box position Y: %03d" (int new-box-y)) 10 40 20 colors/lightgray)

        (rcd/end-drawing!)
        (recur new-box-y)))))
