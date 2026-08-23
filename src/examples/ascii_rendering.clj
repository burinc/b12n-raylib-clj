(ns examples.ascii-rendering
  "raylib [shaders] example - ascii rendering

   A post-processing effect: the scene is drawn into an offscreen render
   texture, then that whole texture is drawn once through a fragment shader
   that replaces each cell with an ASCII glyph. Left and right change the
   cell size, which is the shader's `fontSize` uniform.

   Two details worth pointing at.

   The source rectangle passed when blitting the render texture has a
   NEGATIVE height. That is not a typo: OpenGL render targets are stored
   bottom-up, and flipping the source rect is the standard way to draw one
   the right way up.

   And nothing here draws a glyph. The scene is two ordinary textures; every
   character on screen is produced by the shader sampling the render texture
   per cell and picking a glyph by brightness.

   Difficulty: 3/4
   Based on: shaders/shaders_ascii_rendering.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.shaders :as rcs]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.textures.drawing :as rtdw]
   [raylib.textures.texture-loading :as rtl]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

;; The shader's glyph atlas is built for 9px cells and up; below that the
;; effect breaks down, and above 15 the cells get too coarse to read.
(def min-font-size 9.0)
(def max-font-size 15.0)

(defn initial-state []
  {:font-size min-font-size
   :circle-pos {:x 40.0 :y (* screen-height 0.5)}
   :circle-speed 1.0
   :shader nil :font-size-loc nil :target nil :fudesumi nil :raysan nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shaders] example - ascii rendering")
  (let [shader (rcs/load-shader nil "resources/shaders/glsl330/ascii.fs")
        res-loc (rcs/get-shader-location shader "resolution")
        font-loc (rcs/get-shader-location shader "fontSize")]
    (when (or (neg? res-loc) (neg? font-loc))
      (println "WARNING: ascii.fs is missing a uniform; effect will not apply"))
    ;; Resolution never changes, so it is uploaded once here rather than
    ;; every frame like fontSize.
    (rcs/set-shader-value-vec2! shader res-loc [screen-width screen-height])
    (swap! game-atom assoc
           :shader shader :font-size-loc font-loc
           :fudesumi (rtl/load-texture! "resources/fudesumi.png")
           :raysan (rtl/load-texture! "resources/raysan.png")
           :target (rtl/load-render-texture! screen-width screen-height)))
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [circle-pos circle-speed font-size] :as state}]
  (debug-stats/update!)
  (let [x (+ (:x circle-pos) circle-speed)
        ;; Bounce between 40 and 200. The C flips the sign AFTER stepping,
        ;; so the sprite overshoots by one pixel at each end before turning.
        speed (if (or (> x 200.0) (< x 40.0)) (- circle-speed) circle-speed)]
    (assoc state
           :circle-pos (assoc circle-pos :x x)
           :circle-speed speed
           :font-size (cond
                        (and (rck/is-key-pressed? (:left enums/keyboard-key))
                             (> font-size min-font-size)) (dec font-size)
                        (and (rck/is-key-pressed? (:right enums/keyboard-key))
                             (< font-size max-font-size)) (inc font-size)
                        :else font-size))))

(defn draw [{:keys [shader font-size-loc font-size target fudesumi raysan circle-pos]}]
  (rcs/set-shader-value-float! shader font-size-loc font-size)

  ;; Pass one: the ordinary scene, into the offscreen target.
  (rtl/begin-texture-mode! target)
  (rcd/clear-background! colors/white)
  (rtdw/draw-texture! fudesumi 500 -30 colors/white)
  (rtl/draw-texture-v! raysan
                       {:x (float (:x circle-pos)) :y (float (:y circle-pos))}
                       colors/white)
  (rtl/end-texture-mode!)

  ;; Pass two: that target, drawn once through the shader.
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rcs/begin-shader-mode! shader)
  (let [tex (:texture target)]
    (rtl/draw-texture-rec! tex
                           {:x 0.0 :y 0.0
                            :width (float (:width tex))
                            ;; Negative: flips the bottom-up GL target upright.
                            :height (float (- (:height tex)))}
                           {:x 0.0 :y 0.0} colors/white))
  (rcs/end-shader-mode!)

  (rsb/draw-rectangle! 0 0 screen-width 40 colors/black)
  (rtd/draw-text! (format "Ascii effect - FontSize:%2.0f - [Left] -1 [Right] +1 " font-size)
                  120 10 20 colors/lightgray)
  (rtd/draw-fps! 10 10)
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
  (let [{:keys [target shader fudesumi raysan]} @game-atom]
    (when target (rtl/unload-render-texture! target))
    (when shader (rcs/unload-shader! shader))
    (when fudesumi (rtl/unload-texture! fudesumi))
    (when raysan (rtl/unload-texture! raysan)))
  (rcw/close-window!))
