(ns examples.music-stream
  "raylib [audio] example - music stream
   
   Music streaming with volume and pan control.
   
   Difficulty: ⭐☆☆☆ (1/4)
   Based on: audio/audio_music_stream.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.audio :as ra]
   [raylib.enums :as enums]
   [raylib.colors :as colors]))

(def screen-width 800)
(def screen-height 450)

(defn initial-state []
  {:paused false
   :pan 0.0
   :volume 0.8})

(defn update-state [{:keys [paused pan volume]
                     :as state} music]
  ;; Update music buffer
  (ra/update-music-stream! music)

  ;; Restart music (Space)
  (when (rck/is-key-pressed? (:space enums/keyboard-key))
    (ra/stop-music-stream! music)
    (ra/play-music-stream! music))

  ;; Pause/Resume (P)
  (let [toggle-pause? (rck/is-key-pressed? (:p enums/keyboard-key))
        new-paused (if toggle-pause? (not paused) paused)]
    (when toggle-pause?
      (if new-paused
        (ra/pause-music-stream! music)
        (ra/resume-music-stream! music)))

    ;; Pan control (Left/Right)
    (let [new-pan (cond
                    (rck/is-key-down? (:left enums/keyboard-key))
                    (max -1.0 (- pan 0.05))

                    (rck/is-key-down? (:right enums/keyboard-key))
                    (min 1.0 (+ pan 0.05))

                    :else pan)]
      (when (not= pan new-pan)
        (ra/set-music-pan! music (float new-pan)))

      ;; Volume control (Up/Down)
      (let [new-volume (cond
                         (rck/is-key-down? (:down enums/keyboard-key))
                         (max 0.0 (- volume 0.05))

                         (rck/is-key-down? (:up enums/keyboard-key))
                         (min 1.0 (+ volume 0.05))

                         :else volume)]
        (when (not= volume new-volume)
          (ra/set-music-volume! music (float new-volume)))

        {:paused new-paused
         :pan new-pan
         :volume new-volume}))))

(defn draw-scene! [{:keys [pan volume]} music]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rtd/draw-text! "MUSIC SHOULD BE PLAYING!" 255 150 20 colors/lightgray)

  ;; Pan control slider
  (rtd/draw-text! "LEFT-RIGHT for PAN CONTROL" 320 74 10 colors/darkblue)
  (rsb/draw-rectangle! 300 100 200 12 colors/lightgray)
  (rsb/draw-rectangle-lines! 300 100 200 12 colors/gray)
  (rsb/draw-rectangle! (int (- (+ 300 (* (/ (+ pan 1.0) 2.0) 200)) 5)) 92 10 28 colors/darkgray)

  ;; Time progress bar
  (let [time-played (ra/get-music-time-played music)
        time-length (ra/get-music-time-length music)
        progress (if (pos? time-length)
                   (min 1.0 (/ time-played time-length))
                   0.0)]
    (rsb/draw-rectangle! 200 200 400 12 colors/lightgray)
    (rsb/draw-rectangle! 200 200 (int (* progress 400)) 12 colors/maroon)
    (rsb/draw-rectangle-lines! 200 200 400 12 colors/gray))

  ;; Instructions
  (rtd/draw-text! "PRESS SPACE TO RESTART MUSIC" 215 250 20 colors/lightgray)
  (rtd/draw-text! "PRESS P TO PAUSE/RESUME MUSIC" 208 280 20 colors/lightgray)

  ;; Volume control slider
  (rtd/draw-text! "UP-DOWN for VOLUME CONTROL" 320 334 10 colors/darkgreen)
  (rsb/draw-rectangle! 300 360 200 12 colors/lightgray)
  (rsb/draw-rectangle-lines! 300 360 200 12 colors/gray)
  (rsb/draw-rectangle! (int (- (+ 300 (* volume 200)) 5)) 352 10 28 colors/darkgray)

  (rcd/end-drawing!))

(defn -main [& _args]
  (rcw/init-window! screen-width screen-height "raylib [audio] example - music stream")
  (ra/init-audio-device!)

  (let [music (ra/load-music-stream "resources/country.mp3")]
    (ra/play-music-stream! music)
    (ra/set-music-pan! music 0.0)
    (ra/set-music-volume! music 0.8)

    (rct/set-target-fps! 30)

    (loop [state (initial-state)]
      (if (rcw/window-should-close?)
        (do
          (ra/unload-music-stream! music)
          (ra/close-audio-device!)
          (rcw/close-window!))
        (let [new-state (update-state state music)]
          (draw-scene! new-state music)
          (recur new-state))))))
