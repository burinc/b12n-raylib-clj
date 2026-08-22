(ns examples.render-texture
  "raylib [core] example - render texture

   A ball bounces inside a 300x300 offscreen target, and that whole target
   is then drawn to the window as a single rotating texture. Render textures
   are how you get post-processing, minimaps and split-screen: draw a scene
   once, then treat the result as an image.

   Note the negative source height in the final draw. OpenGL render targets
   are stored bottom-up, so sampling with a negative height flips the image
   back the right way - without it the ball bounces upside down.

   Difficulty: 2/4
   Based on: core/core_render_texture.c"
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

(def target-width 300)
(def target-height 300)
(def ball-radius 20)

(defn initial-state []
  {:target nil
   :ball {:x (/ target-width 2.0) :y (/ target-height 2.0)}
   :speed {:x 5.0 :y 4.0}
   :rotation 0.0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [core] example - render texture")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  (swap! game-atom assoc :target (rtl/load-render-texture! target-width target-height)))

(defn- bounce
  "Flip velocity at either wall. Bounds are the target's edges, not the
   window's - the ball lives in the offscreen texture's coordinate space."
  [pos vel lo hi]
  (if (or (>= pos hi) (<= pos lo)) (- vel) vel))

(defn tick [{:keys [ball speed] :as state}]
  (debug-stats/update!)
  (let [x (+ (:x ball) (:x speed))
        y (+ (:y ball) (:y speed))]
    (-> state
        (assoc :ball {:x x :y y}
               :speed {:x (bounce x (:x speed) ball-radius (- target-width ball-radius))
                       :y (bounce y (:y speed) ball-radius (- target-height ball-radius))})
        (update :rotation + 0.5))))

(defn draw [{:keys [target ball rotation]}]
  (when target
    ;; Pass 1: draw the scene into the offscreen target.
    (rtl/begin-texture-mode! target)
    (rcd/clear-background! colors/skyblue)
    (rsb/draw-rectangle! 0 0 20 20 colors/red)
    (rsb/draw-circle-v! {:x (float (:x ball)) :y (float (:y ball))}
                        (float ball-radius) colors/maroon)
    (rtl/end-texture-mode!))

  ;; Pass 2: draw that target to the window as one rotating image.
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (when target
    (let [tex (:texture target)
          w (float (:width tex))
          h (float (:height tex))]
      (rtl/draw-texture-pro!
       tex
       {:x 0.0 :y 0.0 :width w :height (- h)}   ; negative height flips it upright
       {:x (float (/ screen-width 2.0)) :y (float (/ screen-height 2.0))
        :width w :height h}
       {:x (float (/ w 2.0)) :y (float (/ h 2.0))}
       (float rotation)
       colors/white)))
  (rtd/draw-text! "DRAWING BOUNCING BALL INSIDE RENDER TEXTURE!"
                  10 (- screen-height 40) 20 colors/black)
  (rtd/draw-fps! 10 10)
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
  (rtl/unload-render-texture! (:target @game-atom))
  (rcw/close-window!))
