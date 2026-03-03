(ns examples.logo-raylib-anim
  "raylib [shapes] example - logo raylib animation

   Animated raylib logo construction using a state machine:
   0) Blinking small square
   1) Top and left bars grow
   2) Bottom and right bars grow
   3) Letters appear one by one, then fade out
   4) Press R to replay

   Difficulty: 2/4
   Based on: shapes/shapes_logo_raylib_anim.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def logo-x (- (quot screen-width 2) 128))
(def logo-y (- (quot screen-height 2) 128))

(defn initial-state []
  {:state 0
   :frames-counter 0
   :letters-count 0
   :top-width 16
   :left-height 16
   :bottom-width 16
   :right-height 16
   :alpha 1.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - logo raylib anim")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [state frames-counter letters-count
                    top-width left-height bottom-width right-height alpha] :as game}]
  (debug-stats/update!)
  (case state
    ;; State 0: Small box blinking
    0 (let [fc (inc frames-counter)]
        (if (= fc 120)
          (assoc game :state 1 :frames-counter 0)
          (assoc game :frames-counter fc)))

    ;; State 1: Top and left bars growing
    1 (let [tw (+ top-width 4)
            lh (+ left-height 4)]
        (cond-> (assoc game :top-width tw :left-height lh)
          (= tw 256) (assoc :state 2)))

    ;; State 2: Bottom and right bars growing
    2 (let [bw (+ bottom-width 4)
            rh (+ right-height 4)]
        (cond-> (assoc game :bottom-width bw :right-height rh)
          (= bw 256) (assoc :state 3)))

    ;; State 3: Letters appearing, then fade out
    3 (let [fc (inc frames-counter)
            [fc lc] (if (pos? (quot fc 12))
                      [0 (inc letters-count)]
                      [fc letters-count])]
        (if (>= lc 10)
          (let [a (- alpha 0.02)]
            (if (<= a 0.0)
              (assoc game :alpha 0.0 :state 4 :frames-counter fc :letters-count lc)
              (assoc game :alpha a :frames-counter fc :letters-count lc)))
          (assoc game :frames-counter fc :letters-count lc)))

    ;; State 4: Reset and replay
    4 (if (rck/is-key-pressed? (:r enums/keyboard-key))
        (initial-state)
        game)

    game))

(defn draw [{:keys [state frames-counter letters-count
                    top-width left-height bottom-width right-height alpha]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (case state
    0 (when (odd? (quot (quot frames-counter 15) 2))
        (rsb/draw-rectangle! logo-x logo-y 16 16 colors/black))

    1 (do
        (rsb/draw-rectangle! logo-x logo-y top-width 16 colors/black)
        (rsb/draw-rectangle! logo-x logo-y 16 left-height colors/black))

    2 (do
        (rsb/draw-rectangle! logo-x logo-y top-width 16 colors/black)
        (rsb/draw-rectangle! logo-x logo-y 16 left-height colors/black)
        (rsb/draw-rectangle! (+ logo-x 240) logo-y 16 right-height colors/black)
        (rsb/draw-rectangle! logo-x (+ logo-y 240) bottom-width 16 colors/black))

    3 (let [faded-black (ru/fade colors/black (float alpha))
            faded-white (ru/fade colors/raywhite (float alpha))]
        (rsb/draw-rectangle! logo-x logo-y top-width 16 faded-black)
        (rsb/draw-rectangle! logo-x (+ logo-y 16) 16 (- left-height 32) faded-black)
        (rsb/draw-rectangle! (+ logo-x 240) (+ logo-y 16) 16 (- right-height 32) faded-black)
        (rsb/draw-rectangle! logo-x (+ logo-y 240) bottom-width 16 faded-black)
        (rsb/draw-rectangle! (- (quot screen-width 2) 112) (- (quot screen-height 2) 112) 224 224 faded-white)
        (rtd/draw-text! (subs "raylib" 0 (min letters-count 6))
                        (- (quot screen-width 2) 44) (+ (quot screen-height 2) 48) 50 faded-black))

    4 (rtd/draw-text! "[R] REPLAY" 340 200 20 colors/gray)

    nil)

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
