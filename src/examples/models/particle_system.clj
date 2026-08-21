(ns examples.models.particle-system
  "Raylib [models] example - 3D particle system
   
   A simple 3D particle system with gravity, wind, and color effects.
   Particles emit from a central point and fall with physics.
   
   Complexity: ⭐⭐⭐ Intermediate (3/4)
   
   Controls:
   - SPACE: Burst of particles
   - G: Toggle gravity
   - W: Toggle wind
   - R: Reset particles
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
   [raylib.nrepl :as nrepl]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def MAX_PARTICLES 500)
(def PARTICLE_LIFETIME 3.0) ; seconds
(def EMIT_RATE 50) ; particles per second
(def GRAVITY -9.8)
(def WIND_STRENGTH 2.0)

(defn hsv->rgb
  "Convert HSV (h: 0-360, s: 0-1, v: 0-1) to RGB color map"
  [h s v]
  (let [h (double h)
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

(defn make-particle []
  (let [angle (rand (* 2 Math/PI))
        elevation (- (rand Math/PI) (/ Math/PI 2))
        speed (+ 3 (rand 5))
        vx (* speed (Math/cos elevation) (Math/cos angle))
        vy (+ 5 (rand 3)) ; Initial upward velocity
        vz (* speed (Math/cos elevation) (Math/sin angle))]
    {:position {:x 0.0
                :y 0.0
                :z 0.0}
     :velocity {:x vx
                :y vy
                :z vz}
     :lifetime PARTICLE_LIFETIME
     :age 0.0
     :size (+ 0.05 (rand 0.1))
     :hue (rand 360)}))

(defn make-camera []
  {:position {:x 10.0
              :y 8.0
              :z 10.0}
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
   :particles []
   :emit-accumulator 0.0
   :gravity? true
   :wind? false
   :time 0.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - 3D particle system")
  (rct/set-target-fps! 60))

(defn handle-input [{:keys [particles]
                     :as game}]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)

    ;; Burst of particles
    (rck/is-key-pressed? (:space enums/keyboard-key))
    (update :particles #(into % (repeatedly 50 make-particle)))

    (rck/is-key-pressed? (:g enums/keyboard-key))
    (update :gravity? not)

    (rck/is-key-pressed? (:w enums/keyboard-key))
    (update :wind? not)

    (rck/is-key-pressed? (:r enums/keyboard-key))
    (assoc :particles [] :emit-accumulator 0.0)))

(defn update-particle [{:keys [position velocity age lifetime size hue]
                        :as particle}
                       dt gravity? wind? time]
  (let [;; Apply forces
        gravity-force (if gravity? GRAVITY 0.0)
        wind-x (if wind? (* WIND_STRENGTH (Math/sin (* time 0.5))) 0.0)
        wind-z (if wind? (* WIND_STRENGTH (Math/cos (* time 0.7))) 0.0)

        ;; Update velocity
        new-vx (+ (:x velocity) (* wind-x dt))
        new-vy (+ (:y velocity) (* gravity-force dt))
        new-vz (+ (:z velocity) (* wind-z dt))

        ;; Update position
        new-x (+ (:x position) (* new-vx dt))
        new-y (+ (:y position) (* new-vy dt))
        new-z (+ (:z position) (* new-vz dt))

        ;; Bounce off ground
        [final-y final-vy] (if (< new-y 0)
                             [0.0 (* -0.5 new-vy)]
                             [new-y new-vy])

        new-age (+ age dt)]

    (when (< new-age lifetime)
      (assoc particle
             :position {:x new-x
                        :y final-y
                        :z new-z}
             :velocity {:x new-vx
                        :y final-vy
                        :z new-vz}
             :age new-age))))

(defn emit-particles [{:keys [particles emit-accumulator]
                       :as game} dt]
  (let [new-accumulator (+ emit-accumulator (* EMIT_RATE dt))
        particles-to-emit (int new-accumulator)
        remaining (- new-accumulator particles-to-emit)
        new-particles (if (and (< (count particles) MAX_PARTICLES) (pos? particles-to-emit))
                        (let [space-available (- MAX_PARTICLES (count particles))
                              to-add (min particles-to-emit space-available)]
                          (into particles (repeatedly to-add make-particle)))
                        particles)]
    (assoc game
           :particles new-particles
           :emit-accumulator remaining)))

(defn update-particles [{:keys [particles gravity? wind? time]
                         :as game}]
  (let [dt (rct/get-frame-time)
        new-time (+ time dt)
        updated (keep #(update-particle % dt gravity? wind? new-time) particles)]
    (-> game
        (assoc :particles (vec updated))
        (assoc :time new-time)
        (emit-particles dt))))

(defn tick [game]
  (-> game
      handle-input
      update-particles))

(defn particle-color [{:keys [age lifetime hue]}]
  (let [life-ratio (/ age lifetime)
        ;; Fade out as particle ages
        alpha (int (* 255 (- 1 life-ratio)))
        ;; Shift hue over lifetime
        shifted-hue (mod (+ hue (* life-ratio 60)) 360)
        base-color (hsv->rgb shifted-hue 0.8 1.0)]
    (assoc base-color :a alpha)))

(defn draw [{:keys [camera particles gravity? wind? time]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! {:r 20
                          :g 20
                          :b 30
                          :a 255})

  (rc3d/begin-mode-3d! camera)

  ;; Draw emitter
  (rc3d/draw-sphere! {:x 0.0
                      :y 0.0
                      :z 0.0} 0.3 colors/white)

  ;; Draw particles
  (doseq [{:keys [position size]
           :as p} particles]
    (let [color (particle-color p)]
      (rc3d/draw-sphere! position size color)))

  ;; Draw ground plane
  (rc3d/draw-plane! {:x 0.0
                     :y 0.0
                     :z 0.0} {:x 20.0
                              :y 20.0} {:r 40
                                        :g 40
                                        :b 50
                                        :a 255})

  ;; Draw wind indicator if wind is on
  (when wind?
    (let [wind-x (* WIND_STRENGTH (Math/sin (* time 0.5)))
          wind-z (* WIND_STRENGTH (Math/cos (* time 0.7)))]
      (rc3d/draw-line-3d! {:x 0
                           :y 5
                           :z 0}
                          {:x wind-x
                           :y 5
                           :z wind-z}
                          colors/skyblue)))

  ;; Draw grid
  (rc3d/draw-grid! 20 1.0)

  (rc3d/end-mode-3d!)

  ;; Draw UI
  (rtd/draw-text! "3D Particle System" 10 10 20 colors/white)
  (rtd/draw-text! (str "Particles: " (count particles) "/" MAX_PARTICLES) 10 35 15 colors/lightgray)
  (rtd/draw-text! (str "Gravity: " (if gravity? "ON" "OFF")) 10 55 15
                  (if gravity? colors/green colors/red))
  (rtd/draw-text! (str "Wind: " (if wind? "ON" "OFF")) 10 75 15
                  (if wind? colors/skyblue colors/red))
  (rtd/draw-text! "SPACE: Burst | G: Gravity | W: Wind | R: Reset" 10 (- HEIGHT 25) 15 colors/gray)

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

  ;; Check particle count
  (count (:particles @game-atom))

  ;; Add burst
  (swap! game-atom update :particles #(into % (repeatedly 100 make-particle)))

  ;; Toggle effects
  (swap! game-atom update :gravity? not)
  (swap! game-atom update :wind? not)

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
