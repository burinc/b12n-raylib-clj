(ns examples.undo-redo
  "raylib [core] example - undo and redo

   Move a block around a grid with the arrows, recolour it with SPACE, then
   walk your edits backwards with CTRL+Z and forwards with CTRL+Y. The strip
   along the bottom is the undo buffer itself, drawn slot by slot.

   A note on the port, since Clojure would normally reach for something else
   here. The obvious idiomatic move is a growing vector of immutable states
   plus an index - no wraparound, no bookkeeping. That would be the wrong
   call: this example's whole subject is the fixed ring buffer, and the
   bottom strip visualises its 26 slots with markers for first, last and
   current. Swap in an unbounded vector and the picture stops meaning
   anything.

   So the ring semantics stay - 26 slots, indices that wrap, the oldest
   entry pushed out when the buffer fills. What changes is the mechanism:
   states live in an immutable vector and each edit produces a new one,
   rather than memcpy into a mutable array.

   Difficulty: 3/4
   Based on: core/core_undo_redo.c"
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

(def max-undo-states 26)
(def grid-cell-size 24)
(def grid-cells-x 30)
(def grid-cells-y 13)
(def grid-position {:x 40 :y 60})
(def undo-info-position {:x 110 :y 400})

(defn ring-seq
  "Slot indices from `from` up to but not including `to`, wrapping at
   max-undo-states. The C spells this out four separate times as an
   if/else over whether the range crosses the end of the array; naming it
   once makes the three call sites read as what they are."
  [from to]
  (if (<= from to)
    (range from to)
    (concat (range from max-undo-states) (range 0 to))))

(defn initial-state []
  (let [player {:cell {:x 10 :y 10} :color colors/red}]
    {:player player
     ;; Every slot starts as the initial player, matching the C's calloc +
     ;; memcpy loop, so an undo before any edit is a no-op rather than a
     ;; jump to a zeroed cell.
     :states (vec (repeat max-undo-states player))
     :current 0 :first 0 :last 0
     :frame-counter 0}))

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [core] example - undo redo")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn- clamp [v lo hi] (max lo (min hi v)))

(defn- move-player
  "Arrow keys move one cell. if/else-if like the C, so a diagonal press
   resolves to one axis rather than both."
  [{:keys [cell] :as player}]
  (let [k (fn [n] (rck/is-key-pressed? (get enums/keyboard-key n)))
        {:keys [x y]} cell
        [x y] (cond
                (k :right) [(inc x) y]
                (k :left)  [(dec x) y]
                (k :up)    [x (dec y)]
                (k :down)  [x (inc y)]
                :else      [x y])]
    (assoc player :cell {:x (clamp x 0 (dec grid-cells-x))
                         :y (clamp y 0 (dec grid-cells-y))})))

(defn- recolour [player]
  (if (rck/is-key-pressed? (:space enums/keyboard-key))
    (assoc player :color {:r (ru/get-random-value 20 255)
                          :g (ru/get-random-value 20 220)
                          :b (ru/get-random-value 20 240)
                          :a 255})
    player))

(defn- record
  "Snapshot the player if it differs from the slot we are sitting on.
   Advancing `current` past `first` evicts the oldest entry, which is what
   makes this a ring rather than a stack."
  [{:keys [player states current first] :as state}]
  (if (= player (nth states current))
    state
    (let [current' (mod (inc current) max-undo-states)
          first' (if (= current' first) (mod (inc first) max-undo-states) first)]
      (assoc state
             :states (assoc states current' player)
             :current current'
             :first first'
             :last current'))))

(defn- undo [{:keys [current first states] :as state}]
  (if (= current first)
    state
    (let [current' (mod (dec current) max-undo-states)]
      (assoc state :current current' :player (nth states current')))))

(defn- redo [{:keys [current last first states] :as state}]
  (if (= current last)
    state
    (let [next' (mod (inc current) max-undo-states)]
      (if (= next' first)
        state
        (assoc state :current next' :player (nth states next'))))))

(defn tick [state]
  (debug-stats/update!)
  (let [ctrl? (rck/is-key-down? (:left-control enums/keyboard-key))
        state (-> state (update :player move-player) (update :player recolour))
        ;; The C samples for changes every 2 frames, not every frame - a
        ;; held arrow key would otherwise fill the ring in under half a
        ;; second.
        fc (inc (:frame-counter state))
        state (if (>= fc 2) (assoc (record state) :frame-counter 0)
                  (assoc state :frame-counter fc))]
    (cond
      (and ctrl? (rck/is-key-pressed? (:z enums/keyboard-key))) (undo state)
      (and ctrl? (rck/is-key-pressed? (:y enums/keyboard-key))) (redo state)
      :else state)))

(defn- cell-rect [{:keys [x y]} pad]
  {:x (float (+ (:x grid-position) (* x grid-cell-size)))
   :y (float (+ (:y grid-position) (* y grid-cell-size)))
   :width (float (+ grid-cell-size pad)) :height (float (+ grid-cell-size pad))})

(defn- draw-undo-buffer
  "The strip along the bottom: every slot, the filled span in blue, the
   already-undone span in green, the current slot in gold, plus markers for
   first (outline, left) and last (solid, right)."
  [{:keys [first last current]}]
  (let [{px :x py :y} undo-info-position
        slot 24
        slot-rect (fn [i] {:x (float (+ px (* slot i))) :y (float py)
                           :width (float slot) :height (float slot)})
        fill (fn [idxs bg edge]
               (doseq [i idxs]
                 (rsb/draw-rectangle-rec! (slot-rect i) bg)
                 (rsb/draw-rectangle-lines! (int (+ px (* slot i))) (int py) slot slot edge)))]
    (fill (range max-undo-states) colors/lightgray colors/gray)
    (fill (ring-seq first (mod (inc last) max-undo-states)) colors/skyblue colors/blue)
    (fill (ring-seq first current) colors/green colors/lime)
    (fill [current] colors/gold colors/orange)
    ;; Index markers, offset so they do not sit on top of each other when
    ;; two indices coincide.
    (rsb/draw-rectangle! (+ px 8 (* slot current)) (- py 10) 8 8 colors/red)
    (rsb/draw-rectangle-lines! (+ px 2 (* slot first)) (+ py 27) 8 8 colors/black)
    (rsb/draw-rectangle! (+ px 14 (* slot last)) (+ py 27) 8 8 colors/black)))

(defn draw [{:keys [player states first current] :as state}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rtd/draw-text! "[ARROWS] MOVE PLAYER - [SPACE] CHANGE PLAYER COLOR" 40 20 20 colors/darkgray)

  ;; Ghost trail: where the player has been, up to the current slot.
  (doseq [i (ring-seq first current)]
    (rsb/draw-rectangle-rec! (cell-rect (:cell (nth states i)) 0) colors/lightgray))

  (doseq [y (range (inc grid-cells-y))]
    (rsb/draw-line! (:x grid-position) (+ (:y grid-position) (* y grid-cell-size))
                    (+ (:x grid-position) (* grid-cells-x grid-cell-size))
                    (+ (:y grid-position) (* y grid-cell-size)) colors/gray))
  (doseq [x (range (inc grid-cells-x))]
    (rsb/draw-line! (+ (:x grid-position) (* x grid-cell-size)) (:y grid-position)
                    (+ (:x grid-position) (* x grid-cell-size))
                    (+ (:y grid-position) (* grid-cells-y grid-cell-size)) colors/gray))

  (rsb/draw-rectangle-rec! (cell-rect (:cell player) 1) (:color player))
  (rtd/draw-text! "UNDO STATES:" (- (:x undo-info-position) 85)
                  (+ (:y undo-info-position) 9) 10 colors/darkgray)
  (draw-undo-buffer state)
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
