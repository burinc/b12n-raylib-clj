(ns examples.basic-lighting
  "Raylib [shaders] example - basic lighting
   
   Demonstrates shader-based lighting with multiple colored lights.
   Based on: raylib/examples/shaders/shaders_basic_lighting.c
   
   Complexity: ⭐⭐⭐⭐ Advanced
   
   Controls:
   - Mouse: Orbital camera rotation
   - Y: Toggle yellow light
   - R: Toggle red light
   - G: Toggle green light
   - B: Toggle blue light
   - F1: Toggle debug stats
   - ESC: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.camera3d :as rc3]
   [raylib.core.shaders :as rcs]
   [raylib.text.drawing :as rtd]
   [raylib.lights :as lights]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib.utils :as ru]
   [coffi.mem :as mem]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def GLSL_VERSION 330)

;; Camera struct size: 3*Vector3 (36 bytes) + float (4) + int (4) = 44 bytes
(def CAMERA_SIZE 44)

(defn camera->native!
  "Write camera map to native memory"
  [cam-ptr {:keys [position target up fovy projection]}]
  ;; Position (0-11)
  (mem/write-float cam-ptr 0 (float (:x position)))
  (mem/write-float (mem/slice cam-ptr 4) 0 (float (:y position)))
  (mem/write-float (mem/slice cam-ptr 8) 0 (float (:z position)))
  ;; Target (12-23)
  (mem/write-float (mem/slice cam-ptr 12) 0 (float (:x target)))
  (mem/write-float (mem/slice cam-ptr 16) 0 (float (:y target)))
  (mem/write-float (mem/slice cam-ptr 20) 0 (float (:z target)))
  ;; Up (24-35)
  (mem/write-float (mem/slice cam-ptr 24) 0 (float (:x up)))
  (mem/write-float (mem/slice cam-ptr 28) 0 (float (:y up)))
  (mem/write-float (mem/slice cam-ptr 32) 0 (float (:z up)))
  ;; fovy (36-39)
  (mem/write-float (mem/slice cam-ptr 36) 0 (float fovy))
  ;; projection (40-43)
  (mem/write-int (mem/slice cam-ptr 40) 0 projection))

(defn native->camera
  "Read camera map from native memory"
  [cam-ptr]
  {:position {:x (mem/read-float cam-ptr 0)
              :y (mem/read-float (mem/slice cam-ptr 4) 0)
              :z (mem/read-float (mem/slice cam-ptr 8) 0)}
   :target {:x (mem/read-float (mem/slice cam-ptr 12) 0)
            :y (mem/read-float (mem/slice cam-ptr 16) 0)
            :z (mem/read-float (mem/slice cam-ptr 20) 0)}
   :up {:x (mem/read-float (mem/slice cam-ptr 24) 0)
        :y (mem/read-float (mem/slice cam-ptr 28) 0)
        :z (mem/read-float (mem/slice cam-ptr 32) 0)}
   :fovy (mem/read-float (mem/slice cam-ptr 36) 0)
   :projection (mem/read-int (mem/slice cam-ptr 40) 0)})

