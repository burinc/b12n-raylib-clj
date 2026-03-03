(ns examples.format-text
  "raylib [text] example - text formatting

   Displays formatted text: score, hi-score, lives, and
   real-time frame time using Clojure's format function.

   Difficulty: 1/4
   Based on: text/text_format_text.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def score 100020)
(def hiscore 200450)
(def lives 5)

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [text] example - format text")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn draw []
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rtd/draw-text! (format "Score: %08d" score) 200 80 20 colors/red)
  (rtd/draw-text! (format "HiScore: %08d" hiscore) 200 120 20 colors/green)
  (rtd/draw-text! (format "Lives: %02d" lives) 200 160 40 colors/blue)
  (rtd/draw-text! (format "Elapsed Time: %02.02f ms" (* (rct/get-frame-time) 1000.0))
                  200 220 20 colors/black)

  (debug-stats/draw!)
  (rcd/end-drawing!))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (debug-stats/update!)
      (draw)
      (recur)))
  (rcw/close-window!))
