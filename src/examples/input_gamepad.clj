(ns examples.input-gamepad
  "Raylib [core] example - input gamepad
   
   Displays gamepad input state with visual feedback.
   Shows Xbox and PlayStation controller layouts with button/axis visualization.
   Based on: raylib/examples/core/core_input_gamepad.c
   
   Complexity: ⭐⭐ Easy
   
   Requirements:
   - Gamepad connected to the system (Xbox, PlayStation, or generic)
   
   Controls:
   - LEFT/RIGHT arrows: Switch between gamepads
   - F1: Toggle debug stats
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.gamepad :as rcg]
   [raylib.text.drawing :as rtd]
   [raylib.textures.texture-loading :as rtl]
   [raylib.textures.drawing :as rtd-tex]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [clojure.string :as str]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)

;; Deadzone thresholds
(def STICK_DEADZONE 0.1)
(def TRIGGER_DEADZONE -0.9)

(defn initial-state []
  {:exit? false
   :gamepad 0
   :tex-ps3 nil
   :tex-xbox nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [core] example - input gamepad")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  ;; Load controller textures
  (let [tex-ps3 (rtl/load-texture! "resources/ps3.png")
        tex-xbox (rtl/load-texture! "resources/xbox.png")]
    (swap! game-atom assoc
           :tex-ps3 tex-ps3
           :tex-xbox tex-xbox)))

(defn apply-deadzone [value deadzone]
  (if (and (> value (- deadzone)) (< value deadzone))
    0.0
    value))

(defn apply-trigger-deadzone [value deadzone]
  (if (< value deadzone) -1.0 value))

(defn handle-input [{:keys [gamepad]
                     :as game}]
  (cond-> game
    (rck/is-key-pressed? (:left enums/keyboard-key))
    (update :gamepad #(max 0 (dec %)))

    (rck/is-key-pressed? (:right enums/keyboard-key))
    (update :gamepad inc)

    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (debug-stats/update!)
  (handle-input game))

(defn xbox-controller? [name]
  (when name
    (let [lower-name (str/lower-case name)]
      (or (str/includes? lower-name "xbox")
          (str/includes? lower-name "x-box")))))

(defn ps-controller? [name]
  (when name
    (str/includes? (str/lower-case name) "playstation")))

(defn draw-xbox-layout [gamepad tex-xbox left-x left-y right-x right-y left-trigger right-trigger]
  ;; Draw controller image
  (rtd-tex/draw-texture! tex-xbox 0 0 colors/darkgray)

  ;; Xbox home button
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_MIDDLE))
    (rsb/draw-circle! 394 89 19 colors/red))

  ;; Basic buttons
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_MIDDLE_RIGHT))
    (rsb/draw-circle! 436 150 9 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_MIDDLE_LEFT))
    (rsb/draw-circle! 352 150 9 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_LEFT))
    (rsb/draw-circle! 501 151 15 colors/blue))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_DOWN))
    (rsb/draw-circle! 536 187 15 colors/lime))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_RIGHT))
    (rsb/draw-circle! 572 151 15 colors/maroon))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_UP))
    (rsb/draw-circle! 536 115 15 colors/gold))

  ;; D-pad background
  (rsb/draw-rectangle! 317 202 19 71 colors/black)
  (rsb/draw-rectangle! 293 228 69 19 colors/black)
  ;; D-pad buttons
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_UP))
    (rsb/draw-rectangle! 317 202 19 26 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_DOWN))
    (rsb/draw-rectangle! 317 247 19 26 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_LEFT))
    (rsb/draw-rectangle! 292 228 25 19 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_RIGHT))
    (rsb/draw-rectangle! 336 228 26 19 colors/red))

  ;; Shoulder buttons
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_TRIGGER_1))
    (rsb/draw-circle! 259 61 20 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_TRIGGER_1))
    (rsb/draw-circle! 536 61 20 colors/red))

  ;; Left stick
  (let [stick-color (if (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_THUMB))
                      colors/red colors/black)]
    (rsb/draw-circle! 259 152 39 colors/black)
    (rsb/draw-circle! 259 152 34 colors/lightgray)
    (rsb/draw-circle! (+ 259 (int (* left-x 20))) (+ 152 (int (* left-y 20))) 25 stick-color))

  ;; Right stick
  (let [stick-color (if (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_THUMB))
                      colors/red colors/black)]
    (rsb/draw-circle! 461 237 38 colors/black)
    (rsb/draw-circle! 461 237 33 colors/lightgray)
    (rsb/draw-circle! (+ 461 (int (* right-x 20))) (+ 237 (int (* right-y 20))) 25 stick-color))

  ;; Triggers
  (rsb/draw-rectangle! 170 30 15 70 colors/gray)
  (rsb/draw-rectangle! 604 30 15 70 colors/gray)
  (rsb/draw-rectangle! 170 30 15 (int (* (/ (+ 1 left-trigger) 2) 70)) colors/red)
  (rsb/draw-rectangle! 604 30 15 (int (* (/ (+ 1 right-trigger) 2) 70)) colors/red))

