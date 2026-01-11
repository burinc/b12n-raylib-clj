(ns examples.following-eyes
  "Raylib [shapes] example - following eyes
   
   Two eyes that follow your mouse cursor around the screen.
   Based on: raylib/examples/shapes/shapes_following_eyes.c
   
   Complexity: ⭐⭐ Easy
   
   Controls:
   - Mouse: Move to make eyes follow
   - F1: Toggle debug stats
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def SCLERA_RADIUS 80)
(def IRIS_RADIUS 24)
(def PUPIL_RADIUS 10)

(defn initial-state []
  (let [center-y (/ HEIGHT 2.0)]
    {:exit? false
     ;; Eye sclera (white part) positions - fixed
     :left-sclera {:x (- (/ WIDTH 2.0) 100) :y center-y}
     :right-sclera {:x (+ (/ WIDTH 2.0) 100) :y center-y}
     ;; Iris positions - will follow mouse
     :left-iris {:x (- (/ WIDTH 2.0) 100) :y center-y}
     :right-iris {:x (+ (/ WIDTH 2.0) 100) :y center-y}}))

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [shapes] example - following eyes")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn distance-squared
  "Calculate squared distance between two points"
  [p1 p2]
  (let [dx (- (:x p2) (:x p1))
        dy (- (:y p2) (:y p1))]
    (+ (* dx dx) (* dy dy))))

(defn point-in-circle?
  "Check if a point is inside a circle"
  [point center radius]
  (<= (distance-squared point center) (* radius radius)))

(defn constrain-iris-to-sclera
  "Constrain iris position to stay within sclera bounds"
  [iris-target sclera-center]
  (let [max-distance (- SCLERA_RADIUS IRIS_RADIUS)]
    (if (point-in-circle? iris-target sclera-center max-distance)
      ;; Mouse is within eye bounds, iris follows directly
      iris-target
      ;; Mouse is outside, constrain iris to edge of sclera
      (let [dx (- (:x iris-target) (:x sclera-center))
            dy (- (:y iris-target) (:y sclera-center))
            angle (Math/atan2 dy dx)
            constrained-x (+ (:x sclera-center) (* max-distance (Math/cos angle)))
            constrained-y (+ (:y sclera-center) (* max-distance (Math/sin angle)))]
        {:x constrained-x :y constrained-y}))))

(defn update-eyes [{:keys [left-sclera right-sclera] :as game}]
  (let [mouse-pos (rcm/get-mouse-position)
        mouse {:x (:x mouse-pos) :y (:y mouse-pos)}
        ;; Calculate constrained iris positions
        left-iris (constrain-iris-to-sclera mouse left-sclera)
        right-iris (constrain-iris-to-sclera mouse right-sclera)]
    (assoc game
           :left-iris left-iris
           :right-iris right-iris)))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      handle-input
      update-eyes))

(defn draw-eye
  "Draw a complete eye (sclera + iris + pupil)"
  [sclera-pos iris-pos sclera-color iris-color]
  ;; Draw sclera (white of eye)
  (rsb/draw-circle-v! sclera-pos SCLERA_RADIUS sclera-color)
  ;; Draw iris (colored part)
  (rsb/draw-circle-v! iris-pos IRIS_RADIUS iris-color)
  ;; Draw pupil (black center)
  (rsb/draw-circle-v! iris-pos PUPIL_RADIUS colors/black))

(defn draw [{:keys [left-sclera right-sclera left-iris right-iris]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; Draw left eye (brown iris)
  (draw-eye left-sclera left-iris colors/lightgray colors/brown)

  ;; Draw right eye (green iris)
  (draw-eye right-sclera right-iris colors/lightgray colors/darkgreen)

  ;; Draw instructions
  (ext/draw-line! 0 (- HEIGHT 20) WIDTH (- HEIGHT 20) colors/lightgray)
  (ext/measure-text "Move your mouse to make eyes follow!" 20) ;; just to warm up the binding

  ;; Draw debug stats overlay
  (debug-stats/draw!)

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
  ;; For REPL development - connect to port 7888 after running the game
  @game-atom

  ;; Reset to initial state
  (reset! game-atom (initial-state))

  ;; Move eyes further apart
  (swap! game-atom assoc-in [:left-sclera :x] 200)
  (swap! game-atom assoc-in [:right-sclera :x] 600)
  ;;
  )
