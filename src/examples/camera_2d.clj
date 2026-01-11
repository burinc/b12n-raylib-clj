(ns examples.camera-2d
  "raylib [core] example - 2d camera
   
   2D camera with player movement, zoom, and rotation.
   
   Difficulty: ⭐⭐☆☆ (2/4)
   Based on: core/core_2d_camera.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.core.camera2d :as rc2d]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.utils :as ru]
   [raylib.enums :as enums]
   [raylib.colors :as colors]))

(def screen-width 800)
(def screen-height 450)
(def max-buildings 100)

(defn generate-buildings
  "Generate random buildings with positions and colors"
  []
  (loop [i 0
         spacing 0
         buildings []]
    (if (>= i max-buildings)
      buildings
      (let [width (ru/get-random-value 50 200)
            height (ru/get-random-value 100 800)
            y (- screen-height 130.0 height)
            x (+ -6000.0 spacing)
            color {:r (ru/get-random-value 200 240)
                   :g (ru/get-random-value 200 240)
                   :b (ru/get-random-value 200 250)
                   :a 255}]
        (recur (inc i)
               (+ spacing width)
               (conj buildings {:rect {:x (float x) :y (float y)
                                       :width (float width) :height (float height)}
                                :color color}))))))

(defn clamp [v min-v max-v]
  (max min-v (min max-v v)))

(defn -main [& _args]
  (rcw/init-window! screen-width screen-height "raylib [core] example - 2d camera")
  (rct/set-target-fps! 60)

  (let [buildings (generate-buildings)
        initial-player {:x 400.0 :y 280.0 :width 40.0 :height 40.0}]

    (loop [player initial-player
           camera {:offset {:x (/ screen-width 2.0) :y (/ screen-height 2.0)}
                   :target {:x (+ (:x player) 20.0) :y (+ (:y player) 20.0)}
                   :rotation 0.0
                   :zoom 1.0}]
      (if (rcw/window-should-close?)
        (rcw/close-window!)
        (let [;; Player movement
              new-player-x (cond
                             (rck/is-key-down? (:right enums/keyboard-key)) (+ (:x player) 2)
                             (rck/is-key-down? (:left enums/keyboard-key)) (- (:x player) 2)
                             :else (:x player))
              new-player (assoc player :x new-player-x)

              ;; Camera follows player
              new-target {:x (+ (:x new-player) 20) :y (+ (:y new-player) 20)}

              ;; Camera rotation with A/S
              new-rotation (cond
                             (rck/is-key-down? (:a enums/keyboard-key)) (dec (:rotation camera))
                             (rck/is-key-down? (:s enums/keyboard-key)) (inc (:rotation camera))
                             :else (:rotation camera))
              clamped-rotation (clamp new-rotation -40 40)

              ;; Camera zoom with mouse wheel (logarithmic scaling)
              wheel-move (rcm/get-mouse-wheel-move)
              new-zoom (Math/exp (+ (Math/log (:zoom camera)) (* wheel-move 0.1)))
              clamped-zoom (clamp new-zoom 0.1 3.0)

              ;; Reset with R key
              [final-zoom final-rotation] (if (rck/is-key-pressed? (:r enums/keyboard-key))
                                            [1.0 0.0]
                                            [clamped-zoom clamped-rotation])

              new-camera {:offset (:offset camera)
                          :target new-target
                          :rotation (float final-rotation)
                          :zoom (float final-zoom)}]

          ;; Draw
          (rcd/begin-drawing!)
          (rcd/clear-background! colors/raywhite)

          ;; 2D camera mode
          (rc2d/begin-mode-2d! new-camera)

          ;; Draw ground
          (rsb/draw-rectangle! -6000 320 13000 8000 colors/darkgray)

          ;; Draw buildings
          (doseq [{:keys [rect color]} buildings]
            (rsb/draw-rectangle-rec! rect color))

          ;; Draw player
          (rsb/draw-rectangle-rec! {:x (:x new-player) :y (:y new-player)
                                    :width (:width new-player) :height (:height new-player)}
                                   colors/red)

          ;; Draw crosshair at camera target
          (rsb/draw-line! (int (:x new-target)) (* screen-height -10)
                          (int (:x new-target)) (* screen-height 10) colors/green)
          (rsb/draw-line! (* screen-width -10) (int (:y new-target))
                          (* screen-width 10) (int (:y new-target)) colors/green)

          (rc2d/end-mode-2d!)

          ;; Draw screen border
          (rtd/draw-text! "SCREEN AREA" 640 10 20 colors/red)
          (rsb/draw-rectangle! 0 0 screen-width 5 colors/red)
          (rsb/draw-rectangle! 0 5 5 (- screen-height 10) colors/red)
          (rsb/draw-rectangle! (- screen-width 5) 5 5 (- screen-height 10) colors/red)
          (rsb/draw-rectangle! 0 (- screen-height 5) screen-width 5 colors/red)

          ;; Draw info panel
          (rsb/draw-rectangle! 10 10 250 113 (ru/fade colors/skyblue 0.5))
          (rsb/draw-rectangle-lines! 10 10 250 113 colors/blue)

          (rtd/draw-text! "Free 2D camera controls:" 20 20 10 colors/black)
          (rtd/draw-text! "- Right/Left to move player" 40 40 10 colors/darkgray)
          (rtd/draw-text! "- Mouse Wheel to Zoom in-out" 40 60 10 colors/darkgray)
          (rtd/draw-text! "- A / S to Rotate" 40 80 10 colors/darkgray)
          (rtd/draw-text! "- R to reset Zoom and Rotation" 40 100 10 colors/darkgray)

          (rcd/end-drawing!)
          (recur new-player new-camera))))))
