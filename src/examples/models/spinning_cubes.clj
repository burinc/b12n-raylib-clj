(ns examples.models.spinning-cubes
  "Raylib [models] example - spinning cubes
   
   Multiple cubes spinning at different speeds and positions.
   Demonstrates 3D transformations and color cycling.
   
   Complexity: ⭐ Basic (1/4)
   
   Controls:
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.camera3d :as rc3d]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)

;; Cube definitions: position, size, rotation-speed, color
(def cube-configs
  [{:pos {:x -3.0
          :y 0.0
          :z 0.0}
    :size 1.5
    :speed 1.0
    :hue 0}
   {:pos {:x 0.0
          :y 0.0
          :z 0.0}
    :size 2.0
    :speed 1.5
    :hue 120}
   {:pos {:x 3.0
          :y 0.0
          :z 0.0}
    :size 1.0
    :speed 2.0
    :hue 240}
   {:pos {:x -1.5
          :y 2.5
          :z -1.0}
    :size 0.8
    :speed 2.5
    :hue 60}
   {:pos {:x 1.5
          :y 2.5
          :z 1.0}
    :size 0.8
    :speed 3.0
    :hue 180}])

(defn make-camera []
  {:position {:x 8.0
              :y 8.0
              :z 8.0}
   :target {:x 0.0
            :y 1.0
            :z 0.0}
   :up {:x 0.0
        :y 1.0
        :z 0.0}
   :fovy 45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)
   :rotation 0.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - spinning cubes")
  (rct/set-target-fps! 60))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (let [time (ext/get-time)]
    (-> game
        handle-input
        (assoc :rotation time))))

(defn draw-spinning-cube [{:keys [pos size speed hue]} rotation]
  (let [;; Calculate individual rotation angle
        angle (* rotation speed 60.0) ; Convert to degrees
        ;; Animate hue over time
        animated-hue (mod (+ hue (* rotation 20.0)) 360.0)
        color (ext/color-from-hsv (float animated-hue) 0.8 0.9)
        ;; Make cube "bob" up and down
        bob-offset (* 0.3 (Math/sin (* rotation speed 2.0)))
        adjusted-pos (update pos :y + bob-offset)]
    ;; Draw solid cube
    (rc3d/draw-cube! adjusted-pos size size size color)
    ;; Draw wireframe
    (rc3d/draw-cube-wires! adjusted-pos size size size colors/black)))

(defn draw [{:keys [camera rotation]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3d/begin-mode-3d! camera)

  ;; Draw all cubes
  (doseq [config cube-configs]
    (draw-spinning-cube config rotation))

  ;; Draw grid
  (rc3d/draw-grid! 10 1.0)

  (rc3d/end-mode-3d!)

  (rtd/draw-text! "Spinning Cubes - each with unique speed and color" 10 40 20 colors/darkgray)
  (rtd/draw-fps! 10 10)

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
  ;; REPL development - connect to port 7888
  @game-atom

  ;; Check rotation
  (:rotation @game-atom)

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
