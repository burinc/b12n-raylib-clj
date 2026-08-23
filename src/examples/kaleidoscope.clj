(ns examples.kaleidoscope
  "raylib [shapes] example - kaleidoscope

   Drag the mouse to draw. Each stroke is repeated around `symmetry`
   rotations and mirrored across the horizontal, so a single gesture becomes
   a symmetric figure. The < and > buttons walk back and forward through the
   stored lines, and Reset clears them.

   Two observations about the C, handled differently here and worth naming.

   First, its draw pass wraps the line loop in `for (s = 0; s < symmetry;
   s++)` - but the loop body never reads `s`. The rotations are already
   baked into the stored lines when they are recorded, so that outer loop
   redraws identical opaque lines six times over. It cannot change a pixel;
   it just costs six times the draw calls. Drawn once here.

   Second, the C sets its button flags during the draw pass and acts on them
   at the top of the NEXT frame, so a click takes effect one frame late.
   That is an artifact of where the calls sit rather than an intent, and
   immediate-mode controls that return a value let the click be handled in
   the frame it happens. Invisible either way at 20 FPS.

   Difficulty: 3/4
   Based on: shapes/shapes_kaleidoscope.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.mouse :as rcm]
   [raylib.core.camera2d :as rc2]
   [raylib.core.collision :as rcol]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.raygui :as gui]
   [raylib.raymath :as rm]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def max-draw-lines 8192)
(def symmetry 6)
(def thickness 3.0)

(def offset {:x (/ screen-width 2.0) :y (/ screen-height 2.0)})

;; Mirroring across the horizontal is a component-wise multiply by (1, -1),
;; which is why the C reaches for Vector2Multiply rather than a negate.
(def mirror {:x 1.0 :y -1.0})

(def buttons
  {:reset {:x (- screen-width 55.0) :y 5.0 :width 50.0 :height 25.0}
   :back  {:x (- screen-width 55.0) :y (- screen-height 30.0) :width 25.0 :height 25.0}
   :next  {:x (- screen-width 30.0) :y (- screen-height 30.0) :width 25.0 :height 25.0}})

(defn stroke-lines
  "The lines one mouse movement contributes: for each of `symmetry` steps,
   rotate the segment another `360/symmetry` degrees and emit it together
   with its mirror. Pure, so the symmetry is checkable without a window.

   Note the rotation accumulates across steps rather than being computed
   from the step index - the C rotates the running value in place, so step
   n sits at (n+1) increments, not n."
  [line-start line-end]
  (let [step (Math/toRadians (/ 360.0 symmetry))]
    (loop [acc [] s 0 a line-start b line-end]
      (if (= s symmetry)
        acc
        (let [a (rm/v2-rotate a step)
              b (rm/v2-rotate b step)]
          (recur (conj acc
                       {:start a :end b}
                       {:start (rm/v2-multiply a mirror) :end (rm/v2-multiply b mirror)})
                 (inc s) a b))))))

(defn initial-state []
  {:lines [] :shown 0 :mouse {:x 0.0 :y 0.0} :prev-mouse {:x 0.0 :y 0.0}})

(def game-atom (atom (initial-state)))

(def camera {:offset offset :target {:x 0.0 :y 0.0} :rotation 0.0 :zoom 1.0})

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - kaleidoscope")
  ;; The C runs this at 20 FPS: the stroke is sampled once per frame, so a
  ;; higher rate would lay down many more, much shorter segments.
  (rct/set-target-fps! 20)
  (debug-stats/enable!))

(defn- over-a-button? [point]
  (some (fn [r] (pos? (rcol/check-collision-point-rec? point r))) (vals buttons)))

(defn tick [{:keys [lines shown mouse] :as state}]
  (debug-stats/update!)
  (let [prev-mouse mouse
        mouse (rcm/get-mouse-position)
        drawing? (and (rcm/is-mouse-button-down? (:left enums/mouse-button))
                      (not (over-a-button? mouse)))
        new-lines (when (and drawing? (< (count lines) (dec max-draw-lines)))
                    (stroke-lines (rm/v2-subtract mouse offset)
                                  (rm/v2-subtract prev-mouse offset)))
        lines (if new-lines (into lines new-lines) lines)]
    (assoc state
           :mouse mouse :prev-mouse prev-mouse
           :lines lines
           ;; Newly drawn lines are shown immediately; stepping back with <
           ;; is what makes shown lag the total.
           :shown (if new-lines (count lines) shown))))

(defn draw [{:keys [lines shown] :as state}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rc2/begin-mode-2d! camera)
  (doseq [{:keys [start end]} (take shown lines)]
    (rsb/draw-line-ex! {:x (float (:x start)) :y (float (:y start))}
                       {:x (float (:x end)) :y (float (:y end))}
                       (float thickness) colors/black))
  (rc2/end-mode-2d!)

  ;; A step button is disabled when there is nothing that way to step.
  (when (zero? shown) (gui/disable!))
  (let [back? (gui/button (:back buttons) "<")]
    (gui/enable!)
    (when (>= shown (count lines)) (gui/disable!))
    (let [next? (gui/button (:next buttons) ">")]
      (gui/enable!)
      (let [reset? (gui/button (:reset buttons) "Reset")]
        (rtd/draw-text! (format "LINES: %d/%d" shown max-draw-lines)
                        10 (- screen-height 30) 20 colors/maroon)
        (rtd/draw-fps! 10 10)
        (debug-stats/draw!)
        (rcd/end-drawing!)
        (cond
          reset? (assoc state :lines [] :shown 0)
          back? (update state :shown (fn [n] (max 0 (dec n))))
          next? (update state :shown (fn [n] (min (count lines) (inc n))))
          :else state)))))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (swap! game-atom (comp draw tick))
      (recur)))
  (rcw/close-window!))
