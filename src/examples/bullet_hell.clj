(ns examples.bullet-hell
  "raylib [shapes] example - bullet hell

   A rotating magic circle firing rows of bullets outward, as a throughput
   test. ENTER switches between drawing each bullet as a pre-rendered
   texture and drawing it as two circles, and the FPS readout shows what
   that costs.

   One deliberate departure from the C. It preallocates 500,000 bullet
   slots and never removes anything: an off-screen bullet gets a `disabled`
   flag and stays in the array, because compacting a C array every frame
   would cost more than skipping dead entries. When the slot count is
   finally exhausted the whole array resets and every bullet on screen
   vanishes at once.

   Here off-screen bullets are simply dropped. That is not a shortcut - the
   flag exists to avoid a cost Clojure does not pay, and dropping them
   removes the periodic mass-vanish, which was an artifact of the array
   rather than anything the example set out to show. The cap remains as a
   safety limit, now on live bullets rather than on total ever spawned.

   Difficulty: 3/4
   Based on: shapes/shapes_bullet_hell.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.textures.drawing :as rtdw]
   [raylib.textures.texture-loading :as rtl]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def max-bullets 500000)
(def bullet-radius 10)
(def bullet-colors [colors/red colors/blue])

(defn initial-state []
  {:bullets []
   :speed 3.0 :rows 6
   :base-direction 0.0 :angle-increment 5
   :cooldown 2.0 :cooldown-timer 2.0
   :rotation 0.0 :performance-mode true
   :texture nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - bullet hell")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  ;; One 24x24 target holding a white filled circle with a black outline.
  ;; Drawn once here, then tinted per bullet at draw time - which is the
  ;; whole point of "performance mode".
  (let [tex (rtl/load-render-texture! 24 24)]
    (rtl/begin-texture-mode! tex)
    (rcd/clear-background! {:r 0 :g 0 :b 0 :a 0})
    (rsb/draw-circle! 12 12 (float bullet-radius) colors/white)
    (rsb/draw-circle-lines! 12 12 (float bullet-radius) colors/black)
    (rtl/end-texture-mode!)
    (swap! game-atom assoc :texture tex)))

(defn spawn
  "One bullet per row, evenly spaced around the circle, offset by the
   running base direction - which is what makes the pattern spiral."
  [{:keys [rows base-direction speed]}]
  (let [degrees-per-row (/ 360.0 rows)]
    (for [row (range rows)
          :let [dir (Math/toRadians (+ base-direction (* degrees-per-row row)))]]
      {:pos {:x (/ screen-width 2.0) :y (/ screen-height 2.0)}
       :acc {:x (* speed (Math/cos dir)) :y (* speed (Math/sin dir))}
       :color (nth bullet-colors (mod row 2))})))

(defn on-screen?
  "Kept until fully clear of the edge by two radii, so a bullet does not
   pop out of existence while still partly visible."
  [{{:keys [x y]} :pos}]
  (let [m (* bullet-radius 2)]
    (and (> x (- m)) (< x (+ screen-width m))
         (> y (- m)) (< y (+ screen-height m)))))

(defn advance [bullets]
  (into [] (comp (map (fn [b] (update b :pos #(hash-map :x (+ (:x %) (get-in b [:acc :x]))
                                                        :y (+ (:y %) (get-in b [:acc :y]))))))
                 (filter on-screen?))
        bullets))

