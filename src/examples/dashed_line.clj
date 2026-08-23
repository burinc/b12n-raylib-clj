(ns examples.dashed-line
  "raylib [shapes] example - dashed line

   Draw a dashed line that follows the mouse. Adjust dash
   and space length with arrow keys, cycle color with C.

   Difficulty: 1/4
   Based on: shapes/shapes_dashed_line.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def line-colors [colors/red colors/orange colors/gold colors/green
                  colors/blue colors/violet colors/pink colors/black])

(defn initial-state []
  {:start-pos {:x 20.0 :y 50.0}
   :dash-length 25
   :blank-length 15
   :color-index 0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - dashed line")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [dash-length blank-length color-index] :as state}]
  (debug-stats/update!)
  (let [dash-length (cond-> dash-length
                      (rck/is-key-down? (:up enums/keyboard-key)) inc
                      (and (rck/is-key-down? (:down enums/keyboard-key)) (> dash-length 1)) dec)
        blank-length (cond-> blank-length
                       (rck/is-key-down? (:right enums/keyboard-key)) inc
                       (and (rck/is-key-down? (:left enums/keyboard-key)) (> blank-length 1)) dec)
        color-index (if (rck/is-key-pressed? (:c enums/keyboard-key))
                      (mod (inc color-index) (count line-colors))
                      color-index)]
    (assoc state
           :dash-length dash-length
           :blank-length blank-length
           :color-index color-index)))

(defn draw [{:keys [start-pos dash-length blank-length color-index]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; Draw dashed line to mouse position
  (let [end-pos (rcm/get-mouse-position)
        line-color (nth line-colors color-index)]
    ;; 2.0 keeps this example's original look: the shared helper defaults to
    ;; 1px, matching raylib's own DrawLineDashed, but this drew at 2px before
    ;; the helper existed and its committed demo GIF was recorded that way.
    (rsb/draw-dashed-line! start-pos end-pos dash-length blank-length line-color 2.0))

  ;; UI panel
  (rsb/draw-rectangle! 5 5 265 95 (ru/fade colors/skyblue (float 0.5)))
  (rsb/draw-rectangle-lines! 5 5 265 95 colors/blue)

  (rtd/draw-text! "CONTROLS:" 15 15 10 colors/black)
  (rtd/draw-text! "UP/DOWN: Change Dash Length" 15 35 10 colors/black)
  (rtd/draw-text! "LEFT/RIGHT: Change Space Length" 15 55 10 colors/black)
  (rtd/draw-text! "C: Cycle Color" 15 75 10 colors/black)

  (rtd/draw-text! (format "Dash: %d | Space: %d" dash-length blank-length)
                  15 115 10 colors/darkgray)

  (debug-stats/draw!)
  (rcd/end-drawing!))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (when-not (rcw/window-should-close?)
        (reset! game-atom game)
        (draw game)
        (recur))))
  (rcw/close-window!))
