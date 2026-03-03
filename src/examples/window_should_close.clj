(ns examples.window-should-close
  "raylib [core] example - window should close

   Demonstrates custom window close handling. ESC and X-button
   trigger a confirmation dialog instead of immediately closing.
   Press Y to confirm exit, N to cancel.

   Difficulty: 1/4
   Based on: core/core_window_should_close.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(defn initial-state []
  {:exit-requested false
   :exit-window false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [core] example - window should close")
  (rck/set-exit-key! (:null enums/keyboard-key))
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [exit-requested] :as state}]
  (debug-stats/update!)
  (let [close-requested (or (rcw/window-should-close?)
                            (rck/is-key-pressed? (:escape enums/keyboard-key)))
        state (if close-requested
                (assoc state :exit-requested true)
                state)]
    (if (:exit-requested state)
      (cond
        (rck/is-key-pressed? (:y enums/keyboard-key))
        (assoc state :exit-window true)

        (rck/is-key-pressed? (:n enums/keyboard-key))
        (assoc state :exit-requested false)

        :else state)
      state)))

(defn draw [{:keys [exit-requested]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (if exit-requested
    (do
      (rsb/draw-rectangle! 0 100 screen-width 200 colors/black)
      (rtd/draw-text! "Are you sure you want to exit program? [Y/N]" 40 180 30 colors/white))
    (rtd/draw-text! "Try to close the window to get confirmation message!" 120 200 20 colors/lightgray))

  (debug-stats/draw!)
  (rcd/end-drawing!))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (reset! game-atom game)
      (when-not (:exit-window game)
        (draw game)
        (recur))))
  (rcw/close-window!))
