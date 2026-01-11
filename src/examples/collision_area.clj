(ns examples.collision-area
  "Raylib [shapes] example - collision area
   
   Two rectangles - one moves automatically, one follows mouse.
   Shows collision detection and collision area calculation.
   Based on: raylib/examples/shapes/shapes_collision_area.c
   
   Complexity: ⭐⭐ Easy
   
   Controls:
   - Mouse: Move the blue box
   - SPACE: Pause/resume the gold box
   - F1: Toggle debug stats
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def HEADER_HEIGHT 40)

;; Box A (auto-moving) dimensions
(def BOX_A_WIDTH 200)
(def BOX_A_HEIGHT 100)
(def BOX_A_SPEED 4)

;; Box B (mouse-controlled) dimensions
(def BOX_B_WIDTH 60)
(def BOX_B_HEIGHT 60)

(defn initial-state []
  {:exit? false
   :paused? false
   ;; Box A: Auto-moving box
   :box-a {:x 10
           :y (- (/ HEIGHT 2.0) (/ BOX_A_HEIGHT 2.0))
           :width BOX_A_WIDTH
           :height BOX_A_HEIGHT}
   :box-a-speed BOX_A_SPEED
   ;; Box B: Mouse-controlled box
   :box-b {:x (- (/ WIDTH 2.0) (/ BOX_B_WIDTH 2.0))
           :y (- (/ HEIGHT 2.0) (/ BOX_B_HEIGHT 2.0))
           :width BOX_B_WIDTH
           :height BOX_B_HEIGHT}})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [shapes] example - collision area")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn rects-collide?
  "Check if two rectangles collide"
  [{ax :x
    ay :y
    aw :width
    ah :height}
   {bx :x
    by :y
    bw :width
    bh :height}]
  (and (<= ax (+ bx bw))
       (>= (+ ax aw) bx)
       (<= ay (+ by bh))
       (>= (+ ay ah) by)))

(defn get-collision-rect
  "Get the intersection rectangle of two colliding rectangles"
  [{ax :x
    ay :y
    aw :width
    ah :height}
   {bx :x
    by :y
    bw :width
    bh :height}]
  (let [x (max ax bx)
        y (max ay by)
        right (min (+ ax aw) (+ bx bw))
        bottom (min (+ ay ah) (+ by bh))]
    {:x x
     :y y
     :width (- right x)
     :height (- bottom y)}))

(defn update-box-a [{:keys [paused? box-a box-a-speed]
                     :as game}]
  (if paused?
    game
    (let [new-x (+ (:x box-a) box-a-speed)
          ;; Bounce off walls
          [final-x new-speed]
          (cond
            (>= (+ new-x BOX_A_WIDTH) WIDTH) [(- WIDTH BOX_A_WIDTH) (- box-a-speed)]
            (<= new-x 0) [0 (- box-a-speed)]
            :else [new-x box-a-speed])]
      (-> game
          (assoc-in [:box-a :x] final-x)
          (assoc :box-a-speed new-speed)))))

(defn update-box-b [{:keys [box-b]
                     :as game}]
  (let [mouse-x (rcm/get-mouse-x)
        mouse-y (rcm/get-mouse-y)
        ;; Center box on mouse
        new-x (- mouse-x (/ (:width box-b) 2))
        new-y (- mouse-y (/ (:height box-b) 2))
        ;; Clamp to screen bounds
        clamped-x (max 0 (min (- WIDTH (:width box-b)) new-x))
        clamped-y (max HEADER_HEIGHT (min (- HEIGHT (:height box-b)) new-y))]
    (-> game
        (assoc-in [:box-b :x] clamped-x)
        (assoc-in [:box-b :y] clamped-y))))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-pressed? (:space enums/keyboard-key))
    (update :paused? not)

    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      handle-input
      update-box-a
      update-box-b))

(defn draw [{:keys [box-a box-b paused?]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (let [collision? (rects-collide? box-a box-b)]
    ;; Draw header bar (changes color on collision)
    (rsb/draw-rectangle! 0 0 WIDTH HEADER_HEIGHT
                         (if collision? colors/red colors/black))

    ;; Draw Box A (gold, auto-moving)
    (rsb/draw-rectangle! (int (:x box-a)) (int (:y box-a))
                         (int (:width box-a)) (int (:height box-a))
                         colors/gold)

    ;; Draw Box B (blue, mouse-controlled)
    (rsb/draw-rectangle! (int (:x box-b)) (int (:y box-b))
                         (int (:width box-b)) (int (:height box-b))
                         colors/blue)

    ;; Draw collision area if colliding
    (when collision?
      (let [col-rect (get-collision-rect box-a box-b)
            col-area (* (int (:width col-rect)) (int (:height col-rect)))]
        ;; Draw collision rectangle
        (rsb/draw-rectangle! (int (:x col-rect)) (int (:y col-rect))
                             (int (:width col-rect)) (int (:height col-rect))
                             colors/lime)
        ;; Draw collision message
        (rtd/draw-text! "COLLISION!" (- (/ WIDTH 2) 60) 10 20 colors/black)
        (rtd/draw-text! (str "Collision Area: " col-area)
                        (- (/ WIDTH 2) 80) (+ HEADER_HEIGHT 10) 20 colors/black))))

  ;; Draw instructions
  (rtd/draw-text! "Press SPACE to PAUSE/RESUME" 20 (- HEIGHT 35) 20 colors/lightgray)
  (when paused?
    (rtd/draw-text! "PAUSED" (- (/ WIDTH 2) 40) (/ HEIGHT 2) 30 colors/gray))

  ;; Draw debug stats overlay
  (debug-stats/draw!)

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
  ;; For REPL development - connect to port 7888 after running the game
  @game-atom

  ;; Pause/resume
  (swap! game-atom update :paused? not)

  ;; Speed up box A
  (swap! game-atom assoc :box-a-speed 8)

  ;; Reset to initial state
  (reset! game-atom (initial-state))
  ;;
  )
