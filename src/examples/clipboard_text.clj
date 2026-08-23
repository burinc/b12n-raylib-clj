(ns examples.clipboard-text
  "raylib [core] example - clipboard text

   Cut, copy and paste against the system clipboard, by button or by
   CTRL+X / CTRL+C / CTRL+V. The lower box is a read-only mirror of what the
   clipboard currently holds, so pasting from another application shows up
   there too.

   First example here using `raygui`'s text box and its icon captions. A
   caption like `\"#17#CUT\"` carries a leading icon id; `raylib.raygui` ports
   the five icons these buttons need rather than raygui's full 200+ set.

   Two departures from the C, both consequences of not having a mutable
   `char *`. Its `inputBuffer` is a 256-byte array that the buttons write
   into directly and the text box edits in place; here the text is a value
   threaded through state, and the 256 limit is passed to the text box as a
   bound rather than being a property of the buffer. And `TextCopy` becomes
   ordinary string assignment.

   Difficulty: 2/4
   Based on: core/core_clipboard_text.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.raygui :as gui]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def max-text-length 256)

(def sample-texts
  ["Hello from raylib!"
   "The quick brown fox jumps over the lazy dog"
   "Clipboard operations are useful!"
   "raylib is a simple and easy-to-use library"
   "Copy and paste me!"])

(defn initial-state []
  {:input "Hello from raylib!" :clipboard nil :edit? false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [core] example - clipboard text")
  (gui/set-style! :default :text-size 20)
  (gui/set-icon-scale! 2)
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn- ctrl-down? []
  (or (rck/is-key-down? (:left-control enums/keyboard-key))
      (rck/is-key-down? (:right-control enums/keyboard-key))))

(defn cut [state]
  (rcw/set-clipboard-text! (:input state))
  (assoc state :input "" :clipboard (rcw/get-clipboard-text)))

(defn copy [state]
  (rcw/set-clipboard-text! (:input state))
  (assoc state :clipboard (rcw/get-clipboard-text)))

(defn paste [state]
  (let [c (rcw/get-clipboard-text)]
    (assoc state :clipboard c :input (or c (:input state)))))

(defn tick [state]
  (debug-stats/update!)
  ;; The shortcuts are read here rather than in the draw pass because they
  ;; are plain keyboard state; the buttons are read where they are drawn,
  ;; which is what immediate mode means.
  (if-not (ctrl-down?)
    state
    (cond
      (rck/is-key-pressed? (:x enums/keyboard-key)) (assoc (copy state) :input "")
      (rck/is-key-pressed? (:c enums/keyboard-key)) (copy state)
      (rck/is-key-pressed? (:v enums/keyboard-key)) (paste state)
      :else state)))

(defn draw [{:keys [input clipboard edit?] :as state}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (gui/label {:x 50.0 :y 20.0 :width 700.0 :height 36.0} "Use the BUTTONS or KEY SHORTCUTS:")
  (rtd/draw-text! "[CTRL+X] - CUT | [CTRL+C] COPY | [CTRL+V] | PASTE" 50 60 20 colors/maroon)

  (let [{:keys [text toggled?]}
        (gui/text-box {:x 50.0 :y 120.0 :width 652.0 :height 40.0} input max-text-length edit?)
        random? (gui/button {:x 710.0 :y 120.0 :width 40.0 :height 40.0} "#77#")
        cut? (gui/button {:x 50.0 :y 180.0 :width 158.0 :height 40.0} "#17#CUT")
        copy? (gui/button {:x 215.0 :y 180.0 :width 158.0 :height 40.0} "#16#COPY")
        paste? (gui/button {:x 380.0 :y 180.0 :width 158.0 :height 40.0} "#18#PASTE")
        clear? (gui/button {:x 545.0 :y 180.0 :width 158.0 :height 40.0} "#143#CLEAR")]

    ;; The clipboard readout is drawn disabled and read-only: it reports
    ;; state rather than offering an edit.
    (gui/set-state! gui/state-disabled)
    (gui/label {:x 50.0 :y 260.0 :width 700.0 :height 40.0} "Clipboard current text data:")
    (gui/set-style! :textbox :text-readonly 1)
    (gui/text-box {:x 50.0 :y 300.0 :width 700.0 :height 40.0} clipboard max-text-length false)
    (gui/set-style! :textbox :text-readonly 0)
    (gui/label {:x 50.0 :y 360.0 :width 700.0 :height 40.0}
               "Try copying text from other applications and pasting here!")
    (gui/enable!)

    (debug-stats/draw!)
    (rcd/end-drawing!)

    (let [state (assoc state :input text :edit? (if toggled? (not edit?) edit?))]
      (cond
        cut? (cut state)
        copy? (copy state)
        paste? (paste state)
        clear? (assoc state :input "")
        random? (assoc state :input (rand-nth sample-texts))
        :else state))))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (swap! game-atom (comp draw tick))
      (recur)))
  (rcw/close-window!))
