(ns examples.world-screen
  "Raylib [core] example - world screen
   
   Demonstrates converting 3D world coordinates to 2D screen space.
   Useful for placing UI elements (health bars, labels) above 3D objects.
   Based on: raylib/examples/core/core_world_screen.c
   
   Complexity: ⭐⭐ Easy
   
   Controls:
   - Mouse: Rotate camera (third-person mode)
   - Mouse Wheel: Zoom in/out
   - F1: Toggle debug stats
   - ESC: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.camera3d :as rc3]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [coffi.mem :as mem]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)

(defn initial-state []
  {:camera {:position {:x 10.0
                       :y 10.0
                       :z 10.0}
            :target {:x 0.0
                     :y 0.0
                     :z 0.0}
            :up {:x 0.0
                 :y 1.0
                 :z 0.0}
            :fovy 45.0
            :projection rc3/CAMERA_PERSPECTIVE}
   :cube-position {:x 0.0
                   :y 0.0
                   :z 0.0}
   :cube-screen-position {:x 0.0
                          :y 0.0}
   :camera-ptr nil})

(def game-atom (atom (initial-state)))

;; Native memory helpers for Camera3D (44 bytes total)
(defn camera->native!
  "Write camera map to native memory pointer"
  [camera ptr]
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
  (rcw/init-window! WIDTH HEIGHT "raylib [core] example - world screen")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)

  ;; Allocate native memory for camera (needed for UpdateCamera)
  (let [camera-ptr (mem/alloc 44)]
    (camera->native! (:camera @game-atom) camera-ptr)
    (swap! game-atom assoc :camera-ptr camera-ptr)
    ;; Disable cursor for camera control
    (rc3/disable-cursor!)))

(defn tick [{:keys [camera camera-ptr cube-position]
             :as game}]
  (debug-stats/update!)

  ;; Update camera using raylib's built-in third-person mode
  (rc3/update-camera! camera-ptr rc3/CAMERA_THIRD_PERSON)

  ;; Read back updated camera from native memory
  (let [updated-camera (native->camera camera-ptr)
        ;; Calculate cube screen position (with offset to be above cube)
        cube-top {:x (:x cube-position)
                  :y (+ (:y cube-position) 2.5)
                  :z (:z cube-position)}
        screen-pos (rc3/get-world-to-screen cube-top updated-camera)]
    (assoc game
           :camera updated-camera
           :cube-screen-position screen-pos)))

(defn draw [{:keys [camera cube-position cube-screen-position]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; 3D rendering
  (rc3/begin-mode-3d! camera)

  ;; Draw cube at origin
  (rc3/draw-cube! cube-position 2.0 2.0 2.0 colors/red)
  (rc3/draw-cube-wires! cube-position 2.0 2.0 2.0 colors/maroon)

  ;; Draw grid
  (rc3/draw-grid! 10 1.0)

  (rc3/end-mode-3d!)

  ;; 2D UI elements (drawn after 3D, so they appear on top)
  ;; Draw label above the cube using screen coordinates
  (let [label "Enemy: 100/100"
        text-width (rtd/measure-text label 20)
        x (- (int (:x cube-screen-position)) (/ text-width 2))
        y (int (:y cube-screen-position))]
    (rtd/draw-text! label (int x) y 20 colors/black))

  ;; Info text
  (rtd/draw-text!
   (format "Cube position in screen space coordinates: [%d, %d]"
           (int (:x cube-screen-position))
           (int (:y cube-screen-position)))
   10 10 20 colors/lime)
  (rtd/draw-text! "Text 2d should be always on top of the cube" 10 40 20 colors/gray)

  ;; Draw debug stats overlay
  (debug-stats/draw!)

  (rcd/end-drawing!))

(defn cleanup []
  (rc3/enable-cursor!))

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (let [game (tick @game-atom)]
        (reset! game-atom game)
        (draw game)
        (recur))))
  (cleanup)
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  ;; For REPL development - connect to port 7888 after running the game
  @game-atom

  ;; Check screen position
  (:cube-screen-position @game-atom)

  ;; Check camera
  (:camera @game-atom)
  ;;
  )
