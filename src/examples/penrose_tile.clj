(ns examples.penrose-tile
  "raylib [shapes] example - penrose tile

   A Penrose tiling grown from an L-system and drawn with turtle graphics.
   UP and DOWN change the generation count; each generation rewrites the
   production string through four rules and halves the draw length, so the
   figure gains detail without changing size.

   The alphabet the turtle reads:

     F        move forward, drawing
     + / -    turn by +/- 36 degrees
     [ / ]    push / pop position and heading
     0-9      repeat the NEXT command that many times
     W X Y Z  rewrite-only symbols, never drawn

   Two things carried over deliberately from the C:

   - Rewriting DROPS any existing F rather than copying it. The rules
     reintroduce their own, so an F is always one generation old. Copying
     it instead doubles the line count each pass and the tiling degenerates.
   - Drawing reveals 12 more symbols per frame rather than the whole
     production at once, which is what makes the figure draw itself. The C
     advances that counter inside its draw function; here it moves with the
     rest of the state in tick, since a draw that mutates is a trap for the
     next reader.

   Difficulty: 3/4
   Based on: shapes/shapes_penrose_tile.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def axiom "[X]++[X]++[X]++[X]++[X]")
(def rules
  {\W "YF++ZF4-XF[-YF4-WF]++"
   \X "+YF--ZF[3-WF--XF]+"
   \Y "-WF++XF[+++YF++ZF]-"
   \Z "--YF++++WF[+ZF++++XF]--XF"})

(def theta 36.0)
(def base-draw-length 460.0)
(def max-generations 4)
(def steps-per-frame 12)

(defn rewrite
  "One L-system generation. Rewrite symbols expand to their rule; F is
   dropped because the rules bring their own back; anything else survives."
  [production]
  (apply str (keep (fn [ch] (or (rules ch) (when (not= ch \F) ch))) production)))

(defn grow
  "The production after `n` generations."
  [n]
  (nth (iterate rewrite axiom) n))

(defn draw-length
  "Scaled so the figure keeps its overall size as detail increases: the
   generation's share of the full length, halved once per generation."
  [n]
  (* base-draw-length (/ (double n) max-generations) (Math/pow 0.5 n)))

(defn turtle-segments
  "Walk the first `steps` symbols and return the line segments to draw, as
   [[x1 y1 x2 y2] ...] in world coordinates centred on the origin.

   Pure: no drawing here, which is what makes it testable."
  [production steps len]
  (loop [i 0, pos [0.0 0.0], angle -90.0, stack (), repeats 1, out (transient [])]
    (if (>= i (min steps (count production)))
      (persistent! out)
      (let [ch (nth production i)]
        (cond
          (= ch \F)
          (let [[segs pos'] (reduce (fn [[ss [x y]] _]
                                      (let [r (Math/toRadians angle)
                                            x' (+ x (* len (Math/cos r)))
                                            y' (+ y (* len (Math/sin r)))]
                                        [(conj ss [x y x' y']) [x' y']]))
                                    [[] pos] (range repeats))]
            (recur (inc i) pos' angle stack 1 (reduce conj! out segs)))

          (or (= ch \+) (= ch \-))
          (recur (inc i) pos
                 (+ angle (* repeats (if (= ch \+) theta (- theta))))
                 stack 1 out)

          (= ch \[) (recur (inc i) pos angle (conj stack [pos angle]) repeats out)
          (= ch \]) (let [[p a] (peek stack)]
                      (recur (inc i) p a (pop stack) repeats out))

          (Character/isDigit ch)
          (recur (inc i) pos angle stack (Character/digit ch 10) out)

          :else (recur (inc i) pos angle stack repeats out))))))

(defn initial-state [] {:generations 0 :production axiom :steps 0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/set-config-flags! (:msaa-4x-hint enums/config-flag))
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - penrose tile")
  (rct/set-target-fps! 120)
  (debug-stats/enable!))

(defn tick [{:keys [generations] :as state}]
  (debug-stats/update!)
  (let [g (cond
            (and (rck/is-key-pressed? (:up enums/keyboard-key))
                 (< generations max-generations)) (inc generations)
            (and (rck/is-key-pressed? (:down enums/keyboard-key))
                 (pos? generations)) (dec generations)
            :else generations)]
    (if (not= g generations)
      ;; Regrow from the axiom and restart the reveal.
      {:generations g :production (grow g) :steps 0}
      (update state :steps + steps-per-frame))))

(defn draw [{:keys [generations production steps]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (when (pos? generations)
    (let [cx (/ screen-width 2.0) cy (/ screen-height 2.0)
          ink (ru/fade colors/black (float 0.2))]
      (doseq [[x1 y1 x2 y2] (turtle-segments production steps (draw-length generations))]
        (rsb/draw-line-ex! {:x (float (+ x1 cx)) :y (float (+ y1 cy))}
                           {:x (float (+ x2 cx)) :y (float (+ y2 cy))}
                           (float 2.0) ink))))
  (rtd/draw-text! "penrose l-system" 10 10 20 colors/darkgray)
  (rtd/draw-text! "press up or down to change generations" 10 30 20 colors/darkgray)
  (rtd/draw-text! (format "generations: %d" generations) 10 50 20 colors/darkgray)
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
