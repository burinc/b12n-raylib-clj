(ns examples.circle-sector-drawing
  "raylib [shapes] example - circle sector drawing

   A circle sector with its start angle, end angle, radius and segment count
   on sliders, showing what each does to the shape. Below a certain segment
   count raylib picks the count itself, which is what the MODE readout is
   reporting: MANUAL once you ask for at least as many segments as the arc
   needs, AUTO while you ask for fewer.

   First example here built on `raylib.raygui`, the Clojure port of the
   raygui controls. raygui is header-only C compiled into whatever includes
   it, so the bundled libraylib exports no `Gui*` symbols and there was
   nothing to bind - see that namespace for why the controls return values
   instead of writing through pointers.

   Difficulty: 2/4
   Based on: shapes/shapes_circle_sector_drawing.c"
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
  {:outer-radius 180.0 :start-angle 0.0 :end-angle 180.0 :segments 10.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - circle sector drawing")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn min-segments
  "The fewest segments that still describe the arc: one per 90 degrees,
   rounded up. Below this raylib substitutes its own count, which is the
   AUTO case in the readout."
  [start-angle end-angle]
  (Math/floor (Math/ceil (/ (- end-angle start-angle) 90.0))))

(defn centre []
  {:x (/ (- (rcw/get-screen-width) 300) 2.0)
   :y (/ (rcw/get-screen-height) 2.0)})

(defn tick [state]
  (debug-stats/update!)
  state)

(defn draw [{:keys [outer-radius start-angle end-angle segments] :as state}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; The panel on the right is just a tinted rectangle and a divider; the
  ;; sliders sit on top of it.
  (rsb/draw-line! 500 0 500 (rcw/get-screen-height) (ru/fade colors/lightgray 0.6))
  (rsb/draw-rectangle! 500 0 (- (rcw/get-screen-width) 500) (rcw/get-screen-height)
                       (ru/fade colors/lightgray 0.3))

  (let [c (centre)]
    (rsb/draw-circle-sector! c (float outer-radius) (float start-angle) (float end-angle)
                             (int segments) (ru/fade colors/maroon 0.3))
    (rsb/draw-circle-sector-lines! c (float outer-radius) (float start-angle) (float end-angle)
                                   (int segments) (ru/fade colors/maroon 0.6)))

  ;; Each slider returns its new value, so the frame's draw pass is also
  ;; what produces the next state. That is immediate mode: there is no
  ;; separate update step for these.
  (let [start-angle (gui/slider-bar {:x 600.0 :y 40.0 :width 120.0 :height 20.0}
                                    "StartAngle" (format "%.2f" start-angle)
                                    start-angle 0.0 720.0)
        end-angle (gui/slider-bar {:x 600.0 :y 70.0 :width 120.0 :height 20.0}
                                  "EndAngle" (format "%.2f" end-angle)
                                  end-angle 0.0 720.0)
        outer-radius (gui/slider-bar {:x 600.0 :y 140.0 :width 120.0 :height 20.0}
                                     "Radius" (format "%.2f" outer-radius)
                                     outer-radius 0.0 200.0)
        segments (gui/slider-bar {:x 600.0 :y 170.0 :width 120.0 :height 20.0}
                                 "Segments" (format "%.2f" segments)
                                 segments 0.0 100.0)
        manual? (>= segments (min-segments start-angle end-angle))]
    (rtd/draw-text! (str "MODE: " (if manual? "MANUAL" "AUTO")) 600 200 10
                    (if manual? colors/maroon colors/darkgray))
    (rtd/draw-fps! 10 10)
    (debug-stats/draw!)
    (rcd/end-drawing!)
    (assoc state :start-angle start-angle :end-angle end-angle
           :outer-radius outer-radius :segments segments)))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (swap! game-atom (comp draw tick))
      (recur)))
  (rcw/close-window!))
