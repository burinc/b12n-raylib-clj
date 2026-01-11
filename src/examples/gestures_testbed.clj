(ns examples.gestures-testbed
  "Raylib [core] example - input gestures testbed
   
   Visualizes touch gestures with an interactive testbed.
   Shows gesture log, protractor angle display, and touch point visualization.
   Based on: raylib/examples/core/core_input_gestures_testbed.c
   
   Complexity: ⭐⭐⭐ Intermediate
   
   Note: Optimized for touch screens. On desktop, use mouse to simulate gestures.
   In web browsers, enable Touch Emulation in developer tools.
   
   Controls:
   - Touch/Mouse: Perform gestures
   - Click 'Hide Repeat' button: Toggle repeated gesture logging
   - Click 'Hide Hold' button: Toggle hold gesture logging
   - F1: Toggle debug stats
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.core.gestures :as rcg]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def GESTURE_LOG_SIZE 20)
(def PI 3.14159265358979323846)

;; Gesture helpers
(defn gesture-name [gesture]
  (case gesture
    0 "None"
    1 "Tap"
    2 "Double Tap"
    4 "Hold"
    8 "Drag"
    16 "Swipe Right"
    32 "Swipe Left"
    64 "Swipe Up"
    128 "Swipe Down"
    256 "Pinch In"
    512 "Pinch Out"
    "Unknown"))

(defn gesture-color [gesture]
  (case gesture
    0 colors/black
    1 colors/blue
    2 colors/skyblue
    4 colors/black
    8 colors/lime
    16 colors/red
    32 colors/red
    64 colors/red
    128 colors/red
    256 colors/violet
    512 colors/orange
    colors/black))

(defn initial-state []
  {:exit? false
   :last-gesture 0
   :gesture-log (vec (repeat GESTURE_LOG_SIZE ""))
   :gesture-log-index GESTURE_LOG_SIZE
   :previous-gesture 0
   :log-mode 1 ; 0=show all, 1=hide repeat, 2=hide hold, 3=hide both
   :gesture-color colors/black
   :current-angle 0.0})

(def game-atom (atom (initial-state)))

;; UI positions
(def log-button1 {:x 53
                  :y 7
                  :width 48
                  :height 26})
(def log-button2 {:x 108
                  :y 7
                  :width 36
                  :height 26})
(def protractor-pos {:x 266.0
                     :y 315.0})
(def last-gesture-pos {:x 165
                       :y 130})
(def gesture-log-pos {:x 10
                      :y 10})

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [core] example - input gestures testbed")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn should-fill-log? [current-gesture previous-gesture log-mode]
  (when (not= current-gesture 0)
    (case log-mode
      3 (or (and (not= current-gesture 4) (not= current-gesture previous-gesture))
            (< current-gesture 3))
      2 (not= current-gesture 4)
      1 (not= current-gesture previous-gesture)
      0 true
      false)))

(defn update-gesture-log [{:keys [gesture-log gesture-log-index previous-gesture log-mode]
                           :as game}]
  (let [current-gesture (rcg/get-gesture-detected)]
    (if (should-fill-log? current-gesture previous-gesture log-mode)
      (let [new-index (if (<= gesture-log-index 0) (dec GESTURE_LOG_SIZE) (dec gesture-log-index))
            new-log (assoc gesture-log new-index (gesture-name current-gesture))]
        (assoc game
               :previous-gesture current-gesture
               :gesture-color (gesture-color current-gesture)
               :gesture-log new-log
               :gesture-log-index new-index))
      game)))

(defn update-last-gesture [{:keys [previous-gesture]
                            :as game}]
  (let [current-gesture (rcg/get-gesture-detected)]
    (if (and (not= current-gesture 0)
             (not= current-gesture 4)
             (not= current-gesture previous-gesture))
      (assoc game :last-gesture current-gesture)
      game)))

(defn update-protractor [game]
  (let [current-gesture (rcg/get-gesture-detected)
        drag-angle (rcg/get-gesture-drag-angle)
        pinch-angle (rcg/get-gesture-pinch-angle)
        angle (cond
                (> current-gesture 255) pinch-angle ; Pinch gestures
                (> current-gesture 15) drag-angle ; Swipe gestures
                (> current-gesture 0) 0.0 ; Other gestures
                :else (:current-angle game))]
    (assoc game :current-angle angle)))

(defn handle-log-buttons [{:keys [log-mode]
                           :as game}]
  (if (rcm/is-mouse-button-released? 0)
    (let [mouse-pos (rcm/get-mouse-position)]
      (cond
        (pos? (ext/check-collision-point-rec? mouse-pos log-button1))
        (assoc game :log-mode (case log-mode 3 2, 2 3, 1 0, 1))

        (pos? (ext/check-collision-point-rec? mouse-pos log-button2))
        (assoc game :log-mode (case log-mode 3 1, 2 0, 1 3, 2))

        :else game))
    game))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      handle-input
      handle-log-buttons
      update-last-gesture
      update-gesture-log
      update-protractor))

