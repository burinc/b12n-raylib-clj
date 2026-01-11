(ns examples.bouncing-ball
  "Raylib [shapes] example - bouncing ball
   
   A simple physics demo showing a ball bouncing with optional gravity.
   Based on: raylib/examples/shapes/shapes_bouncing_ball.c
   
   Complexity: ⭐ Beginner
   
   Controls:
   - SPACE: Pause/resume ball movement
   - G: Toggle gravity on/off
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
(def BALL_RADIUS 20)
(def GRAVITY 0.2)

(defn initial-state []
  {:exit? false
   :paused? false
   :use-gravity? true
   :frames-counter 0
   ;; Ball state: position and velocity
   :ball-x (/ WIDTH 2.0)
   :ball-y (/ HEIGHT 2.0)
   :speed-x 5.0
   :speed-y 4.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/set-config-flags :flag/msaa-4x-hint)
  (rcw/init-window! WIDTH HEIGHT "raylib [shapes] example - bouncing ball")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn handle-input [game]
  (cond-> game
    ;; Toggle gravity with G
    (rck/is-key-pressed? (:g enums/keyboard-key))
    (update :use-gravity? not)

    ;; Toggle pause with SPACE
    (rck/is-key-pressed? (:space enums/keyboard-key))
    (update :paused? not)

    ;; Exit with Q
    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn update-physics [{:keys [paused? use-gravity? ball-x ball-y speed-x speed-y]
                       :as game}]
  (if paused?
    ;; When paused, just increment frames counter for blinking text
    (update game :frames-counter inc)
    ;; Update ball physics
    (let [;; Apply velocity
          new-x (+ ball-x speed-x)
          new-y (+ ball-y speed-y)
          ;; Apply gravity to vertical speed if enabled
          new-speed-y (if use-gravity?
                        (+ speed-y GRAVITY)
                        speed-y)
          ;; Check horizontal wall collision
          [final-x final-speed-x]
          (cond
            (>= new-x (- WIDTH BALL_RADIUS))
            [(- WIDTH BALL_RADIUS) (- speed-x)]

            (<= new-x BALL_RADIUS)
            [BALL_RADIUS (- speed-x)]

            :else
            [new-x speed-x])
          ;; Check vertical wall collision (with damping for gravity mode)
          [final-y final-speed-y]
          (cond
            (>= new-y (- HEIGHT BALL_RADIUS))
            [(- HEIGHT BALL_RADIUS) (* new-speed-y -0.95)]

            (<= new-y BALL_RADIUS)
            [BALL_RADIUS (* new-speed-y -0.95)]

            :else
            [new-y new-speed-y])]
      (assoc game
             :ball-x final-x
             :ball-y final-y
             :speed-x final-speed-x
             :speed-y final-speed-y))))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      handle-input
      update-physics))

(defn draw [{:keys [ball-x ball-y use-gravity? paused? frames-counter]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; Draw the ball
  (rsb/draw-circle-v! {:x ball-x
                       :y ball-y} BALL_RADIUS colors/maroon)

  ;; Draw instructions
  (rtd/draw-text! "PRESS SPACE to PAUSE BALL MOVEMENT" 10 (- HEIGHT 25) 20 colors/lightgray)

  ;; Draw gravity status
  (if use-gravity?
    (rtd/draw-text! "GRAVITY: ON (Press G to disable)" 10 (- HEIGHT 50) 20 colors/darkgreen)
    (rtd/draw-text! "GRAVITY: OFF (Press G to enable)" 10 (- HEIGHT 50) 20 colors/red))

  ;; Draw blinking PAUSED text
  (when (and paused? (zero? (mod (quot frames-counter 30) 2)))
    (rtd/draw-text! "PAUSED" 350 200 30 colors/gray))

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

  ;; Reset to initial state
  (reset! game-atom (initial-state))

  ;; Make ball bigger
  (alter-var-root #'BALL_RADIUS (constantly 40))

  ;; Teleport ball to center
  (swap! game-atom assoc :ball-x 400 :ball-y 225)

  ;; Give ball a big kick
  (swap! game-atom assoc :speed-x 10 :speed-y -15)
  ;;
  )
