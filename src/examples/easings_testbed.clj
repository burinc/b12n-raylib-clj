(ns examples.easings-testbed
  "raylib [shapes] example - easings testbed

   Drive a ball with a different easing curve on each axis and watch how
   they combine. Pick the x curve with LEFT/RIGHT and the y curve with
   UP/DOWN, ENTER plays or pauses, SPACE restarts. Q/W change the duration
   in steps of 20, A/S hold for finer steps of 2. T toggles whether time
   stops at the duration or keeps running past it, which is how you see
   what a curve does outside its intended range.

   Both axes default to \"none\", so nothing moves until you choose a curve
   - that is the C's behaviour and it is deliberate, not a broken start.

   The 28 curves live in raylib.easings, a port of raylib's reasings.h.
   They were extracted rather than written inline because three other
   examples already carry private partial copies.

   Difficulty: 2/4
   Based on: shapes/shapes_easings_testbed.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.easings :as ease]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)
(def font-size 20)

(def d-step 20.0)
(def d-step-fine 2.0)
(def d-min 1.0)
(def d-max 10000.0)

;; The C appends a NoEase entry so "no easing selected" is just one more
;; index. Same idea here: the list is the 28 curves plus a terminal
;; ["None" nil], and a nil fn means "hold position".
(def curves (conj (vec ease/all) ["None" nil]))
(def none-index (dec (count curves)))

(def start-pos {:x 100.0 :y 100.0})
(def travel-x (- 700.0 170.0))
(def travel-y (- 400.0 170.0))

(defn initial-state []
  {:ball start-pos :t 0.0 :d 300.0 :paused true :bounded true
   :easing-x none-index :easing-y none-index})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height
                    "raylib [shapes] example - easings testbed")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn- cycle-index
  "Step through the curve list, wrapping at both ends including the trailing
   None entry."
  [i delta]
  (mod (+ i delta) (count curves)))

(defn- key-pressed? [k] (rck/is-key-pressed? (get enums/keyboard-key k)))
(defn- key-down? [k] (rck/is-key-down? (get enums/keyboard-key k)))

(defn tick [{:keys [t d paused bounded easing-x easing-y ball] :as state}]
  (debug-stats/update!)
  (let [bounded (if (key-pressed? :t) (not bounded) bounded)
        easing-x (cond (key-pressed? :right) (cycle-index easing-x 1)
                       (key-pressed? :left)  (cycle-index easing-x -1)
                       :else easing-x)
        easing-y (cond (key-pressed? :down) (cycle-index easing-y 1)
                       (key-pressed? :up)   (cycle-index easing-y -1)
                       :else easing-y)
        d (cond
            (and (key-pressed? :w) (< d (- d-max d-step))) (+ d d-step)
            (and (key-pressed? :q) (> d (+ d-min d-step))) (- d d-step)
            (and (key-down? :s) (< d (- d-max d-step-fine))) (+ d d-step-fine)
            (and (key-down? :a) (> d (+ d-min d-step-fine))) (- d d-step-fine)
            :else d)
        ;; Any control that changes what is being animated rewinds, so you
        ;; always see a curve from its start rather than mid-flight.
        reset? (or (some key-pressed? [:space :t :right :left :down :up :w :q])
                   (key-down? :s) (key-down? :a)
                   (and (key-pressed? :enter) bounded (>= t d)))
        paused (cond reset? true
                     (key-pressed? :enter) (not paused)
                     :else paused)
        state (assoc state :bounded bounded :easing-x easing-x :easing-y easing-y
                     :d d :paused paused)]
    (cond
      reset? (assoc state :t 0.0 :ball start-pos)
      (and (not paused) (or (not bounded) (< t d)))
      (let [fx (second (nth curves easing-x))
            fy (second (nth curves easing-y))]
        (assoc state
               :t (inc t)
               :ball {:x (if fx (fx t (:x start-pos) travel-x d) (:x ball))
                      :y (if fy (fy t (:y start-pos) travel-y d) (:y ball))}))
      :else state)))

(defn draw [{:keys [ball t d bounded easing-x easing-y]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rtd/draw-text! (str "Easing x: " (first (nth curves easing-x)))
                  20 font-size font-size colors/lightgray)
  (rtd/draw-text! (str "Easing y: " (first (nth curves easing-y)))
                  20 (* font-size 2) font-size colors/lightgray)
  (rtd/draw-text! (format "t (%s) = %.2f d = %.2f" (if bounded "b" "u") t d)
                  20 (* font-size 3) font-size colors/lightgray)
  (let [h (rcw/get-screen-height)]
    (rtd/draw-text! "Use ENTER to play or pause movement, use SPACE to restart"
                    20 (- h (* font-size 2)) font-size colors/lightgray)
    (rtd/draw-text! "Use Q and W or A and S keys to change duration"
                    20 (- h (* font-size 3)) font-size colors/lightgray)
    (rtd/draw-text! "Use LEFT or RIGHT keys to choose easing for the x axis"
                    20 (- h (* font-size 4)) font-size colors/lightgray)
    (rtd/draw-text! "Use UP or DOWN keys to choose easing for the y axis"
                    20 (- h (* font-size 5)) font-size colors/lightgray))
  (rsb/draw-circle-v! {:x (float (:x ball)) :y (float (:y ball))}
                      (float 16.0) colors/maroon)
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
