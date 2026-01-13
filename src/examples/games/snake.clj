(ns examples.games.snake
  "Classic game: Snake
   
   Original game by Ian Eito, Albert Martos and Ramon Santamaria.
   Ported to Clojure for raylib-clojure-playground.
   
   Complexity: ⭐⭐ (2/4)
   
   Controls:
   - Arrow keys: Move snake
   - P: Pause game
   - ENTER: Restart when game over
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]))

(def WIDTH 800)
(def HEIGHT 450)
(def SQUARE-SIZE 31)
(def MAX-LENGTH 256)

(defn random-value [min-val max-val]
  (+ min-val (rand-int (- max-val min-val -1))))

(defn calculate-offset []
  {:x (mod WIDTH SQUARE-SIZE)
   :y (mod HEIGHT SQUARE-SIZE)})

(defn initial-snake [offset]
  (let [start-x (/ (:x offset) 2)
        start-y (/ (:y offset) 2)]
    {:segments (vec (repeat MAX-LENGTH {:x start-x :y start-y}))
     :length 1
     :speed {:x SQUARE-SIZE :y 0}}))

(defn spawn-fruit [offset snake]
  (let [grid-w (quot WIDTH SQUARE-SIZE)
        grid-h (quot HEIGHT SQUARE-SIZE)
        half-x (/ (:x offset) 2)
        half-y (/ (:y offset) 2)
        segments (take (:length snake) (:segments snake))
        ;; Generate random position
        gen-pos (fn []
                  {:x (+ (* (random-value 0 (dec grid-w)) SQUARE-SIZE) half-x)
                   :y (+ (* (random-value 0 (dec grid-h)) SQUARE-SIZE) half-y)})
        ;; Check if position overlaps with snake
        overlaps? (fn [pos]
                    (some #(and (= (:x pos) (:x %))
                                (= (:y pos) (:y %)))
                          segments))
        ;; Keep trying until we find a valid position
        find-valid (fn []
                     (loop [attempts 0
                            pos (gen-pos)]
                       (if (or (>= attempts 100) (not (overlaps? pos)))
                         pos
                         (recur (inc attempts) (gen-pos)))))]
    (find-valid)))

(defn initial-state []
  (let [offset (calculate-offset)
        snake (initial-snake offset)]
    {:exit? false
     :game-over? false
     :paused? false
     :frames 0
     :allow-move? false
     :offset offset
     :snake snake
     :fruit {:position (spawn-fruit offset snake)
             :active? true}}))

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "classic game: snake")
  (rct/set-target-fps! 60))

(defn handle-direction [{:keys [snake allow-move?] :as game}]
  (let [{:keys [speed]} snake
        moving-horizontal? (not= 0 (:x speed))
        moving-vertical? (not= 0 (:y speed))]
    (cond
      ;; Can only change direction if allowed and perpendicular to current direction
      (and (rck/is-key-pressed? (:right enums/keyboard-key))
           (not moving-horizontal?) allow-move?)
      (-> game
          (assoc-in [:snake :speed] {:x SQUARE-SIZE :y 0})
          (assoc :allow-move? false))

      (and (rck/is-key-pressed? (:left enums/keyboard-key))
           (not moving-horizontal?) allow-move?)
      (-> game
          (assoc-in [:snake :speed] {:x (- SQUARE-SIZE) :y 0})
          (assoc :allow-move? false))

      (and (rck/is-key-pressed? (:up enums/keyboard-key))
           (not moving-vertical?) allow-move?)
      (-> game
          (assoc-in [:snake :speed] {:x 0 :y (- SQUARE-SIZE)})
          (assoc :allow-move? false))

      (and (rck/is-key-pressed? (:down enums/keyboard-key))
           (not moving-vertical?) allow-move?)
      (-> game
          (assoc-in [:snake :speed] {:x 0 :y SQUARE-SIZE})
          (assoc :allow-move? false))

      :else game)))

(defn move-snake [{:keys [snake frames] :as game}]
  (if (zero? (mod frames 5))
    (let [{:keys [segments length speed]} snake
          ;; Store old positions
          old-positions (take length segments)
          ;; Move head
          head (first segments)
          new-head {:x (+ (:x head) (:x speed))
                    :y (+ (:y head) (:y speed))}
          ;; Each segment takes position of the one in front
          new-segments (vec (concat [new-head]
                                    (take (dec MAX-LENGTH) old-positions)
                                    (repeat (- MAX-LENGTH length) {:x 0 :y 0})))]
      (-> game
          (assoc-in [:snake :segments] new-segments)
          (assoc :allow-move? true)))
    game))

