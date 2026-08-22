(ns examples.rectangle-scaling
  "raylib [shapes] example - rectangle scaling

   Drag the bottom-right corner of a rectangle to resize it.
   Demonstrates mouse interaction and collision detection.

   Difficulty: 2/4
   Based on: shapes/shapes_rectangle_scaling.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.mouse :as rcm]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.utils :as ru]
   [raylib.core.collision :as rcol]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)
(def MARK-SIZE 12)

(defn initial-state []
  {:rec {:x 100.0 :y 100.0 :width 200.0 :height 80.0}
   :scale-ready false
   :scale-mode false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - rectangle scaling")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [rec scale-mode] :as state}]
  (debug-stats/update!)
  (let [mouse (rcm/get-mouse-position)
        mx (:x mouse)
        my (:y mouse)
        ;; Check if mouse is over the scale mark (bottom-right corner)
        mark-rect {:x (float (+ (:x rec) (:width rec) (- MARK-SIZE)))
                   :y (float (+ (:y rec) (:height rec) (- MARK-SIZE)))
                   :width (float MARK-SIZE)
                   :height (float MARK-SIZE)}
        over-mark (pos? (rcol/check-collision-point-rec? mouse mark-rect))]
    (if scale-mode
      ;; Currently scaling
      (let [w (max MARK-SIZE (min (- mx (:x rec)) (- screen-width (:x rec))))
            h (max MARK-SIZE (min (- my (:y rec)) (- screen-height (:y rec))))]
        (assoc state
               :rec (assoc rec :width (float w) :height (float h))
               :scale-ready true
               :scale-mode (not (rcm/is-mouse-button-released? (:left enums/mouse-button)))))
      ;; Not scaling - check hover
      (assoc state
             :scale-ready over-mark
             :scale-mode (and over-mark
                              (rcm/is-mouse-button-pressed? (:left enums/mouse-button)))))))

(defn draw [{:keys [rec scale-ready]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rtd/draw-text! "Scale rectangle dragging from bottom-right corner!" 10 10 20 colors/gray)

  (rsb/draw-rectangle-rec! rec (ru/fade colors/green (float 0.5)))

  (when scale-ready
    (rsb/draw-rectangle-lines-ex! rec (float 1) colors/red)
    (rsb/draw-triangle! {:x (float (+ (:x rec) (:width rec) (- MARK-SIZE)))
                          :y (float (+ (:y rec) (:height rec)))}
                         {:x (float (+ (:x rec) (:width rec)))
                          :y (float (+ (:y rec) (:height rec)))}
                         {:x (float (+ (:x rec) (:width rec)))
                          :y (float (+ (:y rec) (:height rec) (- MARK-SIZE)))}
                         colors/red))

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
