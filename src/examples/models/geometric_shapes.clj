(ns examples.models.geometric-shapes
  "Raylib [models] example - geometric shapes
   
   Displays various 3D geometric primitives: cubes, spheres, cylinders, and capsules.
   Based on: raylib/examples/models/models_geometric_shapes.c
   
   Complexity: ⭐ Basic (1/4)
   
   Controls:
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.camera3d :as rc3d]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)

(defn make-camera []
  {:position {:x 0.0
              :y 10.0
              :z 10.0}
   :target {:x 0.0
            :y 0.0
            :z 0.0}
   :up {:x 0.0
        :y 1.0
        :z 0.0}
   :fovy 45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - geometric shapes")
  (rct/set-target-fps! 60))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (-> game
      handle-input))

(defn draw [{:keys [camera]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3d/begin-mode-3d! camera)

  ;; Draw cube (solid + wireframe)
  (rc3d/draw-cube! {:x -4.0
                    :y 0.0
                    :z 2.0} 2.0 5.0 2.0 colors/red)
  (rc3d/draw-cube-wires! {:x -4.0
                          :y 0.0
                          :z 2.0} 2.0 5.0 2.0 colors/gold)
  (rc3d/draw-cube-wires! {:x -4.0
                          :y 0.0
                          :z -2.0} 3.0 6.0 2.0 colors/maroon)

  ;; Draw spheres
  (rc3d/draw-sphere! {:x -1.0
                      :y 0.0
                      :z -2.0} 1.0 colors/green)
  (rc3d/draw-sphere-wires! {:x 1.0
                            :y 0.0
                            :z 2.0} 2.0 16 16 colors/lime)

  ;; Draw cylinders
  (rc3d/draw-cylinder! {:x 4.0
                        :y 0.0
                        :z -2.0} 1.0 2.0 3.0 4 colors/skyblue)
  (rc3d/draw-cylinder-wires! {:x 4.0
                              :y 0.0
                              :z -2.0} 1.0 2.0 3.0 4 colors/darkblue)
  (rc3d/draw-cylinder-wires! {:x 4.5
                              :y -1.0
                              :z 2.0} 1.0 1.0 2.0 6 colors/brown)

  ;; Draw cone (cylinder with 0 top radius)
  (rc3d/draw-cylinder! {:x 1.0
                        :y 0.0
                        :z -4.0} 0.0 1.5 3.0 8 colors/gold)
  (rc3d/draw-cylinder-wires! {:x 1.0
                              :y 0.0
                              :z -4.0} 0.0 1.5 3.0 8 colors/pink)

  ;; Draw capsules
  (rc3d/draw-capsule! {:x -3.0
                       :y 1.5
                       :z -4.0} {:x -4.0
                                 :y -1.0
                                 :z -4.0} 1.2 8 8 colors/violet)
  (rc3d/draw-capsule-wires! {:x -3.0
                             :y 1.5
                             :z -4.0} {:x -4.0
                                       :y -1.0
                                       :z -4.0} 1.2 8 8 colors/purple)

  ;; Draw grid
  (rc3d/draw-grid! 10 1.0)

  (rc3d/end-mode-3d!)

  (rtd/draw-fps! 10 10)

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
  ;; REPL development - connect to port 7888
  @game-atom

  ;; Adjust camera
  (swap! game-atom assoc-in [:camera :position :y] 15.0)

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
