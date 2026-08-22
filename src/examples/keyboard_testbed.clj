(ns examples.keyboard-testbed
  "raylib [core] example - keyboard testbed

   An on-screen ENG-US keyboard that lights up as you press keys, useful for
   checking what raylib actually reports for a given physical key. Hover a
   key to highlight it; every keycode and character raylib sees is logged to
   stdout.

   raylib's key constants describe an ENG-US layout. On any other physical
   layout the keycode still reflects the US position of that key, which is
   exactly what this testbed makes visible: press the key right of L on an
   AZERTY board and it lights up SEMICOLON.

   The C carries a 100-case switch mapping keycode to display label. Here
   the layout is a table of [key-name label width] rows and the labels live
   in it directly, so a row is the single place a key's code, caption and
   width are declared.

   Difficulty: 2/4
   Based on: core/core_keyboard_testbed.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.core.collision :as rcol]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.utils :as ru]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def key-spacing 4)
(def default-key-width 45)
(def offset-x 26)
(def offset-y 80)

;; Rows of [key-name label] or [key-name label width]. Width defaults to 45.
;; A nil key-name is a blank filler key - drawn as an empty outline, matching
;; the C's KEY_NULL slot on the bottom row.
(def layout
  [{:height 30
    :keys [[:escape "ESC"] [:f1 "F1"] [:f2 "F2"] [:f3 "F3"] [:f4 "F4"] [:f5 "F5"]
           [:f6 "F6"] [:f7 "F7"] [:f8 "F8"] [:f9 "F9"] [:f10 "F10"] [:f11 "F11"]
           [:f12 "F12"] [:print-screen "PRINTSCR" 62] [:pause "PAUSE"]]}
   {:height 38
    :keys [[:grave "`" 25] [:one "1"] [:two "2"] [:three "3"] [:four "4"] [:five "5"]
           [:six "6"] [:seven "7"] [:eight "8"] [:nine "9"] [:zero "0"] [:minus "-"]
           [:equal "="] [:backspace "BACK" 82] [:delete "DEL"]]}
   {:height 38
    :keys [[:tab "TAB" 50] [:q "Q"] [:w "W"] [:e "E"] [:r "R"] [:t "T"] [:y "Y"]
           [:u "U"] [:i "I"] [:o "O"] [:p "P"] [:left-bracket "["]
           [:right-bracket "]"] [:backslash "\\" 57] [:insert "INS"]]}
   {:height 38
    :keys [[:caps-lock "CAPS" 68] [:a "A"] [:s "S"] [:d "D"] [:f "F"] [:g "G"]
           [:h "H"] [:j "J"] [:k "K"] [:l "L"] [:semicolon ";"]
           [:apostrophe "'"] [:enter "ENTER" 88] [:page-up "PGUP"]]}
   {:height 38
    :keys [[:left-shift "LSHIFT" 80] [:z "Z"] [:x "X"] [:c "C"] [:v "V"] [:b "B"]
           [:n "N"] [:m "M"] [:comma ","] [:period "."] [:slash "/"]
           [:right-shift "RSHIFT" 76] [:up "UP"] [:page-down "PGDOWN"]]}
   {:height 38
    :keys [[:left-control "LCTRL" 80] [:left-super "WIN"] [:left-alt "LALT"]
           [:space "SPACE" 208] [:right-alt "ALTGR"] [:iso-backslash "\\"]
           [nil "" 45] [:right-control "RCTRL" 60] [:left "LEFT"]
           [:down "DOWN"] [:right "RIGHT"]]}])

;; The C's bottom row includes a bare 162 where a named constant would go.
;; That is the extra key ISO layouts place beside left-shift, which raylib
;; has no KEY_ constant for - hence the literal. Named here so the layout
;; table stays uniform.
(def iso-backslash 162)

(defn keycode
  "Resolve a layout key-name to a raylib keycode, or nil for a filler slot."
  [k]
  (when k (if (= k :iso-backslash) iso-backslash (get enums/keyboard-key k))))

(defn row-y
  "Top edge of row `i`. Row 0 is 30px tall and the rest are 38px, so this
   is not a flat multiple - it accumulates the heights above it plus the
   spacing between them."
  [i]
  (+ offset-y
     (reduce + (map :height (take i layout)))
     (* key-spacing i)))

(defn row-boxes
  "Lay one row out left to right, returning [{:rect :key :label}...].
   Pure - no raylib call in here, so the geometry is testable."
  [{:keys [height keys]} y]
  (first
   (reduce (fn [[acc x] [k label width]]
             (let [w (or width default-key-width)]
               [(conj acc {:rect {:x (float x) :y (float y)
                                  :width (float w) :height (float height)}
                           :key (keycode k) :label label})
                (+ x w key-spacing)]))
           [[] offset-x] keys)))

(defn all-boxes []
  (into [] (mapcat (fn [i row] (row-boxes row (row-y i)))
                   (range) layout)))

(defn- draw-key [{:keys [rect key label]} mouse]
  (if (nil? key)
    (rsb/draw-rectangle-lines-ex! rect 2.0 colors/lightgray)
    (let [color (if (rck/is-key-down? key) colors/maroon colors/darkgray)]
      (rsb/draw-rectangle-lines-ex! rect 2.0 color)
      (rtd/draw-text! label (+ (int (:x rect)) 4) (+ (int (:y rect)) 4) 10 color)))
  (when (pos? (rcol/check-collision-point-rec? mouse rect))
    (rsb/draw-rectangle-rec! rect (ru/fade colors/red 0.2))
    (rsb/draw-rectangle-lines-ex! rect 3.0 colors/red)))

(defn tick [state]
  (debug-stats/update!)
  ;; Drain both queues each frame. raylib buffers these, so a single read
  ;; would silently drop keys pressed in the same frame.
  (loop [] (let [k (rck/get-key-pressed)]
             (when (pos? k) (println (format "KEY PRESSED:  %d" k)) (recur))))
  (loop [] (let [c (rck/get-char-pressed)]
             (when (pos? c) (println (format "CHAR PRESSED: %c (%d)" (char c) c)) (recur))))
  state)

(defn draw [{:keys [boxes]}]
  (let [mouse (rcm/get-mouse-position)]
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/raywhite)
    (rtd/draw-text! "KEYBOARD LAYOUT: ENG-US" 26 38 20 colors/lightgray)
    (doseq [b boxes] (draw-key b mouse))
    (debug-stats/draw!)
    (rcd/end-drawing!)))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (rcw/init-window! screen-width screen-height "raylib [core] example - keyboard testbed")
  ;; ESC is part of the layout under test, so it must not also close the
  ;; window. Close via the title bar instead.
  (rck/set-exit-key! (:null enums/keyboard-key))
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  (loop [state {:boxes (all-boxes)}]
    (let [state (tick state)]
      (when-not (rcw/window-should-close?)
        (draw state)
        (recur state))))
  (rcw/close-window!))
