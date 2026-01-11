(ns examples.camera-fps
  "Raylib [core] example - 3d camera fps
   
   Advanced FPS camera with physics-based movement, head bobbing,
   crouching, and strafing mechanics.
   Based on: raylib/examples/core/core_3d_camera_fps.c
   
   Complexity: ⭐⭐⭐ Intermediate
   
   Controls:
   - W/A/S/D: Move forward/left/backward/right
   - Mouse: Look around
   - Space: Jump
   - Left-Ctrl: Crouch
   - F1: Toggle debug stats
   - ESC: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.core.camera3d :as rc3]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)

;; Movement constants
(def GRAVITY 32.0)
(def MAX_SPEED 20.0)
(def CROUCH_SPEED 5.0)
(def JUMP_FORCE 12.0)
(def MAX_ACCEL 150.0)
(def FRICTION 0.86)
(def AIR_DRAG 0.98)
(def CONTROL 15.0)
(def CROUCH_HEIGHT 0.0)
(def STAND_HEIGHT 1.0)
(def BOTTOM_HEIGHT 0.5)

(def SENSITIVITY {:x 0.001 :y 0.001})
(def PI 3.14159265358979323846)

;; Vector math helpers
(defn v3+ [a b]
  {:x (+ (:x a) (:x b))
   :y (+ (:y a) (:y b))
   :z (+ (:z a) (:z b))})

(defn v3-scale [v s]
  {:x (* (:x v) s)
   :y (* (:y v) s)
   :z (* (:z v) s)})

(defn v3-length [v]
  (Math/sqrt (+ (* (:x v) (:x v))
                (* (:y v) (:y v))
                (* (:z v) (:z v)))))

(defn v3-normalize [v]
  (let [len (v3-length v)]
    (if (> len 0)
      (v3-scale v (/ 1.0 len))
      v)))

(defn v3-dot [a b]
  (+ (* (:x a) (:x b))
     (* (:y a) (:y b))
     (* (:z a) (:z b))))

(defn v3-cross [a b]
  {:x (- (* (:y a) (:z b)) (* (:z a) (:y b)))
   :y (- (* (:z a) (:x b)) (* (:x a) (:z b)))
   :z (- (* (:x a) (:y b)) (* (:y a) (:x b)))})

(defn v3-negate [v]
  {:x (- (:x v)) :y (- (:y v)) :z (- (:z v))})

(defn v3-lerp [a b t]
  {:x (+ (:x a) (* (- (:x b) (:x a)) t))
   :y (+ (:y a) (* (- (:y b) (:y a)) t))
   :z (+ (:z a) (* (- (:z b) (:z a)) t))})

(defn v3-angle [a b]
  (let [dot (v3-dot a b)
        len-a (v3-length a)
        len-b (v3-length b)]
    (if (and (> len-a 0) (> len-b 0))
      (Math/acos (max -1.0 (min 1.0 (/ dot (* len-a len-b)))))
      0.0)))

(defn v3-rotate-by-axis-angle [v axis angle]
  (let [cos-a (Math/cos angle)
        sin-a (Math/sin angle)
        dot (v3-dot axis v)
        cross (v3-cross axis v)]
    {:x (+ (* (:x v) cos-a) (* (:x cross) sin-a) (* (:x axis) dot (- 1 cos-a)))
     :y (+ (* (:y v) cos-a) (* (:y cross) sin-a) (* (:y axis) dot (- 1 cos-a)))
     :z (+ (* (:z v) cos-a) (* (:z cross) sin-a) (* (:z axis) dot (- 1 cos-a)))}))

(defn v2-length [v]
  (Math/sqrt (+ (* (:x v) (:x v)) (* (:y v) (:y v)))))

(defn lerp [a b t]
  (+ a (* (- b a) t)))

(defn clamp [v min-v max-v]
  (max min-v (min max-v v)))

;; Initial state
(defn initial-state []
  {:player {:position {:x 0.0 :y 0.0 :z 0.0}
            :velocity {:x 0.0 :y 0.0 :z 0.0}
            :dir {:x 0.0 :y 0.0 :z 0.0}
            :grounded? true}
   :camera {:position {:x 0.0 :y 1.5 :z 0.0}
            :target {:x 0.0 :y 1.5 :z -1.0}
            :up {:x 0.0 :y 1.0 :z 0.0}
            :fovy 60.0
            :projection rc3/CAMERA_PERSPECTIVE}
   :look-rotation {:x 0.0 :y 0.0}
   :head-timer 0.0
   :walk-lerp 0.0
   :head-lerp STAND_HEIGHT
   :lean {:x 0.0 :y 0.0}})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [core] example - 3d camera fps")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  (rc3/disable-cursor!))

