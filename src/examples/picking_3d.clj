(ns examples.picking-3d
  "raylib [core] example - 3D picking
   
   Click on the cube to select it. Right-click to toggle camera controls.
   
   Difficulty: ⭐⭐☆☆ (2/4)
   Based on: core/core_3d_picking.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.text.drawing :as rtd]
   [raylib.core.camera3d :as rcc]
   [raylib.core.collision :as rcol]
   [raylib.core.mouse :as rcm]))

(def screen-width 800)
(def screen-height 450)

;; Mouse button constants
(def MOUSE_LEFT 0)
(def MOUSE_RIGHT 1)

(def colors
  {:raywhite {:r 245
              :g 245
              :b 245
              :a 255}
   :gray {:r 130
          :g 130
          :b 130
          :a 255}
   :darkgray {:r 80
              :g 80
              :b 80
              :a 255}
   :red {:r 230
         :g 41
         :b 55
         :a 255}
   :maroon {:r 190
            :g 33
            :b 55
            :a 255}
   :green {:r 0
           :g 228
           :b 48
           :a 255}})

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
   :projection rcc/CAMERA_PERSPECTIVE})

(defn initial-state []
  {:camera (make-camera)
   :cube-position {:x 0.0
                   :y 1.0
                   :z 0.0}
   :cube-size {:x 2.0
               :y 2.0
               :z 2.0}
   :ray {:position {:x 0.0
                    :y 0.0
                    :z 0.0}
         :direction {:x 0.0
                     :y 0.0
                     :z 0.0}}
   :collision {:hit 0
               :distance 0.0
               :point {:x 0.0
                       :y 0.0
                       :z 0.0}
               :normal {:x 0.0
                        :y 0.0
                        :z 0.0}}
   :cursor-hidden false})

(defn update-state [{:keys [camera cube-position cube-size ray collision cursor-hidden]
                     :as state}]
  (let [;; Toggle camera controls with right click
        toggle-cursor? (rcm/is-mouse-button-pressed? MOUSE_RIGHT)
        new-cursor-hidden (if toggle-cursor? (not cursor-hidden) cursor-hidden)

        ;; Update cursor visibility
        _ (when toggle-cursor?
            (if new-cursor-hidden
              (rcc/disable-cursor!)
              (rcc/enable-cursor!)))

        ;; Update camera if cursor is hidden (FPS mode)
        new-camera (if new-cursor-hidden
                     (rcc/update-camera! camera rcc/CAMERA_FIRST_PERSON)
                     camera)

        ;; Handle left click for picking
        left-click? (rcm/is-mouse-button-pressed? MOUSE_LEFT)

        ;; Check if we should do a new pick or clear selection
        [new-ray new-collision]
        (if left-click?
          (if (zero? (:hit collision))
            ;; Do raycast
            (let [mouse-pos (rcm/get-mouse-position)
                  new-ray (rcol/get-screen-to-world-ray mouse-pos new-camera)
                  bbox (rcol/make-bounding-box cube-position cube-size)
                  new-coll (rcol/get-ray-collision-box new-ray bbox)]
              [new-ray new-coll])
            ;; Clear selection
            [ray {:hit 0
                  :distance 0.0
                  :point {:x 0.0
                          :y 0.0
                          :z 0.0}
                  :normal {:x 0.0
                           :y 0.0
                           :z 0.0}}])
          [ray collision])]

    (assoc state
           :camera new-camera
           :ray new-ray
           :collision new-collision
           :cursor-hidden new-cursor-hidden)))

(defn draw-scene! [{:keys [camera cube-position cube-size ray collision]}]
  (let [{sx :x
         sy :y
         sz :z} cube-size
        hit? (not (zero? (:hit collision)))]

    (rcd/begin-drawing!)
    (rcd/clear-background! (:raywhite colors))

    (rcc/begin-mode-3d! camera)

    ;; Draw cube - red if selected, gray otherwise
    (if hit?
      (do
        (rcc/draw-cube! cube-position sx sy sz (:red colors))
        (rcc/draw-cube-wires! cube-position sx sy sz (:maroon colors))
        ;; Selection highlight
        (rcc/draw-cube-wires! cube-position (+ sx 0.2) (+ sy 0.2) (+ sz 0.2) (:green colors)))
      (do
        (rcc/draw-cube! cube-position sx sy sz (:gray colors))
        (rcc/draw-cube-wires! cube-position sx sy sz (:darkgray colors))))

    ;; Draw ray
    (rcol/draw-ray! ray (:maroon colors))

    ;; Draw grid
    (rcc/draw-grid! 10 1.0)

    (rcc/end-mode-3d!)

    ;; UI text
    (rtd/draw-text! "Try clicking on the box with your mouse!" 240 10 20 (:darkgray colors))

    (when hit?
      (let [text "BOX SELECTED"
            text-width (rtd/measure-text text 30)
            x (/ (- screen-width text-width) 2)
            y (int (* screen-height 0.1))]
        (rtd/draw-text! text x y 30 (:green colors))))

    (rtd/draw-text! "Right click mouse to toggle camera controls" 10 430 10 (:gray colors))

    (rtd/draw-fps! 10 10)

    (rcd/end-drawing!)))

(defn -main [& _args]
  (rcw/init-window! screen-width screen-height "raylib [core] example - 3d picking")
  (rct/set-target-fps! 60)

  (loop [state (initial-state)]
    (if (rcw/window-should-close?)
      (rcw/close-window!)
      (let [new-state (update-state state)]
        (draw-scene! new-state)
        (recur new-state)))))
