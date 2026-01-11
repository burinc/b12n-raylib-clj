(ns examples.sound-loading
  "raylib [audio] example - sound loading
   
   Basic sound loading and playback with WAV and OGG files.
   
   Difficulty: ⭐☆☆☆ (1/4)
   Based on: audio/audio_sound_loading.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.audio :as ra]
   [raylib.enums :as enums]
   [raylib.colors :as colors]))

(def screen-width 800)
(def screen-height 450)

(defn -main [& _args]
  (rcw/init-window! screen-width screen-height "raylib [audio] example - sound loading")
  (ra/init-audio-device!)

  (let [fx-wav (ra/load-sound "resources/sound.wav")
        fx-ogg (ra/load-sound "resources/target.ogg")]

    (rct/set-target-fps! 60)

    (loop []
      (when-not (rcw/window-should-close?)
        ;; Play sounds on key press
        (when (rck/is-key-pressed? (:space enums/keyboard-key))
          (ra/play-sound! fx-wav))
        (when (rck/is-key-pressed? (:enter enums/keyboard-key))
          (ra/play-sound! fx-ogg))

        ;; Draw
        (rcd/begin-drawing!)
        (rcd/clear-background! colors/raywhite)

        (rtd/draw-text! "Press SPACE to PLAY the WAV sound!" 200 180 20 colors/lightgray)
        (rtd/draw-text! "Press ENTER to PLAY the OGG sound!" 200 220 20 colors/lightgray)

        (rcd/end-drawing!)
        (recur)))

    ;; Cleanup
    (ra/unload-sound! fx-wav)
    (ra/unload-sound! fx-ogg)
    (ra/close-audio-device!)
    (rcw/close-window!)))
