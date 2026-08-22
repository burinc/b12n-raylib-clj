(ns examples.ellipse-collision
  "raylib [shapes] example - ellipse collision

   Two ellipses, one following the mouse. Press A or B to choose which one
   you steer; both turn red while they overlap.

   The collision test is example-local logic, not a raylib call: the C
   source defines CheckCollisionEllipses and CheckCollisionPointEllipse as
   static helpers, so they are written here in Clojure rather than bound.

   Difficulty: 2/4
   Based on: shapes/shapes_ellipse_collision.c"
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
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def ellipse-a {:rx 120.0 :ry 70.0})
(def ellipse-b {:rx 90.0 :ry 140.0})

(defn- boundary-radius
  "Distance from an ellipse's centre to its edge along `theta`.

   r(theta) = (rx*ry) / sqrt((ry*cos)^2 + (rx*sin)^2)"
  [rx ry cos-t sin-t]
  (/ (* rx ry)
     (Math/sqrt (+ (* (* ry cos-t) (* ry cos-t))
                   (* (* rx sin-t) (* rx sin-t))))))

(defn ellipses-collide?
  "Compare centre distance against the two boundary radii along the line
   joining the centres. Scales correctly with differing radii, unlike a
   bounding-circle test."
  [c1 {rx1 :rx ry1 :ry} c2 {rx2 :rx ry2 :ry}]
  (let [dx (- (:x c2) (:x c1))
        dy (- (:y c2) (:y c1))
        dist (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (if (zero? dist)
      true                                   ; concentric: always overlapping
      (let [theta (Math/atan2 dy dx)
            cos-t (Math/cos theta)
            sin-t (Math/sin theta)]
        (<= dist (+ (boundary-radius rx1 ry1 cos-t sin-t)
                    (boundary-radius rx2 ry2 cos-t sin-t)))))))

(defn point-in-ellipse?
  "Normalise the point into unit-circle space, then test against 1."
  [{px :x py :y} {cx :x cy :y} {:keys [rx ry]}]
  (let [nx (/ (- px cx) rx)
        ny (/ (- py cy) ry)]
    (<= (+ (* nx nx) (* ny ny)) 1.0)))

(defn initial-state []
  {:a {:x (/ screen-width 4.0) :y (/ screen-height 2.0)}
   :b {:x (* screen-width 0.75) :y (/ screen-height 2.0)}
   :controlled :a})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height
                    "raylib [shapes] example - ellipse collision")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [state]
  (debug-stats/update!)
  (let [controlled (cond
                     (rck/is-key-pressed? (:a enums/keyboard-key)) :a
                     (rck/is-key-pressed? (:b enums/keyboard-key)) :b
                     :else (:controlled state))]
    (assoc state
           :controlled controlled
           controlled (rcm/get-mouse-position))))

(defn draw [{:keys [a b controlled]}]
  (let [collide? (ellipses-collide? a ellipse-a b ellipse-b)
        mouse (rcm/get-mouse-position)
        fill-a (if collide? colors/red colors/blue)
        fill-b (if collide? colors/red colors/green)]
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/raywhite)

    (rsb/draw-ellipse! (int (:x a)) (int (:y a))
                       (float (:rx ellipse-a)) (float (:ry ellipse-a)) fill-a)
    (rsb/draw-ellipse! (int (:x b)) (int (:y b))
                       (float (:rx ellipse-b)) (float (:ry ellipse-b)) fill-b)
    (rsb/draw-ellipse-lines! (int (:x a)) (int (:y a))
                             (float (:rx ellipse-a)) (float (:ry ellipse-a)) colors/white)
    (rsb/draw-ellipse-lines! (int (:x b)) (int (:y b))
                             (float (:rx ellipse-b)) (float (:ry ellipse-b)) colors/white)
    (rsb/draw-circle-v! a (float 4.0) colors/white)
    (rsb/draw-circle-v! b (float 4.0) colors/white)

    (if collide?
      (rtd/draw-text! "ELLIPSES COLLIDE" (- (/ screen-width 2) 120) 40 28 colors/red)
      (rtd/draw-text! "NO COLLISION" (- (/ screen-width 2) 80) 40 28 colors/darkgray))

    (rtd/draw-text! (str "Controlling: " (if (= controlled :a) "A" "B"))
                    20 (- screen-height 40) 20 colors/yellow)

    ;; Only report the ellipse you are NOT steering - the steered one always
    ;; contains the cursor, so saying so every frame is noise.
    (when (and (not= controlled :a) (point-in-ellipse? mouse a ellipse-a))
      (rtd/draw-text! "Mouse inside ellipse A" 20 (- screen-height 70) 20 colors/blue))
    (when (and (not= controlled :b) (point-in-ellipse? mouse b ellipse-b))
      (rtd/draw-text! "Mouse inside ellipse B" 20 (- screen-height 70) 20 colors/green))

    (rtd/draw-text! "Press [A] or [B] to switch control" 20 20 20 colors/gray)
    (debug-stats/draw!)
    (rcd/end-drawing!)))

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
