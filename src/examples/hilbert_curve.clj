(ns examples.hilbert-curve
  "raylib [shapes] example - hilbert curve

   A Hilbert curve drawn stroke by stroke, hue cycling along its length. The
   order spinner sets how many times the pattern subdivides, and changing
   either the order or the total size regenerates the path - replaying the
   animation if ANIMATE is ticked.

   The curve is built by `hilbert-step`, which maps an index to a grid cell
   directly rather than by recursion: it reads the index two bits at a time,
   and each pair says how to transform the point accumulated so far. Watch
   the case-2 branch - in the C it FALLS THROUGH into case 1, so it adds the
   quadrant length to BOTH axes, not just to x. That fallthrough is easy to
   lose in translation and turns the curve inside out when you do.

   Difficulty: 3/4
   Based on: shapes/shapes_hilbert_curve.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.shapes.basic :as rsb]
   [raylib.raygui :as gui]
   [raylib.utils :as ru]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

;; The four base cells of a first-order curve, in visiting order.
(def base-points [{:x 0.0 :y 0.0} {:x 0.0 :y 1.0} {:x 1.0 :y 1.0} {:x 1.0 :y 0.0}])

(defn hilbert-step
  "Grid cell visited at `index` on a curve of `order`.

   Reads `index` two bits at a time from the bottom. The lowest pair picks
   one of the four base cells; each higher pair then reflects, swaps or
   offsets the point into the right quadrant of the next-larger square."
  [order index]
  (loop [v (nth base-points (bit-and index 3))
         idx index
         j 1]
    (if (>= j order)
      v
      (let [idx (bit-shift-right idx 2)
            quadrant (bit-and idx 3)
            len (bit-shift-left 1 j)
            v (case quadrant
                ;; Reflect across the diagonal.
                0 {:x (:y v) :y (:x v)}
                ;; Straight up.
                1 {:x (:x v) :y (+ (:y v) len)}
                ;; The C falls through from 2 into 1, so BOTH axes shift.
                2 {:x (+ (:x v) len) :y (+ (:y v) len)}
                ;; Reflect across the anti-diagonal, into the far corner.
                3 {:x (- (* 2 len) 1 (:y v)) :y (- len 1 (:x v))})]
        (recur v idx (inc j))))))

(defn hilbert-path
  "Every point of the curve, scaled to `size` and centred in its cell."
  [order size]
  (let [n (bit-shift-left 1 order)
        len (/ size n)]
    (mapv (fn [i]
            (let [{:keys [x y]} (hilbert-step order i)]
              {:x (+ (* x len) (/ len 2.0)) :y (+ (* y len) (/ len 2.0))}))
          (range (* n n)))))

(defn initial-state []
  (let [order 2 size (double screen-height)]
    {:order order :size size :thick 2.0 :animate? true
     :path (hilbert-path order size) :counter 0}))

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - hilbert curve")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn regenerate
  "Rebuild the path when the order or the integer size changes. The C
   compares size as an int even though the slider is a float, so a
   sub-pixel drag does not thrash the whole curve."
  [{:keys [order size animate?] :as state} prev-order prev-size]
  (if (and (= order prev-order) (= (int size) (int prev-size)))
    state
    (let [path (hilbert-path order size)]
      (assoc state :path path :counter (if animate? 0 (count path))))))

(defn tick [state]
  (debug-stats/update!)
  state)

(defn draw [{:keys [path counter thick order size animate?] :as state}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (let [total (count path)
        ;; While the animation is running only the first `counter` strokes
        ;; are drawn; once it catches up the whole path stays on screen.
        shown (if (< counter total) counter (dec total))]
    (doseq [i (range 1 (inc shown))]
      (rsb/draw-line-ex! (nth path i) (nth path (dec i)) (float thick)
                         (ru/color-from-hsv (float (* (/ (double i) total) 360.0)) 1.0 1.0))))

  (let [animate? (gui/check-box {:x 450.0 :y 50.0 :width 20.0 :height 20.0}
                                "ANIMATE GENERATION ON CHANGE" animate?)
        order' (gui/spinner {:x 585.0 :y 100.0 :width 180.0 :height 30.0}
                            "HILBERT CURVE ORDER:  " order 2 8)
        thick (gui/slider {:x 524.0 :y 150.0 :width 240.0 :height 24.0}
                          "THICKNESS:  " nil thick 1.0 10.0)
        size' (gui/slider {:x 524.0 :y 190.0 :width 240.0 :height 24.0}
                          "TOTAL SIZE: " nil size 10.0 (* (rcw/get-screen-height) 1.5))]
    (debug-stats/draw!)
    (rcd/end-drawing!)
    (-> state
        (assoc :animate? animate? :thick thick :order order' :size size'
               :counter (if (< counter (count path)) (inc counter) counter))
        (regenerate order size))))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (swap! game-atom (comp draw tick))
      (recur)))
  (rcw/close-window!))
