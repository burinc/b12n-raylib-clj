(ns examples.models.lorenz-attractor
  "Raylib [models] example - Lorenz attractor
   
   Visualize the famous Lorenz strange attractor - a chaotic
   system that produces beautiful butterfly-shaped patterns.
   
   Complexity: ⭐⭐ (2/4)
   
   Controls:
   - 1/2/3: Change parameters (rho)
   - UP/DOWN: Adjust sigma
   - LEFT/RIGHT: Adjust beta
   - SPACE: Toggle animation
   - R: Reset simulation
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

(def WIDTH 800)
(def HEIGHT 450)
(def MAX_POINTS 5000)
(def DT 0.01)
(def SCALE 0.15)

;; Lorenz system parameters
(defn initial-state []
  {:exit? false
   :sigma 10.0 ; Prandtl number
   :rho 28.0 ; Rayleigh number  
   :beta (/ 8.0 3) ; Geometric factor
   :points [{:x 0.1
             :y 0.0
             :z 0.0}]
   :animate? true
   :camera {:position {:x 15.0
                       :y 10.0
                       :z 15.0}
            :target {:x 0.0
                     :y 3.0
                     :z 0.0}
            :up {:x 0.0
                 :y 1.0
                 :z 0.0}
            :fovy 45.0
            :projection rc3d/CAMERA_PERSPECTIVE}})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - Lorenz attractor")
  (rct/set-target-fps! 60))

(defn lorenz-derivatives
  "Calculate derivatives for the Lorenz system"
  [{:keys [x y z]} sigma rho beta]
  {:dx (* sigma (- y x))
   :dy (- (* x (- rho z)) y)
   :dz (- (* x y) (* beta z))})

(defn rk4-step
  "Fourth-order Runge-Kutta integration step"
  [point sigma rho beta dt]
  (let [{:keys [x y z]} point
        k1 (lorenz-derivatives point sigma rho beta)
        p2 {:x (+ x (* 0.5 dt (:dx k1)))
            :y (+ y (* 0.5 dt (:dy k1)))
            :z (+ z (* 0.5 dt (:dz k1)))}
        k2 (lorenz-derivatives p2 sigma rho beta)
        p3 {:x (+ x (* 0.5 dt (:dx k2)))
            :y (+ y (* 0.5 dt (:dy k2)))
            :z (+ z (* 0.5 dt (:dz k2)))}
        k3 (lorenz-derivatives p3 sigma rho beta)
        p4 {:x (+ x (* dt (:dx k3)))
            :y (+ y (* dt (:dy k3)))
            :z (+ z (* dt (:dz k3)))}
        k4 (lorenz-derivatives p4 sigma rho beta)]
    {:x (+ x (* (/ dt 6.0) (+ (:dx k1) (* 2 (:dx k2)) (* 2 (:dx k3)) (:dx k4))))
     :y (+ y (* (/ dt 6.0) (+ (:dy k1) (* 2 (:dy k2)) (* 2 (:dy k3)) (:dy k4))))
     :z (+ z (* (/ dt 6.0) (+ (:dz k1) (* 2 (:dz k2)) (* 2 (:dz k3)) (:dz k4))))}))

