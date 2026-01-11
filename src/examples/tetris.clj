(ns examples.tetris
  "Classic tetris game - converted from raylib C example
   Original: https://github.com/raysan5/raylib-games/blob/master/classics/src/tetris.c
   Sample game developed by Marc Palau and Ramon Santamaria"
  (:require
   [raylib.core.window :as rcw]
   [raylib.nrepl :as nrepl]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.shapes.basic :as rsb]
   [raylib-ext :as ext]
   [debug-stats])
  (:gen-class))

;; DEFINES ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def SCREEN_WIDTH 800)
(def SCREEN_HEIGHT 450)

(def SQUARE_SIZE 20)

(def GRID_HORIZONTAL_SIZE 12)
(def GRID_VERTICAL_SIZE 20)

(def LATERAL_SPEED 10)
(def TURNING_SPEED 12)
(def FAST_FALL_AWAIT_COUNTER 30)

(def FADING_TIME 33)

;; Grid square types
(def EMPTY 0)
(def MOVING 1)
(def FULL 2)
(def BLOCK 3)
(def FADING 4)

;; HELPERS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-random-value [min max]
  (+ min (rand-int (inc (- max min)))))

;; GRID UTILITIES ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-empty-grid
  "Create initial grid with borders"
  []
  (vec
   (for [i (range GRID_HORIZONTAL_SIZE)]
     (vec
      (for [j (range GRID_VERTICAL_SIZE)]
        (if (or (= j (dec GRID_VERTICAL_SIZE))  ; bottom
                (= i 0)                          ; left wall
                (= i (dec GRID_HORIZONTAL_SIZE))) ; right wall
          BLOCK
          EMPTY))))))

(defn make-empty-piece
  "Create empty 4x4 piece matrix"
  []
  (vec (repeat 4 (vec (repeat 4 EMPTY)))))

(defn get-random-piece
  "Generate a random tetris piece in a 4x4 matrix"
  []
  (let [piece-type (get-random-value 0 6)
        empty-piece (make-empty-piece)]
    (case piece-type
      ;; Cube (O)
      0 (-> empty-piece
            (assoc-in [1 1] MOVING)
            (assoc-in [2 1] MOVING)
            (assoc-in [1 2] MOVING)
            (assoc-in [2 2] MOVING))
      ;; L
      1 (-> empty-piece
            (assoc-in [1 0] MOVING)
            (assoc-in [1 1] MOVING)
            (assoc-in [1 2] MOVING)
            (assoc-in [2 2] MOVING))
      ;; L inverted (J)
      2 (-> empty-piece
            (assoc-in [1 2] MOVING)
            (assoc-in [2 0] MOVING)
            (assoc-in [2 1] MOVING)
            (assoc-in [2 2] MOVING))
      ;; Line (I)
      3 (-> empty-piece
            (assoc-in [0 1] MOVING)
            (assoc-in [1 1] MOVING)
            (assoc-in [2 1] MOVING)
            (assoc-in [3 1] MOVING))
      ;; T
      4 (-> empty-piece
            (assoc-in [1 0] MOVING)
            (assoc-in [1 1] MOVING)
            (assoc-in [1 2] MOVING)
            (assoc-in [2 1] MOVING))
      ;; S
      5 (-> empty-piece
            (assoc-in [1 1] MOVING)
            (assoc-in [2 1] MOVING)
            (assoc-in [2 2] MOVING)
            (assoc-in [3 2] MOVING))
      ;; Z
      6 (-> empty-piece
            (assoc-in [1 2] MOVING)
            (assoc-in [2 2] MOVING)
            (assoc-in [2 1] MOVING)
            (assoc-in [3 1] MOVING)))))

(defn grid-get [grid i j]
  (get-in grid [i j] EMPTY))

(defn grid-set [grid i j value]
  (assoc-in grid [i j] value))

;; GAME STATE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init-game []
  {:game-over false
   :pause false
   :grid (make-empty-grid)
   :piece (make-empty-piece)
   :incoming-piece (get-random-piece)
   :piece-position-x (int (/ (- GRID_HORIZONTAL_SIZE 4) 2))
   :piece-position-y 0
   :fading-color colors/gray
   :begin-play true
   :piece-active false
   :detection false
   :line-to-delete false
   :level 1
   :lines 0
   :gravity-movement-counter 0
   :lateral-movement-counter 0
   :turn-movement-counter 0
   :fast-fall-movement-counter 0
   :fade-line-counter 0
   :gravity-speed 30})

