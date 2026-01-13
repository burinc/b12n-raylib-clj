(ns examples.models.bouncing-spheres
  "Raylib [models] example - bouncing spheres
   
   Multiple spheres bouncing in a 3D box with simple physics.
   Demonstrates 3D collision detection and sphere rendering.
   
   Complexity: ⭐⭐ Intermediate (2/4)
   
   Controls:
   - SPACE: Add more spheres
   - R: Reset spheres
   - G: Toggle gravity
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
(def BOX_SIZE 10.0)
(def HALF_BOX (/ BOX_SIZE 2.0))
(def GRAVITY -9.8)
(def BOUNCE_DAMPING 0.8)
(def MAX_SPHERES 50)

;; Colors for spheres
(def sphere-colors
  [colors/red colors/green colors/blue colors/yellow
   colors/orange colors/purple colors/pink colors/skyblue])

(defn random-velocity []
  {:x (- (rand 10) 5)
   :y (rand 10)
   :z (- (rand 10) 5)})

(defn make-sphere []
  {:position {:x (- (rand BOX_SIZE) HALF_BOX)
              :y (+ (rand 5) 2)
              :z (- (rand BOX_SIZE) HALF_BOX)}
   :velocity (random-velocity)
   :radius (+ 0.3 (rand 0.4))
   :color (rand-nth sphere-colors)})

(defn make-camera []
  {:position {:x 15.0
              :y 15.0
              :z 15.0}
   :target {:x 0.0
            :y 2.0
            :z 0.0}
   :up {:x 0.0
        :y 1.0
        :z 0.0}
   :fovy 45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)
   :spheres (vec (repeatedly 10 make-sphere))
   :gravity? true})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - bouncing spheres")
  (rct/set-target-fps! 60))

(defn handle-input [{:keys [spheres]
                     :as game}]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)

    (and (rck/is-key-pressed? (:space enums/keyboard-key))
         (< (count spheres) MAX_SPHERES))
    (update :spheres conj (make-sphere))

    (rck/is-key-pressed? (:r enums/keyboard-key))
    (assoc :spheres (vec (repeatedly 10 make-sphere)))

    (rck/is-key-pressed? (:g enums/keyboard-key))
    (update :gravity? not)))

(defn clamp [v min-v max-v]
  (max min-v (min max-v v)))

(defn update-sphere [{:keys [position velocity radius]
                      :as sphere} dt gravity?]
  (let [;; Apply gravity
        vy (if gravity?
             (+ (:y velocity) (* GRAVITY dt))
             (:y velocity))

        ;; Update position
        new-x (+ (:x position) (* (:x velocity) dt))
        new-y (+ (:y position) (* vy dt))
        new-z (+ (:z position) (* (:z velocity) dt))

        ;; Bounce off walls
        [final-x vx] (cond
                       (< new-x (+ (- HALF_BOX) radius))
                       [(+ (- HALF_BOX) radius) (- (* BOUNCE_DAMPING (:x velocity)))]
                       (> new-x (- HALF_BOX radius))
                       [(- HALF_BOX radius) (- (* BOUNCE_DAMPING (:x velocity)))]
                       :else [new-x (:x velocity)])

        [final-y final-vy] (cond
                             (< new-y radius)
                             [radius (- (* BOUNCE_DAMPING vy))]
                             (> new-y (- BOX_SIZE radius))
                             [(- BOX_SIZE radius) (- (* BOUNCE_DAMPING vy))]
                             :else [new-y vy])

        [final-z vz] (cond
                       (< new-z (+ (- HALF_BOX) radius))
                       [(+ (- HALF_BOX) radius) (- (* BOUNCE_DAMPING (:z velocity)))]
                       (> new-z (- HALF_BOX radius))
                       [(- HALF_BOX radius) (- (* BOUNCE_DAMPING (:z velocity)))]
                       :else [new-z (:z velocity)])]

    (assoc sphere
           :position {:x final-x
                      :y final-y
                      :z final-z}
           :velocity {:x vx
                      :y final-vy
                      :z vz})))

