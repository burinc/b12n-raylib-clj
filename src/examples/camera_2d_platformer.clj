(ns examples.camera-2d-platformer
  "raylib [core] example - 2D camera platformer

   A simple platformer with 5 different camera follow modes.
   Press C to cycle through camera modes, R to reset,
   mouse wheel to zoom.

   Difficulty: 3/4
   Based on: core/core_2d_camera_platformer.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.core.camera2d :as rc2d]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def GRAVITY 400)
(def PLAYER-JUMP-SPD 350.0)
(def PLAYER-HOR-SPD 200.0)

(def env-items
  [{:rect {:x 0.0 :y 0.0 :width 1000.0 :height 400.0} :blocking false :color colors/lightgray}
   {:rect {:x 0.0 :y 400.0 :width 1000.0 :height 200.0} :blocking true :color colors/gray}
   {:rect {:x 300.0 :y 200.0 :width 400.0 :height 10.0} :blocking true :color colors/gray}
   {:rect {:x 250.0 :y 300.0 :width 100.0 :height 10.0} :blocking true :color colors/gray}
   {:rect {:x 650.0 :y 300.0 :width 100.0 :height 10.0} :blocking true :color colors/gray}])

(def camera-descriptions
  ["Follow player center"
   "Follow player center, but clamp to map edges"
   "Follow player center; smoothed"
   "Follow player center horizontally; update player center vertically after landing"
   "Player push camera on getting too close to screen edge"])

