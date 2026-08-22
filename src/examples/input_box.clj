(ns examples.input-box
  "raylib [text] example - input box

   Type text into an input field. Click on the box to focus it,
   type characters, and use backspace to delete.

   Difficulty: 2/4
   Based on: text/text_input_box.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.core.collision :as rcol]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)
(def MAX-INPUT-CHARS 9)

;; Mouse cursor constants (from raylib.h)
(def MOUSE-CURSOR-DEFAULT 0)
(def MOUSE-CURSOR-IBEAM 2)

(defn initial-state []
  {:name ""
   :text-box {:x (float (- (/ screen-width 2) 100))
              :y (float 180)
              :width (float 225)
              :height (float 50)}
   :mouse-on-text false
   :frames 0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [text] example - input box")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn collect-chars
  "Collect all characters pressed this frame."
  []
  (loop [chars []
         key (rck/get-char-pressed)]
    (if (zero? key)
      chars
      (recur (if (<= 32 key 125) (conj chars (char key)) chars)
             (rck/get-char-pressed)))))

(defn tick [{:keys [name text-box] :as state}]
  (debug-stats/update!)
  (let [mouse (rcm/get-mouse-position)
        mouse-on-text (pos? (rcol/check-collision-point-rec? mouse text-box))]
    ;; Set cursor shape
    (if mouse-on-text
      (rcm/set-mouse-cursor! MOUSE-CURSOR-IBEAM)
      (rcm/set-mouse-cursor! MOUSE-CURSOR-DEFAULT))

    (let [;; Handle text input when mouse is over box
          name (if mouse-on-text
                 (let [chars (collect-chars)
                       name (reduce (fn [n c]
                                      (if (< (count n) MAX-INPUT-CHARS)
                                        (str n c)
                                        n))
                                    name chars)]
                   ;; Handle backspace
                   (if (and (rck/is-key-pressed? (:backspace enums/keyboard-key))
                            (pos? (count name)))
                     (subs name 0 (dec (count name)))
                     name))
                 name)
          frames (if mouse-on-text
                   (inc (:frames state))
                   0)]
      (assoc state
             :name name
             :mouse-on-text mouse-on-text
             :frames frames))))

(defn draw [{:keys [name text-box mouse-on-text frames]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rtd/draw-text! "PLACE MOUSE OVER INPUT BOX!" 240 140 20 colors/gray)

  (rsb/draw-rectangle-rec! text-box colors/lightgray)
  (if mouse-on-text
    (rsb/draw-rectangle-lines! (int (:x text-box)) (int (:y text-box))
                               (int (:width text-box)) (int (:height text-box)) colors/red)
    (rsb/draw-rectangle-lines! (int (:x text-box)) (int (:y text-box))
                               (int (:width text-box)) (int (:height text-box)) colors/darkgray))

  (rtd/draw-text! name (+ (int (:x text-box)) 5) (+ (int (:y text-box)) 8) 40 colors/maroon)

  (rtd/draw-text! (format "INPUT CHARS: %d/%d" (count name) MAX-INPUT-CHARS)
                  315 250 20 colors/darkgray)

  (when mouse-on-text
    (if (< (count name) MAX-INPUT-CHARS)
      ;; Draw blinking underscore
      (when (zero? (mod (quot frames 20) 2))
        (rtd/draw-text! "_"
                        (+ (int (:x text-box)) 8 (rtd/measure-text name 40))
                        (+ (int (:y text-box)) 12)
                        40 colors/maroon))
      (rtd/draw-text! "Press BACKSPACE to delete chars..." 230 300 20 colors/gray)))

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
