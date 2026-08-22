(ns examples.starfield-effect
  "raylib [shapes] example - starfield effect

   Classic starfield simulation. Stars fly towards the camera
   with perspective projection. Toggle between lines and circles.

   Difficulty: 2/4
   Based on: shapes/shapes_starfield_effect.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.utils :as ru]
   [raylib.shapes.basic :as rsb]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)
(def star-count 420)

(defn- make-star []
  {:x (float (ru/get-random-value (- (/ screen-width 2)) (/ screen-width 2)))
   :y (float (ru/get-random-value (- (/ screen-height 2)) (/ screen-height 2)))
   :z (float 1.0)})

(defn initial-state []
  {:stars (vec (repeatedly star-count make-star))
   :speed (float (/ 10.0 9.0))
   :draw-lines true
   :bg-color (ru/color-lerp colors/darkblue colors/black (float 0.69))})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - starfield effect")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn- star-screen-pos [{:keys [x y z]}]
  {:x (+ (* screen-width 0.5) (/ x z))
   :y (+ (* screen-height 0.5) (/ y z))})

(defn- star-offscreen? [screen-pos]
  (or (< (:x screen-pos) 0) (< (:y screen-pos) 0)
      (> (:x screen-pos) screen-width) (> (:y screen-pos) screen-height)))

(defn- update-star [star dt speed]
  (let [new-z (- (:z star) (* dt speed))
        spos (star-screen-pos (assoc star :z new-z))]
    (if (or (< new-z 0.0) (star-offscreen? spos))
      (make-star)
      (assoc star :z (float new-z)))))

(defn tick [{:keys [speed draw-lines] :as state}]
  (debug-stats/update!)
  (let [wheel-move (rcm/get-mouse-wheel-move)
        speed (cond-> (+ speed (* 2.0 (/ wheel-move 9.0)))
                (< (+ speed (* 2.0 (/ wheel-move 9.0))) 0.0) (max 0.1)
                (> (+ speed (* 2.0 (/ wheel-move 9.0))) 2.0) (min 2.0))
        speed (max 0.1 (min 2.0 speed))
        draw-lines (if (rck/is-key-pressed? (:space enums/keyboard-key))
                     (not draw-lines)
                     draw-lines)
        dt (rct/get-frame-time)]
    (assoc state
           :speed speed
           :draw-lines draw-lines
           :stars (mapv #(update-star % dt speed) (:stars state)))))

(defn- clamp [v mn mx]
  (max mn (min mx v)))

(defn- lerp [t a b]
  (+ a (* t (- b a))))

(defn draw [{:keys [stars speed draw-lines bg-color]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! bg-color)

  (doseq [star stars]
    (let [spos (star-screen-pos star)]
      (if draw-lines
        ;; Draw lines from old position to current
        (let [t (clamp (+ (:z star) (/ 1.0 32.0)) 0.0 1.0)]
          (when (> (- t (:z star)) 1e-3)
            (let [start-pos {:x (float (+ (* screen-width 0.5) (/ (:x star) t)))
                             :y (float (+ (* screen-height 0.5) (/ (:y star) t)))}]
              (rsb/draw-line-v! start-pos
                                {:x (float (:x spos)) :y (float (:y spos))}
                                colors/raywhite))))
        ;; Draw circles
        (let [radius (lerp (:z star) 1.0 5.0)]
          (rsb/draw-circle-v! {:x (float (:x spos)) :y (float (:y spos))}
                              (float radius) colors/raywhite)))))

  (rtd/draw-text! (format "[MOUSE WHEEL] Current Speed: %.0f" (* 9.0 (/ speed 2.0)))
                  10 40 20 colors/raywhite)
  (rtd/draw-text! (format "[SPACE] Current draw mode: %s" (if draw-lines "Lines" "Circles"))
                  10 70 20 colors/raywhite)
  (rtd/draw-fps! 10 10)
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
