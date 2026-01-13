(ns examples.models.lissajous-3d
  "Raylib [models] example - 3D Lissajous curves
   
   Beautiful parametric 3D curves formed by combining
   sine waves at different frequencies.
   
   Complexity: ⭐⭐ (2/4)
   
   Controls:
   - 1/2/3/4/5: Select preset pattern
   - UP/DOWN: Adjust A frequency
   - LEFT/RIGHT: Adjust B frequency
   - W/S: Adjust C frequency
   - SPACE: Toggle animation
   - R: Reset to default
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
(def NUM_POINTS 2000)
(def SCALE 3.0)

;; Lissajous presets [a b c delta-a delta-b]
(def presets
  {:classic [3 2 1 0 (/ Math/PI 2)]
   :knot [2 3 5 0 0]
   :figure8 [1 2 3 (/ Math/PI 4) 0]
   :trefoil [2 3 3 0 (/ Math/PI 2)]
   :complex [5 4 3 (/ Math/PI 3) (/ Math/PI 4)]})

(defn initial-state []
  {:exit? false
   :a 3.0
   :b 2.0
   :c 1.0
   :delta-a 0.0
   :delta-b (/ Math/PI 2)
   :time 0.0
   :animate? true
   :trail-length NUM_POINTS
   :camera {:position {:x 8.0
                       :y 6.0
                       :z 8.0}
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
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - 3D Lissajous curves")
  (rct/set-target-fps! 60))

(defn lissajous-point
  "Calculate 3D Lissajous point at parameter t"
  [t a b c delta-a delta-b]
  {:x (* SCALE (Math/sin (+ (* a t) delta-a)))
   :y (* SCALE (Math/sin (+ (* b t) delta-b)))
   :z (* SCALE (Math/sin (* c t)))})

(defn generate-curve
  "Generate points along the Lissajous curve"
  [{:keys [a b c delta-a delta-b time trail-length]}]
  (let [step (/ (* 2 Math/PI) trail-length)]
    (mapv (fn [i]
            (let [t (+ time (* i step))]
              (lissajous-point t a b c delta-a delta-b)))
          (range trail-length))))

(defn apply-preset [game preset-key]
  (let [[a b c da db] (get presets preset-key [3 2 1 0 (/ Math/PI 2)])]
    (assoc game :a (double a) :b (double b) :c (double c)
           :delta-a da :delta-b db)))

(defn handle-input [game]
  (let [dt (rct/get-frame-time)]
    (cond-> game
      (rck/is-key-pressed? (:q enums/keyboard-key))
      (assoc :exit? true)

      (rck/is-key-pressed? (:space enums/keyboard-key))
      (update :animate? not)

      (rck/is-key-pressed? (:r enums/keyboard-key))
      (merge (initial-state))

      ;; Presets
      (rck/is-key-pressed? (:one enums/keyboard-key))
      (apply-preset :classic)

      (rck/is-key-pressed? (:two enums/keyboard-key))
      (apply-preset :knot)

      (rck/is-key-pressed? (:three enums/keyboard-key))
      (apply-preset :figure8)

      (rck/is-key-pressed? (:four enums/keyboard-key))
      (apply-preset :trefoil)

      (rck/is-key-pressed? (:five enums/keyboard-key))
      (apply-preset :complex)

      ;; Frequency adjustments
      (rck/is-key-down? (:up enums/keyboard-key))
      (update :a + (* 0.5 dt))

      (rck/is-key-down? (:down enums/keyboard-key))
      (update :a #(max 0.1 (- % (* 0.5 dt))))

      (rck/is-key-down? (:right enums/keyboard-key))
      (update :b + (* 0.5 dt))

      (rck/is-key-down? (:left enums/keyboard-key))
      (update :b #(max 0.1 (- % (* 0.5 dt))))

      (rck/is-key-down? (:w enums/keyboard-key))
      (update :c + (* 0.5 dt))

      (rck/is-key-down? (:s enums/keyboard-key))
      (update :c #(max 0.1 (- % (* 0.5 dt)))))))

(defn update-animation [{:keys [animate?]
                         :as game}]
  (if animate?
    (update game :time + (* 0.3 (rct/get-frame-time)))
    game))

(defn update-camera [game]
  (update game :camera #(rc3d/update-camera % rc3d/CAMERA_ORBITAL)))

(defn tick [game]
  (-> game
      handle-input
      update-animation
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

(defn draw-curve! [points]
  (let [n (count points)]
    (doseq [i (range (dec n))]
      (let [p1 (nth points i)
            p2 (nth points (inc i))
            ;; Color gradient along curve
            hue (* 360.0 (/ i n))
            color (hsv->rgb hue 0.8 1.0)]
        (rc3d/draw-line-3d! p1 p2 color)))
    ;; Draw head point
    (when-let [head (last points)]
      (rc3d/draw-sphere! head 0.15 colors/white))))

(defn draw [{:keys [camera a b c animate?]
             :as game}]
  (rcd/begin-drawing!)
  (rcd/clear-background! {:r 20
                          :g 20
                          :b 30
                          :a 255})

  (rc3d/begin-mode-3d! camera)

  ;; Draw axes
  (rc3d/draw-line-3d! {:x -5
                       :y 0
                       :z 0} {:x 5
                              :y 0
                              :z 0} {:r 100
                                     :g 50
                                     :b 50
                                     :a 255})
  (rc3d/draw-line-3d! {:x 0
                       :y -5
                       :z 0} {:x 0
                              :y 5
                              :z 0} {:r 50
                                     :g 100
                                     :b 50
                                     :a 255})
  (rc3d/draw-line-3d! {:x 0
                       :y 0
                       :z -5} {:x 0
                               :y 0
                               :z 5} {:r 50
                                      :g 50
                                      :b 100
                                      :a 255})

  ;; Draw the curve
  (let [points (generate-curve game)]
    (draw-curve! points))

  (rc3d/end-mode-3d!)

  ;; UI
  (rtd/draw-text! "3D Lissajous Curves" 10 10 20 colors/white)
  (rtd/draw-text! (format "A: %.1f | B: %.1f | C: %.1f" a b c) 10 35 15 colors/lime)
  (rtd/draw-text! "1-5: Presets | Arrows/W/S: Adjust | SPACE: Pause" 10 55 15 colors/gray)
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
  (swap! game-atom apply-preset :trefoil)
  (swap! game-atom assoc :animate? false))
