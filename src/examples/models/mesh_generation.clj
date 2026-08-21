(ns examples.models.mesh-generation
  "Raylib [models] example - procedural mesh/shape generation
   
   Showcase of procedurally generated 3D shapes.
   Demonstrates various geometric primitives and custom shapes.
   
   Based on raylib's models_mesh_generation example.
   
   Complexity: ⭐⭐ (2/4)
   
   Controls:
   - LEFT/RIGHT or Click: Cycle through shapes
   - SPACE: Toggle wireframe
   - R: Toggle rotation
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.core.camera3d :as rc3d]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]))

(def WIDTH 800)
(def HEIGHT 450)

;; Shape definitions
(def shapes
  [{:name "CUBE"
    :type :cube}
   {:name "SPHERE"
    :type :sphere}
   {:name "CYLINDER"
    :type :cylinder}
   {:name "TORUS"
    :type :torus}
   {:name "CONE"
    :type :cone}
   {:name "HEMISPHERE"
    :type :hemisphere}
   {:name "CAPSULE"
    :type :capsule}
   {:name "POLYGON"
    :type :polygon}
   {:name "STAR"
    :type :star}
   {:name "SPIRAL"
    :type :spiral}])

(defn initial-state []
  {:exit? false
   :current-shape 0
   :rotation 0.0
   :auto-rotate? true
   :wireframe? false
   :camera {:position {:x 5.0
                       :y 5.0
                       :z 5.0}
            :target {:x 0.0
                     :y 0.0
                     :z 0.0}
            :up {:x 0.0
                 :y 1.0
                 :z 0.0}
            :fovy 45.0
            :projection rc3d/CAMERA_PERSPECTIVE}})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - procedural shape generation")
  (rct/set-target-fps! 60))

