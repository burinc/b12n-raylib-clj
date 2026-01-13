(ns examples.models.terrain-generation
  "Raylib [models] example - procedural terrain generation
   
   Generate and visualize 3D terrain using Perlin-like noise.
   Features real-time terrain modification and color mapping.
   
   Complexity: ⭐⭐⭐ (3/4)
   
   Controls:
   - LEFT/RIGHT: Adjust frequency (detail level)
   - UP/DOWN: Adjust amplitude (height)
   - 1/2/3: Preset terrains (hills/mountains/plains)
   - G: Toggle grid overlay
   - W: Toggle wireframe mode
   - SPACE: Regenerate with new seed
   - R: Reset
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
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]))

(def WIDTH 800)
(def HEIGHT 450)
(def GRID_SIZE 40)
(def CELL_SIZE 0.5)

;; Simple noise function (value noise with interpolation)
(defn hash-2d [x y seed]
  (let [n (unchecked-add (unchecked-multiply (long x) 374761393)
                         (unchecked-add (unchecked-multiply (long y) 668265263)
                                        (long seed)))]
    (-> n
        (bit-xor (bit-shift-right n 13))
        (unchecked-multiply 1274126177)
        (bit-and 0x7fffffff)
        (/ 2147483647.0))))

(defn smoothstep [t]
  (* t t (- 3 (* 2 t))))

(defn lerp [a b t]
  (+ a (* t (- b a))))

(defn value-noise [x y seed]
  (let [xi (int (Math/floor x))
        yi (int (Math/floor y))
        xf (- x xi)
        yf (- y yi)
        ;; Get corner values
        v00 (hash-2d xi yi seed)
        v10 (hash-2d (inc xi) yi seed)
        v01 (hash-2d xi (inc yi) seed)
        v11 (hash-2d (inc xi) (inc yi) seed)
        ;; Smooth interpolation
        sx (smoothstep xf)
        sy (smoothstep yf)]
    (lerp (lerp v00 v10 sx)
          (lerp v01 v11 sx)
          sy)))

(defn fbm
  "Fractal Brownian Motion - layer multiple octaves of noise"
  [x y seed octaves frequency amplitude]
  (loop [i 0
         freq frequency
         amp amplitude
         total 0.0
         max-val 0.0]
    (if (>= i octaves)
      (/ total max-val)
      (recur (inc i)
             (* freq 2.0)
             (* amp 0.5)
             (+ total (* amp (value-noise (* x freq) (* y freq) seed)))
             (+ max-val amp)))))

(defn generate-heightmap
  "Generate heightmap data"
  [{:keys [seed frequency amplitude octaves]}]
  (vec (for [z (range GRID_SIZE)]
         (vec (for [x (range GRID_SIZE)]
                (let [nx (/ x (double GRID_SIZE))
                      nz (/ z (double GRID_SIZE))]
                  (* amplitude (fbm nx nz seed octaves frequency 1.0))))))))

(defn initial-state []
  (let [seed (rand-int 10000)]
    {:exit? false
     :seed seed
     :frequency 4.0
     :amplitude 3.0
     :octaves 4
     :heightmap nil ; Generated lazily
     :show-grid? false
     :wireframe? false
     :camera {:position {:x 15.0
                         :y 12.0
                         :z 15.0}
              :target {:x 10.0
                       :y 0.0
                       :z 10.0}
              :up {:x 0.0
                   :y 1.0
                   :z 0.0}
              :fovy 45.0
              :projection rc3d/CAMERA_PERSPECTIVE}}))

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - procedural terrain")
  (rct/set-target-fps! 60))

(defn apply-preset [game preset]
  (let [params (case preset
                 :hills {:frequency 3.0
                         :amplitude 2.0
                         :octaves 3}
                 :mountains {:frequency 5.0
                             :amplitude 5.0
                             :octaves 5}
                 :plains {:frequency 2.0
                          :amplitude 0.5
                          :octaves 2}
                 {:frequency 4.0
                  :amplitude 3.0
                  :octaves 4})]
    (-> game
        (merge params)
        (assoc :heightmap nil))))

