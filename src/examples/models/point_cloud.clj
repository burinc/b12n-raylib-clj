(ns examples.models.point-cloud
  "Raylib [models] example - point cloud
   
   A colorful spherical point cloud visualization.
   Points are distributed in a sphere with colors based on position.
   Based on: raylib/examples/models/models_point_rendering.c (simplified)
   
   Complexity: ⭐⭐ Intermediate (2/4)
   
   Controls:
   - UP/DOWN: Change point count
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
(def MIN_POINTS 100)
(def MAX_POINTS 5000)

(defn make-camera []
  {:position {:x 5.0
              :y 5.0
              :z 5.0}
   :target {:x 0.0
            :y 0.0
            :z 0.0}
   :up {:x 0.0
        :y 1.0
        :z 0.0}
   :fovy 45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn generate-sphere-point
  "Generate a random point on/inside a sphere using spherical coordinates"
  [max-radius]
  (let [theta (* Math/PI (rand)) ; 0 to PI
        phi (* 2.0 Math/PI (rand)) ; 0 to 2PI
        r (* max-radius (rand)) ; 0 to max-radius
        x (* r (Math/sin theta) (Math/cos phi))
        y (* r (Math/sin theta) (Math/sin phi))
        z (* r (Math/cos theta))
        ;; Color based on radius (distance from center)
        hue (* (/ r max-radius) 360.0)]
    {:pos {:x x
           :y y
           :z z}
     :hue hue
     :radius r}))

(defn generate-points [n max-radius]
  (vec (repeatedly n #(generate-sphere-point max-radius))))

(defn initial-state []
  {:exit? false
   :camera (make-camera)
   :num-points 1000
   :points (generate-points 1000 2.0)
   :max-radius 2.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - point cloud")
  (rct/set-target-fps! 60))

(defn handle-input [{:keys [num-points max-radius]
                     :as game}]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)

    (rck/is-key-pressed? (:up enums/keyboard-key))
    (-> (update :num-points #(min MAX_POINTS (* % 2)))
        (#(assoc % :points (generate-points (:num-points %) max-radius))))

    (rck/is-key-pressed? (:down enums/keyboard-key))
    (-> (update :num-points #(max MIN_POINTS (quot % 2)))
        (#(assoc % :points (generate-points (:num-points %) max-radius))))))

(defn update-camera [game]
  (let [time (rct/get-time)
        camera-time (* time 0.5)
        new-x (* (Math/cos camera-time) 6.0)
        new-z (* (Math/sin camera-time) 6.0)]
    (-> game
        (assoc-in [:camera :position :x] new-x)
        (assoc-in [:camera :position :z] new-z))))

(defn tick [game]
  (-> game
      handle-input
      update-camera))

(defn draw [{:keys [camera points num-points]}]
  (let [time (rct/get-time)]
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/black)

    (rc3d/begin-mode-3d! camera)

    ;; Draw all points with animated colors
    (doseq [{:keys [pos hue]} points]
      (let [animated-hue (mod (+ hue (* time 50.0)) 360.0)
            color (ru/color-from-hsv (float animated-hue) 1.0 1.0)]
        (rc3d/draw-point-3d! pos color)))

    ;; Draw reference sphere wireframe
    (rc3d/draw-sphere-wires! {:x 0.0
                              :y 0.0
                              :z 0.0} 2.0 10 10 colors/yellow)

    (rc3d/end-mode-3d!)

    ;; Draw UI
    (rtd/draw-text! (str "Point Count: " num-points) 10 (- HEIGHT 50) 30 colors/white)
    (rtd/draw-text! "UP - Increase points" 10 40 20 colors/white)
    (rtd/draw-text! "DOWN - Decrease points" 10 65 20 colors/white)
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

  ;; Check point count
  (:num-points @game-atom)

  ;; Regenerate points
  (swap! game-atom #(assoc % :points (generate-points (:num-points %) (:max-radius %))))

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
