(ns examples.split-screen-3d
  "Raylib [core] example - 3D camera split screen
   
   Two-player split screen with independent 3D cameras.
   Each player can move forward/backward in a 3D world of cube trees.
   Based on: raylib/examples/core/core_3d_camera_split_screen.c
   
   Complexity: ⭐⭐⭐ Intermediate
   
   Controls:
   - W/S: Move Player 1 (left screen) forward/backward
   - UP/DOWN: Move Player 2 (right screen) forward/backward
   - F1: Toggle debug stats
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.camera3d :as rc3d]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib.textures.texture-loading :as rtl]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def HALF_WIDTH (/ WIDTH 2))

;; Grid settings for the "forest" of cube trees
(def GRID_COUNT 5)
(def GRID_SPACING 4.0)

;; Movement speed (world units per second)
(def MOVE_SPEED 10.0)

(defn make-camera
  "Create a camera3d map"
  [pos-x pos-y pos-z target-x target-y target-z]
  {:position {:x pos-x
              :y pos-y
              :z pos-z}
   :target {:x target-x
            :y target-y
            :z target-z}
   :up {:x 0.0
        :y 1.0
        :z 0.0}
   :fovy 45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   ;; Player 1: Looks along +Z axis from behind
   :camera1 (make-camera 0.0 1.0 -3.0 0.0 1.0 0.0)
   ;; Player 2: Looks along +X axis from the side
   :camera2 (make-camera -3.0 3.0 0.0 0.0 3.0 0.0)
   ;; Render textures (will be initialized)
   :screen1 nil
   :screen2 nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [core] example - 3D camera split screen")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  ;; Create render textures for split screen
  (let [screen1 (rtl/load-render-texture! HALF_WIDTH HEIGHT)
        screen2 (rtl/load-render-texture! HALF_WIDTH HEIGHT)]
    (swap! game-atom assoc :screen1 screen1 :screen2 screen2)))

(defn move-camera-forward
  "Move camera along its forward direction (Z axis for player1, X axis for player2)"
  [camera axis amount]
  (let [pos (:position camera)
        target (:target camera)]
    (case axis
      :z (-> camera
             (assoc-in [:position :z] (+ (:z pos) amount))
             (assoc-in [:target :z] (+ (:z target) amount)))
      :x (-> camera
             (assoc-in [:position :x] (+ (:x pos) amount))
             (assoc-in [:target :x] (+ (:x target) amount))))))

(defn update-player1 [{:keys [camera1]
                       :as game}]
  (let [dt (rct/get-frame-time)
        offset (* MOVE_SPEED dt)]
    (cond
      (rck/is-key-down? (:w enums/keyboard-key))
      (assoc game :camera1 (move-camera-forward camera1 :z offset))

      (rck/is-key-down? (:s enums/keyboard-key))
      (assoc game :camera1 (move-camera-forward camera1 :z (- offset)))

      :else game)))

(defn update-player2 [{:keys [camera2]
                       :as game}]
  (let [dt (rct/get-frame-time)
        offset (* MOVE_SPEED dt)]
    (cond
      (rck/is-key-down? (:up enums/keyboard-key))
      (assoc game :camera2 (move-camera-forward camera2 :x offset))

      (rck/is-key-down? (:down enums/keyboard-key))
      (assoc game :camera2 (move-camera-forward camera2 :x (- offset)))

      :else game)))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      handle-input
      update-player1
      update-player2))

(defn draw-scene
  "Draw the 3D scene (ground plane, cube trees, player markers)"
  [camera1-pos camera2-pos]
  ;; Draw ground plane
  (rc3d/draw-plane! {:x 0
                     :y 0
                     :z 0} {:x 50
                            :y 50} colors/beige)

  ;; Draw grid of "trees" (cube on a stick)
  (doseq [x (range (- (* GRID_COUNT GRID_SPACING))
                   (inc (* GRID_COUNT GRID_SPACING))
                   GRID_SPACING)
          z (range (- (* GRID_COUNT GRID_SPACING))
                   (inc (* GRID_COUNT GRID_SPACING))
                   GRID_SPACING)]
    ;; Tree top (green cube)
    (rc3d/draw-cube! {:x x
                      :y 1.5
                      :z z} 1.0 1.0 1.0 colors/lime)
    ;; Tree trunk (brown cube)
    (rc3d/draw-cube! {:x x
                      :y 0.5
                      :z z} 0.25 1.0 0.25 colors/brown))

  ;; Draw player position markers
  (rc3d/draw-cube! camera1-pos 1.0 1.0 1.0 colors/red)
  (rc3d/draw-cube! camera2-pos 1.0 1.0 1.0 colors/blue))

(defn draw [{:keys [camera1 camera2 screen1 screen2]}]
  (when (and screen1 screen2)
    ;; Draw Player 1's view to render texture
    (rtl/begin-texture-mode! screen1)
    (rcd/clear-background! colors/skyblue)
    (rc3d/begin-mode-3d! camera1)
    (draw-scene (:position camera1) (:position camera2))
    (rc3d/end-mode-3d!)
    ;; Draw HUD for player 1
    (rsb/draw-rectangle! 0 0 HALF_WIDTH 40 {:r 245
                                            :g 245
                                            :b 245
                                            :a 200})
    (rtd/draw-text! "PLAYER1: W/S to move" 10 10 20 colors/maroon)
    (rtl/end-texture-mode!)

    ;; Draw Player 2's view to render texture
    (rtl/begin-texture-mode! screen2)
    (rcd/clear-background! colors/skyblue)
    (rc3d/begin-mode-3d! camera2)
    (draw-scene (:position camera1) (:position camera2))
    (rc3d/end-mode-3d!)
    ;; Draw HUD for player 2
    (rsb/draw-rectangle! 0 0 HALF_WIDTH 40 {:r 245
                                            :g 245
                                            :b 245
                                            :a 200})
    (rtd/draw-text! "PLAYER2: UP/DOWN to move" 10 10 20 colors/darkblue)
    (rtl/end-texture-mode!)

    ;; Draw both render textures to the screen
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/black)

    ;; Draw left half (player 1) - note: texture is flipped vertically
    (let [tex1 (:texture screen1)
          source-rect {:x 0
                       :y 0
                       :width (:width tex1)
                       :height (- (:height tex1))}] ; Negative height to flip
      (rtl/draw-texture-rec! tex1 source-rect {:x 0
                                               :y 0} colors/white))

    ;; Draw right half (player 2)
    (let [tex2 (:texture screen2)
          source-rect {:x 0
                       :y 0
                       :width (:width tex2)
                       :height (- (:height tex2))}]
      (rtl/draw-texture-rec! tex2 source-rect {:x HALF_WIDTH
                                               :y 0} colors/white))

    ;; Draw divider line
    (rsb/draw-rectangle! (- HALF_WIDTH 2) 0 4 HEIGHT colors/lightgray)

    ;; Draw debug stats overlay
    (debug-stats/draw!)

    (rcd/end-drawing!)))

(defn cleanup [{:keys [screen1 screen2]}]
  (when screen1 (rtl/unload-render-texture! screen1))
  (when screen2 (rtl/unload-render-texture! screen2)))

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

  ;; Check camera positions
  (get-in @game-atom [:camera1 :position])
  (get-in @game-atom [:camera2 :position])

  ;; Teleport player 1
  (swap! game-atom assoc-in [:camera1 :position :z] 10.0)
  (swap! game-atom assoc-in [:camera1 :target :z] 13.0)

  ;; Reset to initial state
  (reset! game-atom (initial-state))
  ;;
  )
