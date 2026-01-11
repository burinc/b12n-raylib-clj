(ns examples.logo-anim
  "Raylib [shapes] example - logo raylib animation
   
   Animated raylib logo with state machine animation.
   Based on: raylib/examples/shapes/shapes_logo_raylib_anim.c
   
   Complexity: ⭐⭐ Easy
   
   Controls:
   - R: Replay animation (after completion)
   - F1: Toggle debug stats
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def LOGO_SIZE 256)

(defn initial-state []
  {:exit? false
   ;; Animation state machine
   :state :blinking ; :blinking -> :top-left -> :bottom-right -> :letters -> :done
   :frames-counter 0
   :letters-count 0
   ;; Bar sizes (grow during animation)
   :top-width 16
   :left-height 16
   :bottom-width 16
   :right-height 16
   ;; Fade alpha
   :alpha 1.0
   ;; Logo position (centered)
   :logo-x (- (/ WIDTH 2) 128)
   :logo-y (- (/ HEIGHT 2) 128)})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [shapes] example - raylib logo animation")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

;; State machine update functions

(defn update-blinking [{:keys [frames-counter]
                        :as game}]
  (let [new-frames (inc frames-counter)]
    (if (>= new-frames 120)
      (-> game
          (assoc :state :top-left)
          (assoc :frames-counter 0))
      (assoc game :frames-counter new-frames))))

(defn update-top-left [{:keys [top-width left-height]
                        :as game}]
  (let [new-top (+ top-width 4)
        new-left (+ left-height 4)]
    (if (>= new-top LOGO_SIZE)
      (assoc game :state :bottom-right :top-width LOGO_SIZE :left-height LOGO_SIZE)
      (assoc game :top-width new-top :left-height new-left))))

(defn update-bottom-right [{:keys [bottom-width right-height]
                            :as game}]
  (let [new-bottom (+ bottom-width 4)
        new-right (+ right-height 4)]
    (if (>= new-bottom LOGO_SIZE)
      (assoc game :state :letters :bottom-width LOGO_SIZE :right-height LOGO_SIZE)
      (assoc game :bottom-width new-bottom :right-height new-right))))

(defn update-letters [{:keys [frames-counter letters-count alpha]
                       :as game}]
  (let [new-frames (inc frames-counter)
        ;; Every 12 frames, add a letter
        [new-letters new-frames-reset]
        (if (>= new-frames 12)
          [(inc letters-count) 0]
          [letters-count new-frames])]
    (if (>= new-letters 10)
      ;; Start fading out
      (let [new-alpha (- alpha 0.02)]
        (if (<= new-alpha 0.0)
          (assoc game :state :done :alpha 0.0)
          (assoc game :alpha new-alpha :letters-count new-letters :frames-counter new-frames-reset)))
      (assoc game :letters-count new-letters :frames-counter new-frames-reset))))

(defn update-done [game]
  ;; Press R to replay
  (if (rck/is-key-pressed? (:r enums/keyboard-key))
    (initial-state)
    game))

(defn update-animation [{:keys [state]
                         :as game}]
  (case state
    :blinking (update-blinking game)
    :top-left (update-top-left game)
    :bottom-right (update-bottom-right game)
    :letters (update-letters game)
    :done (update-done game)
    game))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      handle-input
      update-animation))

(defn fade-color
  "Create a faded version of a color"
  [{:keys [r g b]} alpha]
  {:r r
   :g g
   :b b
   :a (int (* 255 alpha))})

(defn draw [{:keys [state frames-counter letters-count
                    top-width left-height bottom-width right-height
                    alpha logo-x logo-y]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (case state
    :blinking
    ;; Blinking small box
    (when (pos? (mod (quot frames-counter 15) 2))
      (rsb/draw-rectangle! logo-x logo-y 16 16 colors/black))

    :top-left
    ;; Top and left bars growing
    (do
      (rsb/draw-rectangle! logo-x logo-y top-width 16 colors/black)
      (rsb/draw-rectangle! logo-x logo-y 16 left-height colors/black))

    :bottom-right
    ;; All four bars
    (do
      (rsb/draw-rectangle! logo-x logo-y top-width 16 colors/black)
      (rsb/draw-rectangle! logo-x logo-y 16 left-height colors/black)
      (rsb/draw-rectangle! (+ logo-x 240) logo-y 16 right-height colors/black)
      (rsb/draw-rectangle! logo-x (+ logo-y 240) bottom-width 16 colors/black))

    :letters
    ;; Full logo with fading + text
    (let [faded-black (fade-color colors/black alpha)
          faded-white (fade-color colors/raywhite alpha)]
      ;; Draw the logo frame
      (rsb/draw-rectangle! logo-x logo-y top-width 16 faded-black)
      (rsb/draw-rectangle! logo-x (+ logo-y 16) 16 (- left-height 32) faded-black)
      (rsb/draw-rectangle! (+ logo-x 240) (+ logo-y 16) 16 (- right-height 32) faded-black)
      (rsb/draw-rectangle! logo-x (+ logo-y 240) bottom-width 16 faded-black)
      ;; Draw white center
      (rsb/draw-rectangle! (- (/ WIDTH 2) 112) (- (/ HEIGHT 2) 112) 224 224 faded-white)
      ;; Draw text (letter by letter)
      (let [text (subs "raylib" 0 (min letters-count 6))]
        (rtd/draw-text! text (- (/ WIDTH 2) 44) (+ (/ HEIGHT 2) 48) 50 faded-black)))

    :done
    ;; Show replay message
    (rtd/draw-text! "[R] REPLAY" 340 200 20 colors/gray)

    nil)

  ;; Draw debug stats overlay
  (debug-stats/draw!)

  (rcd/end-drawing!))

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (when-not (or (:exit? game) (rcw/window-should-close?))
        (reset! game-atom game)
        (draw game)
        (recur))))
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  ;; For REPL development - connect to port 7888 after running the game
  @game-atom

  ;; Skip to a specific state
  (swap! game-atom assoc :state :letters :letters-count 0 :alpha 1.0)
  (swap! game-atom assoc :state :done)

  ;; Reset animation
  (reset! game-atom (initial-state))
  ;;
  )