(defn- pressed? [& ks] (some #(rck/is-key-pressed? (get enums/keyboard-key %)) ks))

(defn tick [{:keys [bullets cooldown cooldown-timer rows speed
                    angle-increment base-direction] :as state}]
  (debug-stats/update!)
  (let [rows (cond (and (pressed? :right :d) (< rows 359)) (inc rows)
                   (and (pressed? :left :a) (> rows 1)) (dec rows)
                   :else rows)
        speed (cond (pressed? :up :w) (+ speed 0.25)
                    (and (pressed? :down :s) (> speed 0.50)) (- speed 0.25)
                    :else speed)
        cooldown (cond (and (pressed? :z) (> cooldown 1)) (dec cooldown)
                       (pressed? :x) (inc cooldown)
                       :else cooldown)
        angle-increment (if (rck/is-key-down? (:space enums/keyboard-key))
                          (mod (inc angle-increment) 360)
                          angle-increment)
        state (assoc state :rows rows :speed speed :cooldown cooldown
                     :angle-increment angle-increment
                     :performance-mode (if (pressed? :enter)
                                         (not (:performance-mode state))
                                         (:performance-mode state)))
        bullets (if (pressed? :c) [] (advance bullets))
        timer (dec cooldown-timer)
        fire? (and (neg? timer) (< (count bullets) max-bullets))]
    (assoc state
           :rotation (inc (:rotation state))
           :cooldown-timer (if (neg? timer) cooldown timer)
           :base-direction (if fire? (+ base-direction angle-increment) base-direction)
           :bullets (if fire? (into bullets (spawn state)) bullets))))

(defn- draw-magic-circle [rotation]
  (let [cx (/ screen-width 2.0) cy (/ screen-height 2.0)
        square {:x (float cx) :y (float cy) :width 120.0 :height 120.0}]
    (rsb/draw-rectangle-pro! square {:x 60.0 :y 60.0} (float rotation) colors/purple)
    (rsb/draw-rectangle-pro! square {:x 60.0 :y 60.0} (float (+ rotation 45)) colors/purple)
    (doseq [r [70.0 50.0 30.0]]
      (rsb/draw-circle-lines! (int cx) (int cy) (float r) colors/black))))

(defn draw [{:keys [bullets rotation performance-mode rows speed
                    angle-increment cooldown]
             render-texture :texture}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (draw-magic-circle rotation)

  (if (and performance-mode render-texture)
    (let [tex (:texture render-texture)
          hw (* 0.5 (:width tex)) hh (* 0.5 (:height tex))]
      (doseq [{:keys [pos color]} bullets]
        (rtdw/draw-texture! tex (int (- (:x pos) hw)) (int (- (:y pos) hh)) color)))
    (doseq [{:keys [pos color]} bullets]
      (let [p {:x (float (:x pos)) :y (float (:y pos))}]
        (rsb/draw-circle-v! p (float bullet-radius) color)
        (rsb/draw-circle-lines-v! p (float bullet-radius) colors/black))))

  (let [panel {:r 0 :g 0 :b 0 :a 200}]
    (rsb/draw-rectangle! 10 10 280 150 panel)
    (rtd/draw-text! "Controls:" 20 20 10 colors/lightgray)
    (doseq [[i s] (map-indexed vector ["- Right/Left or A/D: Change rows number"
                                       "- Up/Down or W/S: Change bullet speed"
                                       "- Z or X: Change spawn cooldown"
                                       "- Space (Hold): Change the angle increment"
                                       "- Enter: Switch draw method (Performance)"
                                       "- C: Clear bullets"])]
      (rtd/draw-text! s 40 (+ 40 (* 20 i)) 10 colors/lightgray))

    (rsb/draw-rectangle! 610 10 170 30 panel)
    (if performance-mode
      (rtd/draw-text! "Draw method: DrawTexture(*)" 620 20 10 colors/green)
      (rtd/draw-text! "Draw method: DrawCircle(*)" 620 20 10 colors/red))

    (rsb/draw-rectangle! 135 410 530 30 panel)
    (rtd/draw-text!
     (format "[ FPS: %d, Bullets: %d, Rows: %d, Bullet speed: %.2f, Angle increment per frame: %d, Cooldown: %.0f ]"
             (rct/get-fps) (count bullets) rows speed angle-increment cooldown)
     155 420 10 colors/green))
  (debug-stats/draw!)
  (rcd/end-drawing!))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (when-not (rcw/window-should-close?)
        (reset! game-atom game)
        (draw game)
        (recur))))
  (rtl/unload-render-texture! (:texture @game-atom))
  (rcw/close-window!))
