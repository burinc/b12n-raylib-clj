(ns examples.models.cubicmap-rendering
  "raylib [models] example - cubicmap rendering

   A 3D maze generated from a 32x16 PNG: `gen-mesh-cubicmap` reads the image
   and emits a cube for every non-black pixel, so the little bitmap shown in
   the corner IS the level. A separate atlas texture supplies the wall and
   floor faces.

   Two things this leans on that are easy to get wrong. The image is CPU-side
   and the texture is GPU-side: `load-texture-from-image` uploads a copy, so
   the image can be - and is - unloaded immediately afterward while both the
   texture and the generated mesh live on. And the atlas is attached with
   `set-model-material-texture!`, because raylib's own examples assign
   `model.materials[0].maps[DIFFUSE].texture` directly and there is no other
   way to reach that slot.

   Difficulty: 3/4
   Based on: models/models_cubicmap_rendering.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.camera3d :as rc3d]
   [raylib.models :as rm]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.textures.texture-loading :as rtl]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def map-position {:x -16.0 :y 0.0 :z -8.0})

(defn initial-state []
  {:camera {:position {:x 16.0 :y 14.0 :z 16.0}
            :target {:x 0.0 :y 0.0 :z 0.0}
            :up {:x 0.0 :y 1.0 :z 0.0}
            :fovy 45.0
            :projection rc3d/CAMERA_PERSPECTIVE}
   :pause? false :model nil :cubicmap nil :atlas nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [models] example - cubicmap rendering")
  (let [image (rtl/load-image "resources/cubicmap.png")
        cubicmap (rtl/load-texture-from-image image)
        model (rm/load-model-from-mesh (rm/gen-mesh-cubicmap image {:x 1.0 :y 1.0 :z 1.0}))
        atlas (rtl/load-texture! "resources/cubicmap_atlas.png")]
    (rm/set-model-material-texture! model atlas)
    ;; Safe here: the texture and the mesh are both on the GPU already, so
    ;; the CPU-side pixels have no further use.
    (rtl/unload-image! image)
    (swap! game-atom assoc :model model :cubicmap cubicmap :atlas atlas))
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [camera pause?] :as state}]
  (debug-stats/update!)
  (let [pause? (if (rck/is-key-pressed? (:p enums/keyboard-key)) (not pause?) pause?)]
    (assoc state
           :pause? pause?
           :camera (if pause? camera (rc3d/update-camera camera rc3d/CAMERA_ORBITAL)))))

(defn draw [{:keys [camera model cubicmap]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rc3d/begin-mode-3d! camera)
  (rm/draw-model! model map-position 1.0 colors/white)
  (rc3d/end-mode-3d!)

  ;; The source bitmap, drawn 4x so its pixels are legible.
  (let [w (:width cubicmap) h (:height cubicmap)
        x (- screen-width (* w 4.0) 20)]
    (rtl/draw-texture-ex! cubicmap {:x (float x) :y 20.0} 0.0 4.0 colors/white)
    (rsb/draw-rectangle-lines! (int x) 20 (* w 4) (* h 4) colors/green))
  (rtd/draw-text! "cubicmap image used to" 658 90 10 colors/gray)
  (rtd/draw-text! "generate map 3d model" 658 104 10 colors/gray)
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
  (let [{:keys [model cubicmap atlas]} @game-atom]
    (when cubicmap (rtl/unload-texture! cubicmap))
    (when atlas (rtl/unload-texture! atlas))
    (when model (rm/unload-model! model)))
  (rcw/close-window!))
