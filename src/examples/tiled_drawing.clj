(ns examples.tiled-drawing
  "raylib [textures] example - tiled drawing

   Tiles one patch of a texture atlas across the window, with the pattern,
   tint, scale and rotation all selectable. Resize the window and the tiling
   re-fits.

   The interesting part is `tile-rects`, a Clojure version of the example's
   own `DrawTextureTiled` helper - which is example code in the C, not raylib
   API. It has four cases: the tile is bigger than the area in both axes (draw
   one, cropped), bigger in one axis (a single column or row), or smaller in
   both (a grid). The edges of each case draw a partial tile, cropping the
   SOURCE proportionally so the pattern is truncated rather than squashed.

   Written here as a pure function returning the (source, dest) pairs rather
   than as nested draw loops, so the geometry can be checked without a window
   - which matters, because the C's own comment on the guard at the top of
   that function is \"Wanna see a infinite loop?!...just delete this line!\".

   Difficulty: 3/4
   Based on: textures/textures_tiled_drawing.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.core.collision :as rcol]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.textures.drawing :as rtdw]
   [raylib.textures.texture-loading :as rtl]
   [raylib.utils :as ru]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def opt-width 220)
(def margin 8)
(def color-size 16)

;; Where each pattern sits inside patterns.png.
(def pattern-rects
  [{:x 3.0 :y 3.0 :width 66.0 :height 66.0}
   {:x 75.0 :y 3.0 :width 100.0 :height 100.0}
   {:x 3.0 :y 75.0 :width 66.0 :height 66.0}
   {:x 7.0 :y 156.0 :width 50.0 :height 50.0}
   {:x 85.0 :y 106.0 :width 90.0 :height 45.0}
   {:x 75.0 :y 154.0 :width 100.0 :height 60.0}])

(def palette
  [colors/black colors/maroon colors/orange colors/blue colors/purple
   colors/beige colors/lime colors/red colors/darkgray colors/skyblue])

(def color-rects
  "Swatches in two rows of five. The C builds these with a running x/y and a
   mid-loop reset; the row/column split is explicit here."
  (vec (for [i (range (count palette))
             :let [row (quot i (quot (count palette) 2))
                   col (mod i (quot (count palette) 2))]]
         {:x (+ 2.0 margin (* col (+ (* color-size 2) margin)))
          :y (+ 22.0 256.0 margin (* row (+ color-size margin)))
          :width (* color-size 2.0) :height (double color-size)})))

(defn- crop
  "A source rect shortened to the fraction of a tile actually drawn, so a
   partial tile truncates the pattern rather than squashing it."
  [source fw fh]
  {:x (:x source) :y (:y source)
   :width (* fw (:width source)) :height (* fh (:height source))})

