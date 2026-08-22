(ns examples.input-actions
  "raylib [core] example - input actions

   An indirection layer between physical inputs and game actions. Instead of
   asking \"is LEFT held\", the game asks \"is the LEFT action active\", and a
   keyset decides which key and which gamepad button that means. TAB swaps
   between two keysets to show the same game reading different hardware.

   The C keeps a `MAX_ACTION`-sized array of {key, button} structs plus a
   file-scope `gamepadIndex`, and mutates the array in place from two setter
   functions. Here a keyset is just a map from action to {:key :button}, and
   swapping keysets is picking the other map - so both are visible at once
   rather than being two functions that overwrite shared state.

   One thing to be careful of, because it fails silently: the keyboard
   predicates return real booleans, while the gamepad ones return a byte.
   In Clojure 0 is truthy, so using a gamepad result directly would report
   every button as permanently held. They go through `pos?` below.

   Difficulty: 2/4
   Based on: core/core_input_actions.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.gamepad :as rcg]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def gamepad-index 0)

(def keysets
  "Two mappings from action to physical input. The default set uses WASD and
   the gamepad's left face buttons; the alternate uses the arrow keys and the
   right face buttons. FIRE is SPACE in both."
  {:default {:label "Current input set: WASD (default)"
             :actions {:up    {:key :w :button rcg/GAMEPAD_BUTTON_LEFT_FACE_UP}
                       :down  {:key :s :button rcg/GAMEPAD_BUTTON_LEFT_FACE_DOWN}
                       :left  {:key :a :button rcg/GAMEPAD_BUTTON_LEFT_FACE_LEFT}
                       :right {:key :d :button rcg/GAMEPAD_BUTTON_LEFT_FACE_RIGHT}
                       :fire  {:key :space :button rcg/GAMEPAD_BUTTON_RIGHT_FACE_DOWN}}}
   :cursor  {:label "Current input set: Arrow keys"
             :actions {:up    {:key :up :button rcg/GAMEPAD_BUTTON_RIGHT_FACE_UP}
                       :down  {:key :down :button rcg/GAMEPAD_BUTTON_RIGHT_FACE_DOWN}
                       :left  {:key :left :button rcg/GAMEPAD_BUTTON_RIGHT_FACE_LEFT}
                       :right {:key :right :button rcg/GAMEPAD_BUTTON_RIGHT_FACE_RIGHT}
                       :fire  {:key :space :button rcg/GAMEPAD_BUTTON_RIGHT_FACE_DOWN}}}})

(defn- action-input [keyset action]
  (get-in keysets [keyset :actions action]))

(defn- check
  "True when either the bound key or the bound gamepad button satisfies the
   pair of predicates. `gamepad-pred` returns a byte, hence the `pos?`."
  [keyset action key-pred gamepad-pred]
  (when-let [{:keys [key button]} (action-input keyset action)]
    (or (key-pred (get enums/keyboard-key key))
        (pos? (gamepad-pred gamepad-index button)))))

(defn action-down? [keyset action]
  (check keyset action rck/is-key-down? rcg/is-gamepad-button-down?))

(defn action-pressed? [keyset action]
  (check keyset action rck/is-key-pressed? rcg/is-gamepad-button-pressed?))

(defn action-released? [keyset action]
  (check keyset action rck/is-key-released? rcg/is-gamepad-button-released?))

(def size {:x 40.0 :y 40.0})

(defn centred-position []
  {:x (/ (- screen-width (:x size)) 2.0)
   :y (/ (- screen-height (:y size)) 2.0)})

(defn initial-state []
  {:keyset :default
   :position {:x 400.0 :y 200.0}
   :released? false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [core] example - input actions")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [keyset position] :as state}]
  (debug-stats/update!)
  (let [move (fn [pos action dx dy]
               (if (action-down? keyset action)
                 (-> pos (update :x + dx) (update :y + dy))
                 pos))
        position (-> position
                     (move :up 0 -2) (move :down 0 2)
                     (move :left -2 0) (move :right 2 0))]
    (assoc state
           :position (if (action-pressed? keyset :fire) (centred-position) position)
           ;; The C sets this every frame and reads it in the same frame, so
           ;; the rectangle is blue for exactly the frame FIRE comes up.
           :released? (boolean (action-released? keyset :fire))
           :keyset (if (rck/is-key-pressed? (:tab enums/keyboard-key))
                     (if (= keyset :default) :cursor :default)
                     keyset))))

(defn draw [{:keys [position released? keyset]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/gray)
  (rsb/draw-rectangle-v! position size (if released? colors/blue colors/red))
  (rtd/draw-text! (get-in keysets [keyset :label]) 10 10 20 colors/white)
  (rtd/draw-text! "Use TAB key to toggles Actions keyset" 10 50 20 colors/green)
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
