(ns examples.models.rotating-cube
  "Raylib [models] example - rotating cube
   
   A simple rotating cube demo demonstrating 3D transformations.
   Based on: models_rotating_cube.c (simplified without textures)
   
   Complexity: ⭐ Basic (1/4)
   
   Controls:
   - Arrow keys: Change rotation axis
   - +/-: Change rotation speed
   - R: Reset rotation
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

(defn make-camera []
  {:position {:x 4.0
              :y 4.0
              :z 4.0}
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
   :rotation 0.0
   :rotation-speed 60.0 ; degrees per second
   :axis {:x 0.0
          :y 1.0
          :z 0.0} ; Y-axis by default
   :axis-name "Y"})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - rotating cube")
  (rct/set-target-fps! 60))

(defn handle-input [{:keys [rotation-speed]
                     :as game}]
  (let [;; Change rotation axis
        new-axis (cond
                   (rck/is-key-pressed? (:up enums/keyboard-key))
                   {:axis {:x 0.0
                           :y 1.0
                           :z 0.0}
                    :axis-name "Y"}

                   (rck/is-key-pressed? (:down enums/keyboard-key))
                   {:axis {:x 1.0
                           :y 0.0
                           :z 0.0}
                    :axis-name "X"}

                   (rck/is-key-pressed? (:left enums/keyboard-key))
                   {:axis {:x 0.0
                           :y 0.0
                           :z 1.0}
                    :axis-name "Z"}

                   (rck/is-key-pressed? (:right enums/keyboard-key))
                   {:axis {:x 1.0
                           :y 1.0
                           :z 1.0}
                    :axis-name "XYZ"}

                   :else nil)

        ;; Change speed
        speed-delta (cond
                      (rck/is-key-down? (:kp-add enums/keyboard-key)) 30.0
                      (rck/is-key-down? (:equal enums/keyboard-key)) 30.0 ; + key
                      (rck/is-key-down? (:kp-subtract enums/keyboard-key)) -30.0
                      (rck/is-key-down? (:minus enums/keyboard-key)) -30.0
                      :else 0.0)]

    (cond-> game
      (rck/is-key-pressed? (:q enums/keyboard-key))
      (assoc :exit? true)

      (rck/is-key-pressed? (:r enums/keyboard-key))
      (assoc :rotation 0.0)

      new-axis
      (merge new-axis)

      (not= speed-delta 0.0)
      (update :rotation-speed #(max 0.0 (min 360.0 (+ % (* speed-delta (rct/get-frame-time)))))))))

(defn update-rotation [{:keys [rotation rotation-speed]
                        :as game}]
  (let [dt (rct/get-frame-time)
        new-rotation (mod (+ rotation (* rotation-speed dt)) 360.0)]
    (assoc game :rotation new-rotation)))

(defn tick [game]
  (-> game
      handle-input
      update-rotation))

;; Rodrigues' rotation formula for rotating points around arbitrary axis
(defn rotate-point [{:keys [x y z]} rotation axis]
  (let [rad (Math/toRadians rotation)
        cos-r (Math/cos rad)
        sin-r (Math/sin rad)
        ;; Get axis components
        ax (:x axis)
        ay (:y axis)
        az (:z axis)
        ;; Normalize axis
        len (Math/sqrt (+ (* ax ax) (* ay ay) (* az az)))
        ax (/ ax len)
        ay (/ ay len)
        az (/ az len)
        ;; Rotation using Rodrigues' formula
        dot (+ (* ax x) (* ay y) (* az z))
        cross-x (- (* ay z) (* az y))
        cross-y (- (* az x) (* ax z))
        cross-z (- (* ax y) (* ay x))]
    {:x (+ (* x cos-r) (* cross-x sin-r) (* ax dot (- 1 cos-r)))
     :y (+ (* y cos-r) (* cross-y sin-r) (* ay dot (- 1 cos-r)))
     :z (+ (* z cos-r) (* cross-z sin-r) (* az dot (- 1 cos-r)))}))

(defn draw-rotating-cube! [rotation axis size color wire-color]
  (let [half (/ size 2.0)
        nh (- half) ; negative half
        ;; Define cube vertices
        vertices [[nh nh nh]
                  [half nh nh]
                  [half half nh]
                  [nh half nh]
                  [nh nh half]
                  [half nh half]
                  [half half half]
                  [nh half half]]
        ;; Rotate all vertices
        rotated (mapv (fn [[x y z]]
                        (rotate-point {:x x
                                       :y y
                                       :z z} rotation axis))
                      vertices)
        ;; Define edges (pairs of vertex indices)
        edges [[0 1] [1 2] [2 3] [3 0] ; back face
               [4 5] [5 6] [6 7] [7 4] ; front face
               [0 4] [1 5] [2 6] [3 7]]] ; connecting edges

    ;; Draw edges
    (doseq [[i j] edges]
      (rc3d/draw-line-3d! (nth rotated i) (nth rotated j) wire-color))

    ;; Draw corner spheres for visual flair
    (doseq [v rotated]
      (rc3d/draw-sphere! v 0.1 color))))

(defn draw [{:keys [camera rotation axis axis-name rotation-speed]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3d/begin-mode-3d! camera)

  ;; Draw rotating cube
  (draw-rotating-cube! rotation axis 2.0 colors/red colors/maroon)

  ;; Draw rotation axis
  (let [axis-length 3.0
        axis-end {:x (* (:x axis) axis-length)
                  :y (* (:y axis) axis-length)
                  :z (* (:z axis) axis-length)}]
    (rc3d/draw-line-3d! {:x 0
                         :y 0
                         :z 0} axis-end colors/gold)
    (rc3d/draw-sphere! axis-end 0.15 colors/gold))

  ;; Draw grid
  (rc3d/draw-grid! 10 1.0)

  (rc3d/end-mode-3d!)

  ;; Draw UI
  (rtd/draw-text! "Rotating Cube" 10 10 20 colors/darkgray)
  (rtd/draw-text! (format "Rotation: %.1f°" rotation) 10 35 15 colors/gray)
  (rtd/draw-text! (str "Axis: " axis-name) 10 55 15 colors/blue)
  (rtd/draw-text! (format "Speed: %.0f°/s" rotation-speed) 10 75 15 colors/gray)
  (rtd/draw-text! "Arrows: Change axis | +/-: Speed | R: Reset" 10 (- HEIGHT 25) 15 colors/gray)

  (rtd/draw-fps! (- WIDTH 100) 10)

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

  ;; Change rotation speed
  (swap! game-atom assoc :rotation-speed 120.0)

  ;; Change axis
  (swap! game-atom assoc :axis {:x 1.0
                                :y 1.0
                                :z 0.0} :axis-name "XY")

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
