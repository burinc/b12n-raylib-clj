(ns examples.math-angle-rotation
  "raylib [shapes] example - math angle rotation

   Four fixed lines at 0/30/60/90 degrees, each labelled, plus one line
   sweeping a full turn at one degree per frame. The sweeping line takes
   its colour from its own angle through HSV, so hue and heading stay in
   step.

   Difficulty: 1/4
   Based on: shapes/shapes_math_angle_rotation.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 720)
(def screen-height 400)

(def line-length 150.0)
(def center {:x (/ screen-width 2.0) :y (/ screen-height 2.0)})

;; The C keeps a parallel angles[] and a switch over the index for colour.
;; One vector of pairs says the same thing without the index bookkeeping.
(def fixed-lines
  [[0 colors/green]
   [30 colors/orange]
   [60 colors/blue]
   [90 colors/magenta]])

(def ^:private deg->rad (/ Math/PI 180.0))

(defn- point-at
  "Point `distance` from centre along `degrees`."
  [degrees distance]
  (let [rad (* degrees deg->rad)]
    {:x (float (+ (:x center) (* (Math/cos rad) distance)))
     :y (float (+ (:y center) (* (Math/sin rad) distance)))}))

(defn initial-state [] {:total-angle 0.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height
                    "raylib [shapes] example - math angle rotation")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [state]
  (debug-stats/update!)
  (update state :total-angle #(mod (+ % 1.0) 360.0)))

(defn draw [{:keys [total-angle]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/white)
  (rtd/draw-text! "Fixed angles + rotating line" 10 10 20 colors/lightgray)

  (doseq [[degrees color] fixed-lines]
    (rsb/draw-line-ex! (point-at degrees 0.0) (point-at degrees line-length)
                       (float 5.0) color)
    (let [label (point-at degrees (+ line-length 20.0))]
      (rtd/draw-text! (str degrees "°") (int (:x label)) (int (:y label)) 20 color)))

  ;; Hue follows the sweep, so the line's colour reads as its heading.
  (rsb/draw-line-ex! (point-at total-angle 0.0)
                     (point-at total-angle line-length)
                     (float 5.0)
                     (ru/color-from-hsv (float total-angle) (float 0.8) (float 0.9)))

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
