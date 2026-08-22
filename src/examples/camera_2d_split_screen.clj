(ns examples.camera-2d-split-screen
  "raylib [core] example - 2D camera split screen

   Two players on one shared grid, each with their own camera, each
   rendered into its own half-width render texture and then blitted side by
   side. Both halves show the same world from different viewpoints, which
   is what makes it a split screen rather than two windows.

   Player 1: W/S/A/D. Player 2: arrow keys.

   Difficulty: 3/4
   Based on: core/core_2d_camera_split_screen.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.camera2d :as rc2d]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.textures.texture-loading :as rtl]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 440)
(def player-size 40)
(def move-speed 3.0)

(defn- camera-on [{:keys [x y]}]
  {:offset {:x 200.0 :y 200.0} :target {:x (float x) :y (float y)}
   :rotation 0.0 :zoom 1.0})

(defn initial-state []
  {:p1 {:x 200.0 :y 200.0} :p2 {:x 250.0 :y 200.0} :tex1 nil :tex2 nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height
                    "raylib [core] example - 2d camera split screen")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  (swap! game-atom assoc
         :tex1 (rtl/load-render-texture! (quot screen-width 2) screen-height)
         :tex2 (rtl/load-render-texture! (quot screen-width 2) screen-height)))

(defn- move
  "Axis movement from a key pair. The C uses if/else-if so pressing both
   keys favours the first; keeping that rather than cancelling to zero."
  [pos [neg-key pos-key]]
  (cond
    (rck/is-key-down? (get enums/keyboard-key pos-key)) (+ pos move-speed)
    (rck/is-key-down? (get enums/keyboard-key neg-key)) (- pos move-speed)
    :else pos))

(defn tick [state]
  (debug-stats/update!)
  (-> state
      (update-in [:p1 :y] move [:w :s])
      (update-in [:p1 :x] move [:a :d])
      (update-in [:p2 :y] move [:up :down])
      (update-in [:p2 :x] move [:left :right])))

(defn- draw-world
  "The shared scene: a labelled grid plus both players. Drawn once per
   camera, which is the point of the example."
  [p1 p2]
  (let [cols (inc (quot screen-width player-size))
        rows (inc (quot screen-height player-size))]
    (doseq [i (range cols)]
      (rsb/draw-line-v! {:x (float (* player-size i)) :y 0.0}
                        {:x (float (* player-size i)) :y (float screen-height)}
                        colors/lightgray))
    (doseq [i (range rows)]
      (rsb/draw-line-v! {:x 0.0 :y (float (* player-size i))}
                        {:x (float screen-width) :y (float (* player-size i))}
                        colors/lightgray))
    (doseq [i (range (quot screen-width player-size))
            j (range (quot screen-height player-size))]
      (rtd/draw-text! (format "[%d,%d]" i j)
                      (+ 10 (* player-size i)) (+ 15 (* player-size j)) 10 colors/lightgray))
    (rsb/draw-rectangle-rec! {:x (float (:x p1)) :y (float (:y p1))
                              :width (float player-size) :height (float player-size)} colors/red)
    (rsb/draw-rectangle-rec! {:x (float (:x p2)) :y (float (:y p2))
                              :width (float player-size) :height (float player-size)} colors/blue)))

(defn- render-view! [tex player p1 p2 label label-color]
  (rtl/begin-texture-mode! tex)
  (rcd/clear-background! colors/raywhite)
  (rc2d/begin-mode-2d! (camera-on player))
  (draw-world p1 p2)
  (rc2d/end-mode-2d!)
  ;; Banner drawn outside the camera so it stays put as the view scrolls.
  (rsb/draw-rectangle! 0 0 (quot (rcw/get-screen-width) 2) 30
                       (ru/fade colors/raywhite (float 0.6)))
  (rtd/draw-text! label 10 10 10 label-color)
  (rtl/end-texture-mode!))

(defn draw [{:keys [p1 p2 tex1 tex2]}]
  (when (and tex1 tex2)
    (render-view! tex1 p1 p1 p2 "PLAYER1: W/S/A/D to move" colors/maroon)
    (render-view! tex2 p2 p1 p2 "PLAYER2: UP/DOWN/LEFT/RIGHT to move" colors/darkblue))

  (rcd/begin-drawing!)
  (rcd/clear-background! colors/black)
  (when (and tex1 tex2)
    (let [t (:texture tex1)
          ;; Negative height: render targets are stored bottom-up.
          flipped {:x 0.0 :y 0.0 :width (float (:width t)) :height (float (- (:height t)))}]
      (rtl/draw-texture-rec! t flipped {:x 0.0 :y 0.0} colors/white)
      (rtl/draw-texture-rec! (:texture tex2) flipped
                             {:x (float (/ screen-width 2.0)) :y 0.0} colors/white)))
  (rsb/draw-rectangle! (- (quot (rcw/get-screen-width) 2) 2) 0 4
                       (rcw/get-screen-height) colors/lightgray)
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
  (rtl/unload-render-texture! (:tex1 @game-atom))
  (rtl/unload-render-texture! (:tex2 @game-atom))
  (rcw/close-window!))
