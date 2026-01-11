(ns examples.sound-multi
  "raylib [audio] example - sound multi
   
   Playing multiple instances of the same sound using aliases.
   
   Difficulty: ⭐⭐☆☆ (2/4)
   Based on: audio/audio_sound_multi.c"
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

(def MAX_SOUNDS 10)

(defn -main [& _args]
  (rcw/init-window! screen-width screen-height "raylib [audio] example - sound multi")
  (ra/init-audio-device!)

  ;; Load audio file into the first slot as the 'source' sound
  ;; This sound owns the sample data
  (let [source-sound (ra/load-sound "resources/sound.wav")
        ;; Load aliases into slots 1-9. These do not own the sound data
        sound-aliases (vec (repeatedly (dec MAX_SOUNDS) #(ra/load-sound-alias source-sound)))
        all-sounds (into [source-sound] sound-aliases)]

    (rct/set-target-fps! 60)

    (loop [current-sound 0]
      (if (rcw/window-should-close?)
        (do
          ;; Unload aliases first (they don't own the data)
          (doseq [alias sound-aliases]
            (ra/unload-sound-alias! alias))
          ;; Then unload the source sound
          (ra/unload-sound! source-sound)
          (ra/close-audio-device!)
          (rcw/close-window!))

        (let [new-current (if (rck/is-key-pressed? (:space enums/keyboard-key))
                            (do
                             ;; Play the sound at the current slot
                              (ra/play-sound! (nth all-sounds current-sound))
                             ;; Cycle to next slot
                              (mod (inc current-sound) MAX_SOUNDS))
                            current-sound)]

          ;; Draw
          (rcd/begin-drawing!)
          (rcd/clear-background! colors/raywhite)

          (rtd/draw-text! "Press SPACE to PLAY a WAV sound!" 200 180 20 colors/lightgray)
          (rtd/draw-text! (format "Sound slot: %d / %d" (inc new-current) MAX_SOUNDS) 200 220 20 colors/gray)

          (rcd/end-drawing!)

          (recur new-current))))))
