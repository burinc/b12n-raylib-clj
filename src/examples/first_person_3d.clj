(ns examples.first-person-3d
  "Raylib [core] example - 3D camera first person
   
   First-person camera with mouse look and WASD movement.
   Features multiple camera modes and random columns in a walled arena.
   Based on: raylib/examples/core/core_3d_camera_first_person.c
   
   Complexity: ⭐⭐⭐ Intermediate
   
   Controls:
   - W/A/S/D: Move forward/left/backward/right
   - Mouse: Look around
   - Space: Move up
   - Left-Ctrl: Move down
   - 1/2/3/4: Switch camera mode (Free/First-person/Third-person/Orbital)
   - P: Toggle perspective/orthographic projection
   - F1: Toggle debug stats
   - Q: Exit (re-enables cursor)"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.camera3d :as rc3d]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.structs :as rs]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [coffi.mem :as mem]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def MAX_COLUMNS 20)

;; Generate random columns at startup
(defn generate-columns []
  (vec
   (for [_ (range MAX_COLUMNS)]
     (let [height (+ 1.0 (rand 11.0))
           x (- (rand 30.0) 15.0)
           z (- (rand 30.0) 15.0)]
       {:height height
        :position {:x x
                   :y (/ height 2.0)
                   :z z}
        :color {:r (+ 20 (rand-int 235))
                :g (+ 10 (rand-int 45))
                :b 30
                :a 255}}))))

(defn make-camera
  "Create initial camera"
  []
  {:position {:x 0.0
              :y 2.0
              :z 4.0}
   :target {:x 0.0
            :y 2.0
            :z 0.0}
   :up {:x 0.0
        :y 1.0
        :z 0.0}
   :fovy 60.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)
   :camera-mode rc3d/CAMERA_FIRST_PERSON
   :columns (generate-columns)
   ;; Native memory for camera (allocated in init)
   :camera-ptr nil})

(def game-atom (atom (initial-state)))

;; Camera memory helpers
(defn camera->native!
  "Write camera map to native memory pointer"
  [ptr camera]
  (let [pos (:position camera)
        tgt (:target camera)
        up (:up camera)]
    ;; Camera3D layout: position(3f), target(3f), up(3f), fovy(f), projection(i)
    ;; = 3*4 + 3*4 + 3*4 + 4 + 4 = 44 bytes
    (mem/write-float (mem/slice ptr 0 4) (:x pos))
    (mem/write-float (mem/slice ptr 4 4) (:y pos))
    (mem/write-float (mem/slice ptr 8 4) (:z pos))
    (mem/write-float (mem/slice ptr 12 4) (:x tgt))
    (mem/write-float (mem/slice ptr 16 4) (:y tgt))
    (mem/write-float (mem/slice ptr 20 4) (:z tgt))
    (mem/write-float (mem/slice ptr 24 4) (:x up))
    (mem/write-float (mem/slice ptr 28 4) (:y up))
    (mem/write-float (mem/slice ptr 32 4) (:z up))
    (mem/write-float (mem/slice ptr 36 4) (:fovy camera))
    (mem/write-int (mem/slice ptr 40 4) (:projection camera))))

(defn native->camera
  "Read camera map from native memory pointer"
  [ptr]
  {:position {:x (mem/read-float (mem/slice ptr 0 4))
              :y (mem/read-float (mem/slice ptr 4 4))
              :z (mem/read-float (mem/slice ptr 8 4))}
   :target {:x (mem/read-float (mem/slice ptr 12 4))
            :y (mem/read-float (mem/slice ptr 16 4))
            :z (mem/read-float (mem/slice ptr 20 4))}
   :up {:x (mem/read-float (mem/slice ptr 24 4))
        :y (mem/read-float (mem/slice ptr 28 4))
        :z (mem/read-float (mem/slice ptr 32 4))}
   :fovy (mem/read-float (mem/slice ptr 36 4))
   :projection (mem/read-int (mem/slice ptr 40 4))})

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [core] example - 3D camera first person")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  ;; Allocate native memory for camera (44 bytes for Camera3D struct)
  (let [camera-ptr (mem/alloc 44)]
    (swap! game-atom assoc :camera-ptr camera-ptr)
    ;; Initialize native camera memory
    (camera->native! camera-ptr (:camera @game-atom)))
  ;; Disable cursor for FPS-style mouse look
  (rc3d/disable-cursor!))

