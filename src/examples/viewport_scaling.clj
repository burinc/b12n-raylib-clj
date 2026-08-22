(ns examples.viewport-scaling
  "raylib [core] example - viewport scaling

   Six ways to fit a fixed-resolution game onto a resizable window. The game
   is rendered to a small render texture (64x64 up to 3840x2160) and then
   blitted to the window; the six viewport types differ in how the source
   and destination rectangles are computed. Resize the window and the
   readout updates live.

   The three INTEGER variants use integer division for the scale factor, so
   they only ever upscale by a whole number - the right choice for pixel art,
   where a fractional scale produces uneven pixels. The other three accept a
   fractional ratio and can also downscale, which is why 3840x2160 is in the
   resolution list: it cannot scale integrally into an 800x450 window at all.

   Worth knowing before comparing against the C: it exposes six named modes
   but only four distinct behaviours. KEEP_HEIGHT_INTEGER and KEEP_HEIGHT
   compute byte-identical rectangles, as do KEEP_WIDTH_INTEGER and
   KEEP_WIDTH - both members of each pair divide a float by an int, so the
   extra casts in the _INTEGER variants change nothing. Only KEEP_ASPECT
   differs from KEEP_ASPECT_INTEGER, because there the C divides int by int
   and gets truncation. Verified by transcribing both C functions literally
   and comparing across 100 window/game size combinations: zero differed.
   All six stay selectable here so the UI matches upstream.

   The source rectangle carries a NEGATIVE height throughout. That is not a
   quirk of this example - OpenGL render textures are stored bottom-up, so
   flipping the source rect is the standard way to blit one the right way up.

   Difficulty: 2/4
   Based on: core/core_viewport_scaling.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.mouse :as rcm]
   [raylib.core.collision :as rcol]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.textures.texture-loading :as rtl]
   [raylib.utils :as ru]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def resolutions [[64 64] [256 240] [320 180] [3840 2160]])

;; Order matters: the < and > buttons step through this vector, and the
;; first three are the integer-only variants.
(def viewport-types
  [:keep-aspect-integer :keep-height-integer :keep-width-integer
   :keep-aspect :keep-height :keep-width])

(defn- trunc
  "C's (float)(int)x - truncate toward zero, then keep it a float.
   Present in every branch below because the destination rect must land on
   whole pixels or the blit shows seams."
  [x]
  (float (long x)))

;; Each of the six returns {:source rect :dest rect}. They are pure: given
;; the window and game dimensions they compute rectangles and touch nothing
;; else, which is what makes them straightforward to test.

(defn keep-aspect-centered
  "Fit both axes, letterboxing whichever one has slack. `ratio-fn` decides
   whether the scale factor is truncated to an integer (pixel-art safe) or
   left fractional (also allows downscaling)."
  [sw sh gw gh ratio-fn]
  (let [r (min (ratio-fn sw gw) (ratio-fn sh gh))]
    {:source {:x 0.0 :y (float gh) :width (float gw) :height (float (- gh))}
     :dest {:x (trunc (* 0.5 (- sw (* gw r)))) :y (trunc (* 0.5 (- sh (* gh r))))
            :width (trunc (* gw r)) :height (trunc (* gh r))}}))

(defn keep-height-centered
  "Lock the vertical scale to the window height and widen the source to
   whatever the window is - so a wider window shows more of the world
   rather than bigger pixels."
  [sw sh _gw gh]
  (let [r (/ (double sh) gh)
        src-w (trunc (/ sw r))]
    {:source {:x 0.0 :y 0.0 :width src-w :height (float (- gh))}
     :dest {:x (trunc (* 0.5 (- sw (* src-w r)))) :y (trunc (* 0.5 (- sh (* gh r))))
            :width (trunc (* src-w r)) :height (trunc (* gh r))}}))

(defn keep-width-centered
  "The transpose of keep-height-centered: lock the horizontal scale and let
   a taller window reveal more vertically."
  [sw sh gw _gh]
  (let [r (/ (double sw) gw)
        src-h (trunc (/ sh r))]
    {:source {:x 0.0 :y 0.0 :width (float gw) :height (float (- src-h))}
     :dest {:x (trunc (* 0.5 (- sw (* gw r)))) :y (trunc (* 0.5 (- sh (* src-h r))))
            :width (trunc (* gw r)) :height (trunc (* src-h r))}}))

(def ^:private int-ratio (fn [a b] (double (quot a b))))
(def ^:private flt-ratio (fn [a b] (/ (double a) b)))

(defn rects-for
  "Dispatch on viewport type, returning {:source rect :dest rect}."
  [viewport-type sw sh gw gh]
  (let [[f ratio-fn]
        (case viewport-type
          :keep-aspect-integer [keep-aspect-centered int-ratio]
          :keep-height-integer [keep-height-centered nil]
          :keep-width-integer [keep-width-centered nil]
          :keep-aspect [keep-aspect-centered flt-ratio]
          :keep-height [keep-height-centered nil]
          :keep-width [keep-width-centered nil])]
    (select-keys (if ratio-fn (f sw sh gw gh ratio-fn) (f sw sh gw gh))
                 [:source :dest])))

(defn screen->render-texture
  "Map a window-space point into render-texture space.

   The C computes a y ratio here but multiplies both axes by the x ratio.
   That is not a bug: in all six modes the source and destination scale by
   the same factor, so the two ratios are equal. Kept as one ratio to make
   that explicit rather than implying an axis-independent transform."
  [{:keys [x y]} source dest]
  (let [ratio (/ (:width source) (:width dest))]
    {:x (* (- x (:x dest)) ratio)
     :y (* (- y (:y dest)) ratio)}))

