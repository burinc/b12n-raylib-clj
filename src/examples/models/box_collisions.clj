(ns examples.models.box-collisions
  "Raylib [models] example - box collisions
   
   Move a player box around and detect collisions with other 3D objects.
   The player turns red when colliding.
   Based on: raylib/examples/models/models_box_collisions.c
   
   Complexity: ⭐ Basic (1/4)
   
   Controls:
   - Arrow keys: Move player
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.camera3d :as rc3d]
   [raylib.core.collision :as rcc]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def MOVE_SPEED 0.2)

(defn make-camera []
  {:position {:x 0.0
              :y 10.0
              :z 10.0}
   :target {:x 0.0
            :y 0.0
            :z 0.0}
   :up {:x 0.0
        :y 1.0
        :z 0.0}
   :fovy 45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)
   :player-pos {:x 0.0
                :y 1.0
                :z 2.0}
   :player-size {:x 1.0
                 :y 2.0
                 :z 1.0}
   :enemy-box-pos {:x -4.0
                   :y 1.0
                   :z 0.0}
   :enemy-box-size {:x 2.0
                    :y 2.0
                    :z 2.0}
   :enemy-sphere-pos {:x 4.0
                      :y 0.0
                      :z 0.0}
   :enemy-sphere-radius 1.5
   :collision? false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - box collisions")
  (rct/set-target-fps! 60))

(defn handle-input [game]
  (let [player-pos (:player-pos game)]
    (cond-> game
      (rck/is-key-pressed? (:q enums/keyboard-key))
      (assoc :exit? true)

      (rck/is-key-down? (:right enums/keyboard-key))
      (update-in [:player-pos :x] + MOVE_SPEED)

      (rck/is-key-down? (:left enums/keyboard-key))
      (update-in [:player-pos :x] - MOVE_SPEED)

      (rck/is-key-down? (:down enums/keyboard-key))
      (update-in [:player-pos :z] + MOVE_SPEED)

      (rck/is-key-down? (:up enums/keyboard-key))
      (update-in [:player-pos :z] - MOVE_SPEED))))

(defn check-collisions [{:keys [player-pos player-size
                                enemy-box-pos enemy-box-size
                                enemy-sphere-pos enemy-sphere-radius]
                         :as game}]
  (let [player-box (rcc/make-bounding-box player-pos player-size)
        enemy-box (rcc/make-bounding-box enemy-box-pos enemy-box-size)
        ;; Check box vs box collision
        box-collision? (pos? (rcc/check-collision-boxes? player-box enemy-box))
        ;; Check box vs sphere collision
        sphere-collision? (pos? (rcc/check-collision-box-sphere? player-box enemy-sphere-pos enemy-sphere-radius))
        collision? (or box-collision? sphere-collision?)]
    (assoc game :collision? collision?)))

(defn tick [game]
  (-> game
      handle-input
      check-collisions))

(defn draw [{:keys [camera player-pos player-size
                    enemy-box-pos enemy-box-size
                    enemy-sphere-pos enemy-sphere-radius
                    collision?]}]
  (let [player-color (if collision? colors/red colors/green)]
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/raywhite)

    (rc3d/begin-mode-3d! camera)

    ;; Draw enemy box
    (rc3d/draw-cube! enemy-box-pos (:x enemy-box-size) (:y enemy-box-size) (:z enemy-box-size) colors/gray)
    (rc3d/draw-cube-wires! enemy-box-pos (:x enemy-box-size) (:y enemy-box-size) (:z enemy-box-size) colors/darkgray)

    ;; Draw enemy sphere
    (rc3d/draw-sphere! enemy-sphere-pos enemy-sphere-radius colors/gray)
    (rc3d/draw-sphere-wires! enemy-sphere-pos enemy-sphere-radius 16 16 colors/darkgray)

    ;; Draw player
    (rc3d/draw-cube-v! player-pos player-size player-color)

    ;; Draw grid
    (rc3d/draw-grid! 10 1.0)

    (rc3d/end-mode-3d!)

    (rtd/draw-text! "Move player with arrow keys to collide" 220 40 20 colors/gray)
    (rtd/draw-fps! 10 10)

    (rcd/end-drawing!)))

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
  ;; REPL development - connect to port 7888
  @game-atom

  ;; Check player position
  (:player-pos @game-atom)

  ;; Teleport player
  (swap! game-atom assoc :player-pos {:x -4.0
                                      :y 1.0
                                      :z 0.0})

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