(defn camera-mode-name [mode]
  (case mode
    0 "CUSTOM"
    1 "FREE"
    2 "ORBITAL"
    3 "FIRST_PERSON"
    4 "THIRD_PERSON"
    "UNKNOWN"))

(defn projection-name [proj]
  (case proj
    0 "PERSPECTIVE"
    1 "ORTHOGRAPHIC"
    "UNKNOWN"))

(defn handle-camera-mode-switch [{:keys [camera-ptr]
                                  :as game}]
  (cond
    (rck/is-key-pressed? (:one enums/keyboard-key))
    (do
      ;; Reset up vector when switching modes
      (mem/write-float (mem/slice camera-ptr 24 4) 0.0)
      (mem/write-float (mem/slice camera-ptr 28 4) 1.0)
      (mem/write-float (mem/slice camera-ptr 32 4) 0.0)
      (assoc game :camera-mode rc3d/CAMERA_FREE))

    (rck/is-key-pressed? (:two enums/keyboard-key))
    (do
      (mem/write-float (mem/slice camera-ptr 24 4) 0.0)
      (mem/write-float (mem/slice camera-ptr 28 4) 1.0)
      (mem/write-float (mem/slice camera-ptr 32 4) 0.0)
      (assoc game :camera-mode rc3d/CAMERA_FIRST_PERSON))

    (rck/is-key-pressed? (:three enums/keyboard-key))
    (do
      (mem/write-float (mem/slice camera-ptr 24 4) 0.0)
      (mem/write-float (mem/slice camera-ptr 28 4) 1.0)
      (mem/write-float (mem/slice camera-ptr 32 4) 0.0)
      (assoc game :camera-mode rc3d/CAMERA_THIRD_PERSON))

    (rck/is-key-pressed? (:four enums/keyboard-key))
    (do
      (mem/write-float (mem/slice camera-ptr 24 4) 0.0)
      (mem/write-float (mem/slice camera-ptr 28 4) 1.0)
      (mem/write-float (mem/slice camera-ptr 32 4) 0.0)
      (assoc game :camera-mode rc3d/CAMERA_ORBITAL))

    :else game))

(defn handle-projection-toggle [{:keys [camera-ptr]
                                 :as game}]
  (if (rck/is-key-pressed? (:p enums/keyboard-key))
    (let [current-proj (mem/read-int (mem/slice camera-ptr 40 4))]
      (if (= current-proj rc3d/CAMERA_PERSPECTIVE)
        ;; Switch to orthographic
        (do
          (mem/write-int (mem/slice camera-ptr 40 4) rc3d/CAMERA_ORTHOGRAPHIC)
          (mem/write-float (mem/slice camera-ptr 36 4) 20.0) ; fovy = near plane width
          (assoc game :camera-mode rc3d/CAMERA_THIRD_PERSON))
        ;; Switch to perspective
        (do
          (mem/write-int (mem/slice camera-ptr 40 4) rc3d/CAMERA_PERSPECTIVE)
          (mem/write-float (mem/slice camera-ptr 36 4) 60.0)
          game)))
    game))

(defn update-camera-state [{:keys [camera-ptr camera-mode]
                            :as game}]
  ;; Call raylib's UpdateCamera to handle movement
  (rc3d/update-camera! camera-ptr camera-mode)
  ;; Read back updated camera values
  (assoc game :camera (native->camera camera-ptr)))

(defn handle-input [game]
  (if (rck/is-key-down? (:q enums/keyboard-key))
    (assoc game :exit? true)
    game))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      handle-input
      handle-camera-mode-switch
      handle-projection-toggle
      update-camera-state))

