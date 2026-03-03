(ns examples.mouse-trail
  "raylib [shapes] example - mouse trail

   Draws a trail of circles following the mouse cursor.
   Older positions fade out and shrink.

   Difficulty: 1/4
   Based on: shapes/shapes_mouse_trail.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.mouse :as rcm]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)
(def MAX-TRAIL-LENGTH 30)

(defn initial-state []
  {:trail (vec (repeat MAX-TRAIL-LENGTH {:x 0.0 :y 0.0}))})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - mouse trail")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [trail] :as state}]
  (debug-stats/update!)
  (let [mouse (rcm/get-mouse-position)
        ;; Shift all positions back by one, drop oldest, prepend current
        new-trail (into [mouse] (subvec trail 0 (dec MAX-TRAIL-LENGTH)))]
    (assoc state :trail new-trail)))

(defn draw [{:keys [trail]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/black)

  ;; Draw trail circles (oldest to newest)
  (doseq [i (range MAX-TRAIL-LENGTH)]
    (let [{:keys [x y]} (nth trail i)]
      (when (or (not= x 0.0) (not= y 0.0))
        (let [ratio (/ (float (- MAX-TRAIL-LENGTH i)) MAX-TRAIL-LENGTH)
              trail-color (ru/fade colors/skyblue (float (+ (* ratio 0.5) 0.5)))
              trail-radius (* 15.0 ratio)]
          (rsb/draw-circle-v! {:x x :y y} (float trail-radius) trail-color)))))

  ;; Draw current mouse position
  (let [mouse (rcm/get-mouse-position)]
    (rsb/draw-circle-v! mouse (float 15.0) colors/white))

  (rtd/draw-text! "Move the mouse to see the trail effect!" 10 (- screen-height 30) 20 colors/lightgray)

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