;; PIECE OPERATIONS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn place-piece-on-grid
  "Place the current piece on the grid at piece position"
  [grid piece piece-pos-x piece-pos-y]
  (reduce
   (fn [g [i j]]
     (let [piece-val (get-in piece [i j])]
       (if (= piece-val MOVING)
         (grid-set g (+ piece-pos-x i) (+ piece-pos-y j) MOVING)
         g)))
   grid
   (for [i (range 4) j (range 4)] [i j])))

(defn create-piece
  "Create a new piece, moving incoming to current and generating new incoming"
  [game]
  (let [piece-pos-x (int (/ (- GRID_HORIZONTAL_SIZE 4) 2))
        piece-pos-y 0
        {:keys [begin-play incoming-piece grid]} game

        ;; If beginning, generate first incoming piece
        [game incoming-piece] (if begin-play
                                [(assoc game :begin-play false) (get-random-piece)]
                                [game incoming-piece])

        ;; Current piece becomes the incoming piece
        new-piece incoming-piece

        ;; Generate new incoming piece
        new-incoming (get-random-piece)

        ;; Place the piece on the grid
        new-grid (place-piece-on-grid grid new-piece piece-pos-x piece-pos-y)]

    (assoc game
           :piece new-piece
           :incoming-piece new-incoming
           :piece-position-x piece-pos-x
           :piece-position-y piece-pos-y
           :piece-active true
           :grid new-grid)))

;; COLLISION DETECTION ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn check-detection
  "Check if the piece has collided with something below"
  [grid]
  (some
   (fn [[i j]]
     (and (= (grid-get grid i j) MOVING)
          (or (= (grid-get grid i (inc j)) FULL)
              (= (grid-get grid i (inc j)) BLOCK))))
   (for [i (range 1 (dec GRID_HORIZONTAL_SIZE))
         j (range (- GRID_VERTICAL_SIZE 2) -1 -1)]
     [i j])))

;; MOVEMENT ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn resolve-falling-movement
  "Handle piece falling - either lock it or move it down"
  [game]
  (let [{:keys [grid detection piece-position-y]} game]
    (if detection
      ;; Lock the piece in place
      (let [new-grid (reduce
                      (fn [g [i j]]
                        (if (= (grid-get g i j) MOVING)
                          (grid-set g i j FULL)
                          g))
                      grid
                      (for [i (range 1 (dec GRID_HORIZONTAL_SIZE))
                            j (range (- GRID_VERTICAL_SIZE 2) -1 -1)]
                        [i j]))]
        (assoc game
               :grid new-grid
               :detection false
               :piece-active false))

      ;; Move piece down
      (let [new-grid (reduce
                      (fn [g [i j]]
                        (if (= (grid-get g i j) MOVING)
                          (-> g
                              (grid-set i (inc j) MOVING)
                              (grid-set i j EMPTY))
                          g))
                      grid
                      (for [j (range (- GRID_VERTICAL_SIZE 2) -1 -1)
                            i (range 1 (dec GRID_HORIZONTAL_SIZE))]
                        [i j]))]
        (assoc game
               :grid new-grid
               :piece-position-y (inc piece-position-y))))))

(defn check-lateral-collision
  "Check if lateral movement would cause collision"
  [grid direction]
  (some
   (fn [[i j]]
     (when (= (grid-get grid i j) MOVING)
       (if (= direction :left)
         (or (= (dec i) 0)
             (= (grid-get grid (dec i) j) FULL))
         (or (= (inc i) (dec GRID_HORIZONTAL_SIZE))
             (= (grid-get grid (inc i) j) FULL)))))
   (for [j (range (- GRID_VERTICAL_SIZE 2) -1 -1)
         i (range 1 (dec GRID_HORIZONTAL_SIZE))]
     [i j])))

(defn move-piece-lateral
  "Move piece left or right on the grid"
  [grid direction]
  (let [offset (if (= direction :left) -1 1)
        ;; Order matters: left-to-right for left movement, right-to-left for right
        cells (if (= direction :left)
                (for [j (range (- GRID_VERTICAL_SIZE 2) -1 -1)
                      i (range 1 (dec GRID_HORIZONTAL_SIZE))]
                  [i j])
                (for [j (range (- GRID_VERTICAL_SIZE 2) -1 -1)
                      i (range (- GRID_HORIZONTAL_SIZE 1) 0 -1)]
                  [i j]))]
    (reduce
     (fn [g [i j]]
       (if (= (grid-get g i j) MOVING)
         (-> g
             (grid-set (+ i offset) j MOVING)
             (grid-set i j EMPTY))
         g))
     grid
     cells)))

