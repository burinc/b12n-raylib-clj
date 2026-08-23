(ns examples.models.billboard-rendering
  "raylib [models] example - billboard rendering

   Two textured quads that always face the camera, orbiting overhead. One is
   drawn with `draw-billboard!` (axis-aligned, sized by a scalar) and one with
   `draw-billboard-pro!` (explicit up vector, source rect, origin and
   rotation, so it can spin in place).

   The part worth keeping is the draw ORDER. Both billboards have alpha, and
   alpha-blended geometry does not depth-sort correctly on the GPU - whichever
   is drawn second wins the blend regardless of which is actually nearer. So
   each frame the two are sorted by distance from the camera and the FARTHER
   one is drawn first. Remove that and the far billboard punches a hole
   through the near one whenever they overlap.

   Difficulty: 2/4
   Based on: models/models_billboard_rendering.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.camera3d :as rc3d]
   [raylib.models :as rm]
   [raylib.raymath :as rmath]
   [raylib.text.drawing :as rtd]
   [raylib.textures.texture-loading :as rtl]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def static-pos {:x 0.0 :y 2.0 :z 0.0})
(def rotating-pos {:x 1.0 :y 2.0 :z 1.0})
(def bill-up {:x 0.0 :y 1.0 :z 0.0})

(defn initial-state []
  {:camera {:position {:x 5.0 :y 4.0 :z 5.0}
            :target {:x 0.0 :y 2.0 :z 0.0}
            :up {:x 0.0 :y 1.0 :z 0.0}
            :fovy 45.0
            :projection rc3d/CAMERA_PERSPECTIVE}
   :rotation 0.0 :texture nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [models] example - billboard rendering")
  (swap! game-atom assoc :texture (rtl/load-texture! "resources/billboard.png"))
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn farther-first
  "The two billboard positions ordered back to front from `camera-pos`.

   Pure, because this ordering IS the example: alpha billboards must be drawn
   far-to-near or the blend comes out wrong."
  [camera-pos a b]
  (if (> (rmath/v3-distance camera-pos a) (rmath/v3-distance camera-pos b))
    [a b]
    [b a]))

(defn tick [{:keys [camera rotation] :as state}]
  (debug-stats/update!)
  (assoc state
         :camera (rc3d/update-camera camera rc3d/CAMERA_ORBITAL)
         :rotation (+ rotation 0.4)))

(defn draw [{:keys [camera rotation texture]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rc3d/begin-mode-3d! camera)
  (rc3d/draw-grid! 10 1.0)

  (let [source {:x 0.0 :y 0.0
                :width (float (:width texture)) :height (float (:height texture))}
        size {:x (/ (:width texture) (double (:height texture))) :y 1.0}
        origin (rmath/v2-scale size 0.5)
        draw-one (fn [pos]
                   (if (= pos static-pos)
                     (rm/draw-billboard! camera texture pos 2.0 colors/white)
                     (rm/draw-billboard-pro! camera texture source pos bill-up
                                             size origin (float rotation) colors/white)))]
    ;; Far one first - see the namespace docstring.
    (run! draw-one (farther-first (:position camera) static-pos rotating-pos)))

  (rc3d/end-mode-3d!)
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
  (when-let [t (:texture @game-atom)] (rtl/unload-texture! t))
  (rcw/close-window!))
