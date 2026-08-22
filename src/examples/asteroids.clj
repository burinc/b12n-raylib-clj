(ns examples.asteroids
  (:require
   [raylib.core.window :as rcw]
   [raylib.nrepl :as nrepl]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.core.collision :as rcol]
   [raylib.shapes.basic :as rsb]
   [raylib.textures.texture-loading :as rtl]
   [debug-stats])
  (:gen-class))

;; Virtual resolution - the game renders at this fixed size
(def VIRTUAL_WIDTH 800)
(def VIRTUAL_HEIGHT 800)

;; Render target for resolution-independent rendering
(def render-target (atom nil))

;; Current scale and offset for letterboxing
(def screen-state (atom {:scale 1.0
                         :offset-x 0
                         :offset-y 0}))

;; CREDITS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; All the math and helper functions for this asteroids impl are from
;; @cellularmitosis (Jason Pepas)
;; https://github.com/tantona/janetroids/blob/master/main.janet

(defmacro with-drawing
  [& body]
  `(do
     (try
       (rcd/begin-drawing!)
       ~@body
       (finally
         (rcd/end-drawing!)))))

;; CONSTS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Use virtual dimensions for game logic
(def WIDTH VIRTUAL_WIDTH)
(def HEIGHT VIRTUAL_HEIGHT)
(def MAX_ASTEROID_SIZE 3)
(def BASE_ASTEROID_SPEED 1)
(def MAX_BULLET_AGE 120)
(def INITIAL_ASTEROIDS 3)
(def BULLET_RADIUS 2)

;; MATH ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn vector-add [v1 v2]
  [(+ (v1 0) (v2 0))
   (+ (v1 1) (v2 1))])

(defn vector-sub [v1 v2]
  [(- (v1 0) (v2 0))
   (- (v1 1) (v2 1))])

(defn vector-mul [v1 v2]
  [(* (v1 0) (v2 0))
   (* (v1 1) (v2 1))])

(defn vector-wrap [v modulus]
  [(if (< (v 0) 0)
     (+ WIDTH (v 0))
     (mod (Math/abs (v 0)) modulus))
   (if (< (v 1) 0)
     (+ HEIGHT (v 1))
     (mod (Math/abs (v 1)) modulus))])

;; Helper to convert [x y] to {:x x :y y} for raylib structs
(defn vec->point [[x y]]
  {:x (float x)
   :y (float y)})

;; STATE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn asteroid-speed [size]
  (let [base-speed BASE_ASTEROID_SPEED]
    (reduce (fn [speed _]
              (* 1.5 speed)) base-speed (range (- MAX_ASTEROID_SIZE size)))))

(defn random-asteroid-velocity
  "veolcity based on fixed speed in a random direction"
  [size]
  (let [speed (asteroid-speed size)
        angle (* (rand) Math/PI)]
    [(* speed (Math/cos angle))
     (* speed (Math/sin angle))]))

(defn make-asteroid-at
  [x y size]
  {:size size
   :position [x y]
   :velocity (random-asteroid-velocity size)})

(defn make-asteroid
  []
  (make-asteroid-at (* (* WIDTH 0.7) (- (rand) 0.5))
                    (* (* HEIGHT 0.7) (- (rand) 0.5))
                    MAX_ASTEROID_SIZE))

(defn make-bullet [pos velocity]
  {:position pos
   :velocity velocity
   :age 0})

(defn make-ship [width height]
  {:size 30
   :aspect 0.8
   :position [(/ width 2) (/ height 2)]
   :orientation 0.0
   :velocity [0.0 0.0]})

(defn initial-state []
  {:dt 0
   :time (System/nanoTime)
   :time-acc [1]
   :frame-counter -1
   :screen :title
   :ship (make-ship WIDTH HEIGHT)
   :asteroids (map (fn [_] (make-asteroid)) (range INITIAL_ASTEROIDS))
   :bullets []
   :alive true})

(defn asteroid-radius [asteroid]
  (* (asteroid :size) 10))

(defn find-ship-center [ship]
  (let [[ship-x ship-y] (:position ship)
        ship-size (:size ship)
        ship-aspect (:aspect ship)]
    [(+ ship-x (/ ship-size 3))
     (+ ship-y (* (/ ship-size 2) ship-aspect))]))

(defn rotate-ship-point [ship [x1 y1]]
  (let [[x0 y0] (find-ship-center ship)
        theta (:orientation ship)]
    [(- (* (- x1 x0) (Math/cos theta)) (* (- y1 y0) (Math/sin theta)))
     (+ (* (- y1 y0) (Math/cos theta)) (* (- x1 x0) (Math/sin theta)))]))

(defn ship-points [ship]
  (let [[ship-x ship-y] (ship :position)
        ship-size (ship :size)
        ship-aspect (ship :aspect)
        ship-center (find-ship-center ship)
        ship-length ship-size
        ship-width (* ship-size ship-aspect)
        p1 [ship-x ship-y]
        p2 [ship-x (+ ship-y ship-width)]
        p3 [(+ ship-x ship-length) (+ ship-y (/ ship-width 2))]
        rotated-points (map (fn [p] (rotate-ship-point ship p)) [p1 p2 p3])
        translated-rotated-points (mapv (fn [p] (vector-add ship-center p)) rotated-points)]
    translated-rotated-points))

(defn ship-thrust-vector [ship]
  (let [angle (:orientation ship)
        magnitude 0.05]
    [(* (Math/cos angle) magnitude)
     (* (Math/sin angle) magnitude)]))

(defn move-ship [game]
  (update game :ship (fn [{:keys [position velocity]
                           :as  ship}]
                       (assoc ship :position
                              (vector-wrap (vector-add position velocity) WIDTH)))))

(defn move-bullets [game]
  (update game :bullets (fn [bullets]
                          (mapv (fn [bullet]
                                  (let [new-position (vector-add (bullet :position) (bullet :velocity))]
                                    (assoc bullet
                                           :position (vector-wrap new-position WIDTH)
                                           :age (inc (bullet :age)))))
                                bullets))))

(defn cull-bullets [game]
  (update game :bullets (fn [bullets]
                          (remove
                           (fn [bullet]
                             (>= (bullet :age) MAX_BULLET_AGE))
                           bullets))))

(defn move-asteroids [game]
  (update game :asteroids (fn [asteroids]
                            (mapv
                             #(assoc %
                                     :position (vector-wrap (vector-add (:position %) (:velocity %)) WIDTH))
                             asteroids))))

(defn spawn-asteroid [size [x y]]
  (make-asteroid-at x y size))

(defn check-point-circle [point center radius]
  (let [result (rcol/check-collision-point-circle? (vec->point point) (vec->point center) (float radius))]
    (if (boolean? result) result (pos? result))))

(defn check-circles [center1 radius1 center2 radius2]
  (let [result (rcol/check-collision-circles? (vec->point center1) (float radius1) (vec->point center2) (float radius2))]
    (if (boolean? result) result (pos? result))))

(defn ship-collides-asteroid? [sps {:keys [position size]
                                    :as asteroid}]
  (not (every? false?
               (map (fn [point]
                      (check-point-circle point position (asteroid-radius asteroid)))
                    sps))))

(defn collide-ship [game]
  (let [sps (ship-points (:ship game))]
    (if (not (every? false? (map (partial ship-collides-asteroid? sps) (:asteroids game))))
      (assoc game :alive false)
      game)))

(defn bullet-collides-asteroid? [bullet asteroid]
  (check-circles (:position bullet) BULLET_RADIUS (:position asteroid) (asteroid-radius asteroid)))

(defn calc-collisions [bullets asteroids]
  (let [collisions (for [bullet bullets
                         asteroid asteroids
                         :when (bullet-collides-asteroid? bullet asteroid)]
                     [bullet asteroid])]
    collisions))

(defn explode-asteroid [asteroid]
  (if (> (asteroid :size) 1)
    [(spawn-asteroid (- (asteroid :size) 1) (asteroid :position))
     (spawn-asteroid (- (asteroid :size) 1) (asteroid :position))]
    []))

(defn collide-bullets [game]
  (let [asteroids (:asteroids game)
        bullets (:bullets game)
        collisions (calc-collisions bullets asteroids)
        collided-asteroids (mapv second collisions)
        collided-bullets (mapv first collisions)
        asteroids-remaining  (mapcat (fn [asteroid]
                                       (if (some #(= asteroid %) collided-asteroids)
                                         (explode-asteroid asteroid)
                                         [asteroid])) asteroids)
        bullets-remaining (remove (fn [bullet] (some #(= bullet %) collided-bullets)) bullets)]
    (assoc game :bullets bullets-remaining
           :asteroids asteroids-remaining)))

(defn bullet-firing-vector [ship]
  (let [angle (:orientation ship)
        magnitude 5]
    [(* (Math/cos angle) magnitude)
     (* (Math/sin angle) magnitude)]))

(defn spawn-bullet [ship]
  (let [bullet-velocity (vector-add (ship :velocity) (bullet-firing-vector ship))]
    (make-bullet (find-ship-center ship)  bullet-velocity)))

(defn handle-input [game]
  (let [game (cond-> game
               (rck/is-key-down? (:left enums/keyboard-key)) (update :ship (fn [ship] (assoc ship :orientation (- (:orientation ship) 0.05))))
               (rck/is-key-down? (:right enums/keyboard-key)) (update :ship (fn [ship] (assoc ship :orientation (+ (:orientation ship) 0.05))))
               (rck/is-key-down? (:up enums/keyboard-key)) (update :ship (fn [ship]
                                                                           (assoc ship :velocity (vector-add (:velocity ship) (ship-thrust-vector ship)))))
               (rck/is-key-down? (:down enums/keyboard-key)) (update :ship (fn [ship]
                                                                             (assoc ship :velocity (vector-sub (:velocity ship) (ship-thrust-vector ship))))))]
    (if (rck/is-key-pressed? (:space enums/keyboard-key))
      (if (:alive game)
        (update game :bullets (fn [bullets]
                                (conj bullets (spawn-bullet (:ship game)))))
        (initial-state))
      game)))

(defn handle-game [game]
  (-> game
      handle-input
      move-ship
      move-bullets
      move-asteroids
      collide-ship
      cull-bullets
      collide-bullets))

(defn handle-start [game]
  (if (rck/is-key-pressed? (:enter enums/keyboard-key))
    (assoc game :screen :game)
    game))

(def game-atom (atom (initial-state)))

;; SCALING ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn calculate-letterbox-scale
  "Calculate scale and offset for letterboxing to maintain aspect ratio"
  []
  (let [screen-width (rcw/get-screen-width)
        screen-height (rcw/get-screen-height)
        scale-x (/ screen-width (float VIRTUAL_WIDTH))
        scale-y (/ screen-height (float VIRTUAL_HEIGHT))
        scale (min scale-x scale-y)
        offset-x (/ (- screen-width (* VIRTUAL_WIDTH scale)) 2.0)
        offset-y (/ (- screen-height (* VIRTUAL_HEIGHT scale)) 2.0)]
    {:scale scale
     :offset-x offset-x
     :offset-y offset-y}))

(defn update-screen-state!
  "Update screen state when window is resized"
  []
  (when (or (rcw/is-window-resized?) (= 1 (:frame-counter @game-atom)))
    (reset! screen-state (calculate-letterbox-scale))))

(defn tick [{:keys [screen]
             :as game}]
  ;; Update debug stats (handles F1 toggle)
  (debug-stats/update!)

  ;; Handle F11 fullscreen toggle
  (when (rck/is-key-pressed? (:f11 enums/keyboard-key))
    (rcw/toggle-borderless-windowed!)
    (reset! screen-state (calculate-letterbox-scale)))

  ;; Update screen state on resize
  (update-screen-state!)

  ;; Update custom stats
  (debug-stats/set-custom-stat! :asteroids (count (:asteroids game)))
  (debug-stats/set-custom-stat! :bullets (count (:bullets game)))
  (debug-stats/set-custom-stat! :alive? (:alive game))

  (try
    (condp = screen
      :title (handle-start game)
      :game (handle-game game)
      :ending game)
    (catch Exception e
      (println "Exception in tick: " e)
      (Thread/sleep 1000))))

;; DRAW ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-asteroid [{:keys [position]
                      :as asteroid}]
  (let [x (int (Math/floor (first position)))
        y (int (Math/floor (second position)))
        radius (asteroid-radius asteroid)]
    (rsb/draw-circle! x y radius colors/white)))

(defn draw-asteroids [{:keys [asteroids]}]
  (doseq [asteroid asteroids]
    (draw-asteroid asteroid)))

(defn thrust-points [{:keys [ship]}]
  (let [[ship-center-x ship-center-y] (find-ship-center ship)
        p1 [(- ship-center-x 12) (+ ship-center-y 3)]
        p2 [(- ship-center-x 24) ship-center-y]
        p3 [(- ship-center-x 12) (- ship-center-y 3)]
        rotated-points (map (fn [p] (rotate-ship-point ship p)) [p1 p3 p2])
        translated-rotated-points (mapv #(vector-add [ship-center-x ship-center-y] %) rotated-points)]
    translated-rotated-points))

(defn retro-thrust-points1 [ship]
  (let [ship-center (find-ship-center ship)
        [ship-center-x ship-center-y] ship-center
        p1 [ship-center-x (- ship-center-y 7)]
        p2 [(+ ship-center-x 10) (- ship-center-y 9)]
        p3 [ship-center-x (- ship-center-y 11)]
        rotated-points (map (fn [p] (rotate-ship-point ship  p)) [p1 p2 p3])
        translated-rotated-points (mapv (fn [p] (vector-add ship-center p)) rotated-points)]
    translated-rotated-points))

(defn retro-thrust-points2 [ship]
  (let [ship-center (find-ship-center ship)
        [ship-center-x ship-center-y] ship-center
        p1 [ship-center-x (+ ship-center-y 7)]
        p2 [(+ ship-center-x 10) (+ ship-center-y 9)]
        p3 [ship-center-x (+ ship-center-y 11)]
        rotated-points (map (fn [p] (rotate-ship-point ship p)) [p1 p3 p2])
        translated-rotated-points (mapv (fn [p] (vector-add ship-center p)) rotated-points)]
    translated-rotated-points))

(defn draw-triangle [[v1 v2 v3] color]
  (rsb/draw-triangle! (vec->point v1) (vec->point v2) (vec->point v3) color))

(defn draw-thrust [game]
  (draw-triangle (thrust-points game) colors/orange))

(defn draw-retro-thrust [game]
  (draw-triangle (retro-thrust-points1 (:ship game)) colors/orange)
  (draw-triangle (retro-thrust-points2 (:ship game)) colors/orange))

(defn draw-ship [{:keys [frame-counter ship]
                  :as game}]
  (let [is-alternate-frame (< (mod frame-counter 5) 3)]
    (when (and (rck/is-key-down? (:up enums/keyboard-key)) is-alternate-frame)
      (draw-thrust game))
    (when (and (rck/is-key-down? (:down enums/keyboard-key)) is-alternate-frame)
      (draw-retro-thrust game))
    (draw-triangle (ship-points ship) colors/white)))

(defn draw-bullet [bullet]
  (let [[x y] (:position bullet)]
    (rsb/draw-circle! (int (Math/floor x)) (int (Math/floor y)) BULLET_RADIUS colors/white)))

(defn draw-bullets [{:keys [bullets]}]
  (doseq [bullet bullets]
    (draw-bullet bullet)))

(defn draw-dead []
  (let [text "DEAD"
        size 48
        width (rtd/measure-text text size)]
    (rtd/draw-text! text (int (- (quot WIDTH 2) (/ width 2))) (int (- (/ HEIGHT 2) 40)) size colors/red))
  (let [text "press SPACE to restart"
        size 16
        width (rtd/measure-text text size)]
    (rtd/draw-text! text (int (- (quot WIDTH 2) (/ width 2))) (int (+ (/ HEIGHT 2) 20)) size colors/white)))

(defn draw-game-content [game]
  (rcd/clear-background! colors/black)
  (draw-asteroids game)
  (if (:alive game)
    (draw-ship game)
    (draw-dead))
  (draw-bullets game)
  ;; Draw debug stats overlay (F1 to toggle)
  (debug-stats/draw!))

(defn draw-ending [game]
  (let [text "You DIED. Press ENTER to restart"
        size 20
        width (rtd/measure-text text size)]
    (rtd/draw-text! text (int (- (quot WIDTH 2) (/ width 2))) (int (/ HEIGHT 2)) size colors/white)))

(defn draw-title-content [_]
  (rcd/clear-background! colors/black)
  (let [text "press ENTER to start"
        size 20
        width (rtd/measure-text text size)]
    (rtd/draw-text! text (int (- (quot WIDTH 2) (/ width 2))) (int (/ HEIGHT 2)) size colors/white))
  (rtd/draw-text! "F1 for debug stats | F11 toggle fullscreen" 10 (- HEIGHT 30) 14 colors/gray)
  ;; Draw debug stats overlay
  (debug-stats/draw!))

(defn draw-to-render-texture
  "Draw game content to render texture at virtual resolution"
  [{:keys [screen]
    :as game}]
  (rtl/begin-texture-mode! @render-target)
  (condp = screen
    :title (draw-title-content game)
    :game (draw-game-content game)
    :ending (draw-ending game))
  (rtl/end-texture-mode!))

(defn draw-render-texture-to-screen
  "Draw the render texture scaled to fit screen with letterboxing"
  []
  (let [{:keys [scale offset-x offset-y]} @screen-state
        target @render-target
        ;; Source rectangle (entire render texture, flipped Y because OpenGL)
        source {:x 0.0
                :y (float VIRTUAL_HEIGHT)
                :width (float VIRTUAL_WIDTH)
                :height (float (- VIRTUAL_HEIGHT))}
        ;; Destination rectangle (scaled and centered on screen)
        dest {:x (float offset-x)
              :y (float offset-y)
              :width (float (* VIRTUAL_WIDTH scale))
              :height (float (* VIRTUAL_HEIGHT scale))}]
    (rtl/draw-texture-pro! (:texture target) source dest {:x 0.0
                                                          :y 0.0} 0.0 colors/white)))

(defn draw [{:keys [screen]
             :as game}]
  (try
    ;; First render game to virtual resolution texture
    (draw-to-render-texture game)

    ;; Then draw the texture scaled to screen with letterboxing
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/black) ; Black bars for letterboxing
    (draw-render-texture-to-screen)
    (rcd/end-drawing!)
    (catch Exception e
      (println "Exception in draw: " e)
      (Thread/sleep 1000))))

(defn update-fps [game]
  (let [last-time (:time game)
        acc (:time-acc game)
        newtime (System/nanoTime)
        diff (- newtime last-time)
        newacc (vec (take-last 100 (conj acc diff)))
        average-diff (/ (reduce + newacc) (count newacc))
        average-fps (long (/ 1000000000 average-diff))]
    (assoc game :dt (rct/get-frame-time)
           :time newtime
           :time-acc acc
           :avg-fps average-fps
           :frame-counter (inc (:frame-counter game)))))

(defn init []
  ;; Set config flags before creating window
  (rcw/set-config-flags! (bit-or (:window-resizable enums/config-flag)
                                 (:vsync-hint enums/config-flag)))

  ;; Get monitor size for fullscreen
  (rcw/init-window! VIRTUAL_WIDTH VIRTUAL_HEIGHT "Raylib Clojure Asteroids")

  ;; Create render texture at virtual resolution
  (reset! render-target (rtl/load-render-texture! VIRTUAL_WIDTH VIRTUAL_HEIGHT))

  ;; Start in true fullscreen (hides menu bar)
  (rcw/toggle-borderless-windowed!)

  ;; Calculate initial letterbox scaling
  (reset! screen-state (calculate-letterbox-scale))

  ;(rct/set-target-fps! 60)
  ;; Enable debug stats - press F1 to toggle
  (debug-stats/enable!))

(defn -main [& args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick (update-fps @game-atom))]
      (when-not (rcw/window-should-close?)
        (reset! game-atom game)
        (draw game)
        (recur))))
  ;; Cleanup
  (when @render-target
    (rtl/unload-render-texture! @render-target))
  (rcw/close-window!))

(comment
  (future (-main))
  ;;
  )
