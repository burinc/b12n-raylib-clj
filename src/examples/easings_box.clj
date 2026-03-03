(ns examples.easings-box
  "raylib [shapes] example - easings box

   Box animation using different easing functions for each phase:
   drop, flatten, rotate, expand, fade out.

   Difficulty: 2/4
   Based on: shapes/shapes_easings_box.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.utils :as ru]
   [raylib-ext :as ext]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

;; Easing functions (pure Clojure implementations)
(defn- ease-elastic-out [t b c d]
  (let [t (/ t d)]
    (if (>= t 1.0)
      (+ b c)
      (let [p (* d 0.3)
            s (/ p 4.0)]
        (+ b (* c (+ 1.0 (* (Math/pow 2 (* -10 t))
                             (Math/sin (* (/ (- t s) p) 2.0 Math/PI))))))))))

(defn- ease-bounce-out [t b c d]
  (let [t (/ t d)]
    (+ b (* c
            (cond
              (< t (/ 1.0 2.75))
              (* 7.5625 t t)

              (< t (/ 2.0 2.75))
              (let [t (- t (/ 1.5 2.75))]
                (+ (* 7.5625 t t) 0.75))

              (< t (/ 2.5 2.75))
              (let [t (- t (/ 2.25 2.75))]
                (+ (* 7.5625 t t) 0.9375))

              :else
              (let [t (- t (/ 2.625 2.75))]
                (+ (* 7.5625 t t) 0.984375)))))))

(defn- ease-quad-out [t b c d]
  (let [t (/ t d)]
    (+ b (* (- c) t (- t 2)))))

(defn- ease-circ-out [t b c d]
  (let [t (- (/ t d) 1)]
    (+ b (* c (Math/sqrt (- 1 (* t t)))))))

(defn- ease-sine-out [t b c d]
  (+ b (* c (Math/sin (* (/ t d) (/ Math/PI 2))))))

(defn initial-state []
  {:rec {:x (/ screen-width 2.0) :y -100.0 :width 100.0 :height 100.0}
   :rotation 0.0
   :alpha 1.0
   :state 0
   :frames-counter 0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - easings box")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [state frames-counter rec rotation alpha] :as s}]
  (debug-stats/update!)
  (if (rck/is-key-pressed? (:space enums/keyboard-key))
    (initial-state)
    (let [fc (inc frames-counter)]
      (case state
        ;; State 0: Move box down
        0 (let [y (ease-elastic-out (float fc) -100.0 (+ (/ screen-height 2.0) 100.0) 120.0)]
            (if (>= fc 120)
              (assoc s :rec (assoc rec :y y) :state 1 :frames-counter 0)
              (assoc s :rec (assoc rec :y y) :frames-counter fc)))

        ;; State 1: Scale to horizontal bar
        1 (let [h (ease-bounce-out (float fc) 100.0 -90.0 120.0)
                w (ease-bounce-out (float fc) 100.0 (float screen-width) 120.0)]
            (if (>= fc 120)
              (assoc s :rec (assoc rec :height h :width w) :state 2 :frames-counter 0)
              (assoc s :rec (assoc rec :height h :width w) :frames-counter fc)))

        ;; State 2: Rotate
        2 (let [rot (ease-quad-out (float fc) 0.0 270.0 240.0)]
            (if (>= fc 240)
              (assoc s :rotation rot :state 3 :frames-counter 0)
              (assoc s :rotation rot :frames-counter fc)))

        ;; State 3: Increase height to fill screen
        3 (let [h (ease-circ-out (float fc) 10.0 (float screen-width) 120.0)]
            (if (>= fc 120)
              (assoc s :rec (assoc rec :height h) :state 4 :frames-counter 0)
              (assoc s :rec (assoc rec :height h) :frames-counter fc)))

        ;; State 4: Fade out
        4 (let [a (ease-sine-out (float fc) 1.0 -1.0 160.0)]
            (if (>= fc 160)
              (assoc s :alpha a :state 5 :frames-counter 0)
              (assoc s :alpha a :frames-counter fc)))

        ;; State 5: Done
        s))))

(defn draw [{:keys [rec rotation alpha]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (ext/draw-rectangle-pro!
   {:x (float (:x rec)) :y (float (:y rec))
    :width (float (:width rec)) :height (float (:height rec))}
   {:x (float (/ (:width rec) 2)) :y (float (/ (:height rec) 2))}
   (float rotation)
   (ru/fade colors/black (float (max 0.0 alpha))))

  (rtd/draw-text! "PRESS [SPACE] TO RESET BOX ANIMATION!"
                  10 (- screen-height 25) 20 colors/lightgray)

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
