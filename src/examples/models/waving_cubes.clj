(ns examples.models.waving-cubes
  "Raylib [models] example - waving cubes
   
   A mesmerizing 3D animation of cubes that wave and change colors based on their position.
   The camera orbits around the scene automatically.
   Based on: raylib/examples/models/models_waving_cubes.c
   
   Complexity: ⭐⭐⭐ Intermediate (3/4)
   
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
   [raylib.utils :as ru]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def NUM_BLOCKS 15)

(defn make-camera []
  {:position {:x 30.0
              :y 20.0
              :z 30.0}
   :target {:x 0.0
            :y 0.0
            :z 0.0}
   :up {:x 0.0
        :y 1.0
        :z 0.0}
   :fovy 70.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - waving cubes")
  (rct/set-target-fps! 60))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn update-camera [camera time]
  (let [camera-time (* time 0.3)
        new-x (* (Math/cos camera-time) 40.0)
        new-z (* (Math/sin camera-time) 40.0)]
    (-> camera
        (assoc-in [:position :x] new-x)
        (assoc-in [:position :z] new-z))))

(defn tick [game]
  (let [time (rct/get-time)]
    (-> game
        handle-input
        (update :camera update-camera time))))

(defn draw-cubes [time]
  (let [scale (* (+ 2.0 (Math/sin time)) 0.7)]
    (doseq [x (range NUM_BLOCKS)
            y (range NUM_BLOCKS)
            z (range NUM_BLOCKS)]
      (let [;; Scale depends on position
            block-scale (/ (+ x y z) 30.0)
            ;; Scatter creates the waving effect
            scatter (Math/sin (+ (* block-scale 20.0) (* time 4.0)))
            ;; Calculate cube position
            cube-pos {:x (+ (* (- x (/ NUM_BLOCKS 2)) (* scale 3.0)) scatter)
                      :y (+ (* (- y (/ NUM_BLOCKS 2)) (* scale 2.0)) scatter)
                      :z (+ (* (- z (/ NUM_BLOCKS 2)) (* scale 3.0)) scatter)}
            ;; Calculate rainbow color based on position
            hue (float (mod (* (+ x y z) 18) 360))
            cube-color (ru/color-from-hsv hue 0.75 0.9)
            ;; Calculate cube size
            cube-size (* (- 2.4 scale) block-scale)]
        (rc3d/draw-cube! cube-pos cube-size cube-size cube-size cube-color)))))

(defn draw [{:keys [camera]}]
  (let [time (rct/get-time)]
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/raywhite)

    (rc3d/begin-mode-3d! camera)

    (rc3d/draw-grid! 10 5.0)
    (draw-cubes time)

    (rc3d/end-mode-3d!)

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

  ;; Test color generation
  (ru/color-from-hsv 180.0 0.75 0.9)

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