(defn draw-ps-layout [gamepad tex-ps3 left-x left-y right-x right-y left-trigger right-trigger]
  ;; Draw controller image
  (rtd-tex/draw-texture! tex-ps3 0 0 colors/darkgray)

  ;; PS button
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_MIDDLE))
    (rsb/draw-circle! 396 222 13 colors/red))

  ;; Basic buttons
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_MIDDLE_LEFT))
    (rsb/draw-rectangle! 328 170 32 13 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_MIDDLE_RIGHT))
    (rsb/draw-triangle! {:x 436
                         :y 168} {:x 436
                                  :y 185} {:x 464
                                           :y 177} colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_UP))
    (rsb/draw-circle! 557 144 13 colors/lime))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_RIGHT))
    (rsb/draw-circle! 586 173 13 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_DOWN))
    (rsb/draw-circle! 557 203 13 colors/violet))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_LEFT))
    (rsb/draw-circle! 527 173 13 colors/pink))

  ;; D-pad background
  (rsb/draw-rectangle! 225 132 24 84 colors/black)
  (rsb/draw-rectangle! 195 161 84 25 colors/black)
  ;; D-pad buttons
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_UP))
    (rsb/draw-rectangle! 225 132 24 29 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_DOWN))
    (rsb/draw-rectangle! 225 186 24 30 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_LEFT))
    (rsb/draw-rectangle! 195 161 30 25 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_RIGHT))
    (rsb/draw-rectangle! 249 161 30 25 colors/red))

  ;; Shoulder buttons
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_TRIGGER_1))
    (rsb/draw-circle! 239 82 20 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_TRIGGER_1))
    (rsb/draw-circle! 557 82 20 colors/red))

  ;; Left stick
  (let [stick-color (if (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_THUMB))
                      colors/red colors/black)]
    (rsb/draw-circle! 319 255 35 colors/black)
    (rsb/draw-circle! 319 255 31 colors/lightgray)
    (rsb/draw-circle! (+ 319 (int (* left-x 20))) (+ 255 (int (* left-y 20))) 25 stick-color))

  ;; Right stick
  (let [stick-color (if (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_THUMB))
                      colors/red colors/black)]
    (rsb/draw-circle! 475 255 35 colors/black)
    (rsb/draw-circle! 475 255 31 colors/lightgray)
    (rsb/draw-circle! (+ 475 (int (* right-x 20))) (+ 255 (int (* right-y 20))) 25 stick-color))

  ;; Triggers
  (rsb/draw-rectangle! 169 48 15 70 colors/gray)
  (rsb/draw-rectangle! 611 48 15 70 colors/gray)
  (rsb/draw-rectangle! 169 48 15 (int (* (/ (+ 1 left-trigger) 2) 70)) colors/red)
  (rsb/draw-rectangle! 611 48 15 (int (* (/ (+ 1 right-trigger) 2) 70)) colors/red))