(defn handle-input [game]
  (let [dt (rct/get-frame-time)]
    (cond-> game
      (rck/is-key-pressed? (:q enums/keyboard-key))
      (assoc :exit? true)

      (rck/is-key-pressed? (:space enums/keyboard-key))
      (update :animate? not)

      (rck/is-key-pressed? (:r enums/keyboard-key))
      (merge (initial-state))

      ;; Preset rho values
      (rck/is-key-pressed? (:one enums/keyboard-key))
      (-> (assoc :rho 28.0) (assoc :points [{:x 0.1
                                             :y 0.0
                                             :z 0.0}]))

      (rck/is-key-pressed? (:two enums/keyboard-key))
      (-> (assoc :rho 99.96) (assoc :points [{:x 0.1
                                              :y 0.0
                                              :z 0.0}]))

      (rck/is-key-pressed? (:three enums/keyboard-key))
      (-> (assoc :rho 14.0) (assoc :points [{:x 0.1
                                             :y 0.0
                                             :z 0.0}]))

      ;; Adjust sigma
      (rck/is-key-down? (:up enums/keyboard-key))
      (update :sigma + (* 2.0 dt))

      (rck/is-key-down? (:down enums/keyboard-key))
      (update :sigma #(max 1.0 (- % (* 2.0 dt))))

      ;; Adjust beta
      (rck/is-key-down? (:right enums/keyboard-key))
      (update :beta + (* 0.5 dt))

      (rck/is-key-down? (:left enums/keyboard-key))
      (update :beta #(max 0.1 (- % (* 0.5 dt)))))))

(defn update-simulation [{:keys [animate? sigma rho beta points]
                          :as game}]
  (if animate?
    (let [last-point (last points)
          ;; Add multiple points per frame for smoother curves
          new-points (reduce (fn [pts _]
                               (let [p (last pts)
                                     new-p (rk4-step p sigma rho beta DT)]
                                 (conj pts new-p)))
                             points
                             (range 10))
          ;; Keep only last MAX_POINTS
          trimmed (if (> (count new-points) MAX_POINTS)
                    (vec (drop (- (count new-points) MAX_POINTS) new-points))
                    new-points)]
      (assoc game :points trimmed))
    game))

(defn update-camera [game]
  (update game :camera #(rc3d/update-camera % rc3d/CAMERA_ORBITAL)))

(defn tick [game]
  (-> game
      handle-input
      update-simulation
      update-camera))

(defn hsv->rgb [h s v]
  (let [h (double (mod h 360))
        s (double s)
        v (double v)
        c (* v s)
        x (* c (- 1.0 (Math/abs (- (mod (/ h 60.0) 2.0) 1.0))))
        m (- v c)
        [r' g' b'] (cond
                     (< h 60) [c x 0]
                     (< h 120) [x c 0]
                     (< h 180) [0 c x]
                     (< h 240) [0 x c]
                     (< h 300) [x 0 c]
                     :else [c 0 x])]
    {:r (int (* 255 (+ r' m)))
     :g (int (* 255 (+ g' m)))
     :b (int (* 255 (+ b' m)))
     :a 255}))

(defn point->screen [p]
  {:x (* SCALE (:x p))
   :y (* SCALE (:z p)) ; Z becomes Y (height)
   :z (* SCALE (:y p))})

(defn draw-attractor! [points]
  (let [n (count points)]
    (when (> n 1)
      (doseq [i (range (dec n))]
        (let [p1 (point->screen (nth points i))
              p2 (point->screen (nth points (inc i)))
              ;; Color by position in trail (older = darker)
              brightness (/ i n)
              hue (+ 200 (* 160 brightness))
              color (hsv->rgb hue 0.9 brightness)]
          (rc3d/draw-line-3d! p1 p2 color)))
      ;; Draw current point
      (let [head (point->screen (last points))]
        (rc3d/draw-sphere! head 0.2 colors/white)))))

(defn draw [{:keys [camera sigma rho beta points animate?]
             :as game}]
  (rcd/begin-drawing!)
  (rcd/clear-background! {:r 10
                          :g 10
                          :b 20
                          :a 255})

  (rc3d/begin-mode-3d! camera)

  ;; Draw reference grid at y=0
  (rc3d/draw-grid! 20 1.0)

  ;; Draw the attractor
  (draw-attractor! points)

  (rc3d/end-mode-3d!)

  ;; UI
  (rtd/draw-text! "Lorenz Attractor" 10 10 20 colors/white)
  (rtd/draw-text! (format "σ=%.1f  ρ=%.1f  β=%.2f  Points: %d"
                          sigma rho beta (count points))
                  10 35 15 colors/lime)
  (rtd/draw-text! "1-3: Presets | Arrows: Adjust params | SPACE: Pause | R: Reset"
                  10 55 15 colors/gray)
  (when-not animate?
    (rtd/draw-text! "PAUSED" (- WIDTH 80) 10 20 colors/red))

  (rtd/draw-fps! 10 (- HEIGHT 25))
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
  @game-atom
  (swap! game-atom assoc :rho 99.96)
  (swap! game-atom assoc :animate? false))
