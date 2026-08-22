(ns examples.srcrec-dstrec
  "raylib [textures] example - source and destination rectangles

   One draw-texture-pro! call doing four things at once: picking a single
   frame out of a spritesheet (source rect), scaling it to fit a screen
   rect (destination rect), rotating it, and doing that rotation about a
   chosen origin rather than the corner. The two grey lines mark the
   destination's x and y, which is where the origin puts the pivot.

   Difficulty: 2/4
   Based on: textures/textures_srcrec_dstrec.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.textures.texture-loading :as rtl]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

;; scarfy.png is a 6-frame strip; one frame is a sixth of its width.
(def frame-count 6)

(defn initial-state [] {:texture nil :rotation 0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height
                    "raylib [textures] example - srcrec dstrec")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  ;; Textures must load AFTER init-window! - loading needs an OpenGL context.
  (swap! game-atom assoc :texture (rtl/load-texture! "resources/scarfy.png")))

(defn tick [state]
  (debug-stats/update!)
  (update state :rotation inc))

(defn draw [{:keys [texture rotation]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (when texture
    (let [frame-w (/ (:width texture) frame-count)
          frame-h (:height texture)
          dest-x (/ screen-width 2.0)
          dest-y (/ screen-height 2.0)]
      (rtl/draw-texture-pro!
       texture
       {:x 0.0 :y 0.0 :width (float frame-w) :height (float frame-h)}
       {:x (float dest-x) :y (float dest-y)
        :width (float (* frame-w 2.0)) :height (float (* frame-h 2.0))}
       ;; Origin is relative to the destination rect, so this pivots about
       ;; the drawn image's centre rather than its top-left corner.
       {:x (float frame-w) :y (float frame-h)}
       (float rotation)
       colors/white)
      (rsb/draw-line! (int dest-x) 0 (int dest-x) screen-height colors/gray)
      (rsb/draw-line! 0 (int dest-y) screen-width (int dest-y) colors/gray)))

  (rtd/draw-text! "(c) Scarfy sprite by Eiden Marsal"
                  (- screen-width 200) (- screen-height 20) 10 colors/gray)
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
  (rtl/unload-texture! (:texture @game-atom))
  (rcw/close-window!))
