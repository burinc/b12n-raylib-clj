(ns examples.sprite-animation
  "Raylib [textures] example - sprite animation
   
   Demonstrates sprite sheet animation with configurable frame speed.
   Shows how to extract and display individual frames from a sprite sheet.
   Based on: raylib/examples/textures/textures_sprite_animation.c
   
   Complexity: ⭐⭐ Easy
   
   Controls:
   - LEFT/RIGHT: Adjust animation speed
   - F1: Toggle debug stats
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.textures.texture-loading :as rtl]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)

(def MAX_FRAME_SPEED 15)
(def MIN_FRAME_SPEED 1)
(def NUM_FRAMES 6)

(defn initial-state []
  {:exit? false
   :scarfy nil
   :position {:x 350.0
              :y 280.0}
   :current-frame 0
   :frames-counter 0
   :frames-speed 8}) ; frames per second

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [textures] example - sprite animation")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  ;; Load spritesheet
  (let [scarfy (rtl/load-texture! "resources/scarfy.png")]
    (swap! game-atom assoc :scarfy scarfy)))

(defn update-animation [{:keys [scarfy frames-counter frames-speed current-frame]
                         :as game}]
  (let [new-counter (inc frames-counter)]
    (if (>= new-counter (/ 60 frames-speed))
      ;; Advance to next frame
      (let [next-frame (mod (inc current-frame) NUM_FRAMES)]
        (assoc game
               :frames-counter 0
               :current-frame next-frame))
      ;; Just increment counter
      (assoc game :frames-counter new-counter))))

(defn handle-input [{:keys [frames-speed]
                     :as game}]
  (let [new-speed (cond
                    (rck/is-key-pressed? (:right enums/keyboard-key))
                    (min MAX_FRAME_SPEED (inc frames-speed))

                    (rck/is-key-pressed? (:left enums/keyboard-key))
                    (max MIN_FRAME_SPEED (dec frames-speed))

                    :else frames-speed)]
    (cond-> (assoc game :frames-speed new-speed)
      (rck/is-key-down? (:q enums/keyboard-key))
      (assoc :exit? true))))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      handle-input
      update-animation))

(defn draw [{:keys [scarfy position current-frame frames-speed]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (when scarfy
    (let [frame-width (/ (:width scarfy) NUM_FRAMES)
          frame-height (:height scarfy)
          frame-x (* current-frame frame-width)
          frame-rec {:x frame-x
                     :y 0
                     :width frame-width
                     :height frame-height}]

      ;; Draw full spritesheet as reference
      (ext/draw-texture-ex! scarfy {:x 15
                                    :y 40} 0.0 1.0 colors/white)

      ;; Draw outline around full spritesheet
      (ext/draw-rectangle-lines! 15 40 (:width scarfy) (:height scarfy) colors/lime)

      ;; Draw outline around current frame
      (ext/draw-rectangle-lines! (+ 15 (int frame-x)) 40 (int frame-width) (int frame-height) colors/red)

      ;; Draw frame speed controls
      (rtd/draw-text! "FRAME SPEED: " 165 210 10 colors/darkgray)
      (rtd/draw-text! (format "%02d FPS" frames-speed) 575 210 10 colors/darkgray)
      (rtd/draw-text! "PRESS RIGHT/LEFT KEYS to CHANGE SPEED!" 290 240 10 colors/darkgray)

      ;; Draw speed indicator bars
      (doseq [i (range MAX_FRAME_SPEED)]
        (let [bar-x (+ 250 (* 21 i))]
          (when (< i frames-speed)
            (rsb/draw-rectangle! bar-x 205 20 20 colors/red))
          (ext/draw-rectangle-lines! bar-x 205 20 20 colors/maroon)))

      ;; Draw animated sprite
      (ext/draw-texture-rec! scarfy frame-rec position colors/white)

      ;; Draw attribution
      (rtd/draw-text! "(c) Scarfy sprite by Eiden Marsal" (- WIDTH 200) (- HEIGHT 20) 10 colors/gray)))

  ;; Draw debug stats overlay
  (debug-stats/draw!)

  (rcd/end-drawing!))

(defn cleanup [{:keys [scarfy]}]
  (when scarfy (ext/unload-texture! scarfy)))

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

  ;; Check animation state
  (select-keys @game-atom [:current-frame :frames-counter :frames-speed])

  ;; Adjust speed from REPL
  (swap! game-atom assoc :frames-speed 15)
  (swap! game-atom assoc :frames-speed 1)

  ;; Jump to specific frame
  (swap! game-atom assoc :current-frame 3)
  ;;
  )
