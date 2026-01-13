(ns examples.models.solar-system
  "Raylib [models] example - solar system
   
   A simplified solar system with orbiting planets.
   The camera orbits around the scene automatically.
   Based on: raylib/examples/models/models_rlgl_solar_system.c (simplified)
   
   Complexity: ⭐⭐ Intermediate (2/4)
   
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

;; Orbital parameters
(def SUN_RADIUS 4.0)
(def EARTH_RADIUS 0.6)
(def EARTH_ORBIT_RADIUS 8.0)
(def MOON_RADIUS 0.16)
(def MOON_ORBIT_RADIUS 1.5)
(def ROTATION_SPEED 0.2)

(defn make-camera []
  {:position {:x 16.0 :y 16.0 :z 16.0}
   :target {:x 0.0 :y 0.0 :z 0.0}
   :up {:x 0.0 :y 1.0 :z 0.0}
   :fovy 45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)
   :earth-rotation 0.0
   :earth-orbit 0.0
   :moon-rotation 0.0
   :moon-orbit 0.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - solar system")
  (rct/set-target-fps! 60))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn update-rotations [game]
  (-> game
      (update :earth-rotation + (* 5.0 ROTATION_SPEED))
      (update :earth-orbit + (* (/ 365.0 360.0) (* 5.0 ROTATION_SPEED) ROTATION_SPEED))
      (update :moon-rotation + (* 2.0 ROTATION_SPEED))
      (update :moon-orbit + (* 8.0 ROTATION_SPEED))))

(defn update-camera [game]
  (let [time (ext/get-time)
        camera-time (* time 0.3)
        new-x (* (Math/cos camera-time) 20.0)
        new-z (* (Math/sin camera-time) 20.0)]
    (-> game
        (assoc-in [:camera :position :x] new-x)
        (assoc-in [:camera :position :z] new-z))))

(defn tick [game]
  (-> game
      handle-input
      update-rotations
      update-camera))

(defn deg->rad [deg]
  (* deg (/ Math/PI 180.0)))

(defn orbit-position
  "Calculate position on orbit given orbit radius and angle in degrees"
  [orbit-radius angle-deg]
  (let [angle-rad (deg->rad angle-deg)]
    {:x (* orbit-radius (Math/cos angle-rad))
     :y 0.0
     :z (* orbit-radius (Math/sin angle-rad))}))

(defn draw [{:keys [camera earth-orbit moon-orbit]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3d/begin-mode-3d! camera)

  ;; Draw the Sun (gold sphere at origin)
  (rc3d/draw-sphere! {:x 0.0 :y 0.0 :z 0.0} SUN_RADIUS colors/gold)
  (rc3d/draw-sphere-wires! {:x 0.0 :y 0.0 :z 0.0} SUN_RADIUS 16 16 colors/orange)

  ;; Calculate Earth's position on its orbit
  (let [earth-pos (orbit-position EARTH_ORBIT_RADIUS earth-orbit)]
    ;; Draw Earth
    (rc3d/draw-sphere! earth-pos EARTH_RADIUS colors/blue)
    (rc3d/draw-sphere-wires! earth-pos EARTH_RADIUS 16 16 colors/darkblue)

    ;; Calculate Moon's position relative to Earth
    (let [moon-local (orbit-position MOON_ORBIT_RADIUS moon-orbit)
          moon-pos {:x (+ (:x earth-pos) (:x moon-local))
                    :y (:y moon-local)
                    :z (+ (:z earth-pos) (:z moon-local))}]
      ;; Draw Moon
      (rc3d/draw-sphere! moon-pos MOON_RADIUS colors/lightgray)
      (rc3d/draw-sphere-wires! moon-pos MOON_RADIUS 8 8 colors/gray)))

  ;; Draw Earth's orbit path
  (rc3d/draw-circle-3d! {:x 0.0 :y 0.0 :z 0.0}
                        EARTH_ORBIT_RADIUS
                        {:x 1.0 :y 0.0 :z 0.0}
                        90.0
                        (ext/fade colors/red 0.5))

  ;; Draw grid
  (rc3d/draw-grid! 20 1.0)

  (rc3d/end-mode-3d!)

  (rtd/draw-text! "EARTH ORBITING AROUND THE SUN!" 10 40 20 colors/maroon)
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

  ;; Check orbital positions
  (:earth-orbit @game-atom)
  (:moon-orbit @game-atom)

  ;; Test orbit calculation
  (orbit-position 8.0 90.0)

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
