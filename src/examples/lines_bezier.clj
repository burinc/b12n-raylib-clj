(ns examples.lines-bezier
  "raylib [shapes] example - lines bezier

   Drag start and end points to reshape a cubic Bezier curve.

   Difficulty: 1/4
   Based on: shapes/shapes_lines_bezier.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.mouse :as rcm]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib-ext :as ext]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(defn initial-state []
  {:start-point {:x 30.0 :y 30.0}
   :end-point {:x (- screen-width 30.0) :y (- screen-height 30.0)}
   :move-start false
   :move-end false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/set-config-flags :flag/msaa-4x-hint)
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - lines bezier")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [start-point end-point move-start move-end] :as state}]
  (debug-stats/update!)
  (let [mouse (rcm/get-mouse-position)
        left-down (rcm/is-mouse-button-down? (:left enums/mouse-button))
        left-released (rcm/is-mouse-button-released? (:left enums/mouse-button))
        over-start (pos? (ext/check-collision-point-circle? mouse start-point (float 10.0)))
        over-end (pos? (ext/check-collision-point-circle? mouse end-point (float 10.0)))
        ;; Start dragging
        move-start (if (and (not move-start) over-start left-down) true move-start)
        move-end (if (and (not move-end) (not move-start) over-end left-down) true move-end)
        ;; Update positions while dragging
        start-point (if move-start mouse start-point)
        end-point (if move-end mouse end-point)
        ;; Stop dragging on release
        move-start (if (and move-start left-released) false move-start)
        move-end (if (and move-end left-released) false move-end)]
    (assoc state
           :start-point start-point
           :end-point end-point
           :move-start move-start
           :move-end move-end)))

(defn draw [{:keys [start-point end-point move-start move-end]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rtd/draw-text! "MOVE START-END POINTS WITH MOUSE" 15 20 20 colors/gray)

  ;; Draw bezier curve
  (rsb/draw-line-bezier! start-point end-point (float 4.0) colors/blue)

  ;; Draw endpoint circles with hover feedback
  (let [mouse (rcm/get-mouse-position)
        over-start (pos? (ext/check-collision-point-circle? mouse start-point (float 10.0)))
        over-end (pos? (ext/check-collision-point-circle? mouse end-point (float 10.0)))
        start-radius (if over-start (float 14.0) (float 8.0))
        end-radius (if over-end (float 14.0) (float 8.0))
        start-color (if move-start colors/red colors/blue)
        end-color (if move-end colors/red colors/blue)]
    (rsb/draw-circle-v! start-point start-radius start-color)
    (rsb/draw-circle-v! end-point end-radius end-color))

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