(defn resolve-lateral-movement
  "Handle left/right movement"
  [game]
  (let [{:keys [grid piece-position-x]} game]
    (cond
      (rck/is-key-down? (:left enums/keyboard-key))
      (if (check-lateral-collision grid :left)
        game
        (assoc game
               :grid (move-piece-lateral grid :left)
               :piece-position-x (dec piece-position-x)
               :lateral-movement-counter 0))

      (rck/is-key-down? (:right enums/keyboard-key))
      (if (check-lateral-collision grid :right)
        game
        (assoc game
               :grid (move-piece-lateral grid :right)
               :piece-position-x (inc piece-position-x)
               :lateral-movement-counter 0))

      :else game)))

;; ROTATION ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn rotate-piece-matrix
  "Rotate a 4x4 piece matrix 90 degrees clockwise"
  [piece]
  (vec
   (for [i (range 4)]
     (vec
      (for [j (range 4)]
        (get-in piece [(- 3 j) i]))))))

(defn check-rotation-collision
  "Check if rotation would cause collision"
  [grid piece-pos-x piece-pos-y rotated-piece]
  (some
   (fn [[i j]]
     (let [grid-x (+ piece-pos-x i)
           grid-y (+ piece-pos-y j)
           piece-val (get-in rotated-piece [i j])
           grid-val (grid-get grid grid-x grid-y)]
       (and (= piece-val MOVING)
            (not= grid-val EMPTY)
            (not= grid-val MOVING))))
   (for [i (range 4) j (range 4)] [i j])))

(defn clear-moving-from-grid
  "Remove all MOVING cells from grid"
  [grid]
  (reduce
   (fn [g [i j]]
     (if (= (grid-get g i j) MOVING)
       (grid-set g i j EMPTY)
       g))
   grid
   (for [i (range 1 (dec GRID_HORIZONTAL_SIZE))
         j (range (- GRID_VERTICAL_SIZE 2) -1 -1)]
     [i j])))

(defn resolve-turn-movement
  "Handle piece rotation"
  [game]
  (if (rck/is-key-down? (:up enums/keyboard-key))
    (let [{:keys [piece grid piece-position-x piece-position-y]} game
          rotated-piece (rotate-piece-matrix piece)]
      (if (check-rotation-collision grid piece-position-x piece-position-y rotated-piece)
        ;; Collision, can't rotate but still clear and replace
        (let [cleared-grid (clear-moving-from-grid grid)
              new-grid (place-piece-on-grid cleared-grid piece piece-position-x piece-position-y)]
          (assoc game
                 :grid new-grid
                 :turn-movement-counter 0))
        ;; No collision, rotate
        (let [cleared-grid (clear-moving-from-grid grid)
              new-grid (place-piece-on-grid cleared-grid rotated-piece piece-position-x piece-position-y)]
          (assoc game
                 :piece rotated-piece
                 :grid new-grid
                 :turn-movement-counter 0))))
    game))

;; LINE COMPLETION ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn check-line-complete
  "Check if a line j is complete"
  [grid j]
  (every?
   #(= (grid-get grid % j) FULL)
   (range 1 (dec GRID_HORIZONTAL_SIZE))))

(defn mark-completed-lines
  "Mark all completed lines as FADING"
  [grid]
  (reduce
   (fn [g j]
     (if (check-line-complete g j)
       (reduce
        (fn [g2 i]
          (grid-set g2 i j FADING))
        g
        (range 1 (dec GRID_HORIZONTAL_SIZE)))
       g))
   grid
   (range (- GRID_VERTICAL_SIZE 2) -1 -1)))

(defn check-completion
  "Check for completed lines and mark them"
  [game]
  (let [{:keys [grid]} game
        new-grid (mark-completed-lines grid)
        has-fading? (some
                     (fn [[i j]]
                       (= (grid-get new-grid i j) FADING))
                     (for [i (range 1 (dec GRID_HORIZONTAL_SIZE))
                           j (range (- GRID_VERTICAL_SIZE 2) -1 -1)]
                       [i j]))]
    (assoc game
           :grid new-grid
           :line-to-delete has-fading?)))

(defn delete-line
  "Delete a single fading line at row j and move everything above down"
  [grid j]
  (let [;; Clear the line
        cleared (reduce
                 (fn [g i]
                   (grid-set g i j EMPTY))
                 grid
                 (range 1 (dec GRID_HORIZONTAL_SIZE)))
        ;; Move everything above down
        moved (reduce
               (fn [g j2]
                 (reduce
                  (fn [g2 i]
                    (let [val (grid-get g2 i j2)]
                      (if (or (= val FULL) (= val FADING))
                        (-> g2
                            (grid-set i (inc j2) val)
                            (grid-set i j2 EMPTY))
                        g2)))
                  g
                  (range 1 (dec GRID_HORIZONTAL_SIZE))))
               cleared
               (range (dec j) -1 -1))]
    moved))

