(ns examples.camera-3d-mode
  "raylib [core] example - 3d camera mode

   The minimal 3D scene: a perspective camera looking at a red cube on a
   grid. This is the 3D counterpart of hello-world - the smallest program
   that proves BeginMode3D, a Camera3D struct crossing the FFI boundary,
   and the depth buffer are all working.

   Difficulty: 1/4
   Based on: core/core_3d_camera_mode.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.camera3d :as rc3d]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def camera
  {:position {:x 0.0 :y 10.0 :z 10.0}
   :target   {:x 0.0 :y 0.0  :z 0.0}
   :up       {:x 0.0 :y 1.0  :z 0.0}
   :fovy     45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(def cube-position {:x 0.0 :y 0.0 :z 0.0})

(defn init []
  (rcw/init-window! screen-width screen-height
                    "raylib [core] example - 3d camera mode")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn draw []
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3d/begin-mode-3d! camera)
  (rc3d/draw-cube! cube-position (float 2.0) (float 2.0) (float 2.0) colors/red)
  (rc3d/draw-cube-wires! cube-position (float 2.0) (float 2.0) (float 2.0) colors/maroon)
  (rc3d/draw-grid! 10 (float 1.0))
  (rc3d/end-mode-3d!)

  (rtd/draw-text! "Welcome to the third dimension!" 10 40 20 colors/darkgray)
  (rtd/draw-fps! 10 10)
  (debug-stats/draw!)
  (rcd/end-drawing!))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (debug-stats/update!)
    (when-not (rcw/window-should-close?)
      (draw)
      (recur)))
  (rcw/close-window!))
