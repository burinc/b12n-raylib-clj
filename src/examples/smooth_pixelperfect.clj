(ns examples.smooth-pixelperfect
  "raylib [core] example - smooth pixel-perfect camera

   A 160x90 world rendered into a tiny render texture and blown up to fill
   the window, so every world pixel becomes a chunky block. The trick is in
   splitting one camera into two.

   A pixel-art game wants the world drawn on whole-pixel boundaries, or
   sprites shimmer as they move. But snapping the camera to whole pixels
   makes motion visibly stutter. So the camera target is split: the integer
   part goes to the world camera, which keeps rendering pixel-aligned, and
   the leftover fraction goes to a screen-space camera that shifts the
   already-rendered image by a sub-pixel amount. Crisp pixels, smooth
   motion.

   Difficulty: 3/4
   Based on: core/core_smooth_pixelperfect.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.camera2d :as rc2d]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.textures.texture-loading :as rtl]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)
(def virtual-width 160)
(def virtual-height 90)

;; How many screen pixels one world pixel becomes.
(def virtual-ratio (/ (double screen-width) virtual-width))

(def rects
  [[{:x 70.0 :y 35.0 :width 20.0 :height 20.0} colors/black  0.0]
   [{:x 90.0 :y 55.0 :width 30.0 :height 10.0} colors/red   -1.0]   ; -1.0 = negated rotation
   [{:x 80.0 :y 65.0 :width 15.0 :height 25.0} colors/blue  45.0]]) ; 45.0 = rotation + 45

(defn- camera [target] {:offset {:x 0.0 :y 0.0} :target target :rotation 0.0 :zoom 1.0})

(defn initial-state [] {:target nil :rotation 0.0 :world {:x 0.0 :y 0.0} :screen {:x 0.0 :y 0.0}})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height
                    "raylib [core] example - smooth pixelperfect")
  (rct/set-target-fps! 60)
  (debug-stats/enable!)
  (swap! game-atom assoc :target (rtl/load-render-texture! virtual-width virtual-height)))

(defn tick [state]
  (debug-stats/update!)
  (let [t (rct/get-time)
        ;; An arbitrary drift so there is something to look at.
        cam-x (- (* (Math/sin t) 50.0) 10.0)
        cam-y (* (Math/cos t) 30.0)
        ;; Split: whole pixels to the world, the remainder (scaled up to
        ;; screen space) to the smoothing camera.
        world-x (double (long cam-x))
        world-y (double (long cam-y))]
    (assoc state
           :rotation (+ (:rotation state) (* 60.0 (rct/get-frame-time)))
           :world {:x world-x :y world-y}
           :screen {:x (* (- cam-x world-x) virtual-ratio)
                    :y (* (- cam-y world-y) virtual-ratio)})))

(defn draw [{:keys [target rotation world screen]}]
  (when target
    (rtl/begin-texture-mode! target)
    (rcd/clear-background! colors/raywhite)
    (rc2d/begin-mode-2d! (camera {:x (float (:x world)) :y (float (:y world))}))
    (doseq [[rec color spin] rects]
      (rsb/draw-rectangle-pro! rec {:x 0.0 :y 0.0}
                               (float (if (neg? spin) (- rotation) (+ rotation spin)))
                               color))
    (rc2d/end-mode-2d!)
    (rtl/end-texture-mode!))

  (rcd/begin-drawing!)
  (rcd/clear-background! colors/red)
  (when target
    (let [tex (:texture target)]
      (rc2d/begin-mode-2d! (camera {:x (float (:x screen)) :y (float (:y screen))}))
      (rtl/draw-texture-pro!
       tex
       ;; Negative height: OpenGL targets are stored bottom-up.
       {:x 0.0 :y 0.0 :width (float (:width tex)) :height (float (- (:height tex)))}
       ;; Overdraw by one world-pixel on each side so the sub-pixel shift
       ;; never exposes the background at an edge.
       {:x (float (- virtual-ratio)) :y (float (- virtual-ratio))
        :width (float (+ screen-width (* virtual-ratio 2)))
        :height (float (+ screen-height (* virtual-ratio 2)))}
       {:x 0.0 :y 0.0} (float 0.0) colors/white)
      (rc2d/end-mode-2d!)))

  (rtd/draw-text! (format "Screen resolution: %dx%d" screen-width screen-height)
                  10 10 20 colors/darkblue)
  (rtd/draw-text! (format "World resolution: %dx%d" virtual-width virtual-height)
                  10 40 20 colors/darkgreen)
  (rtd/draw-fps! (- (rcw/get-screen-width) 95) 10)
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
