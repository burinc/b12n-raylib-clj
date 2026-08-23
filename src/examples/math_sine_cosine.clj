(ns examples.math-sine-cosine
  "raylib [shapes] example - math sine cosine

   The unit circle, animated. A point travels the circle while its sine,
   cosine, tangent and cotangent are drawn as lines against it, the related
   angles (complementary, supplementary, explementary) as arcs, and the sine
   and cosine waves as graphs in the corner with a marker tracking the
   current angle.

   Two things it needs that the bundled raylib does not provide.
   `DrawLineDashed` is a raylib 6.x addition absent from the 5.5.0 library
   here, so the dashed guides go through the Clojure stand-in in
   `raylib.shapes.basic`. `GuiToggle` and `GuiGroupBox` come from
   `raylib.raygui`.

   Note the arcs are drawn with negative angles - raylib measures the sector
   clockwise from the positive x-axis while the point is placed counter-
   clockwise, so the arc has to be negated to sit under the point.

   Difficulty: 3/4
   Based on: shapes/shapes_math_sine_cosine.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.raygui :as gui]
   [raylib.raymath :as rm]
   [raylib.utils :as ru]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def wave-points 36)
(def radius 130.0)
(def centre {:x (- (/ screen-width 2.0) 30.0) :y (/ screen-height 2.0)})
(def graph {:x 20.0 :y (- screen-height 120.0) :width 200.0 :height 100.0})

(defn wave
  "One period of `f` sampled across the graph box, as spline points."
  [f]
  (mapv (fn [i]
          (let [t (/ (double i) (dec wave-points))
                a (Math/toRadians (* t 360.0))]
            {:x (+ (:x graph) (* t (:width graph)))
             :y (- (+ (:y graph) (/ (:height graph) 2.0))
                   (* (f a) (/ (:height graph) 2.0)))}))
        (range wave-points)))