(defn draw-last-gesture [{:keys [last-gesture gesture-color]}]
  (let [x (:x last-gesture-pos)
        y (:y last-gesture-pos)
        touch-count (rcg/get-touch-point-count)]
    ;; Title
    (rtd/draw-text! "Last gesture" (+ x 33) (- y 47) 20 colors/black)
    (rtd/draw-text! "Swipe         Tap       Pinch  Touch" (+ x 17) (- y 18) 10 colors/black)

    ;; Swipe indicators (D-pad style)
    (rsb/draw-rectangle! (+ x 20) y 20 20 (if (= last-gesture rcg/GESTURE_SWIPE_UP) colors/red colors/lightgray))
    (rsb/draw-rectangle! x (+ y 20) 20 20 (if (= last-gesture rcg/GESTURE_SWIPE_LEFT) colors/red colors/lightgray))
    (rsb/draw-rectangle! (+ x 40) (+ y 20) 20 20 (if (= last-gesture rcg/GESTURE_SWIPE_RIGHT) colors/red colors/lightgray))
    (rsb/draw-rectangle! (+ x 20) (+ y 40) 20 20 (if (= last-gesture rcg/GESTURE_SWIPE_DOWN) colors/red colors/lightgray))

    ;; Tap indicator
    (ext/draw-circle-int! (+ x 80) (+ y 16) 10 (if (= last-gesture rcg/GESTURE_TAP) colors/blue colors/lightgray))

    ;; Drag indicator (ring)
    (ext/draw-ring! {:x (+ x 103)
                     :y (+ y 16)} 6.0 11.0 0.0 360.0 0
                    (if (= last-gesture rcg/GESTURE_DRAG) colors/lime colors/lightgray))

    ;; Double tap indicator
    (ext/draw-circle-int! (+ x 80) (+ y 43) 10 (if (= last-gesture rcg/GESTURE_DOUBLETAP) colors/skyblue colors/lightgray))
    (ext/draw-circle-int! (+ x 103) (+ y 43) 10 (if (= last-gesture rcg/GESTURE_DOUBLETAP) colors/skyblue colors/lightgray))

    ;; Pinch out indicator (triangles pointing outward)
    (ext/draw-triangle! {:x (+ x 122)
                         :y (+ y 16)} {:x (+ x 137)
                                       :y (+ y 26)} {:x (+ x 137)
                                                     :y (+ y 6)}
                        (if (= last-gesture rcg/GESTURE_PINCH_OUT) colors/orange colors/lightgray))
    (ext/draw-triangle! {:x (+ x 147)
                         :y (+ y 6)} {:x (+ x 147)
                                      :y (+ y 26)} {:x (+ x 162)
                                                    :y (+ y 16)}
                        (if (= last-gesture rcg/GESTURE_PINCH_OUT) colors/orange colors/lightgray))

    ;; Pinch in indicator (triangles pointing inward)
    (ext/draw-triangle! {:x (+ x 125)
                         :y (+ y 33)} {:x (+ x 125)
                                       :y (+ y 53)} {:x (+ x 140)
                                                     :y (+ y 43)}
                        (if (= last-gesture rcg/GESTURE_PINCH_IN) colors/violet colors/lightgray))
    (ext/draw-triangle! {:x (+ x 144)
                         :y (+ y 43)} {:x (+ x 159)
                                       :y (+ y 53)} {:x (+ x 159)
                                                     :y (+ y 33)}
                        (if (= last-gesture rcg/GESTURE_PINCH_IN) colors/violet colors/lightgray))

    ;; Touch count indicators
    (doseq [i (range 4)]
      (ext/draw-circle-int! (+ x 180) (+ y 7 (* i 15)) 5
                            (if (<= touch-count i) colors/lightgray gesture-color)))))

(defn draw-gesture-log [{:keys [gesture-log gesture-log-index gesture-color log-mode]}]
  (let [x (:x gesture-log-pos)
        y (:y gesture-log-pos)]
    ;; Title
    (rtd/draw-text! "Log" x y 20 colors/black)

    ;; Log entries (inverted circular queue display)
    (doseq [i (range GESTURE_LOG_SIZE)]
      (let [ii (mod (+ gesture-log-index i) GESTURE_LOG_SIZE)
            entry (get gesture-log ii "")]
        (rtd/draw-text! entry x (- (+ y 410) (* i 20)) 20
                        (if (= i 0) gesture-color colors/lightgray))))

    ;; Log mode buttons
    (let [[btn1-color btn2-color] (case log-mode
                                    3 [colors/maroon colors/maroon]
                                    2 [colors/gray colors/maroon]
                                    1 [colors/maroon colors/gray]
                                    [colors/gray colors/gray])]
      (ext/draw-rectangle-rec! log-button1 btn1-color)
      (rtd/draw-text! "Hide" (+ (:x log-button1) 7) (+ (:y log-button1) 3) 10 colors/white)
      (rtd/draw-text! "Repeat" (+ (:x log-button1) 7) (+ (:y log-button1) 13) 10 colors/white)

      (ext/draw-rectangle-rec! log-button2 btn2-color)
      (rtd/draw-text! "Hide" (+ (:x log-button1) 62) (+ (:y log-button1) 3) 10 colors/white)
      (rtd/draw-text! "Hold" (+ (:x log-button1) 62) (+ (:y log-button1) 13) 10 colors/white))))