(defn update-body [{:keys [player look-rotation] :as game} side forward jump-pressed? crouch?]
  (let [delta (rct/get-frame-time)
        {:keys [position velocity dir grounded?]} player
        rot (:x look-rotation)

        ;; Apply gravity if not grounded
        new-vel-y (if grounded?
                    (:y velocity)
                    (- (:y velocity) (* GRAVITY delta)))

        ;; Handle jump
        [new-vel-y grounded?] (if (and grounded? jump-pressed?)
                                [JUMP_FORCE false]
                                [new-vel-y grounded?])

        ;; Calculate movement directions
        front {:x (Math/sin rot) :y 0.0 :z (Math/cos rot)}
        right {:x (Math/cos (- rot)) :y 0.0 :z (Math/sin (- rot))}

        ;; Desired direction based on input
        desired-dir {:x (+ (* side (:x right)) (* (- forward) (:x front)))
                     :y 0.0
                     :z (+ (* side (:z right)) (* (- forward) (:z front)))}

        ;; Smooth direction change
        new-dir (v3-lerp dir desired-dir (* CONTROL delta))

        ;; Apply friction/drag to horizontal velocity
        decel (if grounded? FRICTION AIR_DRAG)
        hvel {:x (* (:x velocity) decel) :y 0.0 :z (* (:z velocity) decel)}

        ;; Stop very slow movement
        hvel-length (v3-length hvel)
        hvel (if (< hvel-length (* MAX_SPEED 0.01))
               {:x 0.0 :y 0.0 :z 0.0}
               hvel)

        ;; Calculate acceleration (strafing mechanic)
        speed (v3-dot hvel new-dir)
        max-speed (if crouch? CROUCH_SPEED MAX_SPEED)
        accel (clamp (- max-speed speed) 0.0 (* MAX_ACCEL delta))

        ;; Apply acceleration
        new-vel {:x (+ (:x hvel) (* (:x new-dir) accel))
                 :y new-vel-y
                 :z (+ (:z hvel) (* (:z new-dir) accel))}

        ;; Update position
        new-pos {:x (+ (:x position) (* (:x new-vel) delta))
                 :y (+ (:y position) (* (:y new-vel) delta))
                 :z (+ (:z position) (* (:z new-vel) delta))}

        ;; Floor collision
        [new-pos new-vel grounded?] (if (<= (:y new-pos) 0.0)
                                      [{:x (:x new-pos) :y 0.0 :z (:z new-pos)}
                                       {:x (:x new-vel) :y 0.0 :z (:z new-vel)}
                                       true]
                                      [new-pos new-vel grounded?])]

    (assoc game :player {:position new-pos
                         :velocity new-vel
                         :dir new-dir
                         :grounded? grounded?})))

(defn update-camera-fps [{:keys [player look-rotation head-timer walk-lerp lean] :as game}]
  (let [up {:x 0.0 :y 1.0 :z 0.0}
        target-offset {:x 0.0 :y 0.0 :z -1.0}

        ;; Yaw rotation (left/right)
        yaw (v3-rotate-by-axis-angle target-offset up (:x look-rotation))

        ;; Clamp pitch angle
        max-angle-up (- (v3-angle up yaw) 0.001)
        max-angle-down (+ (- (v3-angle (v3-negate up) yaw)) 0.001)
        clamped-y (clamp (- (:y look-rotation)) max-angle-down max-angle-up)

        ;; Right vector for pitch rotation
        right (v3-normalize (v3-cross yaw up))

        ;; Pitch rotation with lean
        pitch-angle (clamp (- clamped-y (:y lean))
                           (+ (- (/ PI 2)) 0.0001)
                           (- (/ PI 2) 0.0001))
        pitch (v3-rotate-by-axis-angle yaw right pitch-angle)

        ;; Head animation
        head-sin (Math/sin (* head-timer PI))
        head-cos (Math/cos (* head-timer PI))
        step-rotation 0.01
        new-up (v3-rotate-by-axis-angle up pitch (+ (* head-sin step-rotation) (:x lean)))

        ;; Camera bob
        bob-side 0.1
        bob-up 0.15
        bobbing {:x (* (:x right) head-sin bob-side)
                 :y (Math/abs (* head-cos bob-up))
                 :z (* (:z right) head-sin bob-side)}

        ;; Camera position with bob
        base-pos (:position (:camera game))
        cam-pos (v3+ base-pos (v3-scale bobbing walk-lerp))
        cam-target (v3+ cam-pos pitch)]

    (-> game
        (assoc-in [:camera :position] cam-pos)
        (assoc-in [:camera :target] cam-target)
        (assoc-in [:camera :up] new-up))))

