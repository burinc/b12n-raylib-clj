(ns examples.clock-of-clocks
  "raylib [shapes] example - clock of clocks

   A digital clock whose digits are drawn by an array of little analogue
   clocks. Each digit is a 4x6 grid of 24 faces, and the two hands of every
   face rotate to positions that together trace the digit's shape. When the
   time changes the hands sweep to their new angles over half a second.

   The lookup table is the whole design. Each cell holds a [big little]
   angle pair chosen from seven named positions - the four corners, a
   horizontal, a vertical, and one \"parked\" position for cells that are
   not part of the digit. Reading a digit's row of the table is close to
   seeing the digit.

   Difficulty: 3/4
   Based on: shapes/shapes_clock_of_clocks.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def face-size 24.0)
(def face-spacing 8.0)
(def section-spacing 16.0)
(def hands-move-duration 0.5)

;; The seven hand positions, as [big-hand little-hand] angle pairs.
(def ^:private TL [0.0 90.0])     ; top-left corner
(def ^:private TR [90.0 180.0])   ; top-right corner
(def ^:private BR [180.0 270.0])  ; bottom-right corner
(def ^:private BL [0.0 270.0])    ; bottom-left corner
(def ^:private HH [0.0 180.0])    ; horizontal line
(def ^:private VV [90.0 270.0])   ; vertical line
(def ^:private ZZ [135.0 135.0])  ; parked - not part of the digit

(def digit-angles
  "One 24-cell row per digit, laid out 4 wide so the shape reads in source."
  [[TL HH HH TR  VV TL TR VV  VV VV VV VV  VV VV VV VV  VV BL BR VV  BL HH HH BR]  ; 0
   [TL HH TR ZZ  BL TR VV ZZ  ZZ VV VV ZZ  ZZ VV VV ZZ  TL BR BL TR  BL HH HH BR]  ; 1
   [TL HH HH TR  BL HH TR VV  TL HH BR VV  VV TL HH BR  VV BL HH TR  BL HH HH BR]  ; 2
   [TL HH HH TR  BL HH TR VV  TL HH BR VV  BL HH TR VV  TL HH BR VV  BL HH HH BR]  ; 3
   [TL TR TL TR  VV VV VV VV  VV BL BR VV  BL HH TR VV  ZZ ZZ VV VV  ZZ ZZ BL BR]  ; 4
   [TL HH HH TR  VV TL HH BR  VV BL HH TR  BL HH TR VV  TL HH BR VV  BL HH HH BR]  ; 5
   [TL HH HH TR  VV TL HH BR  VV BL HH TR  VV TL TR VV  VV BL BR VV  BL HH HH BR]  ; 6
   [TL HH HH TR  BL HH TR VV  ZZ ZZ VV VV  ZZ ZZ VV VV  ZZ ZZ VV VV  ZZ ZZ BL BR]  ; 7
   [TL HH HH TR  VV TL TR VV  VV BL BR VV  VV TL TR VV  VV BL BR VV  BL HH HH BR]  ; 8
   [TL HH HH TR  VV TL TR VV  VV BL BR VV  BL HH TR VV  TL HH BR VV  BL HH HH BR]])  ; 9

(defn- now-digits
  "Current time as six digit values, honouring 12- or 24-hour mode."
  [hour-mode]
  (let [c (java.util.Calendar/getInstance)
        h (mod (.get c java.util.Calendar/HOUR_OF_DAY) hour-mode)]
    (mapv #(Character/digit ^char % 10)
          (format "%02d%02d%02d" h
                  (.get c java.util.Calendar/MINUTE)
                  (.get c java.util.Calendar/SECOND)))))

(defn- lerp [a b t] (+ a (* (- b a) t)))

(defn- smoothstep [t] (* t t (- 3.0 (* 2.0 t))))

(defn- target-angles
  "Destination angle pairs for the six digits. In 12-hour mode a leading
   zero is parked rather than drawn, so 09:xx reads as ' 9'."
  [digits hour-mode]
  (vec (for [[i d] (map-indexed vector digits)]
         (if (and (zero? i) (= hour-mode 12) (zero? d))
           (vec (repeat 24 ZZ))
           (digit-angles d)))))

