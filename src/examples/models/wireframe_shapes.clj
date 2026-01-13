(ns examples.models.wireframe-shapes
  "Raylib [models] example - wireframe shapes
   
   Various 3D wireframe shapes drawn using lines.
   Demonstrates draw-line-3d! for custom wireframe rendering.
   
   Complexity: ⭐⭐ Intermediate (2/4)
   
   Controls:
   - SPACE: Toggle rotation
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
(def TWO-PI (* 2.0 Math/PI))

(defn make-camera []
  {:position {:x 10.0 :y 10.0 :z 10.0}
   :target {:x 0.0 :y 0.0 :z 0.0}
   :up {:x 0.0 :y 1.0 :z 0.0}
   :fovy 45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)
   :rotating? true})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - wireframe shapes")
  (rct/set-target-fps! 60))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)

    (rck/is-key-pressed? (:space enums/keyboard-key))
    (update :rotating? not)))

(defn tick [game]
  (-> game
      handle-input))

;; Wireframe shape generators

(defn draw-wireframe-pyramid
  "Draw a wireframe pyramid at position with given size"
  [pos size color time]
  (let [half (/ size 2.0)
        height size
        ;; Base corners
        p1 {:x (+ (:x pos) half) :y (:y pos) :z (+ (:z pos) half)}
        p2 {:x (- (:x pos) half) :y (:y pos) :z (+ (:z pos) half)}
        p3 {:x (- (:x pos) half) :y (:y pos) :z (- (:z pos) half)}
        p4 {:x (+ (:x pos) half) :y (:y pos) :z (- (:z pos) half)}
        ;; Apex
        apex {:x (:x pos) :y (+ (:y pos) height) :z (:z pos)}]
    ;; Base
    (rc3d/draw-line-3d! p1 p2 color)
    (rc3d/draw-line-3d! p2 p3 color)
    (rc3d/draw-line-3d! p3 p4 color)
    (rc3d/draw-line-3d! p4 p1 color)
    ;; Sides to apex
    (rc3d/draw-line-3d! p1 apex color)
    (rc3d/draw-line-3d! p2 apex color)
    (rc3d/draw-line-3d! p3 apex color)
    (rc3d/draw-line-3d! p4 apex color)))

(defn draw-wireframe-octahedron
  "Draw a wireframe octahedron at position with given size"
  [pos size color]
  (let [;; 6 vertices: top, bottom, and 4 middle
        top {:x (:x pos) :y (+ (:y pos) size) :z (:z pos)}
        bottom {:x (:x pos) :y (- (:y pos) size) :z (:z pos)}
        front {:x (:x pos) :y (:y pos) :z (+ (:z pos) size)}
        back {:x (:x pos) :y (:y pos) :z (- (:z pos) size)}
        left {:x (- (:x pos) size) :y (:y pos) :z (:z pos)}
        right {:x (+ (:x pos) size) :y (:y pos) :z (:z pos)}]
    ;; Top edges
    (rc3d/draw-line-3d! top front color)
    (rc3d/draw-line-3d! top back color)
    (rc3d/draw-line-3d! top left color)
    (rc3d/draw-line-3d! top right color)
    ;; Bottom edges
    (rc3d/draw-line-3d! bottom front color)
    (rc3d/draw-line-3d! bottom back color)
    (rc3d/draw-line-3d! bottom left color)
    (rc3d/draw-line-3d! bottom right color)
    ;; Middle ring
    (rc3d/draw-line-3d! front right color)
    (rc3d/draw-line-3d! right back color)
    (rc3d/draw-line-3d! back left color)
    (rc3d/draw-line-3d! left front color)))

(defn draw-wireframe-torus
  "Draw a wireframe torus at position"
  [pos major-r minor-r segments rings color]
  (doseq [i (range rings)]
    (let [angle1 (* (/ i rings) TWO-PI)
          angle2 (* (/ (inc i) rings) TWO-PI)]
      (doseq [j (range segments)]
        (let [seg-angle1 (* (/ j segments) TWO-PI)
              seg-angle2 (* (/ (inc j) segments) TWO-PI)
              ;; Calculate points on torus surface
              calc-point (fn [ring-a seg-a]
                           (let [r (+ major-r (* minor-r (Math/cos seg-a)))]
                             {:x (+ (:x pos) (* r (Math/cos ring-a)))
                              :y (+ (:y pos) (* minor-r (Math/sin seg-a)))
                              :z (+ (:z pos) (* r (Math/sin ring-a)))}))
              p1 (calc-point angle1 seg-angle1)
              p2 (calc-point angle1 seg-angle2)
              p3 (calc-point angle2 seg-angle1)]
          ;; Draw ring segment
          (rc3d/draw-line-3d! p1 p2 color)
          ;; Draw connecting segment
          (rc3d/draw-line-3d! p1 p3 color))))))

(defn draw-wireframe-spiral
  "Draw a 3D spiral"
  [pos height radius turns segments color]
  (let [total-points (* turns segments)]
    (doseq [i (range (dec total-points))]
      (let [t1 (/ i total-points)
            t2 (/ (inc i) total-points)
            angle1 (* t1 turns TWO-PI)
            angle2 (* t2 turns TWO-PI)
            p1 {:x (+ (:x pos) (* radius (Math/cos angle1)))
                :y (+ (:y pos) (* t1 height))
                :z (+ (:z pos) (* radius (Math/sin angle1)))}
            p2 {:x (+ (:x pos) (* radius (Math/cos angle2)))
                :y (+ (:y pos) (* t2 height))
                :z (+ (:z pos) (* radius (Math/sin angle2)))}]
        (rc3d/draw-line-3d! p1 p2 color)))))

(defn draw [{:keys [camera rotating?]}]
  (let [time (if rotating? (ext/get-time) 0.0)
        rotation (* time 0.5)]
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/raywhite)

    (rc3d/begin-mode-3d! camera)

    ;; Draw pyramid (animated position)
    (let [px (* 4.0 (Math/cos rotation))]
      (draw-wireframe-pyramid
       {:x px :y 0.0 :z -3.0}
       1.5
       colors/red
       time))

    ;; Draw octahedron
    (draw-wireframe-octahedron
     {:x 0.0 :y 1.5 :z 0.0}
     1.0
     colors/blue)

    ;; Draw torus
    (draw-wireframe-torus
     {:x -4.0 :y 1.0 :z 2.0}
     1.0 0.3 12 16
     colors/green)

    ;; Draw spiral
    (draw-wireframe-spiral
     {:x 4.0 :y 0.0 :z 2.0}
     3.0 0.8 3 20
     colors/purple)

    ;; Draw grid
    (rc3d/draw-grid! 10 1.0)

    (rc3d/end-mode-3d!)

    ;; Draw UI
    (rtd/draw-text! "Wireframe Shapes: Pyramid, Octahedron, Torus, Spiral" 10 40 20 colors/darkgray)
    (rtd/draw-text! (str "Rotation: " (if rotating? "ON" "OFF") " (SPACE to toggle)") 10 65 20 colors/gray)
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

  ;; Toggle rotation
  (swap! game-atom update :rotating? not)

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
