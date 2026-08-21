(ns examples.models.yaw-pitch-roll
  "Raylib [models] example - yaw pitch roll
   
   Demonstrates airplane-style rotation with yaw, pitch, and roll.
   Uses a simple wireframe airplane model built from primitives.
   
   Complexity: ⭐⭐ Beginner-Intermediate (2/4)
   
   Controls:
   - UP/DOWN: Pitch (nose up/down)
   - LEFT/RIGHT: Roll (bank left/right)
   - A/S: Yaw (turn left/right)
   - R: Reset orientation
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.camera3d :as rc3d]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def DEG2RAD (/ Math/PI 180.0))

;; Simple rotation matrix operations
(defn rotate-x [angle]
  (let [c (Math/cos angle)
        s (Math/sin angle)]
    [[1 0 0]
     [0 c (- s)]
     [0 s c]]))

(defn rotate-y [angle]
  (let [c (Math/cos angle)
        s (Math/sin angle)]
    [[c 0 s]
     [0 1 0]
     [(- s) 0 c]]))

(defn rotate-z [angle]
  (let [c (Math/cos angle)
        s (Math/sin angle)]
    [[c (- s) 0]
     [s c 0]
     [0 0 1]]))

(defn mat-mult [m1 m2]
  (vec (for [i (range 3)]
         (vec (for [j (range 3)]
                (reduce + (map * (m1 i) (map #(nth % j) m2))))))))

(defn transform-point [matrix [x y z]]
  (let [[[m00 m01 m02] [m10 m11 m12] [m20 m21 m22]] matrix]
    [(+ (* m00 x) (* m01 y) (* m02 z))
     (+ (* m10 x) (* m11 y) (* m12 z))
     (+ (* m20 x) (* m21 y) (* m22 z))]))

;; Airplane wireframe vertices (simple stylized plane)
(def airplane-vertices
  {:fuselage [;; Main body
              [0 0 -3] [0 0 3] ; nose to tail
              ;; Cockpit outline
              [0 0.3 -1.5] [0 0.5 -0.5] [0 0.3 0.5]]
   :wings [;; Left wing
           [0 0 0] [-4 0 0.5]
           [-4 0 0.5] [-4 0 1]
           [-4 0 1] [0 0 1]
           ;; Right wing  
           [0 0 0] [4 0 0.5]
           [4 0 0.5] [4 0 1]
           [4 0 1] [0 0 1]]
   :tail-h [;; Horizontal stabilizer
            [0 0 2.5] [-1.5 0 3]
            [-1.5 0 3] [0 0 3]
            [0 0 2.5] [1.5 0 3]
            [1.5 0 3] [0 0 3]]
   :tail-v [;; Vertical stabilizer
            [0 0 2.5] [0 1 3]
            [0 1 3] [0 0 3]]})

(defn initial-state []
  {:exit? false
   :pitch 0.0 ; rotation around X axis
   :roll 0.0 ; rotation around Z axis
   :yaw 0.0 ; rotation around Y axis
   :camera {:position {:x 0
                       :y 10
                       :z -20}
            :target {:x 0
                     :y 0
                     :z 0}
            :up {:x 0
                 :y 1
                 :z 0}
            :fovy 45.0
            :projection rc3d/CAMERA_PERSPECTIVE}})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - yaw pitch roll")
  (rct/set-target-fps! 60))

(defn smooth-return [value speed]
  "Smoothly return value toward 0"
  (cond
    (> value speed) (- value speed)
    (< value (- speed)) (+ value speed)
    :else 0.0))

(defn handle-input [{:keys [pitch roll yaw]
                     :as game}]
  (let [;; Pitch controls (UP/DOWN)
        pitch-input (cond
                      (rck/is-key-down? (:down enums/keyboard-key)) 0.6
                      (rck/is-key-down? (:up enums/keyboard-key)) -0.6
                      :else nil)
        new-pitch (if pitch-input
                    (+ pitch pitch-input)
                    (smooth-return pitch 0.3))

        ;; Roll controls (LEFT/RIGHT)
        roll-input (cond
                     (rck/is-key-down? (:left enums/keyboard-key)) -1.0
                     (rck/is-key-down? (:right enums/keyboard-key)) 1.0
                     :else nil)
        new-roll (if roll-input
                   (+ roll roll-input)
                   (smooth-return roll 0.5))

        ;; Yaw controls (A/S)
        yaw-input (cond
                    (rck/is-key-down? (:a enums/keyboard-key)) 1.0
                    (rck/is-key-down? (:s enums/keyboard-key)) -1.0
                    :else nil)
        new-yaw (if yaw-input
                  (+ yaw yaw-input)
                  (smooth-return yaw 0.5))]

    (cond-> game
      true (assoc :pitch new-pitch :roll new-roll :yaw new-yaw)

      (rck/is-key-pressed? (:r enums/keyboard-key))
      (assoc :pitch 0.0 :roll 0.0 :yaw 0.0)

      (rck/is-key-pressed? (:q enums/keyboard-key))
      (assoc :exit? true))))

(defn tick [game]
  (handle-input game))

(defn make-rotation-matrix [pitch yaw roll]
  "Create combined rotation matrix for yaw, pitch, roll (in that order)"
  (let [rx (rotate-x (* pitch DEG2RAD))
        ry (rotate-y (* yaw DEG2RAD))
        rz (rotate-z (* roll DEG2RAD))]
    (mat-mult rz (mat-mult rx ry))))

(defn draw-airplane! [matrix color]
  "Draw wireframe airplane with given transformation matrix"
  (let [transform (fn [[x y z]]
                    (let [[tx ty tz] (transform-point matrix [x y z])]
                      {:x tx
                       :y ty
                       :z tz}))]
    ;; Draw fuselage
    (doseq [[start end] (partition 2 1 (:fuselage airplane-vertices))]
      (rc3d/draw-line-3d! (transform start) (transform end) color))

    ;; Draw wings
    (doseq [[start end] (partition 2 (:wings airplane-vertices))]
      (rc3d/draw-line-3d! (transform start) (transform end) colors/blue))

    ;; Draw horizontal tail
    (doseq [[start end] (partition 2 (:tail-h airplane-vertices))]
      (rc3d/draw-line-3d! (transform start) (transform end) colors/green))

    ;; Draw vertical tail
    (doseq [[start end] (partition 2 (:tail-v airplane-vertices))]
      (rc3d/draw-line-3d! (transform start) (transform end) colors/red))

    ;; Draw nose indicator
    (let [nose (transform [0 0 -3])
          nose-tip (transform [0 0 -4])]
      (rc3d/draw-line-3d! nose nose-tip colors/yellow))))

(defn draw-axes! [matrix size]
  "Draw rotated coordinate axes"
  (let [origin [0 0 0]
        transform (fn [[x y z]]
                    (let [[tx ty tz] (transform-point matrix [x y z])]
                      {:x tx
                       :y ty
                       :z tz}))]
    ;; X axis (red)
    (rc3d/draw-line-3d! (transform origin) (transform [size 0 0]) colors/red)
    ;; Y axis (green)  
    (rc3d/draw-line-3d! (transform origin) (transform [0 size 0]) colors/green)
    ;; Z axis (blue)
    (rc3d/draw-line-3d! (transform origin) (transform [0 0 size]) colors/blue)))

(defn draw [{:keys [camera pitch roll yaw]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (let [matrix (make-rotation-matrix pitch yaw roll)]
    (rc3d/begin-mode-3d! camera)

    ;; Draw grid
    (rc3d/draw-grid! 10 2.0)

    ;; Draw airplane
    (draw-airplane! matrix colors/darkgray)

    ;; Draw local axes on airplane
    (draw-axes! matrix 2.0)

    (rc3d/end-mode-3d!))

  ;; Draw info panel
  (rsb/draw-rectangle! 30 370 260 70 {:r 0
                                      :g 228
                                      :b 48
                                      :a 128})
  (rsb/draw-rectangle-lines! 30 370 260 70 colors/darkgreen)
  (rtd/draw-text! "Pitch: UP / DOWN" 40 380 10 colors/darkgray)
  (rtd/draw-text! "Roll: LEFT / RIGHT" 40 395 10 colors/darkgray)
  (rtd/draw-text! "Yaw: A / S" 40 410 10 colors/darkgray)
  (rtd/draw-text! "Reset: R | Quit: Q" 40 425 10 colors/darkgray)

  ;; Draw rotation values
  (rtd/draw-text! (format "Pitch: %.1f°" pitch) 10 10 20 colors/red)
  (rtd/draw-text! (format "Roll: %.1f°" roll) 10 35 20 colors/blue)
  (rtd/draw-text! (format "Yaw: %.1f°" yaw) 10 60 20 colors/green)

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
  ;; REPL development
  @game-atom

  ;; Reset rotation
  (swap! game-atom assoc :pitch 0 :roll 0 :yaw 0)

  ;; Set specific rotation
  (swap! game-atom assoc :pitch 45 :roll 30 :yaw 90)
  ;;
  )
