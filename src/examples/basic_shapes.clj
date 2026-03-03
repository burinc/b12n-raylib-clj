(ns examples.basic-shapes
  "raylib [shapes] example - basic shapes

   Showcases all basic shape drawing functions available in raylib:
   circles, rectangles, triangles, polygons, ellipses, and lines.
   The polygon rotates continuously.

   Difficulty: 1/4
   Based on: shapes/shapes_basic_shapes.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(defn initial-state []
  {:rotation 0.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - basic shapes")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [rotation] :as state}]
  (debug-stats/update!)
  (assoc state :rotation (+ rotation 0.2)))

(defn draw [{:keys [rotation]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rtd/draw-text! "some basic shapes available on raylib" 20 20 20 colors/darkgray)

  ;; Circle shapes and lines
  (rsb/draw-circle! (quot screen-width 5) 120 (float 35) colors/darkblue)
  (rsb/draw-circle-gradient! (quot screen-width 5) 220 (float 60) colors/green colors/skyblue)
  (rsb/draw-circle-lines! (quot screen-width 5) 340 (float 80) colors/darkblue)
  (rsb/draw-ellipse! (quot screen-width 5) 120 (float 25) (float 20) colors/yellow)
  (rsb/draw-ellipse-lines! (quot screen-width 5) 120 (float 30) (float 25) colors/yellow)

  ;; Rectangle shapes and lines
  (rsb/draw-rectangle! (- (* (quot screen-width 4) 2) 60) 100 120 60 colors/red)
  (rsb/draw-rectangle-gradient-h! (- (* (quot screen-width 4) 2) 90) 170 180 130 colors/maroon colors/gold)
  (rsb/draw-rectangle-lines! (- (* (quot screen-width 4) 2) 40) 320 80 60 colors/orange)

  ;; Triangle shapes and lines
  (let [cx (* (/ screen-width 4.0) 3.0)]
    (rsb/draw-triangle! {:x cx :y 80.0}
                         {:x (- cx 60.0) :y 150.0}
                         {:x (+ cx 60.0) :y 150.0}
                         colors/violet)
    (rsb/draw-triangle-lines! {:x cx :y 160.0}
                               {:x (- cx 20.0) :y 230.0}
                               {:x (+ cx 20.0) :y 230.0}
                               colors/darkblue))

  ;; Polygon shapes and lines
  (let [center {:x (* (/ screen-width 4.0) 3) :y 330.0}]
    (rsb/draw-poly! center 6 (float 80) (float rotation) colors/brown)
    (rsb/draw-poly-lines! center 6 (float 90) (float rotation) colors/brown)
    (rsb/draw-poly-lines-ex! center 6 (float 85) (float rotation) (float 6) colors/beige))

  ;; Line
  (rsb/draw-line! 18 42 (- screen-width 18) 42 colors/black)

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
