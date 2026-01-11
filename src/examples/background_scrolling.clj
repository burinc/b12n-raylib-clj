(ns examples.background-scrolling
  "Raylib [textures] example - background scrolling
   
   Parallax scrolling effect with three texture layers moving at different speeds.
   Creates a cyberpunk street scene with depth perception.
   Based on: raylib/examples/textures/textures_background_scrolling.c
   
   Complexity: ⭐ Beginner
   
   Controls:
   - F1: Toggle debug stats
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.textures.texture-loading :as rtl]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)

;; Scrolling speeds (pixels per frame)
(def BACK_SPEED 0.1)
(def MID_SPEED 0.5)
(def FORE_SPEED 1.0)

;; Background color (dark blue: 0x052c46ff)
(def BG_COLOR {:r 5 :g 44 :b 70 :a 255})

(defn initial-state []
  {:exit? false
   :background nil
   :midground nil
   :foreground nil
   :scrolling-back 0.0
   :scrolling-mid 0.0
   :scrolling-fore 0.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [textures] example - background scrolling")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  ;; Load textures
  (let [background (rtl/load-texture! "resources/cyberpunk_street_background.png")
        midground (rtl/load-texture! "resources/cyberpunk_street_midground.png")
        foreground (rtl/load-texture! "resources/cyberpunk_street_foreground.png")]
    (swap! game-atom assoc
           :background background
           :midground midground
           :foreground foreground)))

(defn update-scrolling [{:keys [background midground foreground
                                scrolling-back scrolling-mid scrolling-fore] :as game}]
  (let [;; Update scrolling positions
        new-back (- scrolling-back BACK_SPEED)
        new-mid (- scrolling-mid MID_SPEED)
        new-fore (- scrolling-fore FORE_SPEED)
        ;; Wrap around when texture has scrolled off screen
        ;; NOTE: Texture is scaled 2x, so width * 2
        back-width (* (:width background) 2)
        mid-width (* (:width midground) 2)
        fore-width (* (:width foreground) 2)]
    (assoc game
           :scrolling-back (if (<= new-back (- back-width)) 0.0 new-back)
           :scrolling-mid (if (<= new-mid (- mid-width)) 0.0 new-mid)
           :scrolling-fore (if (<= new-fore (- fore-width)) 0.0 new-fore))))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      handle-input
      update-scrolling))

(defn draw-layer
  "Draw a texture layer twice for seamless scrolling"
  [texture scroll-x y-offset]
  (let [tex-width (* (:width texture) 2)]
    ;; Draw first copy
    (ext/draw-texture-ex! texture {:x scroll-x :y y-offset} 0.0 2.0 colors/white)
    ;; Draw second copy (seamless wrap)
    (ext/draw-texture-ex! texture {:x (+ tex-width scroll-x) :y y-offset} 0.0 2.0 colors/white)))

(defn draw [{:keys [background midground foreground
                    scrolling-back scrolling-mid scrolling-fore]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! BG_COLOR)

  ;; Draw parallax layers (back to front)
  (when background
    (draw-layer background scrolling-back 20))

  (when midground
    (draw-layer midground scrolling-mid 20))

  (when foreground
    (draw-layer foreground scrolling-fore 70))

  ;; Draw title
  (rtd/draw-text! "BACKGROUND SCROLLING & PARALLAX" 10 10 20 colors/red)

  ;; Draw attribution
  (rtd/draw-text! "(c) Cyberpunk Street Environment by Luis Zuno (@ansimuz)"
                  (- WIDTH 330) (- HEIGHT 20) 10 colors/raywhite)

  ;; Draw debug stats overlay
  (debug-stats/draw!)

  (rcd/end-drawing!))

(defn cleanup [{:keys [background midground foreground]}]
  (when background (ext/unload-texture! background))
  (when midground (ext/unload-texture! midground))
  (when foreground (ext/unload-texture! foreground)))

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (when-not (or (:exit? game) (rcw/window-should-close?))
        (reset! game-atom game)
        (draw game)
        (recur))))
  (cleanup @game-atom)
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  ;; For REPL development - connect to port 7888 after running the game
  @game-atom

  ;; Check scrolling positions
  (select-keys @game-atom [:scrolling-back :scrolling-mid :scrolling-fore])

  ;; Adjust scrolling speeds dynamically
  ;; (would need to modify the constants or add them to state)

  ;; Check texture dimensions
  (:background @game-atom)
  ;;
  )
