(ns examples.games.retro-maze-3d
  "Retro Maze 3D - A GameBoy-style first-person maze game
   
   Based on raylib's GGJ 2021 game by Ramon Santamaria.
   Ported to Clojure with procedural maze generation.
   
   Complexity: ⭐⭐⭐ (3/4)
   
   Controls:
   - WASD: Move
   - Mouse: Look around
   - SPACE: Pause menu
   - M: Toggle minimap
   - ENTER: Select/Continue
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcmouse]
   [raylib.core.camera3d :as rc3d]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]))

;; Screen dimensions (GameBoy-ish)
(def WIDTH 320)
(def HEIGHT 240)

;; GameBoy green palette
(def GB-GREEN01 {:r 155
                 :g 188
                 :b 15
                 :a 255}) ;; Lightest
(def GB-GREEN02 {:r 110
                 :g 150
                 :b 27
                 :a 255})
(def GB-GREEN03 {:r 48
                 :g 98
                 :b 48
                 :a 255})
(def GB-GREEN04 {:r 15
                 :g 56
                 :b 15
                 :a 255}) ;; Darkest

;; Maze dimensions
(def MAZE-WIDTH 25)
(def MAZE-HEIGHT 13)
(def CELL-SIZE 1.0)

;; Game screens
(def SCREEN-TITLE 0)
(def SCREEN-GAMEPLAY 1)
(def SCREEN-ENDING 2)

;; ============================================================================
;; Maze Generation (Recursive Backtracking)
;; ============================================================================

(defn make-grid [w h]
  (vec (repeat h (vec (repeat w {:visited false
                                 :walls {:n true
                                         :s true
                                         :e true
                                         :w true}})))))

(defn get-unvisited-neighbors [grid x y]
  (let [h (count grid)
        w (count (first grid))
        neighbors [[(dec x) y :w :e]
                   [(inc x) y :e :w]
                   [x (dec y) :n :s]
                   [x (inc y) :s :n]]]
    (filter (fn [[nx ny _ _]]
              (and (>= nx 0) (< nx w)
                   (>= ny 0) (< ny h)
                   (not (:visited (get-in grid [ny nx])))))
            neighbors)))

(defn remove-wall [grid x y dir]
  (update-in grid [y x :walls] assoc dir false))

(defn generate-maze-step [grid stack]
  (if (empty? stack)
    grid
    (let [[x y] (peek stack)
          neighbors (get-unvisited-neighbors grid x y)]
      (if (empty? neighbors)
        (recur grid (pop stack))
        (let [[nx ny from-dir to-dir] (rand-nth neighbors)
              grid (-> grid
                       (assoc-in [ny nx :visited] true)
                       (remove-wall x y from-dir)
                       (remove-wall nx ny to-dir))]
          (recur grid (conj stack [nx ny])))))))

(defn generate-maze [w h]
  (let [start-x 0
        start-y 0
        grid (-> (make-grid w h)
                 (assoc-in [start-y start-x :visited] true))]
    (generate-maze-step grid [[start-x start-y]])))

(defn maze-to-collision-map
  "Convert maze to a 2D collision map where true = wall"
  [maze]
  (let [mh (count maze)
        mw (count (first maze))
        map-w (inc (* 2 mw))
        map-h (inc (* 2 mh))]
    (vec
     (for [my (range map-h)]
       (vec
        (for [mx (range map-w)]
          (let [cell-x (quot mx 2)
                cell-y (quot my 2)
                in-bounds (and (< cell-x mw) (< cell-y mh))]
            (cond
              ;; Corner posts - always walls
              (and (even? mx) (even? my)) true
              ;; Horizontal walls (north/south)
              (and (odd? mx) (even? my))
              (if (and in-bounds (< cell-y mh))
                (let [cy (if (zero? my) 0 (dec cell-y))]
                  (if (< cy mh)
                    (get-in maze [cy cell-x :walls :s] true)
                    true))
                true)
              ;; Vertical walls (east/west)
              (and (even? mx) (odd? my))
              (if (and in-bounds (< cell-x mw))
                (let [cx (if (zero? mx) 0 (dec cell-x))]
                  (if (< cx mw)
                    (get-in maze [cell-y cx :walls :e] true)
                    true))
                true)
              ;; Cell interiors - passable
              :else false))))))))

;; ============================================================================
;; Game State
;; ============================================================================