(defn draw [{:keys [camera camera-mode columns]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; 3D Scene
  (rc3d/begin-mode-3d! camera)

  ;; Draw ground
  (rc3d/draw-plane! {:x 0
                     :y 0
                     :z 0} {:x 32
                            :y 32} colors/lightgray)

  ;; Draw walls
  (rc3d/draw-cube! {:x -16.0
                    :y 2.5
                    :z 0.0} 1.0 5.0 32.0 colors/blue) ; Blue wall
  (rc3d/draw-cube! {:x 16.0
                    :y 2.5
                    :z 0.0} 1.0 5.0 32.0 colors/lime) ; Green wall
  (rc3d/draw-cube! {:x 0.0
                    :y 2.5
                    :z 16.0} 32.0 5.0 1.0 colors/gold) ; Yellow wall

  ;; Draw random columns
  (doseq [{:keys [position height color]} columns]
    (rc3d/draw-cube! position 2.0 height 2.0 color)
    (rc3d/draw-cube-wires! position 2.0 height 2.0 colors/maroon))

  ;; Draw player cube in third-person mode
  (when (= camera-mode rc3d/CAMERA_THIRD_PERSON)
    (rc3d/draw-cube! (:target camera) 0.5 0.5 0.5 colors/purple)
    (rc3d/draw-cube-wires! (:target camera) 0.5 0.5 0.5 colors/darkpurple))

  (rc3d/end-mode-3d!)

  ;; Draw info box (controls)
  (rsb/draw-rectangle! 5 5 330 100 {:r 102
                                    :g 191
                                    :b 255
                                    :a 127})
  (rtd/draw-text! "Camera controls:" 15 15 10 colors/black)
  (rtd/draw-text! "- Move: W, A, S, D, Space, Left-Ctrl" 15 30 10 colors/black)
  (rtd/draw-text! "- Look around: mouse" 15 45 10 colors/black)
  (rtd/draw-text! "- Camera mode: 1, 2, 3, 4" 15 60 10 colors/black)
  (rtd/draw-text! "- Projection toggle: P" 15 75 10 colors/black)
  (rtd/draw-text! "- Exit: Q" 15 90 10 colors/black)

  ;; Draw camera status box
  (rsb/draw-rectangle! 600 5 195 100 {:r 102
                                      :g 191
                                      :b 255
                                      :a 127})
  (rtd/draw-text! "Camera status:" 610 15 10 colors/black)
  (rtd/draw-text! (str "- Mode: " (camera-mode-name camera-mode)) 610 30 10 colors/black)
  (rtd/draw-text! (str "- Proj: " (projection-name (:projection camera))) 610 45 10 colors/black)
  (rtd/draw-text! (format "- Pos: (%.1f, %.1f, %.1f)"
                          (float (get-in camera [:position :x]))
                          (float (get-in camera [:position :y]))
                          (float (get-in camera [:position :z])))
                  610 60 10 colors/black)
  (rtd/draw-text! (format "- Target: (%.1f, %.1f, %.1f)"
                          (float (get-in camera [:target :x]))
                          (float (get-in camera [:target :y]))
                          (float (get-in camera [:target :z])))
                  610 75 10 colors/black)

  ;; Draw debug stats overlay
  (debug-stats/draw!)

  (rcd/end-drawing!))

(defn cleanup [_game]
  ;; Re-enable cursor before exiting
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
  (cleanup @game-atom)
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  ;; For REPL development - connect to port 7888 after running the game
  @game-atom

  ;; Check camera
  (:camera @game-atom)

  ;; Switch camera mode from REPL
  (swap! game-atom assoc :camera-mode rc3d/CAMERA_FREE)
  (swap! game-atom assoc :camera-mode rc3d/CAMERA_ORBITAL)

  ;; Regenerate columns
  (swap! game-atom assoc :columns (generate-columns))
  ;;
  )