(defn handle-input [game]
  (let [dt (rct/get-frame-time)]
    (cond-> game
      (rck/is-key-pressed? (:q enums/keyboard-key))
      (assoc :exit? true)

      (rck/is-key-pressed? (:space enums/keyboard-key))
      (-> (assoc :seed (rand-int 10000))
          (assoc :heightmap nil))

      (rck/is-key-pressed? (:r enums/keyboard-key))
      (merge (initial-state))

      (rck/is-key-pressed? (:g enums/keyboard-key))
      (update :show-grid? not)

      (rck/is-key-pressed? (:w enums/keyboard-key))
      (update :wireframe? not)

      ;; Presets
      (rck/is-key-pressed? (:one enums/keyboard-key))
      (apply-preset :hills)

      (rck/is-key-pressed? (:two enums/keyboard-key))
      (apply-preset :mountains)

      (rck/is-key-pressed? (:three enums/keyboard-key))
      (apply-preset :plains)

      ;; Adjust frequency
      (rck/is-key-down? (:right enums/keyboard-key))
      (-> (update :frequency + (* 1.0 dt))
          (assoc :heightmap nil))

      (rck/is-key-down? (:left enums/keyboard-key))
      (-> (update :frequency #(max 1.0 (- % (* 1.0 dt))))
          (assoc :heightmap nil))

      ;; Adjust amplitude
      (rck/is-key-down? (:up enums/keyboard-key))
      (-> (update :amplitude + (* 1.0 dt))
          (assoc :heightmap nil))

      (rck/is-key-down? (:down enums/keyboard-key))
      (-> (update :amplitude #(max 0.5 (- % (* 1.0 dt))))
          (assoc :heightmap nil)))))

(defn ensure-heightmap [{:keys [heightmap]
                         :as game}]
  (if heightmap
    game
    (assoc game :heightmap (generate-heightmap game))))

(defn update-camera [game]
  (update game :camera #(rc3d/update-camera % rc3d/CAMERA_ORBITAL)))

(defn tick [game]
  (-> game
      handle-input
      ensure-heightmap
      update-camera))

(defn height->color
  "Map height to terrain color"
  [h max-h]
  (let [ratio (/ h max-h)]
    (cond
      (< ratio 0.2) {:r 50
                     :g 100
                     :b 150
                     :a 255} ; Water blue
      (< ratio 0.3) {:r 194
                     :g 178
                     :b 128
                     :a 255} ; Sand
      (< ratio 0.5) {:r 34
                     :g 139
                     :b 34
                     :a 255} ; Grass green
      (< ratio 0.7) {:r 85
                     :g 107
                     :b 47
                     :a 255} ; Forest green
      (< ratio 0.85) {:r 139
                      :g 137
                      :b 137
                      :a 255} ; Rock gray
      :else {:r 255
             :g 255
             :b 255
             :a 255}))) ; Snow white

(defn draw-terrain! [heightmap wireframe? amplitude]
  (let [half-grid (* GRID_SIZE CELL_SIZE 0.5)]
    (doseq [z (range (dec GRID_SIZE))
            x (range (dec GRID_SIZE))]
      (let [h00 (get-in heightmap [z x] 0)
            h10 (get-in heightmap [z (inc x)] 0)
            h01 (get-in heightmap [(inc z) x] 0)
            h11 (get-in heightmap [(inc z) (inc x)] 0)
            avg-h (/ (+ h00 h10 h01 h11) 4.0)
            color (height->color avg-h amplitude)
            ;; Convert grid to world coords
            x0 (* x CELL_SIZE)
            z0 (* z CELL_SIZE)
            x1 (* (inc x) CELL_SIZE)
            z1 (* (inc z) CELL_SIZE)
            ;; Triangle vertices
            p00 {:x x0
                 :y h00
                 :z z0}
            p10 {:x x1
                 :y h10
                 :z z0}
            p01 {:x x0
                 :y h01
                 :z z1}
            p11 {:x x1
                 :y h11
                 :z z1}]
        (if wireframe?
          (do
            (rc3d/draw-line-3d! p00 p10 colors/green)
            (rc3d/draw-line-3d! p00 p01 colors/green)
            (rc3d/draw-line-3d! p10 p11 colors/green)
            (rc3d/draw-line-3d! p01 p11 colors/green))
          (do
            ;; Draw two triangles per cell
            (rc3d/draw-triangle-3d! p00 p01 p10 color)
            (rc3d/draw-triangle-3d! p10 p01 p11 color)))))))

(defn draw [{:keys [camera heightmap show-grid? wireframe? frequency amplitude]
             :as game}]
  (rcd/begin-drawing!)
  (rcd/clear-background! {:r 135
                          :g 206
                          :b 235
                          :a 255}) ; Sky blue

  (rc3d/begin-mode-3d! camera)

  (when show-grid?
    (rc3d/draw-grid! 40 CELL_SIZE))

  (when heightmap
    (draw-terrain! heightmap wireframe? amplitude))

  (rc3d/end-mode-3d!)

  ;; UI
  (rsb/draw-rectangle! 5 5 300 80 {:r 0
                                   :g 0
                                   :b 0
                                   :a 150})
  (rtd/draw-text! "Procedural Terrain" 10 10 20 colors/white)
  (rtd/draw-text! (format "Freq: %.1f | Amp: %.1f" frequency amplitude) 10 35 15 colors/lime)
  (rtd/draw-text! "Arrows: Adjust | 1-3: Presets | SPACE: New" 10 55 15 colors/gray)
  (rtd/draw-text! "G: Grid | W: Wireframe" 10 70 12 colors/gray)

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
  (swap! game-atom apply-preset :mountains)
  (swap! game-atom assoc :wireframe? true))
