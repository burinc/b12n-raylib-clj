(ns examples.models.basic-voxel
  "raylib [models] example - basic voxel

   An 8x8x8 block of cubes you walk through in first person and remove by
   left-clicking. The pick is a ray cast from the screen centre, tested
   against every voxel's bounding box; the nearest hit is the one removed.

   First example built on `raylib.models`. Worth knowing what it exercises:
   the `Model` struct is passed BY VALUE to every `draw-model!` call, so the
   whole 136-byte layout - including the nested skeleton that raylib 6.0
   added - has to be right or the draw reads garbage.

   It also sets the cube's material colour through
   `set-model-material-color!`, which walks into memory the model owns
   because raylib exposes no function for it. That matters visually: raylib
   MULTIPLIES the material colour by the tint passed to `draw-model!`, and
   the C sets both to BEIGE, so the cubes are a darker beige than either
   alone. Setting only the tint would render them noticeably lighter.

   Difficulty: 3/4
   Based on: models/models_basic_voxel.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.mouse :as rcm]
   [raylib.core.cursor :as rcur]
   [raylib.core.camera3d :as rc3d]
   [raylib.core.collision :as rcol]
   [raylib.models :as rm]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def world-size 8)

(defn voxel-box
  "The unit bounding box centred on a voxel's integer coordinate."
  [[x y z]]
  {:min {:x (- x 0.5) :y (- y 0.5) :z (- z 0.5)}
   :max {:x (+ x 0.5) :y (+ y 0.5) :z (+ z 0.5)}})

(defn all-voxels []
  (set (for [x (range world-size) y (range world-size) z (range world-size)]
         [x y z])))

(defn pick-voxel
  "The voxel in `voxels` that `ray` hits first, or nil.

   Pure: given a ray and a set of coordinates it picks a winner without
   touching raylib state, so the nearest-hit rule is checkable. `hit-fn`
   returns {:hit :distance} for a ray/box pair - raylib's
   GetRayCollisionBox in the running example."
  [hit-fn ray voxels]
  (->> voxels
       (keep (fn [v] (let [c (hit-fn ray (voxel-box v))]
                       (when (pos? (:hit c)) [(:distance c) v]))))
       (reduce (fn [best [d v]] (if (or (nil? best) (< d (first best))) [d v] best)) nil)
       second))

(defn initial-state []
  {:camera {:position {:x -2.0 :y 0.0 :z -2.0}
            :target {:x 0.0 :y 0.0 :z 0.0}
            :up {:x 0.0 :y 1.0 :z 0.0}
            :fovy 45.0
            :projection rc3d/CAMERA_PERSPECTIVE}
   :voxels (all-voxels)
   :model nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [models] example - basic voxel")
  (rcur/disable-cursor!)
  (let [model (rm/load-model-from-mesh (rm/gen-mesh-cube 1.0 1.0 1.0))]
    ;; Multiplies with the BEIGE tint at draw time; see the docstring.
    (rm/set-model-material-color! model colors/beige)
    (swap! game-atom assoc :model model))
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [camera voxels] :as state}]
  (debug-stats/update!)
  (let [camera (rc3d/update-camera camera rc3d/CAMERA_FIRST_PERSON)]
    (if-not (rcm/is-mouse-button-pressed? (:left enums/mouse-button))
      (assoc state :camera camera)
      (let [centre {:x (/ (rcw/get-screen-width) 2.0) :y (/ (rcw/get-screen-height) 2.0)}
            ray (rcol/get-screen-to-world-ray centre camera)
            hit (pick-voxel rcol/get-ray-collision-box ray voxels)]
        (assoc state :camera camera
               :voxels (if hit (disj voxels hit) voxels))))))

(defn draw [{:keys [camera voxels model]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rc3d/begin-mode-3d! camera)
  (rc3d/draw-grid! 10 1.0)
  (doseq [[x y z] voxels
          :let [p {:x (float x) :y (float y) :z (float z)}]]
    (rm/draw-model! model p 1.0 colors/beige)
    (rc3d/draw-cube-wires! p 1.0 1.0 1.0 colors/black))
  (rc3d/end-mode-3d!)
  (rsb/draw-circle! (quot (rcw/get-screen-width) 2) (quot (rcw/get-screen-height) 2)
                    4.0 colors/red)
  (rtd/draw-text! "Left-click a voxel to remove it!" 10 10 20 colors/darkgray)
  (rtd/draw-text! "WASD to move, mouse to look around" 10 35 10 colors/gray)
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
  (when-let [m (:model @game-atom)] (rm/unload-model! m))
  (rcw/close-window!))
