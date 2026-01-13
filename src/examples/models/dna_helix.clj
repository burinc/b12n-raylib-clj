(ns examples.models.dna-helix
  "Raylib [models] example - DNA helix visualization
   
   A 3D double helix structure visualization with animated rotation.
   Demonstrates parametric 3D curves and sphere placement.
   
   Complexity: ⭐⭐ Intermediate (2/4)
   
   Controls:
   - UP/DOWN: Adjust helix turns
   - LEFT/RIGHT: Adjust helix radius
   - SPACE: Toggle rotation
   - R: Reset parameters
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
(def PI Math/PI)

(defn make-camera []
  {:position {:x 12.0 :y 8.0 :z 12.0}
   :target {:x 0.0 :y 0.0 :z 0.0}
   :up {:x 0.0 :y 1.0 :z 0.0}
   :fovy 45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)
   :rotation 0.0
   :rotating? true
   :helix-turns 3.0
   :helix-radius 2.0
   :helix-height 8.0
   :points-per-turn 20})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - DNA helix")
  (rct/set-target-fps! 60))

(defn handle-input [{:keys [helix-turns helix-radius] :as game}]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)

    (rck/is-key-pressed? (:space enums/keyboard-key))
    (update :rotating? not)

    (rck/is-key-pressed? (:r enums/keyboard-key))
    (merge (select-keys (initial-state) [:helix-turns :helix-radius :rotation]))

    (rck/is-key-pressed? (:up enums/keyboard-key))
    (update :helix-turns #(min 6.0 (+ % 0.5)))

    (rck/is-key-pressed? (:down enums/keyboard-key))
    (update :helix-turns #(max 1.0 (- % 0.5)))

    (rck/is-key-pressed? (:right enums/keyboard-key))
    (update :helix-radius #(min 4.0 (+ % 0.2)))

    (rck/is-key-pressed? (:left enums/keyboard-key))
    (update :helix-radius #(max 1.0 (- % 0.2)))))

(defn update-rotation [{:keys [rotating?] :as game}]
  (if rotating?
    (let [dt (rct/get-frame-time)]
      (update game :rotation #(mod (+ % (* 30.0 dt)) 360.0)))
    game))

(defn tick [game]
  (-> game
      handle-input
      update-rotation))

(defn helix-point
  "Generate a point on a helix given parameters"
  [t radius height turns offset-angle]
  (let [angle (+ (* t turns 2 PI) offset-angle)
        y (- (* t height) (/ height 2))]
    {:x (* radius (Math/cos angle))
     :y y
     :z (* radius (Math/sin angle))}))

(defn rotate-y
  "Rotate a point around Y axis"
  [{:keys [x y z]} angle-deg]
  (let [rad (Math/toRadians angle-deg)
        cos-a (Math/cos rad)
        sin-a (Math/sin rad)]
    {:x (- (* x cos-a) (* z sin-a))
     :y y
     :z (+ (* x sin-a) (* z cos-a))}))

(defn draw-helix-strand!
  "Draw one strand of the helix"
  [num-points radius height turns rotation offset color]
  (let [points (for [i (range num-points)]
                 (let [t (/ i (dec num-points))
                       p (helix-point t radius height turns offset)]
                   (rotate-y p rotation)))]
    ;; Draw spheres at each point
    (doseq [p points]
      (rc3d/draw-sphere! p 0.2 color))
    ;; Draw connecting lines
    (doseq [[p1 p2] (partition 2 1 points)]
      (rc3d/draw-line-3d! p1 p2 color))))

(defn draw-base-pairs!
  "Draw the connecting 'rungs' between helix strands"
  [num-pairs radius height turns rotation]
  (doseq [i (range num-pairs)]
    (let [t (/ i (dec num-pairs))
          p1 (rotate-y (helix-point t radius height turns 0) rotation)
          p2 (rotate-y (helix-point t radius height turns PI) rotation)
          ;; Draw the base pair as two colored segments meeting in middle
          mid {:x (/ (+ (:x p1) (:x p2)) 2)
               :y (/ (+ (:y p1) (:y p2)) 2)
               :z (/ (+ (:z p1) (:z p2)) 2)}]
      ;; Adenine-Thymine or Guanine-Cytosine pairs (alternating colors)
      (if (even? i)
        (do
          (rc3d/draw-line-3d! p1 mid colors/red) ; Adenine
          (rc3d/draw-line-3d! mid p2 colors/green)) ; Thymine
        (do
          (rc3d/draw-line-3d! p1 mid colors/blue) ; Guanine
          (rc3d/draw-line-3d! mid p2 colors/yellow))) ; Cytosine
      ;; Draw small sphere at connection points
      (rc3d/draw-sphere! mid 0.08 colors/white))))

(defn draw [{:keys [camera rotation rotating? helix-turns helix-radius helix-height points-per-turn]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! {:r 20 :g 20 :b 35 :a 255})

  (rc3d/begin-mode-3d! camera)

  (let [num-points (* (int helix-turns) points-per-turn)]
    ;; Draw first strand (blue)
    (draw-helix-strand! num-points helix-radius helix-height helix-turns rotation 0 colors/skyblue)

    ;; Draw second strand (orange) - offset by PI
    (draw-helix-strand! num-points helix-radius helix-height helix-turns rotation PI colors/orange)

    ;; Draw base pairs connecting the strands
    (draw-base-pairs! (int (* helix-turns 10)) helix-radius helix-height helix-turns rotation))

  ;; Draw vertical axis
  (rc3d/draw-line-3d! {:x 0 :y (- (/ helix-height 2) 1) :z 0}
                      {:x 0 :y (+ (/ helix-height 2) 1) :z 0}
                      {:r 100 :g 100 :b 100 :a 100})

  ;; Draw grid
  (rc3d/draw-grid! 10 1.0)

  (rc3d/end-mode-3d!)

  ;; Draw UI
  (rtd/draw-text! "DNA Double Helix" 10 10 20 colors/white)
  (rtd/draw-text! (format "Turns: %.1f" helix-turns) 10 35 15 colors/lightgray)
  (rtd/draw-text! (format "Radius: %.1f" helix-radius) 10 55 15 colors/lightgray)
  (rtd/draw-text! (str "Rotating: " (if rotating? "ON" "OFF")) 10 75 15
                  (if rotating? colors/green colors/red))

  ;; Legend
  (rtd/draw-text! "Base Pairs:" (- WIDTH 150) 10 15 colors/white)
  (rtd/draw-text! "A-T" (- WIDTH 150) 30 12 colors/red)
  (rtd/draw-text! "G-C" (- WIDTH 150) 45 12 colors/blue)

  (rtd/draw-text! "UP/DOWN: Turns | LEFT/RIGHT: Radius | SPACE: Rotate" 10 (- HEIGHT 25) 15 colors/gray)

  (rtd/draw-fps! (- WIDTH 100) (- HEIGHT 25))

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

  ;; Adjust parameters
  (swap! game-atom assoc :helix-turns 4.0)
  (swap! game-atom assoc :helix-radius 3.0)

  ;; Toggle rotation
  (swap! game-atom update :rotating? not)

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