(defn delete-complete-lines
  "Delete all fading lines and return [new-grid deleted-count]"
  [grid]
  (loop [g grid
         j (- GRID_VERTICAL_SIZE 2)
         deleted 0]
    (if (< j 0)
      [g deleted]
      (if (= (grid-get g 1 j) FADING)
        (let [new-g (delete-line g j)]
          (recur new-g j (inc deleted)))  ; Check same row again
        (recur g (dec j) deleted)))))

;; GAME OVER CHECK ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn check-game-over
  "Check if any FULL cells are in the top 2 rows"
  [grid]
  (some
   (fn [[i j]]
     (= (grid-get grid i j) FULL))
   (for [j (range 2)
         i (range 1 (dec GRID_HORIZONTAL_SIZE))]
     [i j])))

;; UPDATE GAME ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-game [game]
  ;; Update debug stats
  (debug-stats/update!)

  ;; Handle F11 fullscreen toggle
  (when (rck/is-key-pressed? (:f11 enums/keyboard-key))
    (rcw/toggle-borderless-windowed!))

  (if (:game-over game)
    ;; Game over - check for restart
    (if (rck/is-key-pressed? (:enter enums/keyboard-key))
      (init-game)
      game)

    ;; Game running
    (let [game (if (rck/is-key-pressed? (:p enums/keyboard-key))
                 (update game :pause not)
                 game)]
      (if (:pause game)
        game
        (if (:line-to-delete game)
          ;; Animation for deleting lines
          (let [{:keys [fade-line-counter]} game
                new-counter (inc fade-line-counter)
                new-color (if (< (mod new-counter 8) 4)
                            colors/maroon
                            colors/gray)
                game (assoc game
                            :fade-line-counter new-counter
                            :fading-color new-color)]
            (if (>= new-counter FADING_TIME)
              (let [[new-grid deleted-lines] (delete-complete-lines (:grid game))]
                (assoc game
                       :grid new-grid
                       :fade-line-counter 0
                       :line-to-delete false
                       :lines (+ (:lines game) deleted-lines)))
              game))

          ;; Normal gameplay
          (if (not (:piece-active game))
            ;; Create new piece
            (-> game
                create-piece
                (assoc :fast-fall-movement-counter 0))

            ;; Piece is falling
            (let [;; Update counters
                  game (-> game
                           (update :fast-fall-movement-counter inc)
                           (update :gravity-movement-counter inc)
                           (update :lateral-movement-counter inc)
                           (update :turn-movement-counter inc))

                  ;; Immediate response to key presses
                  game (cond-> game
                         (or (rck/is-key-pressed? (:left enums/keyboard-key))
                             (rck/is-key-pressed? (:right enums/keyboard-key)))
                         (assoc :lateral-movement-counter LATERAL_SPEED)

                         (rck/is-key-pressed? (:up enums/keyboard-key))
                         (assoc :turn-movement-counter TURNING_SPEED))

                  ;; Fast fall
                  game (if (and (rck/is-key-down? (:down enums/keyboard-key))
                                (>= (:fast-fall-movement-counter game) FAST_FALL_AWAIT_COUNTER))
                         (update game :gravity-movement-counter + (:gravity-speed game))
                         game)

                  ;; Gravity movement
                  game (if (>= (:gravity-movement-counter game) (:gravity-speed game))
                         (let [detection (check-detection (:grid game))
                               game (assoc game :detection detection)
                               game (resolve-falling-movement game)
                               game (check-completion game)]
                           (assoc game :gravity-movement-counter 0))
                         game)

                  ;; Lateral movement
                  game (if (>= (:lateral-movement-counter game) LATERAL_SPEED)
                         (resolve-lateral-movement game)
                         game)

                  ;; Turn movement
                  game (if (>= (:turn-movement-counter game) TURNING_SPEED)
                         (resolve-turn-movement game)
                         game)

                  ;; Check game over
                  game (if (check-game-over (:grid game))
                         (assoc game :game-over true)
                         game)]
              game)))))))

;; DRAW GAME ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-square-color [square-type fading-color]
  (case square-type
    0 nil         ; EMPTY - draw grid lines
    1 colors/darkgray  ; MOVING
    2 colors/gray      ; FULL
    3 colors/lightgray ; BLOCK
    4 fading-color     ; FADING
    nil))

