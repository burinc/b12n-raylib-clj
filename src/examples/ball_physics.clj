(ns examples.ball-physics
  "raylib [shapes] example - ball physics

   Grab and throw balls with the mouse. Right-click to spawn
   new balls. Scroll to change gravity. Middle-click to shake.

   Difficulty: 2/4
   Based on: shapes/shapes_ball_physics.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.mouse :as rcm]
   [raylib.core.keyboard :as rck]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(defn make-ball [x y]
  {:pos {:x (float x) :y (float y)}
   :vel {:x (float (- (rand-int 601) 300)) :y (float (- (rand-int 601) 300))}
   :ppos {:x 0.0 :y 0.0}
   :radius (+ 20.0 (rand-int 31))
   :friction 0.99
   :elasticity 0.9
   :color {:r (rand-int 256) :g (rand-int 256) :b (rand-int 256) :a 255}
   :grabbed false})

(defn initial-state []
  {:balls [(assoc (make-ball (/ screen-width 2.0) (/ screen-height 2.0))
                  :vel {:x 200.0 :y 200.0}
                  :radius 40.0
                  :color colors/blue)]
   :grabbed-idx nil
   :press-offset {:x 0.0 :y 0.0}
   :gravity 100.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - ball physics")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn- dist [p1 p2]
  (Math/hypot (- (:x p1) (:x p2)) (- (:y p1) (:y p2))))

(defn- clamp-ball [{:keys [pos vel radius elasticity] :as ball}]
  (let [{:keys [x y]} pos
        {:keys [x vx y vy]}
        (cond-> {:x x :vx (:x vel) :y y :vy (:y vel)}
          (>= (+ x radius) screen-width)
          (assoc :x (- screen-width radius) :vx (* (- (:x vel)) elasticity))
          (<= (- x radius) 0)
          (assoc :x radius :vx (* (- (:x vel)) elasticity))
          (>= (+ y radius) screen-height)
          (assoc :y (- screen-height radius) :vy (* (- (:y vel)) elasticity))
          (<= (- y radius) 0)
          (assoc :y radius :vy (* (- (:y vel)) elasticity)))]
    (assoc ball :pos {:x x :y y} :vel {:x vx :y vy})))

(defn- update-free-ball [ball dt gravity]
  (let [{:keys [pos vel friction]} ball
        new-pos {:x (+ (:x pos) (* (:x vel) dt))
                 :y (+ (:y pos) (* (:y vel) dt))}
        new-vel {:x (* (:x vel) friction)
                 :y (+ (* (:y vel) friction) gravity)}]
    (clamp-ball (assoc ball :pos new-pos :vel new-vel))))

(defn- update-grabbed-ball [ball mouse-pos press-offset dt]
  (let [new-pos {:x (- (:x mouse-pos) (:x press-offset))
                 :y (- (:y mouse-pos) (:y press-offset))}
        new-vel {:x (/ (- (:x new-pos) (:x (:ppos ball))) (max dt 0.001))
                 :y (/ (- (:y new-pos) (:y (:ppos ball))) (max dt 0.001))}]
    (assoc ball :pos new-pos :vel new-vel :ppos new-pos)))

(defn tick [{:keys [balls grabbed-idx press-offset gravity] :as state}]
  (debug-stats/update!)
  (let [dt (rct/get-frame-time)
        mouse-pos (rcm/get-mouse-position)
        left-pressed (rcm/is-mouse-button-pressed? (:left enums/mouse-button))
        left-released (rcm/is-mouse-button-released? (:left enums/mouse-button))
        right-pressed (rcm/is-mouse-button-pressed? (:right enums/mouse-button))
        mid-pressed (rcm/is-mouse-button-pressed? (:middle enums/mouse-button))
        wheel (rcm/get-mouse-wheel-move)

        ;; Grab detection
        [grabbed-idx press-offset]
        (if (and left-pressed (nil? grabbed-idx))
          (let [hit (first (keep-indexed
                            (fn [i ball]
                              (let [off {:x (- (:x mouse-pos) (:x (:pos ball)))
                                         :y (- (:y mouse-pos) (:y (:pos ball)))}]
                                (when (<= (Math/hypot (:x off) (:y off)) (:radius ball))
                                  [i off])))
                            (reverse balls)))]
            (if hit
              [(- (dec (count balls)) (first hit)) (second hit)]
              [grabbed-idx press-offset]))
          [grabbed-idx press-offset])

        ;; Release
        [grabbed-idx balls]
        (if (and left-released grabbed-idx)
          [nil (update balls grabbed-idx assoc :grabbed false)]
          [grabbed-idx (if grabbed-idx
                         (update balls grabbed-idx assoc :grabbed true)
                         balls)])

        ;; Spawn new ball
        balls (if (and right-pressed (< (count balls) 5000))
                (conj balls (make-ball (:x mouse-pos) (:y mouse-pos)))
                balls)

        ;; Shake
        balls (if mid-pressed
                (mapv (fn [ball]
                        (if (:grabbed ball)
                          ball
                          (assoc ball :vel {:x (float (- (rand-int 4001) 2000))
                                           :y (float (- (rand-int 4001) 2000))})))
                      balls)
                balls)

        ;; Gravity change
        gravity (+ gravity (* wheel 5.0))

        ;; Update physics
        balls (mapv (fn [i ball]
                      (if (= i grabbed-idx)
                        (update-grabbed-ball ball mouse-pos press-offset dt)
                        (update-free-ball ball dt gravity)))
                    (range) balls)]
    (assoc state
           :balls balls
           :grabbed-idx grabbed-idx
           :press-offset press-offset
           :gravity gravity)))

(defn draw [{:keys [balls gravity]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (doseq [{:keys [pos radius color]} balls]
    (rsb/draw-circle-v! pos (float radius) color)
    (rsb/draw-circle-lines-v! pos (float radius) colors/black))

  (rtd/draw-text! "grab a ball by pressing with the mouse and throw it by releasing" 10 10 10 colors/darkgray)
  (rtd/draw-text! "right click to create new balls" 10 30 10 colors/darkgray)
  (rtd/draw-text! "use mouse wheel to change gravity" 10 50 10 colors/darkgray)
  (rtd/draw-text! "middle click to shake" 10 70 10 colors/darkgray)
  (rtd/draw-text! (format "BALL COUNT: %d" (count balls)) 10 (- screen-height 70) 20 colors/black)
  (rtd/draw-text! (format "GRAVITY: %.2f" gravity) 10 (- screen-height 40) 20 colors/black)

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
  (rcw/close-window!))
