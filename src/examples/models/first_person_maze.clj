(ns examples.models.first-person-maze
  "Raylib [models] example - first person maze
   
   Navigate through a procedurally generated 3D maze.
   Uses first-person camera with collision detection.
   
   Complexity: ⭐⭐⭐ Intermediate (3/4)
   
   Controls:
   - WASD: Move
   - Mouse: Look around
   - R: Generate new maze
   - M: Toggle minimap
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.camera3d :as rc3d]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def MAZE_SIZE 15) ; Grid size (odd number works best)
(def CELL_SIZE 2.0) ; Size of each cell in 3D space
(def WALL_HEIGHT 2.5)
(def PLAYER_HEIGHT 0.6)
(def PLAYER_RADIUS 0.3)

;; Maze generation using recursive backtracking
(defn init-maze [size]
  (vec (repeat size (vec (repeat size :wall)))))

(defn get-cell [maze x y]
  (get-in maze [y x] :wall))

(defn set-cell [maze x y value]
  (assoc-in maze [y x] value))

(defn get-neighbors [x y size]
  (for [[dx dy] [[0 -2] [0 2] [-2 0] [2 0]]
        :let [nx (+ x dx) ny (+ y dy)]
        :when (and (> nx 0) (< nx (dec size))
                   (> ny 0) (< ny (dec size)))]
    [nx ny]))

(defn generate-maze
  "Generate maze using recursive backtracking"
  [size]
  (let [start-x 1
        start-y 1]
    (loop [maze (-> (init-maze size)
                    (set-cell start-x start-y :floor))
           stack [[start-x start-y]]
           visited #{[start-x start-y]}]
      (if (empty? stack)
        maze
        (let [[x y] (peek stack)
              neighbors (filter #(not (visited %)) (get-neighbors x y size))]
          (if (empty? neighbors)
            (recur maze (pop stack) visited)
            (let [[nx ny] (rand-nth neighbors)
                  ;; Carve passage between current and neighbor
                  mx (/ (+ x nx) 2)
                  my (/ (+ y ny) 2)]
              (recur (-> maze
                         (set-cell nx ny :floor)
                         (set-cell mx my :floor))
                     (conj stack [nx ny])
                     (conj visited [nx ny])))))))))

(defn find-start-position [maze]
  ;; Find a floor cell to start in
  (first (for [y (range MAZE_SIZE)
               x (range MAZE_SIZE)
               :when (= :floor (get-cell maze x y))]
           [(+ (* x CELL_SIZE) (/ CELL_SIZE 2))
            PLAYER_HEIGHT
            (+ (* y CELL_SIZE) (/ CELL_SIZE 2))])))

(defn make-camera [maze]
  (let [[px py pz] (find-start-position maze)]
    {:position {:x px
                :y py
                :z pz}
     :target {:x (+ px 1)
              :y py
              :z pz}
     :up {:x 0.0
          :y 1.0
          :z 0.0}
     :fovy 60.0
     :projection rc3d/CAMERA_PERSPECTIVE}))

(defn initial-state []
  (let [maze (generate-maze MAZE_SIZE)]
    {:exit? false
     :maze maze
     :camera (make-camera maze)
     :show-minimap? true}))

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - first person maze")
  (rc3d/disable-cursor!)
  (rct/set-target-fps! 60))

(defn world->grid [world-pos]
  [(int (/ world-pos CELL_SIZE))])

(defn is-wall? [maze x z]
  (let [gx (int (/ x CELL_SIZE))
        gz (int (/ z CELL_SIZE))]
    (= :wall (get-cell maze gx gz))))

(defn check-collision [maze old-pos new-pos]
  (let [px (:x new-pos)
        pz (:z new-pos)
        ;; Check collision at player bounds
        checks [[(- px PLAYER_RADIUS) pz]
                [(+ px PLAYER_RADIUS) pz]
                [px (- pz PLAYER_RADIUS)]
                [px (+ pz PLAYER_RADIUS)]]]
    (if (some (fn [[x z]] (is-wall? maze x z)) checks)
      old-pos
      new-pos)))

(defn handle-input [{:keys [maze]
                     :as game}]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)

    (rck/is-key-pressed? (:m enums/keyboard-key))
    (update :show-minimap? not)

    (rck/is-key-pressed? (:r enums/keyboard-key))
    (merge (initial-state))))

