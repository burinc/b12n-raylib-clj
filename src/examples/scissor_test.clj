(ns examples.scissor-test
  "raylib [core] example - scissor test
   
   Using scissor mode to clip drawing to a rectangle area.
   
   Difficulty: ⭐☆☆☆ (1/4)
   Based on: core/core_scissor_test.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.enums :as enums]
   [raylib.colors :as colors]))

(def screen-width 800)
(def screen-height 450)
(def scissor-width 300)
(def scissor-height 300)

(defn -main [& _args]
  (rcw/init-window! screen-width screen-height "raylib [core] example - scissor test")
  (rct/set-target-fps! 60)

  (loop [scissor-mode true]
    (if (rcw/window-should-close?)
      (rcw/close-window!)
      (let [;; Toggle scissor mode with S key
            new-scissor-mode (if (rck/is-key-pressed? (:s enums/keyboard-key))
                               (not scissor-mode)
                               scissor-mode)
            ;; Center scissor area around mouse
            mouse-x (rcm/get-mouse-x)
            mouse-y (rcm/get-mouse-y)
            scissor-x (- mouse-x (/ scissor-width 2))
            scissor-y (- mouse-y (/ scissor-height 2))]

        ;; Draw
        (rcd/begin-drawing!)
        (rcd/clear-background! colors/raywhite)

        ;; Begin scissor mode if enabled
        (when new-scissor-mode
          (rcd/begin-scissor-mode! (int scissor-x) (int scissor-y) scissor-width scissor-height))

        ;; Draw full screen rectangle and text
        ;; Only part in scissor area will be visible
        (rsb/draw-rectangle! 0 0 (rcw/get-screen-width) (rcw/get-screen-height) colors/red)
        (rtd/draw-text! "Move the mouse around to reveal this text!" 190 200 20 colors/lightgray)

        (when new-scissor-mode
          (rcd/end-scissor-mode!))

        ;; Draw scissor area outline
        (rsb/draw-rectangle-lines-ex! {:x scissor-x :y scissor-y
                                       :width (float scissor-width)
                                       :height (float scissor-height)}
                                      1.0 colors/black)
        (rtd/draw-text! "Press S to toggle scissor test" 10 10 20 colors/black)

        (rcd/end-drawing!)
        (recur new-scissor-mode)))))
