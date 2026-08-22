(ns examples.input-virtual-controls
  "raylib [core] example - input virtual controls

   An on-screen D-pad. Press a button with the mouse (or a finger on a
   touchscreen) and the maroon circle moves. This is how a touch-only build
   gets directional input without a keyboard.

   The hit test is a diamond, not a circle: it sums the x and y distances
   and compares against the radius, which is Manhattan distance. That makes
   the pressable area a rotated square, so the four buttons tile without
   gaps or overlap. Kept as-is rather than 'corrected' to a circle.

   Difficulty: 2/4
   Based on: core/core_input_virtual_controls.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.mouse :as rcm]
   [raylib.core.gestures :as rcg]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def pad-position {:x 100.0 :y 350.0})
(def button-radius 30.0)
(def player-speed 75.0)

(def buttons
  "Each D-pad button: where it sits, which way it moves the player, its
   arrow triangle, and the arrow's colour. Derived once from pad-position
   rather than repeated as literals the way the C's parallel arrays are."
  (let [r button-radius
        off (* r 1.5)
        {px :x py :y} pad-position
        mk (fn [cx cy dir color tri]
             {:pos {:x cx :y cy} :dir dir :color color
              :tri (mapv (fn [[dx dy]] {:x (float (+ cx dx)) :y (float (+ cy dy))}) tri)})]
    [(mk px (- py off) [0.0 -1.0] colors/yellow [[0 -12] [-9 9] [9 9]])
     (mk (- px off) py [-1.0 0.0] colors/blue   [[9 -9] [-12 0] [9 9]])
     (mk (+ px off) py [1.0 0.0]  colors/red    [[12 0] [-9 -9] [-9 9]])
     (mk px (+ py off) [0.0 1.0]  colors/green  [[-9 -9] [0 12] [9 -9]])]))

(defn- input-position
  "Touch wins if there is one, otherwise the mouse - so the same code path
   serves a touchscreen and a desktop."
  []
  (if (pos? (rcg/get-touch-point-count))
    (rcg/get-touch-position 0)
    (rcm/get-mouse-position)))

(defn- engaged?
  "On touch, any contact counts. On desktop the left button must be held,
   or the player would follow the cursor around without a click."
  []
  (or (pos? (rcg/get-touch-point-count))
      (rcm/is-mouse-button-down? (:left enums/mouse-button))))

(defn pressed-button
  "Index of the button under `pos`, using the C's Manhattan hit test."
  [pos]
  (first (keep-indexed
          (fn [i {p :pos}]
            (when (< (+ (Math/abs (- (:x p) (:x pos)))
                        (Math/abs (- (:y p) (:y pos))))
                     button-radius)
              i))
          buttons)))

(defn initial-state []
  {:player {:x (/ screen-width 2.0) :y (/ screen-height 2.0)} :pressed nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height
                    "raylib [core] example - input virtual controls")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [state]
  (debug-stats/update!)
  (let [pressed (when (engaged?) (pressed-button (input-position)))
        [dx dy] (if pressed (:dir (nth buttons pressed)) [0.0 0.0])
        step (* player-speed (rct/get-frame-time))]
    (-> state
        (assoc :pressed pressed)
        (update-in [:player :x] + (* dx step))
        (update-in [:player :y] + (* dy step)))))

(defn draw [{:keys [player pressed]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rsb/draw-circle-v! {:x (float (:x player)) :y (float (:y player))}
                      (float 50.0) colors/maroon)
  (doseq [[i {:keys [pos tri color]}] (map-indexed vector buttons)]
    (rsb/draw-circle-v! pos (float button-radius)
                        (if (= i pressed) colors/darkgray colors/black))
    (rsb/draw-triangle! (nth tri 0) (nth tri 1) (nth tri 2) color))
  (rtd/draw-text! "move the player with D-Pad buttons" 10 10 20 colors/darkgray)
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
