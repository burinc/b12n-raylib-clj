(ns examples.ring-drawing
  "raylib [shapes] example - ring drawing

   A ring with its inner and outer radius, start and end angle, and segment
   count on sliders, plus three checkboxes selecting which of the three
   drawing calls to show at once - filled ring, ring outline, and the
   circle-sector outline for comparison.

   The MODE readout works the same way as in `circle-sector-drawing`:
   raylib substitutes its own segment count when you ask for fewer than the
   arc needs, and AUTO is reporting that substitution.

   Angles here run -450 to 450, so unlike the sector example the arc can be
   negative. `min-segments` uses ceil alone, matching the C, which means a
   backwards arc yields a negative threshold that any segment count clears.

   Difficulty: 2/4
   Based on: shapes/shapes_ring_drawing.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.raygui :as gui]
   [raylib.utils :as ru]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(defn initial-state []
  {:inner-radius 80.0 :outer-radius 190.0
   :start-angle 0.0 :end-angle 360.0 :segments 0.0
   :ring? true :ring-lines? false :circle-lines? false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - ring drawing")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn min-segments
  "One segment per 90 degrees of arc, rounded up. No truncation toward zero
   here - the C uses a bare ceilf - so a backwards arc gives a negative
   threshold, which every segment count satisfies."
  [start-angle end-angle]
  (long (Math/ceil (/ (- end-angle start-angle) 90.0))))

(defn centre []
  {:x (/ (- (rcw/get-screen-width) 300) 2.0)
   :y (/ (rcw/get-screen-height) 2.0)})

(defn tick [state]
  (debug-stats/update!)
  state)

(defn draw [{:keys [inner-radius outer-radius start-angle end-angle segments
                    ring? ring-lines? circle-lines?] :as state}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rsb/draw-line! 500 0 500 (rcw/get-screen-height) (ru/fade colors/lightgray 0.6))
  (rsb/draw-rectangle! 500 0 (- (rcw/get-screen-width) 500) (rcw/get-screen-height)
                       (ru/fade colors/lightgray 0.3))

  (let [c (centre)
        inner (float inner-radius) outer (float outer-radius)
        from (float start-angle) to (float end-angle)
        seg (int segments)]
    (when ring?
      (rsb/draw-ring! c inner outer from to seg (ru/fade colors/maroon 0.3)))
    (when ring-lines?
      (rsb/draw-ring-lines! c inner outer from to seg (ru/fade colors/black 0.4)))
    (when circle-lines?
      (rsb/draw-circle-sector-lines! c outer from to seg (ru/fade colors/black 0.4))))

  (let [bar (fn [y label v mn mx]
              (gui/slider-bar {:x 600.0 :y (double y) :width 120.0 :height 20.0}
                              label (format "%.2f" v) v mn mx))
        cb (fn [y label v]
             (gui/check-box {:x 600.0 :y (double y) :width 20.0 :height 20.0} label v))
        start-angle (bar 40 "StartAngle" start-angle -450.0 450.0)
        end-angle (bar 70 "EndAngle" end-angle -450.0 450.0)
        inner-radius (bar 140 "InnerRadius" inner-radius 0.0 100.0)
        outer-radius (bar 170 "OuterRadius" outer-radius 0.0 200.0)
        segments (bar 240 "Segments" segments 0.0 100.0)
        ring? (cb 320 "Draw Ring" ring?)
        ring-lines? (cb 350 "Draw RingLines" ring-lines?)
        circle-lines? (cb 380 "Draw CircleLines" circle-lines?)
        manual? (>= segments (min-segments start-angle end-angle))]
    (rtd/draw-text! (str "MODE: " (if manual? "MANUAL" "AUTO")) 600 270 10
                    (if manual? colors/maroon colors/darkgray))
    (rtd/draw-fps! 10 10)
    (debug-stats/draw!)
    (rcd/end-drawing!)
    (assoc state
           :start-angle start-angle :end-angle end-angle
           :inner-radius inner-radius :outer-radius outer-radius :segments segments
           :ring? ring? :ring-lines? ring-lines? :circle-lines? circle-lines?)))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (swap! game-atom (comp draw tick))
      (recur)))
  (rcw/close-window!))
