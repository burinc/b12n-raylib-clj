(ns examples.delta-time
  "raylib [core] example - delta time

   Two circles crossing the screen. The red one advances by
   (get-frame-time) * speed, the blue one by a fixed step per frame. Change
   the FPS target with the scroll wheel and the difference is the whole
   lesson: the red circle keeps its real-world speed, the blue one speeds
   up or slows down with the frame rate.

   Difficulty: 1/4
   Based on: core/core_delta_time.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def speed 10.0)
(def circle-radius 32.0)

;; The C multiplies the delta-driven circle by an arbitrary 6.0 so the two
;; circles travel at a similar pace at 60 fps - otherwise the comparison is
;; hard to see. Kept, with the reason stated.
(def delta-scale 6.0)

(defn initial-state []
  {:current-fps 60
   :delta-x 0.0
   :frame-x 0.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [core] example - delta time")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn- wrap [x] (if (> x screen-width) 0.0 x))

(defn tick [{:keys [current-fps] :as state}]
  (debug-stats/update!)
  (let [wheel (rcm/get-mouse-wheel-move)
        fps (if (zero? wheel)
              current-fps
              (let [n (max 0 (+ current-fps (int wheel)))]
                (rct/set-target-fps! n)
                n))
        reset? (rck/is-key-pressed? (:r enums/keyboard-key))]
    (if reset?
      (assoc state :current-fps fps :delta-x 0.0 :frame-x 0.0)
      (-> state
          (assoc :current-fps fps)
          (update :delta-x #(wrap (+ % (* (rct/get-frame-time) delta-scale speed))))
          (update :frame-x #(wrap (+ % (* 0.1 speed))))))))

(defn draw [{:keys [current-fps delta-x frame-x]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rsb/draw-circle-v! {:x (float delta-x) :y (float (/ screen-height 3.0))}
                      (float circle-radius) colors/red)
  (rsb/draw-circle-v! {:x (float frame-x) :y (float (* screen-height (/ 2.0 3.0)))}
                      (float circle-radius) colors/blue)

  (rtd/draw-text! (if (<= current-fps 0)
                    (format "FPS: unlimited (%d)" (rct/get-fps))
                    (format "FPS: %d (target: %d)" (rct/get-fps) current-fps))
                  10 10 20 colors/darkgray)
  (rtd/draw-text! (format "Frame time: %05.2f ms" (* 1000.0 (rct/get-frame-time)))
                  10 30 20 colors/darkgray)
  (rtd/draw-text! "Use the scroll wheel to change the fps limit, r to reset"
                  10 50 20 colors/darkgray)
  (rtd/draw-text! "FUNC: x += GetFrameTime()*speed" 10 90 20 colors/red)
  (rtd/draw-text! "FUNC: x += speed" 10 240 20 colors/blue)

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