(defn update-spheres [{:keys [spheres gravity?]
                       :as game}]
  (let [dt (rct/get-frame-time)]
    (assoc game :spheres (mapv #(update-sphere % dt gravity?) spheres))))

(defn tick [game]
  (-> game
      handle-input
      update-spheres))

(defn draw-bounding-box []
  ;; Draw the wireframe box
  (let [half HALF_BOX]
    ;; Bottom face
    (rc3d/draw-line-3d! {:x (- half)
                         :y 0
                         :z (- half)} {:x half
                                       :y 0
                                       :z (- half)} colors/darkgray)
    (rc3d/draw-line-3d! {:x half
                         :y 0
                         :z (- half)} {:x half
                                       :y 0
                                       :z half} colors/darkgray)
    (rc3d/draw-line-3d! {:x half
                         :y 0
                         :z half} {:x (- half)
                                   :y 0
                                   :z half} colors/darkgray)
    (rc3d/draw-line-3d! {:x (- half)
                         :y 0
                         :z half} {:x (- half)
                                   :y 0
                                   :z (- half)} colors/darkgray)

    ;; Top face
    (rc3d/draw-line-3d! {:x (- half)
                         :y BOX_SIZE
                         :z (- half)} {:x half
                                       :y BOX_SIZE
                                       :z (- half)} colors/darkgray)
    (rc3d/draw-line-3d! {:x half
                         :y BOX_SIZE
                         :z (- half)} {:x half
                                       :y BOX_SIZE
                                       :z half} colors/darkgray)
    (rc3d/draw-line-3d! {:x half
                         :y BOX_SIZE
                         :z half} {:x (- half)
                                   :y BOX_SIZE
                                   :z half} colors/darkgray)
    (rc3d/draw-line-3d! {:x (- half)
                         :y BOX_SIZE
                         :z half} {:x (- half)
                                   :y BOX_SIZE
                                   :z (- half)} colors/darkgray)

    ;; Vertical edges
    (rc3d/draw-line-3d! {:x (- half)
                         :y 0
                         :z (- half)} {:x (- half)
                                       :y BOX_SIZE
                                       :z (- half)} colors/darkgray)
    (rc3d/draw-line-3d! {:x half
                         :y 0
                         :z (- half)} {:x half
                                       :y BOX_SIZE
                                       :z (- half)} colors/darkgray)
    (rc3d/draw-line-3d! {:x half
                         :y 0
                         :z half} {:x half
                                   :y BOX_SIZE
                                   :z half} colors/darkgray)
    (rc3d/draw-line-3d! {:x (- half)
                         :y 0
                         :z half} {:x (- half)
                                   :y BOX_SIZE
                                   :z half} colors/darkgray)))

(defn draw [{:keys [camera spheres gravity?]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3d/begin-mode-3d! camera)

  ;; Draw floor
  (rc3d/draw-plane! {:x 0.0
                     :y 0.0
                     :z 0.0} {:x BOX_SIZE
                              :y BOX_SIZE} colors/lightgray)

  ;; Draw bounding box
  (draw-bounding-box)

  ;; Draw all spheres
  (doseq [{:keys [position radius color]} spheres]
    (rc3d/draw-sphere! position radius color))

  ;; Draw grid
  (rc3d/draw-grid! 10 1.0)

  (rc3d/end-mode-3d!)

  ;; Draw UI
  (rtd/draw-text! "Bouncing Spheres" 10 10 20 colors/darkgray)
  (rtd/draw-text! (str "Spheres: " (count spheres) "/" MAX_SPHERES) 10 35 15 colors/gray)
  (rtd/draw-text! (str "Gravity: " (if gravity? "ON" "OFF")) 10 55 15
                  (if gravity? colors/green colors/red))
  (rtd/draw-text! "SPACE: Add | R: Reset | G: Toggle gravity" 10 (- HEIGHT 25) 15 colors/gray)

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

  ;; Check sphere count
  (count (:spheres @game-atom))

  ;; Add a sphere
  (swap! game-atom update :spheres conj (make-sphere))

  ;; Toggle gravity
  (swap! game-atom update :gravity? not)

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