(defn initial-state []
  {:player-x 400.0
   :player-y 280.0
   :player-speed 0.0
   :can-jump false
   :camera {:offset {:x (/ screen-width 2.0) :y (/ screen-height 2.0)}
            :target {:x 400.0 :y 280.0}
            :rotation (float 0.0)
            :zoom (float 1.0)}
   :camera-option 0
   ;; State for even-out-on-landing camera
   :evening-out false
   :even-out-target 0.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [core] example - 2d camera platformer")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn update-player [{:keys [player-x player-y player-speed can-jump] :as state} delta]
  (let [player-x (cond-> player-x
                    (rck/is-key-down? (:left enums/keyboard-key))
                    (- (* PLAYER-HOR-SPD delta))
                    (rck/is-key-down? (:right enums/keyboard-key))
                    (+ (* PLAYER-HOR-SPD delta)))
        [player-speed can-jump]
        (if (and (rck/is-key-down? (:space enums/keyboard-key)) can-jump)
          [(- PLAYER-JUMP-SPD) false]
          [player-speed can-jump])
        ;; Check for obstacle collision
        hit-result
        (reduce
         (fn [_ {:keys [rect blocking]}]
           (when (and blocking
                      (<= (:x rect) player-x)
                      (>= (+ (:x rect) (:width rect)) player-x)
                      (>= (:y rect) player-y)
                      (<= (:y rect) (+ player-y (* player-speed delta))))
             (reduced {:hit true :y (:y rect)})))
         nil
         env-items)]
    (if (:hit hit-result)
      (assoc state
             :player-x player-x
             :player-y (:y hit-result)
             :player-speed 0.0
             :can-jump true)
      (assoc state
             :player-x player-x
             :player-y (+ player-y (* player-speed delta))
             :player-speed (+ player-speed (* GRAVITY delta))
             :can-jump false))))

(defn camera-center [{:keys [player-x player-y] :as state}]
  (assoc-in
   (assoc-in state [:camera :offset] {:x (/ screen-width 2.0) :y (/ screen-height 2.0)})
   [:camera :target] {:x player-x :y player-y}))

(defn camera-center-inside-map [{:keys [player-x player-y camera] :as state}]
  (let [camera (assoc camera
                      :target {:x player-x :y player-y}
                      :offset {:x (/ screen-width 2.0) :y (/ screen-height 2.0)})
        ;; Find map bounds
        {:keys [min-x min-y max-x max-y]}
        (reduce (fn [acc {:keys [rect]}]
                  {:min-x (min (:min-x acc) (:x rect))
                   :max-x (max (:max-x acc) (+ (:x rect) (:width rect)))
                   :min-y (min (:min-y acc) (:y rect))
                   :max-y (max (:max-y acc) (+ (:y rect) (:height rect)))})
                {:min-x 1000.0 :max-x -1000.0 :min-y 1000.0 :max-y -1000.0}
                env-items)
        max-screen (rc2d/get-world-to-screen-2d {:x max-x :y max-y} camera)
        min-screen (rc2d/get-world-to-screen-2d {:x min-x :y min-y} camera)
        offset-x (:x (:offset camera))
        offset-y (:y (:offset camera))
        offset-x (cond-> offset-x
                    (< (:x max-screen) screen-width)
                    (- (- (:x max-screen) screen-width) (- (/ screen-width 2.0)))
                    (> (:x min-screen) 0)
                    (- (:x min-screen) (/ screen-width 2.0)))
        offset-y (cond-> offset-y
                    (< (:y max-screen) screen-height)
                    (- (- (:y max-screen) screen-height) (- (/ screen-height 2.0)))
                    (> (:y min-screen) 0)
                    (- (:y min-screen) (/ screen-height 2.0)))]
    ;; Recompute with corrected offsets
    (let [cam2 (assoc camera :offset {:x offset-x :y offset-y})
          max-screen2 (rc2d/get-world-to-screen-2d {:x max-x :y max-y} cam2)
          min-screen2 (rc2d/get-world-to-screen-2d {:x min-x :y min-y} cam2)
          ox (:x (:offset cam2))
          oy (:y (:offset cam2))
          ox (cond
               (< (:x max-screen2) screen-width)
               (+ screen-width (- (/ screen-width 2.0) (:x max-screen2)))
               (> (:x min-screen2) 0)
               (- (/ screen-width 2.0) (:x min-screen2))
               :else ox)
          oy (cond
               (< (:y max-screen2) screen-height)
               (+ screen-height (- (/ screen-height 2.0) (:y max-screen2)))
               (> (:y min-screen2) 0)
               (- (/ screen-height 2.0) (:y min-screen2))
               :else oy)]
      (assoc state :camera (assoc cam2 :offset {:x ox :y oy})))))

(defn camera-smooth-follow [{:keys [player-x player-y camera] :as state} delta]
  (let [min-speed 30.0
        min-effect-length 10.0
        fraction-speed 0.8
        camera (assoc camera :offset {:x (/ screen-width 2.0) :y (/ screen-height 2.0)})
        target (:target camera)
        diff-x (- player-x (:x target))
        diff-y (- player-y (:y target))
        length (Math/sqrt (+ (* diff-x diff-x) (* diff-y diff-y)))]
    (if (> length min-effect-length)
      (let [speed (max (* fraction-speed length) min-speed)
            factor (/ (* speed delta) length)
            new-target {:x (+ (:x target) (* diff-x factor))
                        :y (+ (:y target) (* diff-y factor))}]
        (assoc state :camera (assoc camera :target new-target)))
      (assoc state :camera camera))))

(defn camera-even-out-on-landing [{:keys [player-x player-y player-speed can-jump
                                           evening-out even-out-target camera] :as state} delta]
  (let [even-out-speed 700.0
        camera (assoc camera :offset {:x (/ screen-width 2.0) :y (/ screen-height 2.0)})
        camera (assoc-in camera [:target :x] player-x)]
    (if evening-out
      (let [target-y (:y (:target camera))]
        (if (> even-out-target target-y)
          (let [new-y (+ target-y (* even-out-speed delta))]
            (if (> new-y even-out-target)
              (assoc state
                     :camera (assoc-in camera [:target :y] even-out-target)
                     :evening-out false)
              (assoc state :camera (assoc-in camera [:target :y] new-y))))
          (let [new-y (- target-y (* even-out-speed delta))]
            (if (< new-y even-out-target)
              (assoc state
                     :camera (assoc-in camera [:target :y] even-out-target)
                     :evening-out false)
              (assoc state :camera (assoc-in camera [:target :y] new-y))))))
      (if (and can-jump (zero? player-speed) (not= player-y (:y (:target camera))))
        (assoc state
               :camera camera
               :evening-out true
               :even-out-target player-y)
        (assoc state :camera camera)))))

(defn camera-player-bounds-push [{:keys [player-x player-y camera] :as state}]
  (let [bbox-x 0.2
        bbox-y 0.2
        bbox-world-min (rc2d/get-screen-to-world-2d
                        {:x (* (- 1.0 bbox-x) 0.5 screen-width)
                         :y (* (- 1.0 bbox-y) 0.5 screen-height)}
                        camera)
        bbox-world-max (rc2d/get-screen-to-world-2d
                        {:x (* (+ 1.0 bbox-x) 0.5 screen-width)
                         :y (* (+ 1.0 bbox-y) 0.5 screen-height)}
                        camera)
        camera (assoc camera :offset {:x (* (- 1.0 bbox-x) 0.5 screen-width)
                                       :y (* (- 1.0 bbox-y) 0.5 screen-height)})
        target-x (:x (:target camera))
        target-y (:y (:target camera))
        target-x (cond
                    (< player-x (:x bbox-world-min)) player-x
                    (> player-x (:x bbox-world-max))
                    (+ (:x bbox-world-min) (- player-x (:x bbox-world-max)))
                    :else target-x)
        target-y (cond
                    (< player-y (:y bbox-world-min)) player-y
                    (> player-y (:y bbox-world-max))
                    (+ (:y bbox-world-min) (- player-y (:y bbox-world-max)))
                    :else target-y)]
    (assoc state :camera (assoc camera :target {:x target-x :y target-y}))))

(defn update-camera [state delta]
  (case (:camera-option state)
    0 (camera-center state)
    1 (camera-center-inside-map state)
    2 (camera-smooth-follow state delta)
    3 (camera-even-out-on-landing state delta)
    4 (camera-player-bounds-push state)))

(defn tick [state]
  (debug-stats/update!)
  (let [delta (rct/get-frame-time)
        state (update-player state delta)
        ;; Zoom with mouse wheel
        wheel (rcm/get-mouse-wheel-move)
        zoom (-> (+ (get-in state [:camera :zoom]) (* wheel 0.05))
                 (max 0.25)
                 (min 3.0))
        state (assoc-in state [:camera :zoom] (float zoom))
        ;; Reset with R
        state (if (rck/is-key-pressed? (:r enums/keyboard-key))
                (assoc state
                       :player-x 400.0 :player-y 280.0
                       :player-speed 0.0 :can-jump false)
                state)
        state (assoc-in state [:camera :zoom]
                        (float (if (rck/is-key-pressed? (:r enums/keyboard-key))
                                 1.0
                                 (get-in state [:camera :zoom]))))
        ;; Cycle camera mode with C
        state (if (rck/is-key-pressed? (:c enums/keyboard-key))
                (update state :camera-option #(mod (inc %) (count camera-descriptions)))
                state)]
    (update-camera state delta)))

(defn draw [{:keys [player-x player-y camera camera-option]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/lightgray)

  (rc2d/begin-mode-2d! camera)

  ;; Draw environment
  (doseq [{:keys [rect color]} env-items]
    (rsb/draw-rectangle-rec! rect color))

  ;; Draw player
  (rsb/draw-rectangle-rec! {:x (float (- player-x 20))
                             :y (float (- player-y 40))
                             :width (float 40)
                             :height (float 40)}
                            colors/red)
  (rsb/draw-circle-v! {:x player-x :y player-y} (float 5) colors/gold)

  (rc2d/end-mode-2d!)

  ;; Draw UI
  (rtd/draw-text! "Controls:" 20 20 10 colors/black)
  (rtd/draw-text! "- Right/Left to move" 40 40 10 colors/darkgray)
  (rtd/draw-text! "- Space to jump" 40 60 10 colors/darkgray)
  (rtd/draw-text! "- Mouse Wheel to Zoom in-out" 40 80 10 colors/darkgray)
  (rtd/draw-text! "- R to reset position + zoom" 40 100 10 colors/darkgray)
  (rtd/draw-text! "- C to change camera mode" 40 120 10 colors/darkgray)
  (rtd/draw-text! "Current camera mode:" 20 140 10 colors/black)
  (rtd/draw-text! (nth camera-descriptions camera-option) 40 160 10 colors/darkgray)

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
  (rcw/close-window!))
