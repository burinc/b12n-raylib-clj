(ns examples.input-multitouch
  "raylib [core] example - input multitouch

   Draws a numbered circle under every active touch point. On a desktop
   without a touchscreen the mouse registers as touch point 0, so one
   circle follows the cursor while a button is held.

   Difficulty: 1/4
   Based on: core/core_input_multitouch.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.gestures :as rcg]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

;; raylib's own cap. Points beyond this are ignored rather than grown into,
;; matching the C's fixed touchPositions[] array.
(def max-touch-points 10)

(defn touch-points
  "Active touch positions, capped and with the (0,0) no-touch sentinel dropped.
   raylib reports an unused slot as the origin, so a point exactly there is
   indistinguishable from no touch - the C makes the same trade."
  []
  (->> (range (min (rcg/get-touch-point-count) max-touch-points))
       (map (fn [i] [i (rcg/get-touch-position i)]))
       (filter (fn [[_ p]] (and (pos? (:x p)) (pos? (:y p)))))))

(defn init []
  (rcw/init-window! screen-width screen-height
                    "raylib [core] example - input multitouch")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn draw []
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (doseq [[i pos] (touch-points)]
    (rsb/draw-circle-v! pos (float 34.0) colors/orange)
    (rtd/draw-text! (str i) (- (int (:x pos)) 10) (- (int (:y pos)) 70) 40 colors/black))
  (rtd/draw-text! "touch the screen at multiple locations to get multiple balls"
                  10 10 20 colors/darkgray)
  (debug-stats/draw!)
  (rcd/end-drawing!))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (debug-stats/update!)
    (when-not (rcw/window-should-close?)
      (draw)
      (recur)))
  (rcw/close-window!))