(defn draw-grid-square [x y square-type fading-color]
  (let [color (get-square-color square-type fading-color)]
    (if color
      (rsb/draw-rectangle! x y SQUARE_SIZE SQUARE_SIZE color)
      ;; Draw empty cell grid lines
      (do
        (ext/draw-line! x y (+ x SQUARE_SIZE) y colors/lightgray)
        (ext/draw-line! x y x (+ y SQUARE_SIZE) colors/lightgray)
        (ext/draw-line! (+ x SQUARE_SIZE) y (+ x SQUARE_SIZE) (+ y SQUARE_SIZE) colors/lightgray)
        (ext/draw-line! x (+ y SQUARE_SIZE) (+ x SQUARE_SIZE) (+ y SQUARE_SIZE) colors/lightgray)))))

(defn draw-game [game]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (when game
    (if-not (:game-over game)
      (do
        ;; Calculate grid offset
        (let [offset-x (- (/ SCREEN_WIDTH 2) (/ (* GRID_HORIZONTAL_SIZE SQUARE_SIZE) 2) 50)
              offset-y (- (+ (/ SCREEN_HEIGHT 2)
                             (- (* (dec GRID_VERTICAL_SIZE) (/ SQUARE_SIZE 2)))
                             (* SQUARE_SIZE 2))
                          50)
              fading-color (:fading-color game)]

          ;; Draw main grid
          (doseq [j (range GRID_VERTICAL_SIZE)
                  i (range GRID_HORIZONTAL_SIZE)]
            (let [x (int (+ offset-x (* i SQUARE_SIZE)))
                  y (int (+ offset-y (* j SQUARE_SIZE)))
                  square-type (grid-get (:grid game) i j)]
              (draw-grid-square x y square-type fading-color)))

          ;; Draw incoming piece preview
          (let [preview-x 500
                preview-y 45]
            (doseq [j (range 4)
                    i (range 4)]
              (let [x (+ preview-x (* i SQUARE_SIZE))
                    y (+ preview-y (* j SQUARE_SIZE))
                    piece-val (get-in (:incoming-piece game) [i j])]
                (if (= piece-val MOVING)
                  (rsb/draw-rectangle! x y SQUARE_SIZE SQUARE_SIZE colors/gray)
                  ;; Draw empty cell grid lines
                  (do
                    (ext/draw-line! x y (+ x SQUARE_SIZE) y colors/lightgray)
                    (ext/draw-line! x y x (+ y SQUARE_SIZE) colors/lightgray)
                    (ext/draw-line! (+ x SQUARE_SIZE) y (+ x SQUARE_SIZE) (+ y SQUARE_SIZE) colors/lightgray)
                    (ext/draw-line! x (+ y SQUARE_SIZE) (+ x SQUARE_SIZE) (+ y SQUARE_SIZE) colors/lightgray)))))

            ;; Draw labels
            (rtd/draw-text! "INCOMING:" preview-x (- preview-y 15) 10 colors/gray)
            (rtd/draw-text! (str "LINES:      " (format "%04d" (:lines game))) preview-x (+ preview-y 100) 10 colors/gray))

          ;; Draw pause message
          (when (:pause game)
            (let [text "GAME PAUSED"
                  size 40
                  width (ext/measure-text text size)]
              (rtd/draw-text! text (int (- (/ SCREEN_WIDTH 2) (/ width 2)))
                              (int (- (/ SCREEN_HEIGHT 2) 40)) size colors/gray))))

        ;; Draw controls info
        (rtd/draw-text! "F1 debug | F11 fullscreen | P pause | Arrows: move | Up: rotate" 10 (- SCREEN_HEIGHT 25) 12 colors/darkgray)

        ;; Draw debug stats
        (debug-stats/draw!))

      ;; Game over screen
      (let [text "PRESS [ENTER] TO PLAY AGAIN"
            size 20
            width (ext/measure-text text size)]
        (rtd/draw-text! text (int (- (/ (rcw/get-screen-width) 2) (/ width 2)))
                        (int (- (/ (rcw/get-screen-height) 2) 50)) size colors/gray)
        (debug-stats/draw!))))

  (rcd/end-drawing!))

;; MAIN ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def game-state (atom nil))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (rcw/set-config-flags! (bit-or (:window-resizable enums/config-flag)
                                 (:vsync-hint enums/config-flag)))
  (rcw/init-window! SCREEN_WIDTH SCREEN_HEIGHT "classic game: tetris")

  ;; Enable debug stats - press F1 to toggle
  (debug-stats/enable!)

  (reset! game-state (init-game))

  (loop []
    (when-not (rcw/window-should-close?)
      (swap! game-state update-game)
      (draw-game @game-state)
      (recur)))

  (rcw/close-window!))

(comment
  (future (-main))
  ;;
  )
