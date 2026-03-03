(ns examples.easings-ball
  "raylib [shapes] example - easings ball

   Ball animation using easing functions: elastic position,
   elastic radius growth, and cubic alpha fade.

   Difficulty: 2/4
   Based on: shapes/shapes_easings_ball.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
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

;; Easing functions (pure Clojure implementations of reasings.h)
(defn ease-elastic-out
  "Elastic easing out"
  [t b c d]
  (let [t (/ t d)]
    (if (== t 0.0) b
        (if (== t 1.0) (+ b c)
            (let [p (* d 0.3)
                  s (/ p 4.0)]
              (+ (* c (Math/pow 2.0 (* -10.0 t))
                    (Math/sin (/ (* (- t s) 2.0 Math/PI) p)))
                 c b))))))

(defn ease-elastic-in
  "Elastic easing in"
  [t b c d]
  (let [t (/ t d)]
    (if (== t 0.0) b
        (if (== t 1.0) (+ b c)
            (let [p (* d 0.3)
                  s (/ p 4.0)
                  t (dec t)]
              (+ (- (* c (Math/pow 2.0 (* 10.0 t))
                       (Math/sin (/ (* (- t s) 2.0 Math/PI) p))))
                 b))))))

(defn ease-cubic-out
  "Cubic easing out"
  [t b c d]
  (let [t (dec (/ t d))]
    (+ (* c (inc (* t t t))) b)))

(defn initial-state []
  {:ball-x -100
   :ball-radius 20
   :ball-alpha 0.0
   :state 0
   :frames 0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - easings ball")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [state frames] :as game-state}]
  (debug-stats/update!)
  (let [frames (inc frames)]
    (cond
      ;; State 0: Move ball position X with elastic easing
      (= state 0)
      (let [ball-x (int (ease-elastic-out (float frames) -100.0 (+ (/ screen-width 2.0) 100.0) 120.0))]
        (if (>= frames 120)
          (assoc game-state :ball-x ball-x :frames 0 :state 1)
          (assoc game-state :ball-x ball-x :frames frames)))

      ;; State 1: Increase ball radius with elastic easing
      (= state 1)
      (let [ball-radius (int (ease-elastic-in (float frames) 20.0 500.0 200.0))]
        (if (>= frames 200)
          (assoc game-state :ball-radius ball-radius :frames 0 :state 2)
          (assoc game-state :ball-radius ball-radius :frames frames)))

      ;; State 2: Change ball alpha with cubic easing
      (= state 2)
      (let [ball-alpha (ease-cubic-out (float frames) 0.0 1.0 200.0)]
        (if (>= frames 200)
          (assoc game-state :ball-alpha ball-alpha :frames 0 :state 3)
          (assoc game-state :ball-alpha ball-alpha :frames frames)))

      ;; State 3: Wait for ENTER to replay
      (= state 3)
      (if (rck/is-key-pressed? (:enter enums/keyboard-key))
        (initial-state)
        game-state)

      :else game-state)))

(defn draw [{:keys [ball-x ball-radius ball-alpha state]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (when (>= state 2)
    (rsb/draw-rectangle! 0 0 screen-width screen-height colors/green))

  (rsb/draw-circle! ball-x 200 (float ball-radius)
                     (ru/fade colors/red (float (- 1.0 ball-alpha))))

  (when (= state 3)
    (rtd/draw-text! "PRESS [ENTER] TO PLAY AGAIN!" 240 200 20 colors/black))

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