(defn check-wall-collision [{:keys [snake offset] :as game}]
  (let [head (first (:segments snake))
        max-x (- WIDTH (/ (:x offset) 2))
        max-y (- HEIGHT (/ (:y offset) 2))]
    (if (or (>= (:x head) max-x)
            (>= (:y head) max-y)
            (< (:x head) 0)
            (< (:y head) 0))
      (assoc game :game-over? true)
      game)))

(defn check-self-collision [{:keys [snake] :as game}]
  (let [{:keys [segments length]} snake
        head (first segments)
        body (take (dec length) (rest segments))]
    (if (some #(and (= (:x head) (:x %))
                    (= (:y head) (:y %)))
              body)
      (assoc game :game-over? true)
      game)))

(defn check-fruit-collision [{:keys [snake fruit offset] :as game}]
  (let [head (first (:segments snake))
        fpos (:position fruit)]
    (if (and (:active? fruit)
             (< (:x head) (+ (:x fpos) SQUARE-SIZE))
             (> (+ (:x head) SQUARE-SIZE) (:x fpos))
             (< (:y head) (+ (:y fpos) SQUARE-SIZE))
             (> (+ (:y head) SQUARE-SIZE) (:y fpos)))
      ;; Snake eats fruit - grow and spawn new fruit
      (let [new-length (inc (:length snake))
            new-snake (assoc snake :length new-length)
            new-fruit-pos (spawn-fruit offset new-snake)]
        (-> game
            (assoc :snake new-snake)
            (assoc :fruit {:position new-fruit-pos :active? true})))
      game)))

(defn handle-input [{:keys [game-over? paused?] :as game}]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)

    (and game-over? (rck/is-key-pressed? (:enter enums/keyboard-key)))
    (merge (initial-state))

    (and (not game-over?) (rck/is-key-pressed? (:p enums/keyboard-key)))
    (update :paused? not)))

(defn tick [{:keys [game-over? paused?] :as game}]
  (let [game (handle-input game)]
    (if (or game-over? paused?)
      game
      (-> game
          handle-direction
          move-snake
          check-wall-collision
          check-self-collision
          check-fruit-collision
          (update :frames inc)))))

(defn draw-grid [offset]
  (let [half-x (int (/ (:x offset) 2))
        half-y (int (/ (:y offset) 2))
        cols (inc (quot WIDTH SQUARE-SIZE))
        rows (inc (quot HEIGHT SQUARE-SIZE))]
    ;; Vertical lines
    (doseq [i (range cols)]
      (let [x (int (+ (* SQUARE-SIZE i) half-x))]
        (rsb/draw-line! x half-y x (- HEIGHT half-y) colors/lightgray)))
    ;; Horizontal lines
    (doseq [i (range rows)]
      (let [y (int (+ (* SQUARE-SIZE i) half-y))]
        (rsb/draw-line! half-x y (- WIDTH half-x) y colors/lightgray)))))

(defn draw-snake [{:keys [segments length]}]
  (doseq [i (range length)]
    (let [seg (nth segments i)
          color (if (zero? i) colors/darkblue colors/blue)]
      (rsb/draw-rectangle! (int (:x seg)) (int (:y seg)) SQUARE-SIZE SQUARE-SIZE color))))

(defn draw-fruit [{:keys [position active?]}]
  (when active?
    (rsb/draw-rectangle! (int (:x position)) (int (:y position)) SQUARE-SIZE SQUARE-SIZE colors/skyblue)))

(defn draw [{:keys [game-over? paused? snake fruit offset]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (if game-over?
    (let [text "PRESS [ENTER] TO PLAY AGAIN"
          text-width (ext/measure-text text 20)]
      (rtd/draw-text! text
                      (- (quot WIDTH 2) (quot text-width 2))
                      (- (quot HEIGHT 2) 50)
                      20
                      colors/gray))
    (do
      ;; Draw grid
      (draw-grid offset)
      ;; Draw snake
      (draw-snake snake)
      ;; Draw fruit
      (draw-fruit fruit)
      ;; Draw pause overlay
      (when paused?
        (let [text "GAME PAUSED"
              text-width (ext/measure-text text 40)]
          (rtd/draw-text! text
                          (- (quot WIDTH 2) (quot text-width 2))
                          (- (quot HEIGHT 2) 40)
                          40
                          colors/gray)))))

  ;; Score display
  (rtd/draw-text! (str "Score: " (dec (:length snake))) 10 10 20 colors/darkgray)

  (rtd/draw-fps! 10 (- HEIGHT 25))
  (rcd/end-drawing!))

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (when-not (or (:exit? game) (rcw/window-should-close?))
        (reset! game-atom game)
        (draw game)
        (recur))))
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  @game-atom
  (swap! game-atom assoc :paused? true)
  (swap! game-atom assoc :game-over? true))
