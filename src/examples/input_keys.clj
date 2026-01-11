(ns examples.input-keys
  "Raylib [core] example - keyboard input
   
   Move a ball around the screen using arrow keys.
   Based on: raylib/examples/core/core_input_keys.c
   
   Complexity: ⭐ Beginner
   
   Controls:
   - Arrow keys: Move the ball
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
(def BALL_RADIUS 50)
(def MOVE_SPEED 4.0)

(defn initial-state []
  {:exit? false
   :ball-x (/ WIDTH 2.0)
   :ball-y (/ HEIGHT 2.0)})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [core] example - keyboard input")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn handle-input [{:keys [ball-x ball-y]
                     :as game}]
  (let [;; Calculate new position based on arrow key input
        new-x (cond-> ball-x
                (rck/is-key-down? (:right enums/keyboard-key)) (+ MOVE_SPEED)
                (rck/is-key-down? (:left enums/keyboard-key)) (- MOVE_SPEED))
        new-y (cond-> ball-y
                (rck/is-key-down? (:down enums/keyboard-key)) (+ MOVE_SPEED)
                (rck/is-key-down? (:up enums/keyboard-key)) (- MOVE_SPEED))
        ;; Clamp to screen bounds
        clamped-x (max BALL_RADIUS (min (- WIDTH BALL_RADIUS) new-x))
        clamped-y (max BALL_RADIUS (min (- HEIGHT BALL_RADIUS) new-y))]
    (cond-> game
      true (assoc :ball-x clamped-x :ball-y clamped-y)
      (rck/is-key-down? (:q enums/keyboard-key)) (assoc :exit? true))))

(defn tick [game]
  (debug-stats/update!)
  (handle-input game))

(defn draw [{:keys [ball-x ball-y]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; Draw the ball
  (rsb/draw-circle-v! {:x ball-x
                       :y ball-y} BALL_RADIUS colors/maroon)

  ;; Draw instructions
  (rtd/draw-text! "Move the ball with ARROW KEYS" 10 10 20 colors/darkgray)
  (rtd/draw-text! "Press Q to exit" 10 35 20 colors/gray)

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

  ;; Teleport ball to center
  (swap! game-atom assoc :ball-x 400 :ball-y 225)

  ;; Reset to initial state
  (reset! game-atom (initial-state))
  ;;
  )
