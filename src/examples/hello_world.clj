(ns examples.hello-world
  (:require [raylib.core.window :as rcw]
            [raylib.core.timing :as rct]
            [raylib.core.drawing :as rcd]
            [raylib.core.keyboard :as rck]
            [raylib.text.drawing :as rtd]
            [raylib.colors :as colors]
            [raylib.enums :as enums]
            [raylib.nrepl :as nrepl]
            [debug-stats]))

(defn init []
  (rcw/set-config-flags :flag/vsync-hint
                        :flag/borderless-windowed-mode
                        :flag/window-resizable)
  (rcw/init-window! 800 450 "raylib [core] example - basic window")
  (rct/set-target-fps! 60)
  ;; Enable debug stats - press F1 to toggle
  (debug-stats/enable!))

(defn tick [game]
  ;; Update debug stats (handles F1 toggle)
  (debug-stats/update!)

  (let [last-time (:time game)
        acc (:time-acc game)
        newtime (System/nanoTime)
        diff (- newtime last-time)
        newacc (vec (take-last 100 (conj acc diff)))
        average-diff (/ (reduce + newacc) (count newacc))
        average-fps (long (/ 1000000000 average-diff))]

    ;; Example: add custom stat
    (debug-stats/set-custom-stat! :avg-fps average-fps)

    (if (rck/is-key-down? (:q enums/keyboard-key))
      (assoc game :exit? true)
      (-> game
          (assoc :time newtime)
          (assoc :time-acc newacc)
          (assoc :avg-fps average-fps)))))

(defn draw [game]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rtd/draw-text! (:label game) 100 100 20 colors/purple)
  (rtd/draw-text! "press Q to exit" 100 150 20 colors/purple)
  (rtd/draw-text! "press F1 for debug stats" 100 180 20 colors/gray)
  (rtd/draw-text! (str "dt: " (:dt game)) 100 220 20 colors/purple)
  ;(rtd/draw-text! (str "fps: " (:avg-fps game)) 100 250 20 colors/purple)
  (rtd/draw-text {:text (str "fps: " (:avg-fps game))
                  :x 100
                  :y 250
                  :size 20
                  :color colors/purple})
  ;; Draw debug stats overlay (only shows when F1 toggled on)
  (debug-stats/draw!)
  (rcd/end-drawing!))

(def game-atom (atom
                {:exit? false
                 :label "Hello world"
                 :dt 0
                 :time (System/nanoTime)
                 :time-acc [1]}))

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick (assoc @game-atom
                            :dt (rct/get-frame-time)))]
      (when-not (or (:exit? game) (rcw/window-should-close?))
        (reset! game-atom game)
        (draw game)
        (recur))))
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  (init)
  (start)
  (future (start))
  ;;
  )