(defn handle-input [game]
  (let [delta (rct/get-frame-time)
        mouse-delta (rcm/get-mouse-delta)
        look-rotation (:look-rotation game)

        ;; Update look rotation from mouse
        new-look {:x (- (:x look-rotation) (* (:x mouse-delta) (:x SENSITIVITY)))
                  :y (+ (:y look-rotation) (* (:y mouse-delta) (:y SENSITIVITY)))}

        ;; Get movement input
        side (- (if (rck/is-key-down? (:d enums/keyboard-key)) 1 0)
                (if (rck/is-key-down? (:a enums/keyboard-key)) 1 0))
        forward (- (if (rck/is-key-down? (:w enums/keyboard-key)) 1 0)
                   (if (rck/is-key-down? (:s enums/keyboard-key)) 1 0))
        crouch? (rck/is-key-down? (:left-control enums/keyboard-key))
        jump-pressed? (rck/is-key-pressed? (:space enums/keyboard-key))

        ;; Update head animation
        {:keys [player head-timer walk-lerp head-lerp lean]} game
        grounded? (:grounded? player)
        moving? (and grounded? (or (not= forward 0) (not= side 0)))

        new-head-timer (if moving? (+ head-timer (* delta 3.0)) head-timer)
        new-walk-lerp (lerp walk-lerp (if moving? 1.0 0.0) (* 10.0 delta))
        new-fovy (lerp (:fovy (:camera game)) (if moving? 55.0 60.0) (* 5.0 delta))
        new-head-lerp (lerp head-lerp (if crouch? CROUCH_HEIGHT STAND_HEIGHT) (* 20.0 delta))

        ;; Update lean
        new-lean {:x (lerp (:x lean) (* side 0.02) (* 10.0 delta))
                  :y (lerp (:y lean) (* forward 0.015) (* 10.0 delta))}]

    (-> game
        (assoc :look-rotation new-look)
        (assoc :head-timer new-head-timer)
        (assoc :walk-lerp new-walk-lerp)
        (assoc :head-lerp new-head-lerp)
        (assoc :lean new-lean)
        (assoc-in [:camera :fovy] new-fovy)
        (update-body side forward jump-pressed? crouch?)
        ;; Update camera base position from player
        (as-> g
              (let [player-pos (:position (:player g))]
                (assoc-in g [:camera :position]
                          {:x (:x player-pos)
                           :y (+ (:y player-pos) BOTTOM_HEIGHT (:head-lerp g))
                           :z (:z player-pos)})))
        update-camera-fps)))

(defn tick [game]
  (debug-stats/update!)
  (handle-input game))

(defn draw-level []
  (let [floor-extent 25
        tile-size 5.0
        tile-color1 {:r 150 :g 200 :b 200 :a 255}]

    ;; Floor tiles (checkerboard)
    (doseq [y (range (- floor-extent) floor-extent)
            x (range (- floor-extent) floor-extent)]
      (when (or (and (odd? y) (odd? x))
                (and (even? y) (even? x)))
        (rc3/draw-plane! {:x (* x tile-size) :y 0.0 :z (* y tile-size)}
                         {:x tile-size :y tile-size}
                         (if (and (odd? y) (odd? x)) tile-color1 colors/lightgray))))

    ;; Tower cubes at corners
    (let [tower-size {:x 16.0 :y 32.0 :z 16.0}
          tower-color {:r 150 :g 200 :b 200 :a 255}]
      (doseq [[tx tz] [[16.0 16.0] [-16.0 16.0] [-16.0 -16.0] [16.0 -16.0]]]
        (let [pos {:x tx :y 16.0 :z tz}]
          (rc3/draw-cube-v! pos tower-size tower-color)
          (rc3/draw-cube-wires-v! pos tower-size colors/darkblue))))

    ;; Red sun
    (rc3/draw-sphere! {:x 300.0 :y 300.0 :z 0.0} 100.0 colors/red)))

(defn draw [{:keys [camera player]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3/begin-mode-3d! camera)
  (draw-level)
  (rc3/end-mode-3d!)

  ;; Info box
  (rsb/draw-rectangle! 5 5 330 75 (ext/fade colors/skyblue 0.5))
  (ext/draw-rectangle-lines! 5 5 330 75 colors/blue)

  (rtd/draw-text! "Camera controls:" 15 15 10 colors/black)
  (rtd/draw-text! "- Move keys: W, A, S, D, Space, Left-Ctrl" 15 30 10 colors/black)
  (rtd/draw-text! "- Look around: mouse" 15 45 10 colors/black)
  (let [vel (:velocity player)
        vel-len (v2-length {:x (:x vel) :y (:z vel)})]
    (rtd/draw-text! (format "- Velocity Len: (%06.3f)" vel-len) 15 60 10 colors/black))

  ;; Draw debug stats overlay
  (debug-stats/draw!)

  (rcd/end-drawing!))

(defn cleanup []
  (rc3/enable-cursor!))

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (let [game (tick @game-atom)]
        (reset! game-atom game)
        (draw game)
        (recur))))
  (cleanup)
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  ;; For REPL development
  @game-atom

  ;; Check player state
  (:player @game-atom)

  ;; Check camera
  (:camera @game-atom)
  ;;
  )
