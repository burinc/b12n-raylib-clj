(ns examples.input-mouse
  "Raylib [core] example - mouse input
   
   A ball that follows your mouse. Click to change colors!
   Based on: raylib/examples/core/core_input_mouse.c
   
   Complexity: ⭐ Beginner
   
   Controls:
   - Mouse: Move the ball
   - Left click: Red color
   - Right click: Blue color
   - Middle click: Green color
   - F1: Toggle debug stats
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def BALL_RADIUS 40)

;; Mouse button constants (from raylib)
(def MOUSE_LEFT 0)
(def MOUSE_RIGHT 1)
(def MOUSE_MIDDLE 2)

(defn initial-state []
  {:exit? false
   :ball-x (/ WIDTH 2.0)
   :ball-y (/ HEIGHT 2.0)
   :ball-color colors/darkblue})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [core] example - mouse input")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn update-ball-position [game]
  (let [mouse-pos (rcm/get-mouse-position)]
    (assoc game
           :ball-x (:x mouse-pos)
           :ball-y (:y mouse-pos))))

(defn update-ball-color [game]
  (cond
    (rcm/is-mouse-button-pressed? MOUSE_LEFT)
    (assoc game :ball-color colors/maroon)

    (rcm/is-mouse-button-pressed? MOUSE_RIGHT)
    (assoc game :ball-color colors/darkblue)

    (rcm/is-mouse-button-pressed? MOUSE_MIDDLE)
    (assoc game :ball-color colors/lime)

    :else game))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      update-ball-position
      update-ball-color
      handle-input))

(defn draw [{:keys [ball-x ball-y ball-color]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; Draw the ball at mouse position
  (rsb/draw-circle-v! {:x ball-x :y ball-y} BALL_RADIUS ball-color)

  ;; Draw instructions
  (rtd/draw-text! "Move ball with mouse, click to change color" 10 10 20 colors/darkgray)
  (rtd/draw-text! "LEFT=Red  MIDDLE=Green  RIGHT=Blue" 10 35 20 colors/gray)
  (rtd/draw-text! "Press Q to exit" 10 60 20 colors/gray)

  ;; Draw mouse position
  (rtd/draw-text! (str "Mouse: " (int ball-x) ", " (int ball-y)) 10 (- HEIGHT 30) 20 colors/lightgray)

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

  ;; Change ball color from REPL
  (swap! game-atom assoc :ball-color colors/gold)
  (swap! game-atom assoc :ball-color colors/purple)
  (swap! game-atom assoc :ball-color colors/orange)

  ;; Reset to initial state
  (reset! game-atom (initial-state))
  ;;
  )
