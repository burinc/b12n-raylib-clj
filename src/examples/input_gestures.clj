(ns examples.input-gestures
  "raylib [core] example - input gestures

   Perform gestures inside the test area and each newly detected one is
   appended to a log on the left. Only transitions are recorded, so holding
   a drag logs DRAG once rather than sixty times a second.

   On desktop the mouse stands in for a finger: click for TAP, click twice
   for DOUBLETAP, press and wait for HOLD, press and move for DRAG, and
   flick for the SWIPE gestures.

   Difficulty: 2/4
   Based on: core/core_input_gestures.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.gestures :as rcg]
   [raylib.core.collision :as rcol]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

;; raylib's gesture flags are a bit set, one bit per gesture.
(def gesture-none 0)
(def gesture-names
  {1 "GESTURE TAP"          2 "GESTURE DOUBLETAP"
   4 "GESTURE HOLD"         8 "GESTURE DRAG"
   16 "GESTURE SWIPE RIGHT" 32 "GESTURE SWIPE LEFT"
   64 "GESTURE SWIPE UP"    128 "GESTURE SWIPE DOWN"
   256 "GESTURE PINCH IN"   512 "GESTURE PINCH OUT"})

;; The C uses a fixed char[20][32] and wraps by clearing the whole array.
;; A vector with a cap does the same job; the log clears on overflow to
;; match, rather than scrolling.
(def max-gesture-strings 20)

(def touch-area {:x 220.0 :y 10.0
                 :width (- screen-width 230.0) :height (- screen-height 20.0)})

(defn initial-state [] {:log [] :last-gesture gesture-none})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [core] example - input gestures")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [log last-gesture] :as state}]
  (debug-stats/update!)
  (let [current (rcg/get-gesture-detected)
        touch (rcg/get-touch-position 0)
        ;; Log only on a change, and only inside the test area - otherwise a
        ;; held drag would append every frame.
        new? (and (not= current gesture-none)
                  (not= current last-gesture)
                  (pos? (rcol/check-collision-point-rec? touch touch-area))
                  (contains? gesture-names current))
        log (cond
              (not new?) log
              (>= (count log) max-gesture-strings) []
              :else (conj log (gesture-names current)))]
    (assoc state :log log :last-gesture current)))

(defn draw [{:keys [log]}]
  (let [current (rcg/get-gesture-detected)
        touch (rcg/get-touch-position 0)]
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/raywhite)

    (rsb/draw-rectangle-rec! touch-area colors/gray)
    (rsb/draw-rectangle! 225 15 (- screen-width 240) (- screen-height 30) colors/raywhite)
    (rtd/draw-text! "GESTURES TEST AREA" (- screen-width 270) (- screen-height 40) 20
                    (ru/fade colors/gray (float 0.5)))

    (doseq [[i s] (map-indexed vector log)]
      ;; Alternating row shading, faint enough to read as banding not stripes.
      (rsb/draw-rectangle! 10 (+ 30 (* 20 i)) 200 20
                           (ru/fade colors/lightgray (float (if (even? i) 0.5 0.3))))
      (rtd/draw-text! s 35 (+ 36 (* 20 i)) 10
                      (if (= i (dec (count log))) colors/maroon colors/darkgray)))

    (rsb/draw-rectangle-lines! 10 29 200 (- screen-height 50) colors/gray)
    (rtd/draw-text! "DETECTED GESTURES" 50 15 10 colors/gray)

    (when (not= current gesture-none)
      (rsb/draw-circle-v! touch (float 30.0) colors/maroon))

    (debug-stats/draw!)
    (rcd/end-drawing!)))

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
