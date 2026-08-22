(ns examples.window-letterbox
  "raylib [core] example - window letterbox

   Demonstrates resolution-independent rendering using a render texture.
   Resize the window and the game content scales with letterboxing.

   Difficulty: 2/4
   Based on: core/core_window_letterbox.c"
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
   [raylib.utils :as ru]
   [raylib.textures.texture-loading :as rtl]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)
(def game-screen-width 640)
(def game-screen-height 480)

(def ^:const TEXTURE-FILTER-BILINEAR 1)

(defn- random-color []
  {:r (ru/get-random-value 100 250)
   :g (ru/get-random-value 50 150)
   :b (ru/get-random-value 10 100)
   :a 255})

(defn initial-state []
  {:bar-colors (vec (repeatedly 10 random-color))
   :target nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/set-config-flags :flag/window-resizable :flag/vsync-hint)
  (rcw/init-window! screen-width screen-height "raylib [core] example - window letterbox")
  (rcw/set-window-min-size! 320 240)
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  (let [target (rtl/load-render-texture! game-screen-width game-screen-height)]
    (rtl/set-texture-filter! (:texture target) TEXTURE-FILTER-BILINEAR)
    (swap! game-atom assoc :target target)))

(defn tick [{:keys [bar-colors] :as state}]
  (debug-stats/update!)
  (if (rck/is-key-pressed? (:space enums/keyboard-key))
    (assoc state :bar-colors (vec (repeatedly 10 random-color)))
    state))

(defn- clamp [v mn mx]
  (max mn (min mx v)))

(defn draw [{:keys [bar-colors target]}]
  (let [scale (min (/ (float (rcw/get-screen-width)) game-screen-width)
                   (/ (float (rcw/get-screen-height)) game-screen-height))
        mouse (rcm/get-mouse-position)
        virtual-mouse-x (clamp (/ (- (:x mouse) (* (- (rcw/get-screen-width) (* game-screen-width scale)) 0.5)) scale)
                                0.0 (float game-screen-width))
        virtual-mouse-y (clamp (/ (- (:y mouse) (* (- (rcw/get-screen-height) (* game-screen-height scale)) 0.5)) scale)
                                0.0 (float game-screen-height))]

    ;; Draw to render texture
    (when target
      (rtl/begin-texture-mode! target)
      (rcd/clear-background! colors/raywhite)

      (let [bar-height (/ game-screen-height 10)]
        (doseq [i (range 10)]
          (rsb/draw-rectangle! 0 (* bar-height i) game-screen-width bar-height
                               (nth bar-colors i))))

      (rtd/draw-text! "If executed inside a window,\nyou can resize the window,\nand see the screen scaling!"
                      10 25 20 colors/white)
      (rtd/draw-text! (format "Default Mouse: [%d , %d]" (int (:x mouse)) (int (:y mouse)))
                      350 25 20 colors/green)
      (rtd/draw-text! (format "Virtual Mouse: [%d , %d]" (int virtual-mouse-x) (int virtual-mouse-y))
                      350 55 20 colors/yellow)
      (rtl/end-texture-mode!))

    ;; Draw render texture to screen with scaling
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/black)

    (when target
      (let [tex (:texture target)
            dest-w (* game-screen-width scale)
            dest-h (* game-screen-height scale)
            offset-x (* (- (rcw/get-screen-width) dest-w) 0.5)
            offset-y (* (- (rcw/get-screen-height) dest-h) 0.5)]
        (rtl/draw-texture-pro!
         tex
         {:x 0.0 :y 0.0 :width (float (:width tex)) :height (float (- (:height tex)))}
         {:x (float offset-x) :y (float offset-y) :width (float dest-w) :height (float dest-h)}
         {:x 0.0 :y 0.0}
         (float 0.0)
         colors/white)))

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
  (when-let [target (:target @game-atom)]
    (rtl/unload-render-texture! target))
  (rcw/close-window!))