(defn update-camera [{:keys [camera maze]
                      :as game}]
  (let [old-pos (:position camera)
        ;; Update camera with raylib's built-in first person controls
        updated-camera (rc3d/update-camera camera rc3d/CAMERA_FIRST_PERSON)
        new-pos (:position updated-camera)
        ;; Apply collision detection
        final-pos (check-collision maze old-pos new-pos)]
    (assoc game :camera (assoc updated-camera :position final-pos))))

(defn tick [game]
  (-> game
      handle-input
      update-camera))

(defn draw-maze! [maze]
  ;; Draw floor
  (rc3d/draw-plane! {:x (* MAZE_SIZE CELL_SIZE 0.5)
                     :y 0
                     :z (* MAZE_SIZE CELL_SIZE 0.5)}
                    {:x (* MAZE_SIZE CELL_SIZE)
                     :y (* MAZE_SIZE CELL_SIZE)}
                    {:r 60
                     :g 60
                     :b 70
                     :a 255})

  ;; Draw walls
  (doseq [y (range MAZE_SIZE)
          x (range MAZE_SIZE)
          :when (= :wall (get-cell maze x y))]
    (let [wx (+ (* x CELL_SIZE) (/ CELL_SIZE 2))
          wz (+ (* y CELL_SIZE) (/ CELL_SIZE 2))]
      (rc3d/draw-cube! {:x wx
                        :y (/ WALL_HEIGHT 2)
                        :z wz}
                       CELL_SIZE WALL_HEIGHT CELL_SIZE
                       {:r 100
                        :g 100
                        :b 120
                        :a 255})
      (rc3d/draw-cube-wires! {:x wx
                              :y (/ WALL_HEIGHT 2)
                              :z wz}
                             CELL_SIZE WALL_HEIGHT CELL_SIZE
                             {:r 70
                              :g 70
                              :b 90
                              :a 255}))))

(defn draw-minimap! [maze camera]
  (let [map-scale 6
        map-x (- WIDTH (* MAZE_SIZE map-scale) 10)
        map-y 10
        player-gx (int (/ (:x (:position camera)) CELL_SIZE))
        player-gz (int (/ (:z (:position camera)) CELL_SIZE))]
    ;; Background
    (rsb/draw-rectangle! map-x map-y
                         (* MAZE_SIZE map-scale) (* MAZE_SIZE map-scale)
                         {:r 0
                          :g 0
                          :b 0
                          :a 150})
    ;; Walls
    (doseq [y (range MAZE_SIZE)
            x (range MAZE_SIZE)]
      (let [cell (get-cell maze x y)
            color (if (= cell :wall)
                    {:r 100
                     :g 100
                     :b 120
                     :a 255}
                    {:r 40
                     :g 40
                     :b 50
                     :a 255})]
        (rsb/draw-rectangle! (+ map-x (* x map-scale))
                             (+ map-y (* y map-scale))
                             map-scale map-scale color)))
    ;; Player
    (rsb/draw-rectangle! (+ map-x (* player-gx map-scale))
                         (+ map-y (* player-gz map-scale))
                         map-scale map-scale colors/red)
    ;; Border
    (rsb/draw-rectangle-lines! map-x map-y
                               (* MAZE_SIZE map-scale) (* MAZE_SIZE map-scale)
                               colors/green)))

(defn draw [{:keys [camera maze show-minimap?]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! {:r 30
                          :g 30
                          :b 40
                          :a 255})

  (rc3d/begin-mode-3d! camera)
  (draw-maze! maze)
  (rc3d/end-mode-3d!)

  ;; Draw UI
  (rtd/draw-text! "First Person Maze" 10 10 20 colors/white)
  (rtd/draw-text! "WASD: Move | Mouse: Look | R: New maze | M: Minimap" 10 35 15 colors/gray)

  (when show-minimap?
    (draw-minimap! maze camera))

  (rtd/draw-fps! 10 (- HEIGHT 25))

  (rcd/end-drawing!))

(defn cleanup []
  (rc3d/enable-cursor!))

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (when-not (or (:exit? game) (rcw/window-should-close?))
        (reset! game-atom game)
        (draw game)
        (recur))))
  (cleanup)
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  ;; REPL development - connect to port 7888
  @game-atom

  ;; Generate new maze
  (swap! game-atom merge (initial-state))

  ;; Toggle minimap
  (swap! game-atom update :show-minimap? not)
  ;;
  )
