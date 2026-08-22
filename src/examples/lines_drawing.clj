(ns examples.lines-drawing
  "raylib [shapes] example - lines drawing

   Draw on a canvas with the mouse. Left button draws rainbow lines,
   right button erases. Mouse wheel adjusts thickness. Middle click clears.

   Difficulty: 1/4
   Based on: shapes/shapes_lines_drawing.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.mouse :as rcm]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.textures.texture-loading :as rtl]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(defn initial-state []
  {:start-text true
   :prev-mouse {:x 0.0 :y 0.0}
   :line-thickness 8.0
   :line-hue 0.0
   :canvas nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - lines drawing")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  (let [canvas (rtl/load-render-texture! screen-width screen-height)]
    ;; Clear canvas to white
    (rtl/begin-texture-mode! canvas)
    (rcd/clear-background! colors/raywhite)
    (rtl/end-texture-mode!)
    (swap! game-atom assoc :canvas canvas)))

(defn- v2-distance [a b]
  (let [dx (- (:x b) (:x a))
        dy (- (:y b) (:y a))]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn tick [{:keys [canvas start-text prev-mouse line-thickness line-hue] :as state}]
  (debug-stats/update!)
  (let [mouse-pos (rcm/get-mouse-position)
        start-text (if (and start-text (rcm/is-mouse-button-pressed? (:left enums/mouse-button)))
                     false start-text)
        ;; Clear canvas on middle click
        _ (when (rcm/is-mouse-button-pressed? (:middle enums/mouse-button))
            (rtl/begin-texture-mode! canvas)
            (rcd/clear-background! colors/raywhite)
            (rtl/end-texture-mode!))
        ;; Draw on canvas
        left-down (rcm/is-mouse-button-down? (:left enums/mouse-button))
        right-down (rcm/is-mouse-button-down? (:right enums/mouse-button))
        line-hue (if (or left-down right-down)
                   (let [new-hue (if left-down
                                   (let [h (+ line-hue (/ (v2-distance prev-mouse mouse-pos) 3.0))]
                                     (loop [h h] (if (>= h 360.0) (recur (- h 360.0)) h)))
                                   line-hue)
                         draw-color (if left-down
                                      (ru/color-from-hsv (float new-hue) (float 1.0) (float 1.0))
                                      colors/raywhite)]
                     (rtl/begin-texture-mode! canvas)
                     (rsb/draw-circle-v! prev-mouse (float (/ line-thickness 2.0)) draw-color)
                     (rsb/draw-circle-v! mouse-pos (float (/ line-thickness 2.0)) draw-color)
                     (rsb/draw-line-ex! prev-mouse mouse-pos (float line-thickness) draw-color)
                     (rtl/end-texture-mode!)
                     new-hue)
                   line-hue)
        ;; Update thickness from mouse wheel
        wheel (rcm/get-mouse-wheel-move)
        line-thickness (max 1.0 (min 500.0 (+ line-thickness wheel)))]
    (assoc state
           :start-text start-text
           :prev-mouse mouse-pos
           :line-thickness line-thickness
           :line-hue line-hue)))

(defn draw [{:keys [canvas start-text line-thickness]}]
  (rcd/begin-drawing!)

  ;; Draw canvas (flipped vertically)
  (when canvas
    (let [tex (:texture canvas)]
      (rtl/draw-texture-rec!
       tex
       {:x 0.0 :y 0.0 :width (float (:width tex)) :height (float (- (:height tex)))}
       {:x 0.0 :y 0.0}
       colors/white)))

  ;; Draw preview circle when not drawing
  (when-not (rcm/is-mouse-button-down? (:left enums/mouse-button))
    (let [mouse-pos (rcm/get-mouse-position)]
      (rsb/draw-circle-lines-v! mouse-pos (float (/ line-thickness 2.0))
                                {:r 127 :g 127 :b 127 :a 127})))

  ;; Draw hint text
  (when start-text
    (rtd/draw-text! "try clicking and dragging!" 275 215 20 colors/lightgray))

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
  (when-let [canvas (:canvas @game-atom)]
    (rtl/unload-render-texture! canvas))
  (rcw/close-window!))