(defn initial-state []
  (let [maze (generate-maze MAZE-WIDTH MAZE-HEIGHT)
        collision-map (maze-to-collision-map maze)]
    {:screen SCREEN-TITLE
     :exit? false
     :frames 0
     ;; Title screen
     :menu-option 0
     :title-anim-y -100
     ;; Gameplay
     :maze maze
     :collision-map collision-map
     :camera {:position {:x 1.5
                         :y 0.3
                         :z 1.5}
              :target {:x 2.5
                       :y 0.3
                       :z 1.5}
              :up {:x 0.0
                   :y 1.0
                   :z 0.0}
              :fovy 60.0
              :projection rc3d/CAMERA_PERSPECTIVE}
     :player-angle 0.0
     :time-remaining 180 ;; 3 minutes
     :stamina 100.0
     :show-minimap false
     :paused false
     :player-moving false
     ;; Exit position (bottom-right of maze)
     :exit-cell [(- (* 2 MAZE-WIDTH) 1) (- (* 2 MAZE-HEIGHT) 1)]
     ;; Ending
     :won? false}))

(def game-atom (atom (initial-state)))

;; ============================================================================
;; Initialization
;; ============================================================================

(defn init []
  (rcw/init-window! WIDTH HEIGHT "RETRO MAZE 3D")
  (rct/set-target-fps! 60)
  (rc3d/disable-cursor!))

;; ============================================================================
;; Collision Detection
;; ============================================================================

(defn check-collision [collision-map x z radius]
  (let [map-h (count collision-map)
        map-w (count (first collision-map))
        ;; Check cells around player
        cell-x (int (Math/floor x))
        cell-y (int (Math/floor z))]
    (some true?
          (for [dy [-1 0 1]
                dx [-1 0 1]
                :let [cx (+ cell-x dx)
                      cy (+ cell-y dy)]
                :when (and (>= cx 0) (< cx map-w)
                           (>= cy 0) (< cy map-h))]
            (when (get-in collision-map [cy cx])
              ;; Simple AABB collision with player as circle
              (let [rect-x (float cx)
                    rect-z (float cy)
                    closest-x (max rect-x (min x (+ rect-x 1.0)))
                    closest-z (max rect-z (min z (+ rect-z 1.0)))
                    dx (- x closest-x)
                    dz (- z closest-z)
                    dist-sq (+ (* dx dx) (* dz dz))]
                (< dist-sq (* radius radius))))))))

;; ============================================================================
;; Update Functions
;; ============================================================================

