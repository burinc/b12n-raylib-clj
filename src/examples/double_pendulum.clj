(ns examples.double-pendulum
  "raylib [shapes] example - double pendulum

   Double pendulum physics simulation with trails rendered
   to a framebuffer. Demonstrates chaotic motion.

   Difficulty: 2/4
   Based on: shapes/shapes_double_pendulum.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.colors :as colors]
   [raylib.utils :as ru]
   [raylib-ext :as ext]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)
(def ^:const SIMULATION-STEPS 30)
(def ^:const G 9.81)
(def ^:const DEG2RAD (/ Math/PI 180.0))
(def ^:const RAD2DEG (/ 180.0 Math/PI))

(defn- pendulum-endpoint [l theta]
  {:x (float (* 10 l (Math/sin theta)))
   :y (float (* 10 l (Math/cos theta)))})

(defn- double-pendulum-endpoint [l1 theta1 l2 theta2]
  (let [e1 (pendulum-endpoint l1 theta1)
        e2 (pendulum-endpoint l2 theta2)]
    {:x (float (+ (:x e1) (:x e2)))
     :y (float (+ (:y e1) (:y e2)))}))

(defn initial-state []
  (let [l1 15.0 m1 0.2 theta1 (* DEG2RAD 170) w1 0.0
        l2 15.0 m2 0.1 theta2 (* DEG2RAD 0) w2 0.0
        prev (double-pendulum-endpoint l1 theta1 l2 theta2)]
    {:l1 l1 :m1 m1 :theta1 theta1 :w1 w1
     :l2 l2 :m2 m2 :theta2 theta2 :w2 w2
     :length-scaler 0.1
     :prev-pos {:x (float (+ (/ screen-width 2.0) (:x prev)))
                :y (float (+ (- (/ screen-height 2.0) 100) (:y prev)))}
     :target nil}))

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - double pendulum")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  (let [target (ext/load-render-texture! screen-width screen-height)]
    (swap! game-atom assoc :target target)
    ;; Clear the render texture to black
    (ext/begin-texture-mode! target)
    (rcd/clear-background! colors/black)
    (ext/end-texture-mode!)))

(defn- physics-step [{:keys [l1 m1 theta1 w1 l2 m2 theta2 w2 length-scaler]} step]
  (let [big-l1 (* l1 length-scaler)
        big-l2 (* l2 length-scaler)
        total-m (+ m1 m2)
        step2 (* step step)
        delta (- theta1 theta2)
        sin-d (Math/sin delta)
        cos-d (Math/cos delta)
        cos-2d (Math/cos (* 2 delta))
        ww1 (* w1 w1)
        ww2 (* w2 w2)
        ;; Angular acceleration for pendulum 1
        a1 (/ (+ (* (- G) (+ (* 2 m1) m2) (Math/sin theta1))
               (* (- m2) G (Math/sin (- theta1 (* 2 theta2))))
               (* -2 sin-d m2 (+ (* ww2 big-l2) (* ww1 big-l1 cos-d))))
            (* big-l1 (- (+ (* 2 m1) m2) (* m2 cos-2d))))
        ;; Angular acceleration for pendulum 2
        a2 (/ (* 2 sin-d (+ (* ww1 big-l1 total-m)
                             (* G total-m (Math/cos theta1))
                             (* ww2 big-l2 m2 cos-d)))
            (* big-l2 (- (+ (* 2 m1) m2) (* m2 cos-2d))))
        ;; Update angles
        new-theta1 (+ theta1 (* w1 step) (* 0.5 a1 step2))
        new-theta2 (+ theta2 (* w2 step) (* 0.5 a2 step2))
        ;; Update angular velocities
        new-w1 (+ w1 (* a1 step))
        new-w2 (+ w2 (* a2 step))]
    {:theta1 new-theta1 :theta2 new-theta2
     :w1 new-w1 :w2 new-w2}))

(defn tick [{:keys [target l1 l2 theta1 theta2 prev-pos] :as state}]
  (debug-stats/update!)
  (let [dt (rct/get-frame-time)
        step (/ dt SIMULATION-STEPS)
        ;; Run simulation steps
        result (reduce
                (fn [acc _]
                  (let [s (merge state acc)
                        r (physics-step s step)]
                    (merge acc r)))
                {:theta1 theta1 :theta2 theta2 :w1 (:w1 state) :w2 (:w2 state)}
                (range SIMULATION-STEPS))
        ;; Calculate new endpoint position
        cur (double-pendulum-endpoint l1 (:theta1 result) l2 (:theta2 result))
        cur-pos {:x (float (+ (/ screen-width 2.0) (:x cur)))
                 :y (float (+ (- (/ screen-height 2.0) 100) (:y cur)))}]
    ;; Draw trail to render texture
    (when target
      (ext/begin-texture-mode! target)
      ;; Fade effect - smaller alpha = longer trails
      (ext/draw-rectangle-rec!
       {:x 0.0 :y 0.0 :width (float screen-width) :height (float screen-height)}
       (ru/fade colors/black (float 0.01)))
      ;; Draw trail segment
      (ext/draw-circle-v! prev-pos (float 2.0) colors/red)
      (ext/draw-line-ex! prev-pos cur-pos (float 4.0) colors/red)
      (ext/end-texture-mode!))
    (merge state result {:prev-pos cur-pos})))

(defn draw [{:keys [target l1 l2 theta1 theta2]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/black)

  ;; Draw trails texture (flipped vertically because render textures are upside down)
  (when target
    (let [tex (:texture target)]
      (ext/draw-texture-rec!
       tex
       {:x 0.0 :y 0.0 :width (float (:width tex)) :height (float (- (:height tex)))}
       {:x 0.0 :y 0.0}
       colors/white)))

  ;; Draw pendulum arms
  (let [pivot-x (/ screen-width 2.0)
        pivot-y (- (/ screen-height 2.0) 100)
        line-thick 20.0
        e1 (pendulum-endpoint l1 theta1)]
    ;; First arm
    (ext/draw-rectangle-pro!
     {:x (float pivot-x) :y (float pivot-y)
      :width (float (* 10 l1)) :height (float line-thick)}
     {:x 0.0 :y (float (* line-thick 0.5))}
     (float (- 90 (* RAD2DEG theta1)))
     colors/raywhite)
    ;; Second arm
    (ext/draw-rectangle-pro!
     {:x (float (+ pivot-x (:x e1))) :y (float (+ pivot-y (:y e1)))
      :width (float (* 10 l2)) :height (float line-thick)}
     {:x 0.0 :y (float (* line-thick 0.5))}
     (float (- 90 (* RAD2DEG theta2)))
     colors/raywhite))

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
  (when-let [target (:target @game-atom)]
    (ext/unload-render-texture! target))
  (rcw/close-window!))
