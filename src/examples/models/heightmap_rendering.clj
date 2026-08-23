(ns examples.models.heightmap-rendering
  "raylib [models] example - heightmap rendering

   Terrain from a greyscale PNG: `gen-mesh-heightmap` reads each pixel's
   brightness as an elevation and emits two triangles per pixel quad. The
   128x128 source therefore yields exactly (128-1)^2 * 2 = 32,258 triangles,
   which is a useful sanity check that the mesh generated is the mesh meant.

   The same image is then used twice over - as the terrain's shape via the
   mesh, and as its surface via the texture uploaded from it - which is why
   the inset preview in the corner looks like a height map and the terrain
   looks like the same map draped over hills.

   Difficulty: 3/4
   Based on: models/models_heightmap_rendering.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.camera3d :as rc3d]
   [raylib.models :as rm]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.textures.drawing :as rtdw]
   [raylib.textures.texture-loading :as rtl]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def map-position {:x -8.0 :y 0.0 :z -8.0})

(defn initial-state []
  {:camera {:position {:x 18.0 :y 21.0 :z 18.0}
            :target {:x 0.0 :y 0.0 :z 0.0}
            :up {:x 0.0 :y 1.0 :z 0.0}
            :fovy 45.0
            :projection rc3d/CAMERA_PERSPECTIVE}
   :model nil :texture nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [models] example - heightmap rendering")
  (let [image (rtl/load-image "resources/heightmap.png")
        texture (rtl/load-texture-from-image image)
        model (rm/load-model-from-mesh (rm/gen-mesh-heightmap image {:x 16.0 :y 8.0 :z 16.0}))]
    (rm/set-model-material-texture! model texture)
    ;; The mesh and the texture both live on the GPU now; the pixels do not.
    (rtl/unload-image! image)
    (swap! game-atom assoc :model model :texture texture))
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [state]
  (debug-stats/update!)
  (update state :camera rc3d/update-camera rc3d/CAMERA_ORBITAL))

(defn draw [{:keys [camera model texture]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rc3d/begin-mode-3d! camera)
  (rm/draw-model! model map-position 1.0 colors/red)
  (rc3d/draw-grid! 20 1.0)
  (rc3d/end-mode-3d!)

  ;; The source heightmap at 1:1, so the terrain and its input sit side by side.
  (let [x (- screen-width (:width texture) 20)]
    (rtdw/draw-texture! texture x 20 colors/white)
    (rsb/draw-rectangle-lines! x 20 (:width texture) (:height texture) colors/green))
  (rtd/draw-fps! 10 10)
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
  (let [{:keys [model texture]} @game-atom]
    (when texture (rtl/unload-texture! texture))
    (when model (rm/unload-model! model)))
  (rcw/close-window!))