(def buttons
  {:resolution-down {:x 200.0 :y 30.0 :width 10.0 :height 10.0}
   :resolution-up {:x 215.0 :y 30.0 :width 10.0 :height 10.0}
   :type-down {:x 200.0 :y 45.0 :width 10.0 :height 10.0}
   :type-up {:x 215.0 :y 45.0 :width 10.0 :height 10.0}})

(defn initial-state []
  {:resolution-index 0 :type-index 0
   :screen-width screen-width :screen-height screen-height
   :source nil :dest nil :target nil})

(def game-atom (atom (initial-state)))

(defn resize!
  "Recompute the rectangles for the current window and reallocate the render
   texture to match. The C reallocates on every change too - the source rect
   IS the texture size, so a resize is a new texture, not a rescale."
  [{:keys [resolution-index type-index target] :as state}]
  (let [sw (rcw/get-screen-width) sh (rcw/get-screen-height)
        [gw gh] (nth resolutions resolution-index)
        {:keys [source dest]} (rects-for (nth viewport-types type-index) sw sh gw gh)]
    (when target (rtl/unload-render-texture! target))
    (assoc state
           :screen-width sw :screen-height sh
           :source source :dest dest
           :target (rtl/load-render-texture! (int (:width source))
                                             (int (- (:height source)))))))

(defn init []
  (rcw/set-config-flags! (:flag/window-resizable rcw/config-flag))
  (rcw/init-window! screen-width screen-height "raylib [core] example - viewport scaling")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  (swap! game-atom resize!))

(defn- clicked? [mouse rect pressed?]
  (and pressed? (pos? (rcol/check-collision-point-rec? mouse rect))))

(defn tick [state]
  (debug-stats/update!)
  (let [mouse (rcm/get-mouse-position)
        pressed? (rcm/is-mouse-button-pressed? (:left enums/mouse-button))
        hit (fn [k] (clicked? mouse (buttons k) pressed?))
        step (fn [i n d] (mod (+ i d) n))
        state (cond-> state
                (hit :resolution-down) (update :resolution-index step (count resolutions) -1)
                (hit :resolution-up) (update :resolution-index step (count resolutions) 1)
                (hit :type-down) (update :type-index step (count viewport-types) -1)
                (hit :type-up) (update :type-index step (count viewport-types) 1))
        changed? (or (rcw/is-window-resized?)
                     (hit :resolution-down) (hit :resolution-up)
                     (hit :type-down) (hit :type-up))]
    (assoc (if changed? (resize! state) state) :mouse mouse)))

(defn- draw-info [{:keys [screen-width screen-height resolution-index type-index source dest]}]
  (let [[gw gh] (nth resolutions resolution-index)
        rect {:x 5.0 :y 5.0 :width 330.0 :height 105.0}
        scale-x (/ (:width dest) (:width source))
        scale-y (/ (- (:height dest)) (:height source))]
    (rsb/draw-rectangle-rec! rect (ru/fade colors/lightgray 0.7))
    (rsb/draw-rectangle-lines-ex! rect 1.0 colors/blue)
    (rtd/draw-text! (format "Window Resolution: %d x %d" screen-width screen-height) 15 15 10 colors/black)
    (rtd/draw-text! (format "Game Resolution: %d x %d" gw gh) 15 30 10 colors/black)
    (rtd/draw-text! (format "Type: %s" (name (nth viewport-types type-index))) 15 45 10 colors/black)
    (rtd/draw-text! (if (or (< scale-x 0.001) (< scale-y 0.001))
                      "Scale ratio: INVALID"
                      (format "Scale ratio: %.2f x %.2f" scale-x scale-y))
                    15 60 10 colors/black)
    (rtd/draw-text! (format "Source size: %.2f x %.2f" (:width source) (- (:height source))) 15 75 10 colors/black)
    (rtd/draw-text! (format "Destination size: %.2f x %.2f" (:width dest) (:height dest)) 15 90 10 colors/black)
    (doseq [[k label] [[:type-down "<"] [:type-up ">"] [:resolution-down "<"] [:resolution-up ">"]]
            :let [b (buttons k)]]
      (rsb/draw-rectangle-rec! b colors/skyblue)
      (rtd/draw-text! label (+ (int (:x b)) 3) (+ (int (:y b)) 1) 10 colors/black))))

(defn draw [{:keys [target source dest mouse] :as state}]
  ;; The scene itself: one circle following the mouse, rendered at the game's
  ;; own resolution so the scaling is visible in how chunky the circle looks.
  (rtl/begin-texture-mode! target)
  (rcd/clear-background! colors/white)
  (let [{:keys [x y]} (screen->render-texture (or mouse {:x 0.0 :y 0.0}) source dest)]
    (rsb/draw-circle-v! {:x (float x) :y (float y)} 20.0 colors/lime))
  (rtl/end-texture-mode!)

  (rcd/begin-drawing!)
  (rcd/clear-background! colors/black)
  (rtl/draw-texture-pro! (:texture target) source dest {:x 0.0 :y 0.0} 0.0 colors/white)
  (draw-info state)
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
  (when-let [t (:target @game-atom)] (rtl/unload-render-texture! t))
  (rcw/close-window!))
