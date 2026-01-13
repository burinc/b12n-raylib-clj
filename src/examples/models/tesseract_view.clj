(ns examples.models.tesseract-view
  "Raylib [models] example - tesseract view
   
   Visualize a tesseract (4D hypercube) projected into 3D space.
   The tesseract rotates in 4D and is then projected to 3D for rendering.
   Based on: raylib/examples/models/models_tesseract_view.c
   
   Complexity: ⭐⭐ Intermediate (2/4)
   
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
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def DEG2RAD (/ Math/PI 180.0))

;; Tesseract vertices: all 16 combinations of ±1 for x, y, z, w
(def tesseract-vertices
  [[1 1 1 1] [1 1 1 -1]
   [1 1 -1 1] [1 1 -1 -1]
   [1 -1 1 1] [1 -1 1 -1]
   [1 -1 -1 1] [1 -1 -1 -1]
   [-1 1 1 1] [-1 1 1 -1]
   [-1 1 -1 1] [-1 1 -1 -1]
   [-1 -1 1 1] [-1 -1 1 -1]
   [-1 -1 -1 1] [-1 -1 -1 -1]])

(defn make-camera []
  {:position {:x 4.0 :y 4.0 :z 4.0}
   :target {:x 0.0 :y 0.0 :z 0.0}
   :up {:x 0.0 :y 0.0 :z 1.0} ; Z-up for this example
   :fovy 50.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - tesseract view")
  (rct/set-target-fps! 60))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (-> game
      handle-input))

;; Rotate a 2D point by angle (radians)
(defn rotate-2d [[x y] angle]
  (let [cos-a (Math/cos angle)
        sin-a (Math/sin angle)]
    [(- (* x cos-a) (* y sin-a))
     (+ (* x sin-a) (* y cos-a))]))

;; Transform a 4D vertex: rotate in XW plane and project to 3D
(defn transform-vertex [[x y z w] rotation]
  ;; Rotate the XW part of the vector
  (let [[new-x new-w] (rotate-2d [x w] rotation)
        ;; Perspective projection from 4D to 3D
        ;; Project from point (0,0,0,3) through the vertex to W=0 plane
        c (/ 3.0 (- 3.0 new-w))
        proj-x (* c new-x)
        proj-y (* c y)
        proj-z (* c z)]
    {:pos {:x proj-x :y proj-y :z proj-z}
     :w new-w}))

;; Check if two vertices differ by exactly 1 coordinate (are connected by an edge)
(defn vertices-connected? [v1 v2]
  (let [diffs (map (fn [a b] (if (= a b) 1 0)) v1 v2)]
    (= 3 (reduce + diffs))))

(defn draw [{:keys [camera]}]
  (let [time (ext/get-time)
        rotation (* DEG2RAD 45.0 time)
        ;; Transform all vertices
        transformed (mapv #(transform-vertex % rotation) tesseract-vertices)]

    (rcd/begin-drawing!)
    (rcd/clear-background! colors/raywhite)

    (rc3d/begin-mode-3d! camera)

    ;; Draw vertices as spheres (size based on W value)
    (doseq [{:keys [pos w]} transformed]
      (rc3d/draw-sphere! pos (Math/abs (* w 0.1)) colors/red))

    ;; Draw edges
    (doseq [i (range 16)
            j (range (inc i) 16)]
      (when (vertices-connected? (nth tesseract-vertices i)
                                 (nth tesseract-vertices j))
        (rc3d/draw-line-3d! (:pos (nth transformed i))
                            (:pos (nth transformed j))
                            colors/maroon)))

    (rc3d/end-mode-3d!)

    (rtd/draw-text! "4D Tesseract rotating in XW plane" 10 40 20 colors/darkgray)
    (rtd/draw-fps! 10 10)

    (rcd/end-drawing!)))

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

  ;; Check tesseract vertices
  tesseract-vertices

  ;; Test vertex transformation
  (transform-vertex [1 1 1 1] 0.0)

  ;; Test edge connectivity
  (vertices-connected? [1 1 1 1] [1 1 1 -1]) ; true - differ by w
  (vertices-connected? [1 1 1 1] [-1 -1 1 1]) ; false - differ by x and y

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