(defn handle-input [{:keys [current-shape]
                     :as game}]
  (let [num-shapes (count shapes)]
    (cond-> game
      (rck/is-key-pressed? (:q enums/keyboard-key))
      (assoc :exit? true)

      (rck/is-key-pressed? (:space enums/keyboard-key))
      (update :wireframe? not)

      (rck/is-key-pressed? (:r enums/keyboard-key))
      (update :auto-rotate? not)

      (or (rck/is-key-pressed? (:right enums/keyboard-key))
          (rcm/is-mouse-button-pressed? 0))
      (update :current-shape #(mod (inc %) num-shapes))

      (rck/is-key-pressed? (:left enums/keyboard-key))
      (update :current-shape #(mod (+ % (dec num-shapes)) num-shapes)))))

(defn update-rotation [{:keys [auto-rotate?]
                        :as game}]
  (if auto-rotate?
    (update game :rotation + (* 30.0 (rct/get-frame-time)))
    game))

(defn update-camera [game]
  (update game :camera #(rc3d/update-camera % rc3d/CAMERA_ORBITAL)))

(defn tick [game]
  (-> game
      handle-input
      update-rotation
      update-camera))

;; Shape drawing functions
(defn draw-cube! [rotation wireframe?]
  (let [size 2.0
        pos {:x 0
             :y 0
             :z 0}
        color {:r 230
               :g 41
               :b 55
               :a 255}]
    (if wireframe?
      (rc3d/draw-cube-wires! pos size size size color)
      (do
        (rc3d/draw-cube! pos size size size color)
        (rc3d/draw-cube-wires! pos size size size colors/darkgray)))))

(defn draw-sphere! [rotation wireframe?]
  (let [radius 1.5
        pos {:x 0
             :y 0
             :z 0}
        color {:r 0
               :g 228
               :b 48
               :a 255}]
    (if wireframe?
      (rc3d/draw-sphere-wires! pos radius 16 16 color)
      (do
        (rc3d/draw-sphere! pos radius color)
        (rc3d/draw-sphere-wires! pos radius 16 16 colors/darkgray)))))

(defn draw-cylinder! [rotation wireframe?]
  (let [pos {:x 0
             :y -1
             :z 0}
        color {:r 0
               :g 121
               :b 241
               :a 255}]
    (if wireframe?
      (rc3d/draw-cylinder-wires! pos 1.0 1.0 2.0 16 color)
      (do
        (rc3d/draw-cylinder! pos 1.0 1.0 2.0 16 color)
        (rc3d/draw-cylinder-wires! pos 1.0 1.0 2.0 16 colors/darkgray)))))

(defn draw-torus! [rotation wireframe?]
  ;; Draw torus as a series of circles
  (let [major-radius 1.5
        minor-radius 0.5
        segments 24
        color {:r 253
               :g 249
               :b 0
               :a 255}]
    (doseq [i (range segments)]
      (let [angle (* 2 Math/PI (/ i segments))
            cx (* major-radius (Math/cos angle))
            cz (* major-radius (Math/sin angle))
            ;; Draw circle at this position
            next-angle (* 2 Math/PI (/ (inc i) segments))
            nx (* major-radius (Math/cos next-angle))
            nz (* major-radius (Math/sin next-angle))]
        ;; Draw ring segments
        (doseq [j (range 16)]
          (let [ring-angle (* 2 Math/PI (/ j 16))
                ry (* minor-radius (Math/sin ring-angle))
                rr (* minor-radius (Math/cos ring-angle))
                ;; Point on current ring
                px (+ cx (* rr (Math/cos angle)))
                py ry
                pz (+ cz (* rr (Math/sin angle)))
                ;; Next point on ring
                next-ring-angle (* 2 Math/PI (/ (inc j) 16))
                nry (* minor-radius (Math/sin next-ring-angle))
                nrr (* minor-radius (Math/cos next-ring-angle))
                npx (+ cx (* nrr (Math/cos angle)))
                npy nry
                npz (+ cz (* nrr (Math/sin angle)))]
            (rc3d/draw-line-3d! {:x px
                                 :y py
                                 :z pz}
                                {:x npx
                                 :y npy
                                 :z npz}
                                color)))))))

(defn draw-cone! [rotation wireframe?]
  (let [pos {:x 0
             :y -1
             :z 0}
        color {:r 200
               :g 122
               :b 255
               :a 255}]
    (if wireframe?
      (rc3d/draw-cylinder-wires! pos 0.0 1.5 2.0 16 color)
      (do
        (rc3d/draw-cylinder! pos 0.0 1.5 2.0 16 color)
        (rc3d/draw-cylinder-wires! pos 0.0 1.5 2.0 16 colors/darkgray)))))

(defn draw-hemisphere! [rotation wireframe?]
  ;; Draw hemisphere as latitude/longitude lines
  (let [radius 1.5
        color {:r 255
               :g 161
               :b 0
               :a 255}
        lat-segments 8
        lon-segments 16]
    ;; Latitude lines (horizontal circles)
    (doseq [i (range (inc lat-segments))]
      (let [lat-angle (* (/ Math/PI 2) (/ i lat-segments))
            y (* radius (Math/sin lat-angle))
            r (* radius (Math/cos lat-angle))]
        (when (> r 0.01)
          (doseq [j (range lon-segments)]
            (let [lon1 (* 2 Math/PI (/ j lon-segments))
                  lon2 (* 2 Math/PI (/ (inc j) lon-segments))]
              (rc3d/draw-line-3d! {:x (* r (Math/cos lon1))
                                   :y y
                                   :z (* r (Math/sin lon1))}
                                  {:x (* r (Math/cos lon2))
                                   :y y
                                   :z (* r (Math/sin lon2))}
                                  color))))))
    ;; Longitude lines (vertical arcs)
    (doseq [j (range lon-segments)]
      (let [lon (* 2 Math/PI (/ j lon-segments))]
        (doseq [i (range lat-segments)]
          (let [lat1 (* (/ Math/PI 2) (/ i lat-segments))
                lat2 (* (/ Math/PI 2) (/ (inc i) lat-segments))
                r1 (* radius (Math/cos lat1))
                r2 (* radius (Math/cos lat2))
                y1 (* radius (Math/sin lat1))
                y2 (* radius (Math/sin lat2))]
            (rc3d/draw-line-3d! {:x (* r1 (Math/cos lon))
                                 :y y1
                                 :z (* r1 (Math/sin lon))}
                                {:x (* r2 (Math/cos lon))
                                 :y y2
                                 :z (* r2 (Math/sin lon))}
                                color)))))))

(defn draw-capsule! [rotation wireframe?]
  ;; Capsule = cylinder with hemispherical caps
  (let [radius 0.8
        height 1.5
        color {:r 102
               :g 191
               :b 255
               :a 255}]
    ;; Middle cylinder
    (if wireframe?
      (rc3d/draw-cylinder-wires! {:x 0
                                  :y (- (/ height 2))
                                  :z 0} radius radius height 16 color)
      (rc3d/draw-cylinder! {:x 0
                            :y (- (/ height 2))
                            :z 0} radius radius height 16 color))
    ;; Top sphere
    (if wireframe?
      (rc3d/draw-sphere-wires! {:x 0
                                :y (/ height 2)
                                :z 0} radius 16 16 color)
      (rc3d/draw-sphere! {:x 0
                          :y (/ height 2)
                          :z 0} radius color))
    ;; Bottom sphere
    (if wireframe?
      (rc3d/draw-sphere-wires! {:x 0
                                :y (- (/ height 2))
                                :z 0} radius 16 16 color)
      (rc3d/draw-sphere! {:x 0
                          :y (- (/ height 2))
                          :z 0} radius color))))

(defn draw-polygon! [rotation wireframe?]
  ;; Draw a pentagon prism
  (let [sides 5
        radius 1.5
        height 1.0
        color {:r 127
               :g 106
               :b 79
               :a 255}]
    (doseq [i (range sides)]
      (let [angle1 (* 2 Math/PI (/ i sides))
            angle2 (* 2 Math/PI (/ (inc i) sides))
            x1 (* radius (Math/cos angle1))
            z1 (* radius (Math/sin angle1))
            x2 (* radius (Math/cos angle2))
            z2 (* radius (Math/sin angle2))
            y-top (/ height 2)
            y-bot (- (/ height 2))]
        ;; Top edge
        (rc3d/draw-line-3d! {:x x1
                             :y y-top
                             :z z1} {:x x2
                                     :y y-top
                                     :z z2} color)
        ;; Bottom edge
        (rc3d/draw-line-3d! {:x x1
                             :y y-bot
                             :z z1} {:x x2
                                     :y y-bot
                                     :z z2} color)
        ;; Vertical edge
        (rc3d/draw-line-3d! {:x x1
                             :y y-top
                             :z z1} {:x x1
                                     :y y-bot
                                     :z z1} color)
        ;; Diagonals to center (top)
        (rc3d/draw-line-3d! {:x 0
                             :y y-top
                             :z 0} {:x x1
                                    :y y-top
                                    :z z1} color)
        ;; Diagonals to center (bottom)
        (rc3d/draw-line-3d! {:x 0
                             :y y-bot
                             :z 0} {:x x1
                                    :y y-bot
                                    :z z1} color)))))

(defn draw-star! [rotation wireframe?]
  ;; 3D star shape
  (let [outer-radius 2.0
        inner-radius 0.8
        points 5
        color {:r 255
               :g 109
               :b 194
               :a 255}]
    (doseq [i (range (* 2 points))]
      (let [angle1 (* Math/PI (/ i points))
            angle2 (* Math/PI (/ (inc i) points))
            r1 (if (even? i) outer-radius inner-radius)
            r2 (if (odd? i) outer-radius inner-radius)
            x1 (* r1 (Math/cos angle1))
            z1 (* r1 (Math/sin angle1))
            x2 (* r2 (Math/cos angle2))
            z2 (* r2 (Math/sin angle2))]
        ;; Connect star points
        (rc3d/draw-line-3d! {:x x1
                             :y 0
                             :z z1} {:x x2
                                     :y 0
                                     :z z2} color)
        ;; Connect to center top
        (rc3d/draw-line-3d! {:x x1
                             :y 0
                             :z z1} {:x 0
                                     :y 1.0
                                     :z 0} color)
        ;; Connect to center bottom
        (rc3d/draw-line-3d! {:x x1
                             :y 0
                             :z z1} {:x 0
                                     :y -1.0
                                     :z 0} color)))))

(defn draw-spiral! [rotation wireframe?]
  ;; 3D spiral/helix
  (let [radius 1.0
        height 3.0
        turns 3
        segments 100
        color {:r 0
               :g 255
               :b 255
               :a 255}]
    (doseq [i (range segments)]
      (let [t1 (/ i segments)
            t2 (/ (inc i) segments)
            angle1 (* 2 Math/PI turns t1)
            angle2 (* 2 Math/PI turns t2)
            y1 (- (* height t1) (/ height 2))
            y2 (- (* height t2) (/ height 2))
            x1 (* radius (Math/cos angle1))
            z1 (* radius (Math/sin angle1))
            x2 (* radius (Math/cos angle2))
            z2 (* radius (Math/sin angle2))]
        (rc3d/draw-line-3d! {:x x1
                             :y y1
                             :z z1} {:x x2
                                     :y y2
                                     :z z2} color)))))

(defn draw-shape! [shape-type rotation wireframe?]
  (case shape-type
    :cube (draw-cube! rotation wireframe?)
    :sphere (draw-sphere! rotation wireframe?)
    :cylinder (draw-cylinder! rotation wireframe?)
    :torus (draw-torus! rotation wireframe?)
    :cone (draw-cone! rotation wireframe?)
    :hemisphere (draw-hemisphere! rotation wireframe?)
    :capsule (draw-capsule! rotation wireframe?)
    :polygon (draw-polygon! rotation wireframe?)
    :star (draw-star! rotation wireframe?)
    :spiral (draw-spiral! rotation wireframe?)))

(defn draw [{:keys [camera current-shape rotation wireframe? auto-rotate?]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3d/begin-mode-3d! camera)

  (rc3d/draw-grid! 10 1.0)

  (let [shape (nth shapes current-shape)]
    (draw-shape! (:type shape) rotation wireframe?))

  (rc3d/end-mode-3d!)

  ;; UI - bottom info bar
  (rsb/draw-rectangle! 30 400 310 30 {:r 135
                                      :g 206
                                      :b 235
                                      :a 128})
  (rsb/draw-rectangle-lines! 30 400 310 30 {:r 0
                                            :g 82
                                            :b 172
                                            :a 128})
  (rtd/draw-text! "LEFT/RIGHT or CLICK to cycle shapes" 40 408 10 colors/blue)

  ;; Shape name
  (let [shape (nth shapes current-shape)]
    (rtd/draw-text! (:name shape) (- WIDTH 120) 10 20 colors/darkblue))

  ;; Status
  (when wireframe?
    (rtd/draw-text! "WIREFRAME" 10 40 15 colors/gray))
  (when-not auto-rotate?
    (rtd/draw-text! "ROTATION PAUSED" 10 60 15 colors/gray))

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
  @game-atom
  (swap! game-atom assoc :current-shape 3)
  (swap! game-atom update :wireframe? not))
