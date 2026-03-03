(ns examples.writing-anim
  "raylib [text] example - text writing animation

   Shows a typewriter-style text animation effect.
   Hold SPACE to speed up, press ENTER to restart.

   Difficulty: 2/4
   Based on: text/text_writing_anim.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def message "This sample illustrates a text writing\nanimation effect! Check it out! ;)")

(defn initial-state []
  {:frames-counter 0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [text] example - writing anim")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [frames-counter] :as state}]
  (debug-stats/update!)
  (let [frames-counter (if (rck/is-key-pressed? (:enter enums/keyboard-key))
                          0
                          (if (rck/is-key-down? (:space enums/keyboard-key))
                            (+ frames-counter 8)
                            (inc frames-counter)))]
    (assoc state :frames-counter frames-counter)))

(defn draw [{:keys [frames-counter]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; Draw substring of message based on frames counter
  (let [char-count (min (quot frames-counter 10) (count message))
        visible-text (subs message 0 char-count)]
    (rtd/draw-text! visible-text 210 160 20 colors/maroon))

  (rtd/draw-text! "PRESS [ENTER] to RESTART!" 240 260 20 colors/lightgray)
  (rtd/draw-text! "HOLD [SPACE] to SPEED UP!" 239 300 20 colors/lightgray)

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
