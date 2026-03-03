(ns examples.easings-rectangles
  "raylib [shapes] example - easings rectangles

   Grid of rectangles that shrink and rotate using easing functions.
   Press SPACE to restart the animation.

   Difficulty: 3/4
   Based on: shapes/shapes_easings_rectangles.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib-ext :as ext]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)
(def ^:const RECS-WIDTH 50)
(def ^:const RECS-HEIGHT 50)
(def ^:const MAX-RECS-X (/ 800 RECS-WIDTH))
(def ^:const MAX-RECS-Y (/ 450 RECS-HEIGHT))
(def ^:const PLAY-TIME 240)

;; Easing functions
(defn- ease-circ-out [t b c d]
  (let [t (- (/ t d) 1.0)]
    (+ b (* c (Math/sqrt (- 1.0 (* t t)))))))

(defn- ease-linear-in [t b c d]
  (+ b (* c (/ t d))))

(defn- make-recs []
  (vec (for [y (range MAX-RECS-Y)
             x (range MAX-RECS-X)]
         {:x (float (+ (/ RECS-WIDTH 2.0) (* RECS-WIDTH x)))
          :y (float (+ (/ RECS-HEIGHT 2.0) (* RECS-HEIGHT y)))
          :width (float RECS-WIDTH)
          :height (float RECS-HEIGHT)})))

(defn initial-state []
  {:recs (make-recs)
   :rotation 0.0
   :frames-counter 0
   :state 0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - easings rectangles")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [state frames-counter] :as s}]
  (debug-stats/update!)
  (case state
    ;; Playing
    0 (let [fc (inc frames-counter)
            new-h (max 0.0 (ease-circ-out (float fc) (float RECS-HEIGHT) (float (- RECS-HEIGHT)) (float PLAY-TIME)))
            new-w (max 0.0 (ease-circ-out (float fc) (float RECS-WIDTH) (float (- RECS-WIDTH)) (float PLAY-TIME)))
            rotation (ease-linear-in (float fc) 0.0 360.0 (float PLAY-TIME))
            done? (and (<= new-h 0) (<= new-w 0))]
        (assoc s
               :frames-counter fc
               :rotation rotation
               :state (if done? 1 0)
               :recs (mapv #(assoc % :width (float new-w) :height (float new-h)) (:recs s))))

    ;; Finished - press SPACE to restart
    1 (if (rck/is-key-pressed? (:space enums/keyboard-key))
        (initial-state)
        s)

    s))

(defn draw [{:keys [recs rotation state]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (if (= state 0)
    (doseq [rec recs]
      (ext/draw-rectangle-pro!
       rec
       {:x (float (/ (:width rec) 2)) :y (float (/ (:height rec) 2))}
       (float rotation)
       colors/red))
    (rtd/draw-text! "PRESS [SPACE] TO PLAY AGAIN!" 240 200 20 colors/gray))

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
