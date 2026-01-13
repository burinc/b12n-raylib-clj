(ns examples.models.ray-picking
  "Raylib [models] example - ray picking
   
   Click on cubes to select them. Selected cubes change color.
   Demonstrates ray casting for 3D object selection.
   Based on: core_3d_picking.c
   
   Complexity: ⭐⭐ Intermediate (2/4)
   
   Controls:
   - Left Click: Select cube
   - Right Click: Toggle camera control
   - WASD: Move camera (when unlocked)
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.core.camera3d :as rc3d]
   [raylib.core.collision :as rcol]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def CUBE_SIZE {:x 2.0 :y 2.0 :z 2.0})

;; Define cube positions in a grid
(def cube-positions
  (for [x (range -4 5 4)
        z (range -4 5 4)]
    {:x (float x) :y 1.0 :z (float z)}))

(defn make-camera []
  {:position {:x 10.0 :y 10.0 :z 10.0}
   :target {:x 0.0 :y 1.0 :z 0.0}
   :up {:x 0.0 :y 1.0 :z 0.0}
   :fovy 45.0
   :projection rc3d/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:exit? false
   :camera (make-camera)
   :camera-mode rc3d/CAMERA_FREE
   :cursor-enabled? true
   :selected-cube nil
   :hover-cube nil
   :cubes (vec (map-indexed (fn [i pos] {:id i :position pos :selected? false})
                            cube-positions))})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [models] example - ray picking")
  (rct/set-target-fps! 60))

(defn make-bounding-box [pos size]
  {:min {:x (- (:x pos) (/ (:x size) 2))
         :y (- (:y pos) (/ (:y size) 2))
         :z (- (:z pos) (/ (:z size) 2))}
   :max {:x (+ (:x pos) (/ (:x size) 2))
         :y (+ (:y pos) (/ (:y size) 2))
         :z (+ (:z pos) (/ (:z size) 2))}})

(defn check-ray-hit [ray cube]
  (let [bbox (make-bounding-box (:position cube) CUBE_SIZE)
        collision (rcol/get-ray-collision-box ray bbox)]
    (when (pos? (:hit collision))
      {:cube cube
       :distance (:distance collision)
       :point (:point collision)})))

(defn find-closest-hit [ray cubes]
  (->> cubes
       (map #(check-ray-hit ray %))
       (filter some?)
       (sort-by :distance)
       first))

(defn handle-input [{:keys [camera cursor-enabled? cubes] :as game}]
  (let [;; Toggle cursor on right click
        toggle-cursor? (rcm/is-mouse-button-pressed? 1)
        new-cursor-enabled? (if toggle-cursor? (not cursor-enabled?) cursor-enabled?)

        ;; Get mouse ray for picking
        mouse-pos (rcm/get-mouse-position)
        ray (rcol/get-screen-to-world-ray mouse-pos camera)

        ;; Find what we're hovering over
        hover-hit (find-closest-hit ray cubes)
        hover-cube (:cube hover-hit)

        ;; Handle selection on left click
        select? (and cursor-enabled? (rcm/is-mouse-button-pressed? 0))
        selected-cube (when (and select? hover-cube) hover-cube)]

    ;; Manage cursor state
    (when (and toggle-cursor? new-cursor-enabled?)
      (rc3d/enable-cursor!))
    (when (and toggle-cursor? (not new-cursor-enabled?))
      (rc3d/disable-cursor!))

    (cond-> game
      (rck/is-key-pressed? (:q enums/keyboard-key))
      (assoc :exit? true)

      toggle-cursor?
      (assoc :cursor-enabled? new-cursor-enabled?)

      true
      (assoc :hover-cube hover-cube
             :ray ray)

      selected-cube
      (-> (assoc :selected-cube selected-cube)
          (update :cubes (fn [cs]
                           (mapv #(if (= (:id %) (:id selected-cube))
                                    (update % :selected? not)
                                    %)
                                 cs)))))))

(defn update-camera [{:keys [camera cursor-enabled?] :as game}]
  (if cursor-enabled?
    game ; Don't update camera when cursor is enabled (for picking)
    (let [updated-camera (rc3d/update-camera camera rc3d/CAMERA_FREE)]
      (assoc game :camera updated-camera))))

(defn tick [game]
  (-> game
      handle-input
      update-camera))

(defn draw-cube-with-state [{:keys [position selected?]} hover?]
  (let [color (cond
                selected? colors/lime
                hover? colors/yellow
                :else colors/gray)
        wire-color (if selected? colors/darkgreen colors/darkgray)]
    (rc3d/draw-cube! position (:x CUBE_SIZE) (:y CUBE_SIZE) (:z CUBE_SIZE) color)
    (rc3d/draw-cube-wires! position (:x CUBE_SIZE) (:y CUBE_SIZE) (:z CUBE_SIZE) wire-color)))

(defn draw [{:keys [camera cubes hover-cube ray cursor-enabled?]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rc3d/begin-mode-3d! camera)

  ;; Draw all cubes
  (doseq [cube cubes]
    (draw-cube-with-state cube (= (:id cube) (:id hover-cube))))

  ;; Draw ray for debugging (when camera is locked)
  (when (and ray (not cursor-enabled?))
    (rcol/draw-ray! ray colors/red))

  ;; Draw grid
  (rc3d/draw-grid! 20 1.0)

  (rc3d/end-mode-3d!)

  ;; Draw UI
  (rtd/draw-text! "Ray Picking Demo" 10 10 20 colors/darkgray)
  (rtd/draw-text! "Left Click: Select cube | Right Click: Toggle camera" 10 35 15 colors/gray)
  (rtd/draw-text! (if cursor-enabled? "Mode: PICKING" "Mode: CAMERA (WASD)") 10 55 15
                  (if cursor-enabled? colors/green colors/blue))

  ;; Show selection count
  (let [selected-count (count (filter :selected? cubes))]
    (rtd/draw-text! (str "Selected: " selected-count " cubes") 10 (- HEIGHT 30) 15 colors/maroon))

  (rtd/draw-fps! (- WIDTH 100) 10)

  (rcd/end-drawing!))

(defn cleanup [{:keys [cursor-enabled?]}]
  (when-not cursor-enabled?
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

  ;; Check selected cubes
  (filter :selected? (:cubes @game-atom))

  ;; Clear all selections
  (swap! game-atom update :cubes (fn [cs] (mapv #(assoc % :selected? false) cs)))

  ;; Reset
  (reset! game-atom (initial-state))
  ;;
  )
