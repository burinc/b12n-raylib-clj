(ns examples.models.camera-modes
  "Raylib [models] example - camera modes
   
   Demonstrates different 3D camera modes:
   - Free camera (WASD + mouse)
   - Orbital camera (auto-rotate around target)
   - First person camera
   
   Complexity: ⭐⭐ Intermediate (2/4)
   
   Controls:
   - 1/2/3: Switch camera mode
   - WASD/Arrow keys: Move camera (in Free/First Person)
   - Mouse: Look around
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

;; Camera modes
(def CAMERA_FREE 1)
(def CAMERA_ORBITAL 2)
(def CAMERA_FIRST_PERSON 3)

(def mode-names
  {CAMERA_FREE "FREE (WASD + Mouse)"
   CAMERA_ORBITAL "ORBITAL (Auto-rotate)"
   CAMERA_FIRST_PERSON "FIRST PERSON (WASD + Mouse)"})

(defn make-camera []
  {:position {:x 10.0
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
   :camera (make-camera)
   :camera-mode CAMERA_ORBITAL
   :cursor-disabled? false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - camera modes")
  (rct/set-target-fps! 60))

(defn handle-input [{:keys [camera-mode cursor-disabled?]
                     :as game}]
  (let [new-mode (cond
                   (rck/is-key-pressed? (:one enums/keyboard-key)) CAMERA_FREE
                   (rck/is-key-pressed? (:two enums/keyboard-key)) CAMERA_ORBITAL
                   (rck/is-key-pressed? (:three enums/keyboard-key)) CAMERA_FIRST_PERSON
                   :else camera-mode)
        mode-changed? (not= new-mode camera-mode)
        ;; Manage cursor based on mode
        should-disable? (or (= new-mode CAMERA_FREE)
                            (= new-mode CAMERA_FIRST_PERSON))]
    (when (and mode-changed? should-disable? (not cursor-disabled?))
      (rc3d/disable-cursor!))
    (when (and mode-changed? (not should-disable?) cursor-disabled?)
      (rc3d/enable-cursor!))
    (cond-> game
      (rck/is-key-pressed? (:q enums/keyboard-key))
      (assoc :exit? true)

      mode-changed?
      (-> (assoc :camera-mode new-mode)
          (assoc :cursor-disabled? should-disable?)
          ;; Reset camera when changing modes
          (assoc :camera (make-camera))))))

(defn update-camera [{:keys [camera camera-mode]
                      :as game}]
  (let [updated-camera (rc3d/update-camera camera camera-mode)]
    (assoc game :camera updated-camera)))

(defn tick [game]
  (-> game
      handle-input
      update-camera))

(defn draw-scene [{:keys [camera]}]
  ;; Draw some reference geometry
  ;; Central pillar
  (rc3d/draw-cube! {:x 0.0
                    :y 1.0
                    :z 0.0} 2.0 2.0 2.0 colors/red)
  (rc3d/draw-cube-wires! {:x 0.0
                          :y 1.0
                          :z 0.0} 2.0 2.0 2.0 colors/maroon)

  ;; Corner cubes
  (doseq [x [-5.0 5.0]
          z [-5.0 5.0]]
    (rc3d/draw-cube! {:x x
                      :y 0.5
                      :z z} 1.0 1.0 1.0 colors/blue)
    (rc3d/draw-cube-wires! {:x x
                            :y 0.5
                            :z z} 1.0 1.0 1.0 colors/darkblue))

  ;; Spheres at different heights
  (rc3d/draw-sphere! {:x 3.0
                      :y 1.0
                      :z 0.0} 0.5 colors/green)
  (rc3d/draw-sphere! {:x -3.0
                      :y 2.0
                      :z 0.0} 0.5 colors/orange)
  (rc3d/draw-sphere! {:x 0.0
                      :y 3.0
                      :z 3.0} 0.5 colors/purple)

  ;; Ground plane indicator
  (rc3d/draw-plane! {:x 0.0
                     :y 0.0
                     :z 0.0} {:x 10.0
                              :y 10.0} colors/lightgray)

  ;; Grid
  (rc3d/draw-grid! 20 1.0))

(defn draw [{:keys [camera camera-mode]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3d/begin-mode-3d! camera)
  (draw-scene {:camera camera})
  (rc3d/end-mode-3d!)

  ;; Draw UI
  (rtd/draw-text! "Camera Modes Demo" 10 10 20 colors/darkgray)
  (rtd/draw-text! (str "Current: " (get mode-names camera-mode)) 10 35 20 colors/maroon)
  (rtd/draw-text! "Press 1: FREE | 2: ORBITAL | 3: FIRST PERSON" 10 60 15 colors/gray)

  ;; Camera position info
  (let [{:keys [x y z]} (:position camera)]
    (rtd/draw-text! (format "Pos: (%.1f, %.1f, %.1f)" x y z) 10 (- HEIGHT 30) 15 colors/darkgray))

  (rtd/draw-fps! (- WIDTH 100) 10)

  (rcd/end-drawing!))

(defn cleanup [{:keys [cursor-disabled?]}]
  (when cursor-disabled?
    (rc3d/enable-cursor!)))

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (when-not (or (:exit? game) (rcw/window-should-close?))
        (reset! game-atom game)
        (draw game)
        (recur))))
  (cleanup @game-atom)
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  ;; REPL development - connect to port 7888
  @game-atom

  ;; Check camera mode
  (:camera-mode @game-atom)

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