(defn update-title [{:keys [menu-option title-anim-y]
                     :as game}]
  (let [new-anim-y (min 20 (+ title-anim-y 2))]
    (cond-> (assoc game :title-anim-y new-anim-y)
      (rck/is-key-pressed? (:down enums/keyboard-key))
      (update :menu-option #(min 1 (inc %)))

      (rck/is-key-pressed? (:up enums/keyboard-key))
      (update :menu-option #(max 0 (dec %)))

      (rck/is-key-pressed? (:enter enums/keyboard-key))
      (-> (assoc :screen (if (zero? (:menu-option game)) SCREEN-GAMEPLAY SCREEN-TITLE))
          (assoc :exit? (= (:menu-option game) 1))))))

(defn update-camera-first-person [{:keys [camera player-angle collision-map]
                                   :as game}]
  (let [;; Get mouse movement for looking
        mouse-delta (rcmouse/get-mouse-delta)
        sensitivity 0.003
        new-angle (+ player-angle (* (:x mouse-delta) sensitivity))

        ;; Get movement input
        forward? (rck/is-key-down? (:w enums/keyboard-key))
        backward? (rck/is-key-down? (:s enums/keyboard-key))
        strafe-right? (rck/is-key-down? (:d enums/keyboard-key))
        strafe-left? (rck/is-key-down? (:a enums/keyboard-key))

        moving? (or forward? backward? strafe-right? strafe-left?)

        ;; Calculate movement
        speed 0.05
        dx (+ (if forward? (* (Math/sin new-angle) speed) 0)
              (if backward? (* (Math/sin new-angle) (- speed)) 0)
              (if strafe-right? (* (Math/cos new-angle) speed) 0)
              (if strafe-left? (* (Math/cos new-angle) (- speed)) 0))
        dz (+ (if forward? (* (Math/cos new-angle) speed) 0)
              (if backward? (* (Math/cos new-angle) (- speed)) 0)
              (if strafe-right? (* (Math/sin new-angle) (- speed)) 0)
              (if strafe-left? (* (Math/sin new-angle) speed) 0))

        ;; Current position
        pos (:position camera)
        new-x (+ (:x pos) dx)
        new-z (+ (:z pos) dz)

        ;; Check collision and update position
        radius 0.2
        final-x (if (check-collision collision-map new-x (:z pos) radius) (:x pos) new-x)
        final-z (if (check-collision collision-map (:x pos) new-z radius) (:z pos) new-z)

        ;; Update camera
        new-pos {:x final-x
                 :y 0.3
                 :z final-z}
        new-target {:x (+ final-x (Math/sin new-angle))
                    :y 0.3
                    :z (+ final-z (Math/cos new-angle))}]
    (-> game
        (assoc :player-angle new-angle)
        (assoc :player-moving moving?)
        (assoc-in [:camera :position] new-pos)
        (assoc-in [:camera :target] new-target))))

(defn update-gameplay [{:keys [frames paused collision-map camera exit-cell stamina time-remaining]
                        :as game}]
  (cond
    ;; Toggle pause
    (rck/is-key-pressed? (:space enums/keyboard-key))
    (-> game
        (update :paused not)
        (assoc :menu-option 0))

    ;; Toggle minimap
    (rck/is-key-pressed? (:m enums/keyboard-key))
    (update game :show-minimap not)

    ;; Paused - handle menu
    paused
    (cond-> game
      (rck/is-key-pressed? (:down enums/keyboard-key))
      (update :menu-option #(min 2 (inc %)))

      (rck/is-key-pressed? (:up enums/keyboard-key))
      (update :menu-option #(max 0 (dec %)))

      (rck/is-key-pressed? (:enter enums/keyboard-key))
      (-> (as-> g
                (case (:menu-option g)
                  0 (assoc g :paused false) ;; Resume
                  1 (assoc (initial-state) :screen SCREEN-TITLE) ;; Back to title
                  2 (assoc g :exit? true))))) ;; Exit

    ;; Normal gameplay
    :else
    (let [game (update-camera-first-person game)
          game (update game :frames inc)

          ;; Update timer every 60 frames
          game (if (zero? (mod (:frames game) 60))
                 (update game :time-remaining dec)
                 game)

          ;; Drain stamina when moving
          game (if (:player-moving game)
                 (update game :stamina - 0.02)
                 game)

          ;; Check win condition
          pos (get-in game [:camera :position])
          player-cell-x (int (:x pos))
          player-cell-z (int (:z pos))
          [exit-x exit-z] exit-cell
          won? (and (= player-cell-x exit-x) (= player-cell-z exit-z))]

      (cond
        ;; Win!
        won?
        (-> game (assoc :screen SCREEN-ENDING :won? true))

        ;; Time's up or stamina depleted
        (or (<= (:time-remaining game) 0) (<= (:stamina game) 0))
        (-> game (assoc :screen SCREEN-ENDING :won? false))

        :else game))))

(defn update-ending [game]
  (if (rck/is-key-pressed? (:enter enums/keyboard-key))
    (initial-state)
    game))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [{:keys [screen]
             :as game}]
  (-> game
      handle-input
      (as-> g
            (case screen
              0 (update-title g)
              1 (update-gameplay g)
              2 (update-ending g)
              g))))

;; ============================================================================
;; Drawing Functions
;; ============================================================================

(defn draw-title [{:keys [menu-option title-anim-y]}]
  (rcd/clear-background! GB-GREEN01)

  ;; Title
  (rtd/draw-text! "RETRO MAZE" 70 (int title-anim-y) 30 GB-GREEN03)
  (rtd/draw-text! "3D" 130 (+ (int title-anim-y) 35) 30 GB-GREEN02)

  ;; Menu
  (rsb/draw-rectangle! 80 (+ 120 (* menu-option 25)) 160 22 GB-GREEN02)
  (rtd/draw-text! "START GAME" 100 125 15 (if (= menu-option 0) GB-GREEN03 GB-GREEN02))
  (rtd/draw-text! "EXIT" 130 150 15 (if (= menu-option 1) GB-GREEN03 GB-GREEN02))

  ;; Footer
  (rtd/draw-text! "GGJ 2021 REMAKE" 10 220 10 GB-GREEN02)
  (rtd/draw-text! "WASD + Mouse" 220 220 10 GB-GREEN02))

(defn draw-maze-3d [{:keys [collision-map exit-cell]}]
  (let [map-h (count collision-map)
        map-w (count (first collision-map))
        [exit-x exit-z] exit-cell]
    ;; Draw floor
    (rc3d/draw-plane! {:x (/ map-w 2.0)
                       :y 0.0
                       :z (/ map-h 2.0)}
                      {:x (float map-w)
                       :y (float map-h)}
                      GB-GREEN02)

    ;; Draw walls
    (doseq [z (range map-h)
            x (range map-w)
            :when (get-in collision-map [z x])]
      (rc3d/draw-cube! {:x (+ x 0.5)
                        :y 0.5
                        :z (+ z 0.5)}
                       1.0 1.0 1.0
                       GB-GREEN03))

    ;; Draw exit marker (pulsing)
    (let [pulse (+ 0.3 (* 0.2 (Math/sin (* (rct/get-time) 3))))]
      (rc3d/draw-cube! {:x (+ exit-x 0.5)
                        :y 0.5
                        :z (+ exit-z 0.5)}
                       0.8 0.8 0.8
                       {:r 0
                        :g 200
                        :b 0
                        :a (int (* 255 pulse))}))))

(defn draw-minimap [{:keys [collision-map camera exit-cell]}]
  (let [map-h (count collision-map)
        map-w (count (first collision-map))
        scale 4
        offset-x (- (/ WIDTH 2) (/ (* map-w scale) 2))
        offset-y (- (/ HEIGHT 2) (/ (* map-h scale) 2))]

    ;; Draw map background
    (rsb/draw-rectangle! (int offset-x) (int offset-y)
                         (* map-w scale) (* map-h scale)
                         GB-GREEN01)

    ;; Draw walls
    (doseq [z (range map-h)
            x (range map-w)
            :when (get-in collision-map [z x])]
      (rsb/draw-rectangle! (+ (int offset-x) (* x scale))
                           (+ (int offset-y) (* z scale))
                           scale scale
                           GB-GREEN03))

    ;; Draw player position
    (let [pos (:position camera)
          px (+ (int offset-x) (* (int (:x pos)) scale))
          pz (+ (int offset-y) (* (int (:z pos)) scale))]
      (rsb/draw-rectangle! px pz scale scale GB-GREEN04))

    ;; Draw exit
    (let [[exit-x exit-z] exit-cell]
      (rsb/draw-rectangle! (+ (int offset-x) (* exit-x scale))
                           (+ (int offset-y) (* exit-z scale))
                           scale scale
                           {:r 0
                            :g 255
                            :b 0
                            :a 255}))))

(defn draw-hud [{:keys [time-remaining stamina]}]
  ;; Bottom bar
  (rsb/draw-rectangle! 0 (- HEIGHT 20) WIDTH 20 GB-GREEN01)

  ;; Stamina
  (rtd/draw-text! "STAMINA:" 10 (- HEIGHT 16) 10 GB-GREEN03)
  (rsb/draw-rectangle-lines! 70 (- HEIGHT 16) 80 12 GB-GREEN03)
  (rsb/draw-rectangle! 72 (- HEIGHT 14) (int (* 76 (/ (max 0 stamina) 100.0))) 8 GB-GREEN02)

  ;; Timer
  (let [mins (quot time-remaining 60)
        secs (mod time-remaining 60)]
    (rtd/draw-text! (format "TIME: %d:%02d" mins secs) 220 (- HEIGHT 16) 10 GB-GREEN03)))

(defn draw-pause-menu [{:keys [menu-option]}]
  (rsb/draw-rectangle! 0 80 WIDTH 84 GB-GREEN01)
  (rsb/draw-rectangle! 80 (+ 95 (* menu-option 20)) 160 18 GB-GREEN02)
  (rtd/draw-text! "RESUME" 120 97 12 (if (= menu-option 0) GB-GREEN03 GB-GREEN02))
  (rtd/draw-text! "BACK TO TITLE" 100 117 12 (if (= menu-option 1) GB-GREEN03 GB-GREEN02))
  (rtd/draw-text! "EXIT GAME" 112 137 12 (if (= menu-option 2) GB-GREEN03 GB-GREEN02)))

(defn draw-gameplay [{:keys [camera paused show-minimap]
                      :as game}]
  (rcd/clear-background! GB-GREEN01)

  ;; 3D view
  (rc3d/begin-mode-3d! camera)
  (draw-maze-3d game)
  (rc3d/end-mode-3d!)

  ;; HUD
  (draw-hud game)

  ;; Minimap overlay
  (when show-minimap
    (draw-minimap game))

  ;; Pause menu
  (when paused
    (draw-pause-menu game)))

(defn draw-ending [{:keys [won?]}]
  (rcd/clear-background! GB-GREEN01)

  (if won?
    (do
      (rtd/draw-text! "YOU ESCAPED!" 70 80 25 GB-GREEN03)
      (rtd/draw-text! "CONGRATULATIONS!" 60 115 15 GB-GREEN02))
    (do
      (rtd/draw-text! "YOU ARE LOST..." 60 80 25 GB-GREEN03)
      (rtd/draw-text! "BETTER LUCK NEXT TIME" 40 115 12 GB-GREEN02)))

  (rsb/draw-rectangle! 100 170 120 20 GB-GREEN02)
  (rtd/draw-text! "CONTINUE" 120 173 12 GB-GREEN03))

(defn draw [{:keys [screen]
             :as game}]
  (rcd/begin-drawing!)
  (case screen
    0 (draw-title game)
    1 (draw-gameplay game)
    2 (draw-ending game))
  (rtd/draw-fps! 5 5)
  (rcd/end-drawing!))

;; ============================================================================
;; Main Loop
;; ============================================================================

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (when-not (or (:exit? game) (rcw/window-should-close?))
        (reset! game-atom game)
        (draw game)
        (recur))))
  (rc3d/enable-cursor!)
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  @game-atom
  (swap! game-atom assoc :show-minimap true)
  (swap! game-atom assoc :time-remaining 60))
