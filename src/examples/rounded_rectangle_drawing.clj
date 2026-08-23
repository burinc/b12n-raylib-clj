(ns examples.rounded-rectangle-drawing
  "raylib [shapes] example - rounded rectangle drawing

   A rounded rectangle with its size, corner roundness, outline thickness
   and segment count on sliders, and checkboxes to overlay the plain
   rectangle and the rounded outline for comparison.

   The MODE readout is a fixed threshold here rather than one derived from
   the shape: raylib substitutes its own corner segment count below 4, so
   the C compares against a literal 4 instead of computing a minimum the way
   the ring and sector examples do.

   Difficulty: 2/4
   Based on: shapes/shapes_rounded_rectangle_drawing.c"
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

(def min-manual-segments
  "raylib picks its own corner segment count below this."
  4)

(defn initial-state []
  {:roundness 0.2 :width 200.0 :height 100.0 :segments 0.0 :line-thick 1.0
   :rect? false :rounded-rect? true :rounded-lines? false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height
                    "raylib [shapes] example - rounded rectangle drawing")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn shape-rect
  "The rectangle under test, centred in the area left of the control panel.
   The 250 offset keeps it clear of the panel rather than of the divider,
   so it stays centred in the visible space as the width slider moves."
  [screen-w screen-h width height]
  {:x (/ (- screen-w width 250) 2.0)
   :y (/ (- screen-h height) 2.0)
   :width (float width) :height (float height)})

(defn tick [state]
  (debug-stats/update!)
  state)

(defn draw [{:keys [roundness width height segments line-thick
                    rect? rounded-rect? rounded-lines?] :as state}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rsb/draw-line! 560 0 560 (rcw/get-screen-height) (ru/fade colors/lightgray 0.6))
  (rsb/draw-rectangle! 560 0 (- (rcw/get-screen-width) 500) (rcw/get-screen-height)
                       (ru/fade colors/lightgray 0.3))

  (let [rec (shape-rect (rcw/get-screen-width) (rcw/get-screen-height) width height)
        seg (int segments)]
    (when rect?
      (rsb/draw-rectangle-rec! rec (ru/fade colors/gold 0.6)))
    (when rounded-rect?
      (rsb/draw-rectangle-rounded! rec (float roundness) seg (ru/fade colors/maroon 0.2)))
    (when rounded-lines?
      (rsb/draw-rectangle-rounded-lines-ex! rec (float roundness) seg (float line-thick)
                                            (ru/fade colors/maroon 0.4))))

  (let [bar (fn [y label v mn mx]
              (gui/slider-bar {:x 640.0 :y (double y) :width 105.0 :height 20.0}
                              label (format "%.2f" v) v mn mx))
        cb (fn [y label v]
             (gui/check-box {:x 640.0 :y (double y) :width 20.0 :height 20.0} label v))
        width (bar 40 "Width" width 0.0 (double (- (rcw/get-screen-width) 300)))
        height (bar 70 "Height" height 0.0 (double (- (rcw/get-screen-height) 50)))
        roundness (bar 140 "Roundness" roundness 0.0 1.0)
        line-thick (bar 170 "Thickness" line-thick 0.0 20.0)
        segments (bar 240 "Segments" segments 0.0 60.0)
        rounded-rect? (cb 320 "DrawRoundedRect" rounded-rect?)
        rounded-lines? (cb 350 "DrawRoundedLines" rounded-lines?)
        rect? (cb 380 "DrawRect" rect?)
        manual? (>= segments min-manual-segments)]
    (rtd/draw-text! (str "MODE: " (if manual? "MANUAL" "AUTO")) 640 280 10
                    (if manual? colors/maroon colors/darkgray))
    (rtd/draw-fps! 10 10)
    (debug-stats/draw!)
    (rcd/end-drawing!)
    (assoc state :width width :height height :roundness roundness
           :line-thick line-thick :segments segments
           :rect? rect? :rounded-rect? rounded-rect? :rounded-lines? rounded-lines?)))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (swap! game-atom (comp draw tick))
      (recur)))
  (rcw/close-window!))