(defn draw-generic-layout [gamepad left-x left-y right-x right-y left-trigger right-trigger]
  ;; Draw generic controller background
  (rsb/draw-rectangle-rounded! {:x 175
                                :y 110
                                :width 460
                                :height 220} 0.3 16 colors/darkgray)

  ;; Basic buttons
  (rsb/draw-circle! 365 170 12 colors/raywhite)
  (rsb/draw-circle! 405 170 12 colors/raywhite)
  (rsb/draw-circle! 445 170 12 colors/raywhite)
  (rsb/draw-circle! 516 191 17 colors/raywhite)
  (rsb/draw-circle! 551 227 17 colors/raywhite)
  (rsb/draw-circle! 587 191 17 colors/raywhite)
  (rsb/draw-circle! 551 155 17 colors/raywhite)

  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_MIDDLE_LEFT))
    (rsb/draw-circle! 365 170 10 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_MIDDLE))
    (rsb/draw-circle! 405 170 10 colors/green))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_MIDDLE_RIGHT))
    (rsb/draw-circle! 445 170 10 colors/blue))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_LEFT))
    (rsb/draw-circle! 516 191 15 colors/gold))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_DOWN))
    (rsb/draw-circle! 551 227 15 colors/blue))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_RIGHT))
    (rsb/draw-circle! 587 191 15 colors/green))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_FACE_UP))
    (rsb/draw-circle! 551 155 15 colors/red))

  ;; D-pad
  (rsb/draw-rectangle! 245 145 28 88 colors/raywhite)
  (rsb/draw-rectangle! 215 174 88 29 colors/raywhite)
  (rsb/draw-rectangle! 247 147 24 84 colors/black)
  (rsb/draw-rectangle! 217 176 84 25 colors/black)
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_UP))
    (rsb/draw-rectangle! 247 147 24 29 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_DOWN))
    (rsb/draw-rectangle! 247 201 24 30 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_LEFT))
    (rsb/draw-rectangle! 217 176 30 25 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_FACE_RIGHT))
    (rsb/draw-rectangle! 271 176 30 25 colors/red))

  ;; Shoulder buttons
  (rsb/draw-rectangle-rounded! {:x 215
                                :y 98
                                :width 100
                                :height 10} 0.5 16 colors/darkgray)
  (rsb/draw-rectangle-rounded! {:x 495
                                :y 98
                                :width 100
                                :height 10} 0.5 16 colors/darkgray)
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_TRIGGER_1))
    (rsb/draw-rectangle-rounded! {:x 215
                                  :y 98
                                  :width 100
                                  :height 10} 0.5 16 colors/red))
  (when (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_TRIGGER_1))
    (rsb/draw-rectangle-rounded! {:x 495
                                  :y 98
                                  :width 100
                                  :height 10} 0.5 16 colors/red))

  ;; Left stick
  (let [stick-color (if (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_LEFT_THUMB))
                      colors/red colors/black)]
    (rsb/draw-circle! 345 260 40 colors/black)
    (rsb/draw-circle! 345 260 35 colors/lightgray)
    (rsb/draw-circle! (+ 345 (int (* left-x 20))) (+ 260 (int (* left-y 20))) 25 stick-color))

  ;; Right stick
  (let [stick-color (if (pos? (rcg/is-gamepad-button-down? gamepad rcg/GAMEPAD_BUTTON_RIGHT_THUMB))
                      colors/red colors/black)]
    (rsb/draw-circle! 465 260 40 colors/black)
    (rsb/draw-circle! 465 260 35 colors/lightgray)
    (rsb/draw-circle! (+ 465 (int (* right-x 20))) (+ 260 (int (* right-y 20))) 25 stick-color))

  ;; Triggers
  (rsb/draw-rectangle! 151 110 15 70 colors/gray)
  (rsb/draw-rectangle! 644 110 15 70 colors/gray)
  (rsb/draw-rectangle! 151 110 15 (int (* (/ (+ 1 left-trigger) 2) 70)) colors/red)
  (rsb/draw-rectangle! 644 110 15 (int (* (/ (+ 1 right-trigger) 2) 70)) colors/red))