(defn draw-protractor [{:keys [current-angle gesture-color]}]
  (let [x (:x protractor-pos)
        y (:y protractor-pos)
        angle-length 90.0
        angle-radians (* (+ current-angle 90.0) (/ PI 180.0))
        final-x (+ (* angle-length (Math/sin angle-radians)) x)
        final-y (+ (* angle-length (Math/cos angle-radians)) y)]
    ;; Angle text
    (rtd/draw-text! "Angle" (int (+ x 55)) (int (+ y 76)) 10 colors/black)
    (rtd/draw-text! (format "%.2f" current-angle) (int (+ x 55)) (int (+ y 92)) 20 gesture-color)

    ;; Protractor circle
    (ext/draw-circle-v! protractor-pos 80.0 colors/white)

    ;; Cross lines
    (ext/draw-line-ex! {:x (- x 90)
                        :y y} {:x (+ x 90)
                               :y y} 3.0 colors/lightgray)
    (ext/draw-line-ex! {:x x
                        :y (- y 90)} {:x x
                                      :y (+ y 90)} 3.0 colors/lightgray)

    ;; Diagonal lines (30/150 degree markers)
    (ext/draw-line-ex! {:x (- x 80)
                        :y (- y 45)} {:x (+ x 80)
                                      :y (+ y 45)} 3.0 colors/green)
    (ext/draw-line-ex! {:x (- x 80)
                        :y (+ y 45)} {:x (+ x 80)
                                      :y (- y 45)} 3.0 colors/green)

    ;; Angle labels
    (rtd/draw-text! "0" (int (+ x 96)) (int (- y 9)) 20 colors/black)
    (rtd/draw-text! "30" (int (+ x 74)) (int (- y 68)) 20 colors/black)
    (rtd/draw-text! "90" (int (- x 11)) (int (- y 110)) 20 colors/black)
    (rtd/draw-text! "150" (int (- x 100)) (int (- y 68)) 20 colors/black)
    (rtd/draw-text! "180" (int (- x 124)) (int (- y 9)) 20 colors/black)
    (rtd/draw-text! "210" (int (- x 100)) (int (+ y 50)) 20 colors/black)
    (rtd/draw-text! "270" (int (- x 18)) (int (+ y 92)) 20 colors/black)
    (rtd/draw-text! "330" (int (+ x 72)) (int (+ y 50)) 20 colors/black)

    ;; Current angle line
    (when (not= current-angle 0.0)
      (ext/draw-line-ex! protractor-pos {:x final-x
                                         :y final-y} 3.0 gesture-color))))

(defn draw-touch-points [{:keys [gesture-color]}]
  (let [current-gesture (rcg/get-gesture-detected)
        touch-count (rcg/get-touch-point-count)]
    (when (not= current-gesture rcg/GESTURE_NONE)
      (if (pos? touch-count)
        ;; Draw touch points
        (do
          (doseq [i (range touch-count)]
            (let [pos (rcg/get-touch-position i)]
              (ext/draw-circle-v! pos 50.0 (ext/fade gesture-color 0.5))
              (ext/draw-circle-v! pos 5.0 gesture-color)))
          ;; Draw line between two touch points (for pinch gestures)
          (when (= touch-count 2)
            (let [pos0 (rcg/get-touch-position 0)
                  pos1 (rcg/get-touch-position 1)
                  thickness (if (= current-gesture 512) 8.0 12.0)]
              (ext/draw-line-ex! pos0 pos1 thickness gesture-color))))
        ;; Draw mouse position
        (let [mouse-pos (rcm/get-mouse-position)]
          (ext/draw-circle-v! mouse-pos 35.0 (ext/fade gesture-color 0.5))
          (ext/draw-circle-v! mouse-pos 5.0 gesture-color))))))

(defn draw [game]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; Info messages
  (rtd/draw-text! "*" 165 12 10 colors/black)
  (rtd/draw-text! "Example optimized for Web/HTML5\non Smartphones with Touch Screen." 175 12 10 colors/black)
  (rtd/draw-text! "*" 165 42 10 colors/black)
  (rtd/draw-text! "While running on Desktop Web Browsers,\ninspect and turn on Touch Emulation." 175 42 10 colors/black)

  ;; Draw components
  (draw-last-gesture game)
  (draw-gesture-log game)
  (draw-protractor game)
  (draw-touch-points game)

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

  ;; Check gesture state
  (select-keys @game-atom [:last-gesture :current-angle :log-mode])

  ;; Toggle log mode
  (swap! game-atom assoc :log-mode 0) ; Show all
  (swap! game-atom assoc :log-mode 1) ; Hide repeats
  ;;
  )