(defn tile-rects
  "The (source, dest) pairs that tile `source` across `dest` at `scale`.

   Returns [] for a degenerate input - a zero-sized source or a non-positive
   scale - which is the guard the C warns about: without it the tiling loops
   never advance."
  [source dest scale]
  (let [tw (long (* (:width source) scale))
        th (long (* (:height source) scale))
        {dx0 :x dy0 :y dw :width dh :height} dest]
    (if (or (<= scale 0) (zero? (:width source)) (zero? (:height source))
            (zero? tw) (zero? th))
      []
      (cond
        ;; Bigger than the area in both axes: one tile, cropped to fit.
        (and (< dw tw) (< dh th))
        [{:source (crop source (/ dw tw) (/ dh th)) :dest dest}]

        ;; Bigger horizontally: a single column.
        (<= dw tw)
        (let [full (take-while #(< (+ % th) dh) (iterate #(+ % th) 0))
              rows (mapv (fn [dy] {:source (crop source (/ dw tw) 1.0)
                                   :dest {:x dx0 :y (+ dy0 dy) :width dw :height (double th)}})
                         full)
              used (* th (count full))]
          (cond-> rows
            (< used dh) (conj {:source (crop source (/ dw tw) (/ (- dh used) th))
                               :dest {:x dx0 :y (+ dy0 used) :width dw :height (- dh used)}})))

        ;; Bigger vertically: a single row.
        (<= dh th)
        (let [full (take-while #(< (+ % tw) dw) (iterate #(+ % tw) 0))
              cols (mapv (fn [dx] {:source (crop source 1.0 (/ dh th))
                                   :dest {:x (+ dx0 dx) :y dy0 :width (double tw) :height dh}})
                         full)
              used (* tw (count full))]
          (cond-> cols
            (< used dw) (conj {:source (crop source (/ (- dw used) tw) (/ dh th))
                               :dest {:x (+ dx0 used) :y dy0 :width (- dw used) :height dh}})))

        ;; Smaller in both: a grid, with partial tiles down the right and
        ;; bottom edges and one corner tile cropped in both axes.
        :else
        (let [xs (take-while #(< (+ % tw) dw) (iterate #(+ % tw) 0))
              ys (take-while #(< (+ % th) dh) (iterate #(+ % th) 0))
              used-x (* tw (count xs)) used-y (* th (count ys))
              full (for [dx xs dy ys]
                     {:source source
                      :dest {:x (+ dx0 dx) :y (+ dy0 dy) :width (double tw) :height (double th)}})
              bottom (when (< used-y dh)
                       (for [dx xs]
                         {:source (crop source 1.0 (/ (- dh used-y) th))
                          :dest {:x (+ dx0 dx) :y (+ dy0 used-y)
                                 :width (double tw) :height (- dh used-y)}}))
              right (when (< used-x dw)
                      (for [dy ys]
                        {:source (crop source (/ (- dw used-x) tw) 1.0)
                         :dest {:x (+ dx0 used-x) :y (+ dy0 dy)
                                :width (- dw used-x) :height (double th)}}))
              corner (when (and (< used-x dw) (< used-y dh))
                       [{:source (crop source (/ (- dw used-x) tw) (/ (- dh used-y) th))
                         :dest {:x (+ dx0 used-x) :y (+ dy0 used-y)
                                :width (- dw used-x) :height (- dh used-y)}}])]
          (vec (concat full bottom right corner)))))))

(defn initial-state []
  {:pattern 0 :colour 0 :scale 1.0 :rotation 0.0 :texture nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/set-config-flags! (:flag/window-resizable rcw/config-flag))
  (rcw/init-window! screen-width screen-height "raylib [textures] example - tiled drawing")
  (let [tex (rtl/load-texture! "resources/patterns.png")]
    (rtl/set-texture-filter! tex (:bilinear rtl/texture-filter))
    (swap! game-atom assoc :texture tex))
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [scale rotation] :as state}]
  (debug-stats/update!)
  (let [mouse (rcm/get-mouse-position)
        clicked? (rcm/is-mouse-button-pressed? (:left enums/mouse-button))
        hit (fn [rects offset-x offset-y]
              (when clicked?
                (first (keep-indexed
                        (fn [i r]
                          (when (pos? (rcol/check-collision-point-rec?
                                       mouse (update (update r :x + offset-x) :y + offset-y)))
                            i))
                        rects))))
        pattern (or (hit pattern-rects (+ 2 margin) (+ 40 margin)) (:pattern state))
        colour (or (hit color-rects 0 0) (:colour state))
        scale (cond (rck/is-key-pressed? (:up enums/keyboard-key)) (+ scale 0.25)
                    (rck/is-key-pressed? (:down enums/keyboard-key)) (- scale 0.25)
                    :else scale)
        scale (cond (> scale 10.0) 10.0 (<= scale 0.0) 0.25 :else scale)
        rotation (cond (rck/is-key-pressed? (:left enums/keyboard-key)) (- rotation 25.0)
                       (rck/is-key-pressed? (:right enums/keyboard-key)) (+ rotation 25.0)
                       :else rotation)
        reset? (rck/is-key-pressed? (:space enums/keyboard-key))]
    (assoc state :pattern pattern :colour colour
           :scale (if reset? 1.0 scale)
           :rotation (if reset? 0.0 rotation))))

(defn draw [{:keys [pattern colour scale rotation texture]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (let [sw (rcw/get-screen-width) sh (rcw/get-screen-height)
        area {:x (double (+ opt-width margin)) :y (double margin)
              :width (- sw opt-width (* 2.0 margin)) :height (- sh (* 2.0 margin))}
        tint (nth palette colour)]
    (doseq [{:keys [source dest]} (tile-rects (nth pattern-rects pattern) area scale)]
      (rtl/draw-texture-pro! texture
                             (update source :width float)
                             (-> dest (update :x float) (update :y float)
                                 (update :width float) (update :height float))
                             {:x 0.0 :y 0.0} (float rotation) tint))

    (rsb/draw-rectangle! margin margin (- opt-width margin) (- sh (* 2 margin))
                         (ru/fade colors/lightgray 0.5))
    (rtd/draw-text! "Select Pattern" (+ 2 margin) (+ 30 margin) 10 colors/black)
    (rtdw/draw-texture! texture (+ 2 margin) (+ 40 margin) colors/black)
    (let [p (nth pattern-rects pattern)]
      (rsb/draw-rectangle! (+ 2 margin (int (:x p))) (+ 40 margin (int (:y p)))
                           (int (:width p)) (int (:height p))
                           (ru/fade colors/darkblue 0.3)))

    (rtd/draw-text! "Select Color" (+ 2 margin) (+ 10 256 margin) 10 colors/black)
    (doseq [[i r] (map-indexed vector color-rects)]
      (rsb/draw-rectangle-rec! r (nth palette i))
      (when (= i colour)
        (rsb/draw-rectangle-lines-ex! r 3.0 (ru/fade colors/white 0.5))))

    (rtd/draw-text! "Scale (UP/DOWN to change)" (+ 2 margin) (+ 80 256 margin) 10 colors/black)
    (rtd/draw-text! (format "%.2fx" scale) (+ 2 margin) (+ 92 256 margin) 20 colors/black)
    (rtd/draw-text! "Rotation (LEFT/RIGHT to change)" (+ 2 margin) (+ 122 256 margin) 10 colors/black)
    (rtd/draw-text! (format "%.0f degrees" rotation) (+ 2 margin) (+ 134 256 margin) 20 colors/black)
    (rtd/draw-text! "Press [SPACE] to reset" (+ 2 margin) (+ 164 256 margin) 10 colors/darkblue)
    (rtd/draw-text! (format "%d FPS" (rct/get-fps)) (+ 2 margin) (+ 2 margin) 20 colors/black))

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
  (when-let [t (:texture @game-atom)] (rtl/unload-texture! t))
  (rcw/close-window!))