;; Initial state
(defn initial-state []
  {:camera {:position {:x 2.0
                       :y 4.0
                       :z 6.0}
            :target {:x 0.0
                     :y 0.5
                     :z 0.0}
            :up {:x 0.0
                 :y 1.0
                 :z 0.0}
            :fovy 45.0
            :projection rc3/CAMERA_PERSPECTIVE}
   :camera-ptr nil
   :shader nil
   :view-loc nil
   :lights []})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/set-config-flags :flag/msaa-4x-hint)
  (rcw/init-window! WIDTH HEIGHT "raylib [shaders] example - basic lighting")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)

  ;; Reset lights counter
  (lights/reset-lights!)

  ;; Allocate native memory for camera
  (let [camera-ptr (mem/alloc CAMERA_SIZE)]
    (camera->native! camera-ptr (:camera @game-atom))
    (swap! game-atom assoc :camera-ptr camera-ptr))

  ;; Load lighting shader
  (let [shader (rcs/load-shader
                (format "resources/shaders/glsl%d/lighting.vs" GLSL_VERSION)
                (format "resources/shaders/glsl%d/lighting.fs" GLSL_VERSION))
        view-loc (rcs/get-shader-location shader "viewPos")
        ambient-loc (rcs/get-shader-location shader "ambient")]

    ;; Set ambient light level
    (rcs/set-shader-value-vec4! shader ambient-loc [0.1 0.1 0.1 1.0])

    ;; Create lights
    (let [light0 (lights/create-light lights/LIGHT_POINT
                                      {:x -2.0
                                       :y 1.0
                                       :z -2.0}
                                      {:x 0.0
                                       :y 0.0
                                       :z 0.0}
                                      colors/yellow shader)
          light1 (lights/create-light lights/LIGHT_POINT
                                      {:x 2.0
                                       :y 1.0
                                       :z 2.0}
                                      {:x 0.0
                                       :y 0.0
                                       :z 0.0}
                                      colors/red shader)
          light2 (lights/create-light lights/LIGHT_POINT
                                      {:x -2.0
                                       :y 1.0
                                       :z 2.0}
                                      {:x 0.0
                                       :y 0.0
                                       :z 0.0}
                                      colors/green shader)
          light3 (lights/create-light lights/LIGHT_POINT
                                      {:x 2.0
                                       :y 1.0
                                       :z -2.0}
                                      {:x 0.0
                                       :y 0.0
                                       :z 0.0}
                                      colors/blue shader)]
      (swap! game-atom assoc
             :shader shader
             :view-loc view-loc
             :lights [light0 light1 light2 light3]))))

(defn handle-input [{:keys [shader lights]
                     :as game}]
  (let [;; Toggle lights with Y, R, G, B keys
        lights (cond-> lights
                 (rck/is-key-pressed? (:y enums/keyboard-key))
                 (update 0 lights/toggle-light)

                 (rck/is-key-pressed? (:r enums/keyboard-key))
                 (update 1 lights/toggle-light)

                 (rck/is-key-pressed? (:g enums/keyboard-key))
                 (update 2 lights/toggle-light)

                 (rck/is-key-pressed? (:b enums/keyboard-key))
                 (update 3 lights/toggle-light))]

    ;; Update light values in shader
    (doseq [light lights]
      (lights/update-light-values! shader light))

    (assoc game :lights lights)))

(defn tick [{:keys [camera-ptr shader view-loc]
             :as game}]
  (debug-stats/update!)

  ;; Update camera (orbital mode)
  (rc3/update-camera! camera-ptr rc3/CAMERA_ORBITAL)

  ;; Read back updated camera
  (let [camera (native->camera camera-ptr)]
    ;; Update shader with camera view position
    (rcs/set-shader-value-vec3! shader view-loc
                                [(:x (:position camera))
                                 (:y (:position camera))
                                 (:z (:position camera))])

    (-> game
        (assoc :camera camera)
        handle-input)))

(defn draw [{:keys [camera shader lights]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3/begin-mode-3d! camera)

  ;; Draw scene with lighting shader
  (rcs/begin-shader-mode! shader)
  (rc3/draw-plane! {:x 0.0
                    :y 0.0
                    :z 0.0} {:x 10.0
                             :y 10.0} colors/white)
  (rc3/draw-cube! {:x 0.0
                   :y 2.0
                   :z 0.0} 2.0 4.0 2.0 colors/white)
  (rcs/end-shader-mode!)

  ;; Draw light indicators (spheres)
  (doseq [light lights]
    (let [{:keys [enabled position color]} light]
      (if enabled
        (rc3/draw-sphere-ex! position 0.2 8 8 color)
        (rc3/draw-sphere-wires! position 0.2 8 8 (ru/fade color 0.3)))))

  (rc3/draw-grid! 10 1.0)

  (rc3/end-mode-3d!)

  ;; Draw UI
  (rtd/draw-fps! 10 10)
  (rtd/draw-text! "Use keys [Y][R][G][B] to toggle lights" 10 40 20 colors/darkgray)

  ;; Draw debug stats overlay
  (debug-stats/draw!)

  (rcd/end-drawing!))

(defn cleanup [{:keys [shader]}]
  (when shader
    (rcs/unload-shader! shader)))

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (let [game (tick @game-atom)]
        (reset! game-atom game)
        (draw game)
        (recur))))
  (cleanup @game-atom)
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  ;; For REPL development
  @game-atom

  ;; Check lights
  (:lights @game-atom)

  ;; Check shader
  (:shader @game-atom)
  ;;
  )
