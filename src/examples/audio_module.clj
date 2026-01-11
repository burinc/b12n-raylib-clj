(ns examples.audio-module
  "raylib [audio] example - module playing
   
   Music visualization with animated circles that react to the music.
   
   Difficulty: ⭐☆☆☆ (1/4)
   Based on: audio/audio_module_playing.c"
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

(def MAX_CIRCLES 64)

;; Color palette for circles
(def circle-colors
  [colors/orange colors/red colors/gold colors/lime
   colors/blue colors/violet colors/brown colors/lightgray
   colors/pink colors/yellow colors/green colors/skyblue
   colors/purple colors/beige])

;; Helper to fade a color (set alpha)
(defn fade-color [color alpha]
  (assoc color :a (int (* 255 (max 0.0 (min 1.0 alpha))))))

;; Create a random circle
(defn make-circle []
  (let [radius (+ 10 (rand-int 31))]
    {:position {:x (+ radius (rand-int (- screen-width (* 2 radius))))
                :y (+ radius (rand-int (- screen-height (* 2 radius))))}
     :radius (float radius)
     :alpha 0.0
     :speed (/ (+ 1 (rand-int 100)) 2000.0)
     :color (rand-nth circle-colors)}))

;; Update a single circle
(defn update-circle [circle paused?]
  (if paused?
    circle
    (let [{:keys [alpha speed radius]} circle
          new-alpha (+ alpha speed)
          new-radius (+ radius (* speed 10.0))
          ;; Reverse direction when alpha reaches 1
          new-speed (if (> new-alpha 1.0) (- (Math/abs speed)) speed)]
      (if (<= new-alpha 0.0)
        ;; Reset circle when it fades out
        (make-circle)
        (assoc circle
               :alpha new-alpha
               :radius new-radius
               :speed new-speed)))))

(defn initial-state []
  {:circles (vec (repeatedly MAX_CIRCLES make-circle))
   :pitch 1.0
   :paused false})

(defn update-state [{:keys [circles pitch paused] :as state} music]
  (let [;; Update music buffer
        _ (ra/update-music-stream! music)

        ;; Restart music (Space)
        restart? (rck/is-key-pressed? (:space enums/keyboard-key))
        _ (when restart?
            (ra/stop-music-stream! music)
            (ra/play-music-stream! music))

        ;; Pause/Resume (P)
        toggle-pause? (rck/is-key-pressed? (:p enums/keyboard-key))
        new-paused (if toggle-pause? (not paused) paused)
        new-paused (if restart? false new-paused)
        _ (when toggle-pause?
            (if new-paused
              (ra/pause-music-stream! music)
              (ra/resume-music-stream! music)))

        ;; Pitch control (Up/Down)
        new-pitch (cond
                    (rck/is-key-down? (:down enums/keyboard-key)) (- pitch 0.01)
                    (rck/is-key-down? (:up enums/keyboard-key)) (+ pitch 0.01)
                    :else pitch)
        _ (when (not= pitch new-pitch)
            (ra/set-music-pitch! music (float new-pitch)))

        ;; Update circles
        new-circles (mapv #(update-circle % new-paused) circles)]

    (assoc state
           :circles new-circles
           :pitch new-pitch
           :paused new-paused)))

(defn draw-scene! [{:keys [circles pitch]} music]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; Draw circles (in reverse order for proper layering)
  (doseq [{:keys [position radius alpha color]} (reverse circles)]
    (rsb/draw-circle-v! position radius (fade-color color alpha)))

  ;; Calculate time bar
  (let [time-played (ra/get-music-time-played music)
        time-length (ra/get-music-time-length music)
        bar-width (if (pos? time-length)
                    (int (* (/ time-played time-length) (- screen-width 40)))
                    0)]

    ;; Draw time bar
    (rsb/draw-rectangle! 20 (- screen-height 32) (- screen-width 40) 12 colors/lightgray)
    (rsb/draw-rectangle! 20 (- screen-height 32) bar-width 12 colors/maroon)
    (rsb/draw-rectangle-lines! 20 (- screen-height 32) (- screen-width 40) 12 colors/gray))

  ;; Draw help instructions
  (rsb/draw-rectangle! 20 20 425 145 colors/white)
  (rsb/draw-rectangle-lines! 20 20 425 145 colors/gray)
  (rtd/draw-text! "PRESS SPACE TO RESTART MUSIC" 40 40 20 colors/black)
  (rtd/draw-text! "PRESS P TO PAUSE/RESUME" 40 70 20 colors/black)
  (rtd/draw-text! "PRESS UP/DOWN TO CHANGE SPEED" 40 100 20 colors/black)
  (rtd/draw-text! (format "SPEED: %.2f" pitch) 40 130 20 colors/maroon)

  (rcd/end-drawing!))

(defn -main [& _args]
  ;; Set MSAA 4x hint before window creation
  (rcw/set-config-flags :flag/msaa-4x-hint)

  (rcw/init-window! screen-width screen-height "raylib [audio] example - module playing")
  (ra/init-audio-device!)

  (let [music (ra/load-music-stream "resources/mini1111.xm")]
    ;; Note: music.looping is false by default in this struct layout
    (ra/play-music-stream! music)
    (rct/set-target-fps! 60)

    (loop [state (initial-state)]
      (if (rcw/window-should-close?)
        (do
          (ra/unload-music-stream! music)
          (ra/close-audio-device!)
          (rcw/close-window!))
        (let [new-state (update-state state music)]
          (draw-scene! new-state music)
          (recur new-state))))))