(def sine-points (wave #(Math/sin %)))
(def cos-points (wave #(Math/cos %)))

(defn trig-state
  "Everything derived from the current angle. Pure, so the geometry is
   checkable without a window."
  [angle]
  (let [rad (Math/toRadians angle)
        cos-r (Math/cos rad) sin-r (Math/sin rad)
        ;; Tangent runs to infinity at 90 and 270 degrees, so it is clamped
        ;; to keep the drawn line on screen; cotangent is guarded against
        ;; the reciprocal blowing up in the same way.
        tangent (rm/clamp (Math/tan rad) -10.0 10.0)
        cotangent (if (> (Math/abs tangent) 0.001)
                    (rm/clamp (/ 1.0 tangent) (- radius) radius)
                    0.0)]
    {:rad rad :cos cos-r :sin sin-r :tangent tangent :cotangent cotangent
     :point {:x (+ (:x centre) (* cos-r radius)) :y (- (:y centre) (* sin-r radius))}
     :limit-min {:x (- (:x centre) radius) :y (- (:y centre) radius)}
     :limit-max {:x (+ (:x centre) radius) :y (+ (:y centre) radius)}
     :tangent-point {:x (+ (:x centre) radius) :y (- (:y centre) (* tangent radius))}
     :cotangent-point {:x (+ (:x centre) (* cotangent radius)) :y (- (:y centre) radius)}
     :complementary (- 90.0 angle)
     :supplementary (- 180.0 angle)
     :explementary (- 360.0 angle)}))

(defn initial-state [] {:angle 0.0 :pause? false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/set-config-flags! (:flag/msaa-4x-hint rcw/config-flag))
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - math sine cosine")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [angle pause?] :as state}]
  (debug-stats/update!)
  (assoc state :angle (rm/wrap (+ angle (if pause? 0.0 1.0)) 0.0 360.0)))

(defn- v2 [x y] {:x (float x) :y (float y)})

(defn draw [{:keys [angle pause?] :as state}]
  (let [{:keys [cos sin tangent cotangent point limit-min limit-max
                tangent-point cotangent-point
                complementary supplementary explementary]} (trig-state angle)
        gx (:x graph) gy (:y graph) gw (:width graph) gh (:height graph)]
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/raywhite)

    ;; Cotangent (orange), drawn first so the axes sit over it.
    (rsb/draw-line-ex! (v2 (:x centre) (:y limit-min))
                       (v2 (:x cotangent-point) (:y limit-min)) 2.0 colors/orange)
    (rsb/draw-dashed-line! centre cotangent-point 10 4 colors/orange)

    (rsb/draw-line! 580 0 580 (rcw/get-screen-height) {:r 218 :g 218 :b 218 :a 255})
    (rsb/draw-rectangle! 580 0 (rcw/get-screen-width) (rcw/get-screen-height)
                         {:r 232 :g 232 :b 232 :a 255})

    (rsb/draw-circle-lines-v! (v2 (:x centre) (:y centre)) (float radius) colors/gray)
    (rsb/draw-line-ex! (v2 (:x centre) (:y limit-min)) (v2 (:x centre) (:y limit-max)) 1.0 colors/gray)
    (rsb/draw-line-ex! (v2 (:x limit-min) (:y centre)) (v2 (:x limit-max) (:y centre)) 1.0 colors/gray)

    ;; Wave graph frame and labels.
    (rsb/draw-line-ex! (v2 gx gy) (v2 gx (+ gy gh)) 2.0 colors/gray)
    (rsb/draw-line-ex! (v2 (+ gx gw) gy) (v2 (+ gx gw) (+ gy gh)) 2.0 colors/gray)
    (rsb/draw-line-ex! (v2 gx (+ gy (/ gh 2))) (v2 (+ gx gw) (+ gy (/ gh 2))) 2.0 colors/gray)
    (rtd/draw-text! "1" (- (int gx) 8) (int gy) 6 colors/gray)
    (rtd/draw-text! "0" (- (int gx) 8) (- (+ (int gy) (int (/ gh 2))) 6) 6 colors/gray)
    (rtd/draw-text! "-1" (- (int gx) 12) (- (+ (int gy) (int gh)) 8) 6 colors/gray)
    (rtd/draw-text! "0" (- (int gx) 2) (+ (int gy) (int gh) 4) 6 colors/gray)
    (rtd/draw-text! "360" (- (+ (int gx) (int gw)) 8) (+ (int gy) (int gh) 4) 6 colors/gray)

    ;; Sine (red, vertical) and its wave.
    (rsb/draw-line-ex! (v2 (:x centre) (:y centre)) (v2 (:x centre) (:y point)) 2.0 colors/red)
    (rsb/draw-dashed-line! {:x (:x point) :y (:y centre)} point 10 4 colors/red)
    (rtd/draw-text! (format "Sine %.2f" sin) 640 190 6 colors/red)
    (rsb/draw-circle-v! (v2 (+ gx (* (/ angle 360.0) gw))
                            (+ gy (* (+ (- sin) 1) (/ gh 2.0)))) 4.0 colors/red)
    (rsb/draw-spline-linear! sine-points 1.0 colors/red)

    ;; Cosine (blue, horizontal) and its wave.
    (rsb/draw-line-ex! (v2 (:x centre) (:y centre)) (v2 (:x point) (:y centre)) 2.0 colors/blue)
    (rsb/draw-dashed-line! {:x (:x centre) :y (:y point)} point 10 4 colors/blue)
    (rtd/draw-text! (format "Cosine %.2f" cos) 640 210 6 colors/blue)
    (rsb/draw-circle-v! (v2 (+ gx (* (/ angle 360.0) gw))
                            (+ gy (* (+ (- cos) 1) (/ gh 2.0)))) 4.0 colors/blue)
    (rsb/draw-spline-linear! cos-points 1.0 colors/blue)

    ;; Tangent (purple) and the cotangent readout.
    (rsb/draw-line-ex! (v2 (:x limit-max) (:y centre))
                       (v2 (:x limit-max) (:y tangent-point)) 2.0 colors/purple)
    (rsb/draw-dashed-line! centre tangent-point 10 4 colors/purple)
    (rtd/draw-text! (format "Tangent %.2f" tangent) 640 230 6 colors/purple)
    (rtd/draw-text! (format "Cotangent %.2f" cotangent) 640 250 6 colors/orange)

    ;; Related angles, as arcs of decreasing radius.
    (doseq [[r to color label y] [[0.6 -90.0 colors/beige (format "Complementary  %.0f" complementary) 150]
                                  [0.5 -180.0 colors/darkblue (format "Supplementary  %.0f" supplementary) 130]
                                  [0.4 -360.0 colors/pink (format "Explementary  %.0f" explementary) 170]]]
      (rsb/draw-circle-sector-lines! (v2 (:x centre) (:y centre)) (float (* radius r))
                                     (float (- angle)) (float to) 36 color)
      (rtd/draw-text! label 640 y 6 color))

    (rsb/draw-circle-sector-lines! (v2 (:x centre) (:y centre)) (float (* radius 0.7))
                                   (float (- angle)) 0.0 36 colors/lime)
    (rsb/draw-line-ex! (v2 (:x centre) (:y centre)) (v2 (:x point) (:y point)) 2.0 colors/black)
    (rsb/draw-circle-v! (v2 (:x point) (:y point)) 4.0 colors/black)

    (gui/set-style! :label :text-normal (ru/color-to-int colors/gray))
    (let [pause? (gui/toggle {:x 640.0 :y 70.0 :width 120.0 :height 20.0} "Pause" pause?)]
      (gui/set-style! :label :text-normal (ru/color-to-int colors/lime))
      (let [angle (gui/slider-bar {:x 640.0 :y 40.0 :width 120.0 :height 20.0}
                                  "Angle" (format "%.0f" angle) angle 0.0 360.0)]
        (gui/group-box {:x 620.0 :y 110.0 :width 140.0 :height 170.0} "Angle Values")
        (rtd/draw-fps! 10 10)
        (debug-stats/draw!)
        (rcd/end-drawing!)
        (assoc state :angle angle :pause? pause?)))))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (swap! game-atom (comp draw tick))
      (recur)))
  (rcw/close-window!))