(defn draw [{:keys [gamepad tex-ps3 tex-xbox]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (if (pos? (rcg/is-gamepad-available? gamepad))
    (let [name (rcg/get-gamepad-name gamepad)
          ;; Get axis values with deadzones
          left-x (apply-deadzone (rcg/get-gamepad-axis-movement gamepad rcg/GAMEPAD_AXIS_LEFT_X) STICK_DEADZONE)
          left-y (apply-deadzone (rcg/get-gamepad-axis-movement gamepad rcg/GAMEPAD_AXIS_LEFT_Y) STICK_DEADZONE)
          right-x (apply-deadzone (rcg/get-gamepad-axis-movement gamepad rcg/GAMEPAD_AXIS_RIGHT_X) STICK_DEADZONE)
          right-y (apply-deadzone (rcg/get-gamepad-axis-movement gamepad rcg/GAMEPAD_AXIS_RIGHT_Y) STICK_DEADZONE)
          left-trigger (apply-trigger-deadzone (rcg/get-gamepad-axis-movement gamepad rcg/GAMEPAD_AXIS_LEFT_TRIGGER) TRIGGER_DEADZONE)
          right-trigger (apply-trigger-deadzone (rcg/get-gamepad-axis-movement gamepad rcg/GAMEPAD_AXIS_RIGHT_TRIGGER) TRIGGER_DEADZONE)]

      ;; Draw gamepad name
      (rtd/draw-text! (format "GP%d: %s" gamepad (or name "Unknown")) 10 10 10 colors/black)

      ;; Draw appropriate controller layout
      (cond
        (xbox-controller? name)
        (draw-xbox-layout gamepad tex-xbox left-x left-y right-x right-y left-trigger right-trigger)

        (ps-controller? name)
        (draw-ps-layout gamepad tex-ps3 left-x left-y right-x right-y left-trigger right-trigger)

        :else
        (draw-generic-layout gamepad left-x left-y right-x right-y left-trigger right-trigger))

      ;; Draw axis info
      (let [axis-count (rcg/get-gamepad-axis-count gamepad)]
        (rtd/draw-text! (format "DETECTED AXIS [%d]:" axis-count) 10 50 10 colors/maroon)
        (doseq [i (range axis-count)]
          (rtd/draw-text! (format "AXIS %d: %.2f" i (rcg/get-gamepad-axis-movement gamepad i))
                          20 (+ 70 (* 20 i)) 10 colors/darkgray)))

      ;; Draw detected button
      (let [btn (rcg/get-gamepad-button-pressed)]
        (if (not= btn rcg/GAMEPAD_BUTTON_UNKNOWN)
          (rtd/draw-text! (format "DETECTED BUTTON: %d" btn) 10 430 10 colors/red)
          (rtd/draw-text! "DETECTED BUTTON: NONE" 10 430 10 colors/gray))))

    ;; No gamepad detected
    (do
      (rtd/draw-text! (format "GP%d: NOT DETECTED" gamepad) 10 10 10 colors/gray)
      (rtd/draw-text! "Connect a gamepad and press LEFT/RIGHT to select" 10 30 10 colors/darkgray)
      (when tex-xbox
        (rtd-tex/draw-texture! tex-xbox 0 0 colors/lightgray))))

  ;; Draw debug stats overlay
  (debug-stats/draw!)

  (rcd/end-drawing!))

(defn cleanup [{:keys [tex-ps3 tex-xbox]}]
  (when tex-ps3 (rtl/unload-texture! tex-ps3))
  (when tex-xbox (rtl/unload-texture! tex-xbox)))

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
  ;; For REPL development - connect to port 7888 after running the game
  @game-atom

  ;; Check gamepad status
  (rcg/is-gamepad-available? 0)
  (rcg/get-gamepad-name 0)

  ;; Switch gamepad from REPL
  (swap! game-atom assoc :gamepad 1)
  ;;
  )
