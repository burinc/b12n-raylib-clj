(ns examples.screen-manager
  "Raylib [core] example - basic screen manager
   
   Demonstrates a simple state machine for managing game screens.
   Based on: raylib/examples/core/core_basic_screen_manager.c
   
   Complexity: ⭐ Beginner
   
   Controls:
   - ENTER: Navigate between screens
   - Q: Exit (from any screen)"
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

;; Screen states
(def screens [:logo :title :gameplay :ending])

(defn initial-state []
  {:exit? false
   :screen :logo
   :frames-counter 0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [core] example - basic screen manager")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

;; =============================================================================
;; Screen Update Functions
;; =============================================================================

(defn update-logo [{:keys [frames-counter] :as game}]
  (let [new-frames (inc frames-counter)]
    (if (> new-frames 120) ;; Wait 2 seconds (120 frames at 60fps)
      (-> game
          (assoc :screen :title)
          (assoc :frames-counter 0))
      (assoc game :frames-counter new-frames))))

(defn update-title [game]
  (if (rck/is-key-pressed? (:enter enums/keyboard-key))
    (assoc game :screen :gameplay)
    game))

(defn update-gameplay [game]
  (if (rck/is-key-pressed? (:enter enums/keyboard-key))
    (assoc game :screen :ending)
    game))

(defn update-ending [game]
  (if (rck/is-key-pressed? (:enter enums/keyboard-key))
    (-> game
        (assoc :screen :title)
        (assoc :frames-counter 0))
    game))

;; =============================================================================
;; Screen Draw Functions
;; =============================================================================

(defn draw-logo [{:keys [frames-counter]}]
  (rcd/clear-background! colors/raywhite)
  (rtd/draw-text! "LOGO SCREEN" 20 20 40 colors/lightgray)
  (rtd/draw-text! "WAIT for 2 SECONDS..." 290 220 20 colors/gray)
  ;; Draw a simple loading bar
  (let [progress (min 1.0 (/ frames-counter 120.0))
        bar-width (int (* progress 200))]
    (rsb/draw-rectangle! 300 260 200 20 colors/lightgray)
    (rsb/draw-rectangle! 300 260 bar-width 20 colors/darkgreen)))

(defn draw-title [_game]
  (rsb/draw-rectangle! 0 0 WIDTH HEIGHT colors/green)
  (rtd/draw-text! "TITLE SCREEN" 20 20 40 colors/darkgreen)
  (rtd/draw-text! "PRESS ENTER to JUMP to GAMEPLAY SCREEN" 120 220 20 colors/darkgreen))

(defn draw-gameplay [_game]
  (rsb/draw-rectangle! 0 0 WIDTH HEIGHT colors/purple)
  (rtd/draw-text! "GAMEPLAY SCREEN" 20 20 40 colors/maroon)
  (rtd/draw-text! "PRESS ENTER to JUMP to ENDING SCREEN" 130 220 20 colors/maroon)
  ;; Draw some placeholder game elements
  (rsb/draw-circle-v! {:x 400 :y 300} 50 colors/gold)
  (rsb/draw-rectangle! 100 350 100 50 colors/skyblue)
  (rsb/draw-rectangle! 600 350 100 50 colors/orange))

(defn draw-ending [_game]
  (rsb/draw-rectangle! 0 0 WIDTH HEIGHT colors/blue)
  (rtd/draw-text! "ENDING SCREEN" 20 20 40 colors/darkblue)
  (rtd/draw-text! "PRESS ENTER to RETURN to TITLE SCREEN" 120 220 20 colors/darkblue)
  (rtd/draw-text! "Thanks for playing!" 280 280 30 colors/white))

;; =============================================================================
;; Main Game Loop
;; =============================================================================

(defn handle-global-input [game]
  (cond-> game
    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (debug-stats/update!)
  (let [game (handle-global-input game)]
    (case (:screen game)
      :logo (update-logo game)
      :title (update-title game)
      :gameplay (update-gameplay game)
      :ending (update-ending game)
      game)))

(defn draw [game]
  (rcd/begin-drawing!)

  (case (:screen game)
    :logo (draw-logo game)
    :title (draw-title game)
    :gameplay (draw-gameplay game)
    :ending (draw-ending game)
    nil)

  ;; Draw current screen indicator
  (rtd/draw-text! (str "Current: " (name (:screen game))) 10 (- HEIGHT 30) 20 colors/darkgray)

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

  ;; Jump to a specific screen
  (swap! game-atom assoc :screen :gameplay)
  (swap! game-atom assoc :screen :ending)
  (swap! game-atom assoc :screen :title)
  (swap! game-atom assoc :screen :logo :frames-counter 0)

  ;; Reset to initial state
  (reset! game-atom (initial-state))
  ;;
  )
