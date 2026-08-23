(ns examples.palette-switch
  "raylib [shaders] example - palette switch

   Eight horizontal bands drawn in near-black greys - RGB (0,0,0) through
   (7,7,7) - which a fragment shader then remaps through a palette. The
   colour you see is never drawn by this program: the pixel's red channel
   carries an INDEX, and the shader looks it up. Left and right swap between
   a 3-bit RGB palette, a GameBoy-like AMMO-8, and a two-strip film RKBV.

   This is the example that exercises
   `raylib.core.shaders/set-shader-value-ints!`. Its `count` argument is the
   number of GROUPS rather than of ints - 24 ints as IVEC3 is 8 colours - and
   getting that wrong reads past the end of the buffer, so the helper derives
   it from the uniform type rather than taking it on trust.

   Difficulty: 3/4
   Based on: shaders/shaders_palette_switch.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.shaders :as rcs]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def colors-per-palette 8)

(def palettes
  "Eight RGB triplets each, flat, as the shader wants them."
  [{:name "3-BIT RGB"
    :colors [0 0 0,  255 0 0,  0 255 0,  0 0 255,
             0 255 255,  255 0 255,  255 255 0,  255 255 255]}
   {:name "AMMO-8 (GameBoy-like)"
    :colors [4 12 6,  17 35 24,  30 58 41,  48 93 66,
             77 128 97,  137 162 87,  190 220 127,  238 255 204]}
   {:name "RKBV (2-strip film)"
    :colors [21 25 26,  138 76 88,  217 98 117,  230 184 193,
             69 107 115,  75 151 166,  165 189 194,  255 245 247]}])

(defn initial-state []
  {:palette 0 :shader nil :palette-loc nil})

(def game-atom (atom (initial-state)))

(def line-height (quot screen-height colors-per-palette))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shaders] example - palette switch")
  ;; nil for the vertex shader means "use raylib's default one" - the C
  ;; passes 0 for the same reason.
  (let [shader (rcs/load-shader nil "resources/shaders/glsl330/palette_switch.fs")
        loc (rcs/get-shader-location shader "palette")]
    (when (neg? loc)
      (println "WARNING: shader has no 'palette' uniform; bands will draw unmapped"))
    (swap! game-atom assoc :shader shader :palette-loc loc))
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [palette] :as state}]
  (debug-stats/update!)
  (let [n (count palettes)
        palette (cond (rck/is-key-pressed? (:right enums/keyboard-key)) (inc palette)
                      (rck/is-key-pressed? (:left enums/keyboard-key)) (dec palette)
                      :else palette)]
    (assoc state :palette (mod palette n))))

(defn draw [{:keys [palette shader palette-loc]}]
  ;; Uploaded every frame, as the C does. It only changes on a keypress, but
  ;; re-sending 24 ints is cheaper than tracking whether it is dirty.
  (rcs/set-shader-value-ints! shader palette-loc
                              (:colors (nth palettes palette))
                              rcs/SHADER_UNIFORM_IVEC3)
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (rcs/begin-shader-mode! shader)
  ;; The index is smuggled through the colour channels: band i is drawn as
  ;; RGB (i,i,i), and the shader reads red as the palette index. Nothing
  ;; here knows what colour will appear.
  (doseq [i (range colors-per-palette)]
    (rsb/draw-rectangle! 0 (* line-height i) (rcw/get-screen-width) line-height
                         {:r i :g i :b i :a 255}))
  (rcs/end-shader-mode!)

  (rtd/draw-text! "< >" 10 10 30 colors/darkblue)
  (rtd/draw-text! "CURRENT PALETTE:" 60 15 20 colors/raywhite)
  (rtd/draw-text! (:name (nth palettes palette)) 300 15 20 colors/red)
  (rtd/draw-fps! 700 15)
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
  (when-let [s (:shader @game-atom)] (rcs/unload-shader! s))
  (rcw/close-window!))