(defn- unwind
  "Hands only ever sweep forwards. If a source angle is past its
   destination, push it back a full turn so the lerp goes the long way
   round instead of snapping backwards."
  [src dst]
  (mapv (fn [s d] (if (> s d) (- s 360.0) s)) src dst))

(defn initial-state []
  (let [parked (vec (repeat 6 (vec (repeat 24 ZZ))))]
    {:hour-mode 24 :prev-second -1 :timer 0.0
     :current parked :src parked :dst parked}))

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/set-config-flags! (:msaa-4x-hint enums/config-flag))
  (rcw/init-window! screen-width screen-height
                    "raylib [shapes] example - clock of clocks")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [hour-mode prev-second timer current] :as state}]
  (debug-stats/update!)
  (let [hour-mode (if (rck/is-key-pressed? (:space enums/keyboard-key))
                    (- 36 hour-mode) hour-mode)
        sec (.get (java.util.Calendar/getInstance) java.util.Calendar/SECOND)
        changed? (not= sec prev-second)
        dst (if changed? (target-angles (now-digits hour-mode) hour-mode) (:dst state))
        src (if changed?
              (mapv (fn [c d] (mapv (fn [cp dp] (unwind cp dp)) c d)) current dst)
              (:src state))
        timer (if changed?
                (- (rct/get-frame-time))
                (min hands-move-duration (+ timer (rct/get-frame-time))))
        t (smoothstep (max 0.0 (min 1.0 (/ (max 0.0 timer) hands-move-duration))))]
    (assoc state
           :hour-mode hour-mode
           :prev-second sec
           :timer timer
           :src src :dst dst
           :current (mapv (fn [sd dd]
                            (mapv (fn [sp dp] [(lerp (first sp) (first dp) t)
                                               (lerp (second sp) (second dp) t)])
                                  sd dd))
                          src dst))))

(defn- draw-face [cx cy [big little] hands-color]
  (rsb/draw-ring! {:x (float cx) :y (float cy)}
                  (float (- (* face-size 0.5) 2.0)) (float (* face-size 0.5))
                  (float 0.0) (float 360.0) 24 colors/darkgray)
  (rsb/draw-rectangle-pro! {:x (float cx) :y (float cy)
                            :width (float (+ (* face-size 0.5) 4.0)) :height (float 4.0)}
                           {:x 2.0 :y 2.0} (float big) hands-color)
  (rsb/draw-rectangle-pro! {:x (float cx) :y (float cy)
                            :width (float (+ (* face-size 0.5) 2.0)) :height (float 4.0)}
                           {:x 2.0 :y 2.0} (float little) hands-color))

(defn draw [{:keys [current hour-mode]}]
  (let [bg (ru/color-lerp colors/darkblue colors/black (float 0.75))
        hands (ru/color-lerp colors/yellow colors/raywhite (float 0.25))
        step (+ face-size face-spacing)]
    (rcd/begin-drawing!)
    (rcd/clear-background! bg)
    (rtd/draw-text! (format "%d-h mode, space to change" hour-mode) 10 30 20 colors/raywhite)
    (loop [digit 0 x-offset 4.0]
      (when (< digit 6)
        (doseq [row (range 6) col (range 4)]
          (draw-face (+ x-offset (* col step) (* face-size 0.5))
                     (+ 100 (* row step) (* face-size 0.5))
                     (get-in current [digit (+ (* row 4) col)])
                     hands))
        (let [x (+ x-offset (* step 4))]
          ;; A colon after every second digit, drawn as two small rings.
          (if (odd? digit)
            (do (rsb/draw-ring! {:x (float (+ x 4.0)) :y (float 160.0)}
                                (float 6.0) (float 8.0) (float 0.0) (float 360.0) 24 hands)
                (rsb/draw-ring! {:x (float (+ x 4.0)) :y (float 225.0)}
                                (float 6.0) (float 8.0) (float 0.0) (float 360.0) 24 hands)
                (recur (inc digit) (+ x section-spacing)))
            (recur (inc digit) x)))))
    (rtd/draw-fps! 10 10)
    (debug-stats/draw!)
    (rcd/end-drawing!)))

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
