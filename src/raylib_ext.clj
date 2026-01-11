(ns raylib-ext
  "Extended raylib bindings for missing functions"
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem :refer [defalias]]
   [coffi.ffi :refer [defcfn]]))

;; Missing struct definition
(defalias ::rectangle
  [::mem/struct
   [[:x ::mem/float]
    [:y ::mem/float]
    [:width ::mem/float]
    [:height ::mem/float]]])

;; Drawing functions
(defcfn draw-line!
  "Draw a line"
  {:arglists '([start-x start-y end-x end-y color])}
  "DrawLine"
  [::mem/int ::mem/int ::mem/int ::mem/int ::rs/color] ::mem/void)

(defcfn draw-circle!
  "Draw a color-filled circle"
  {:arglists '([center-x center-y radius color])}
  "DrawCircle"
  [::mem/int ::mem/int ::mem/float ::rs/color] ::mem/void)

(defcfn draw-triangle!
  "Draw a color-filled triangle (vertex in counter-clockwise order!)"
  {:arglists '([v1 v2 v3 color])}
  "DrawTriangle"
  [::rs/vector-2 ::rs/vector-2 ::rs/vector-2 ::rs/color] ::mem/void)

;; Text functions
(defcfn measure-text
  "Measure string width for default font"
  {:arglists '([text font-size])}
  "MeasureText"
  [::mem/c-string ::mem/int] ::mem/int)

;; Collision detection
(defcfn check-collision-circle-rec?
  "Check collision between circle and rectangle"
  {:arglists '([center radius rec])}
  "CheckCollisionCircleRec"
  [::rs/vector-2 ::mem/float ::rectangle] ::mem/byte)

(defcfn check-collision-point-circle?
  "Check if point is inside circle"
  {:arglists '([point center radius])}
  "CheckCollisionPointCircle"
  [::rs/vector-2 ::rs/vector-2 ::mem/float] ::mem/byte)

(defcfn check-collision-circles?
  "Check collision between two circles"
  {:arglists '([center1 radius1 center2 radius2])}
  "CheckCollisionCircles"
  [::rs/vector-2 ::mem/float ::rs/vector-2 ::mem/float] ::mem/byte)

;; RenderTexture functions
(defcfn load-render-texture!
  "Load texture for rendering (framebuffer)"
  {:arglists '([width height])}
  "LoadRenderTexture"
  [::mem/int ::mem/int] ::rs/render-texture)

(defcfn unload-render-texture!
  "Unload render texture from GPU memory (VRAM)"
  {:arglists '([target])}
  "UnloadRenderTexture"
  [::rs/render-texture] ::mem/void)

(defcfn begin-texture-mode!
  "Begin drawing to render texture"
  {:arglists '([target])}
  "BeginTextureMode"
  [::rs/render-texture] ::mem/void)

(defcfn end-texture-mode!
  "End drawing to render texture"
  "EndTextureMode"
  [] ::mem/void)

;; Advanced texture drawing
(defcfn draw-texture-pro!
  "Draw a part of a texture defined by a rectangle with 'pro' parameters"
  {:arglists '([texture source dest origin rotation tint])}
  "DrawTexturePro"
  [::rs/texture ::rectangle ::rectangle ::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

(defcfn draw-texture-rec!
  "Draw a part of a texture defined by a rectangle"
  {:arglists '([texture source position tint])}
  "DrawTextureRec"
  [::rs/texture ::rectangle ::rs/vector-2 ::rs/color] ::mem/void)

(defcfn draw-texture-ex!
  "Draw a texture with extended parameters"
  {:arglists '([texture position rotation scale tint])}
  "DrawTextureEx"
  [::rs/texture ::rs/vector-2 ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn unload-texture!
  "Unload texture from GPU memory (VRAM)"
  {:arglists '([texture])}
  "UnloadTexture"
  [::rs/texture] ::mem/void)

(defcfn get-color
  "Get Color structure from hexadecimal value"
  {:arglists '([hex-value])}
  "GetColor"
  [::mem/int] ::rs/color)

;; Additional shape drawing functions
(defcfn draw-circle-int!
  "Draw a color-filled circle (int version)"
  {:arglists '([center-x center-y radius color])}
  "DrawCircle"
  [::mem/int ::mem/int ::mem/float ::rs/color] ::mem/void)

(defcfn draw-rectangle-rounded!
  "Draw rectangle with rounded edges"
  {:arglists '([rec roundness segments color])}
  "DrawRectangleRounded"
  [::rectangle ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-rectangle-rec!
  "Draw a color-filled rectangle (Rectangle version)"
  {:arglists '([rec color])}
  "DrawRectangleRec"
  [::rectangle ::rs/color] ::mem/void)

(defcfn draw-rectangle-lines!
  "Draw rectangle outline"
  {:arglists '([x y width height color])}
  "DrawRectangleLines"
  [::mem/int ::mem/int ::mem/int ::mem/int ::rs/color] ::mem/void)

(defcfn draw-circle-v!
  "Draw a color-filled circle (Vector version)"
  {:arglists '([center radius color])}
  "DrawCircleV"
  [::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

(defcfn draw-line-ex!
  "Draw a line with thickness"
  {:arglists '([start-pos end-pos thick color])}
  "DrawLineEx"
  [::rs/vector-2 ::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

(defcfn draw-ring!
  "Draw ring"
  {:arglists '([center inner-radius outer-radius start-angle end-angle segments color])}
  "DrawRing"
  [::rs/vector-2 ::mem/float ::mem/float ::mem/float ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn fade
  "Get color with alpha applied"
  {:arglists '([color alpha])}
  "Fade"
  [::rs/color ::mem/float] ::rs/color)

(defcfn check-collision-point-rec?
  "Check if point is inside rectangle"
  {:arglists '([point rec])}
  "CheckCollisionPointRec"
  [::rs/vector-2 ::rectangle] ::mem/byte)

(defcfn measure-text
  "Measure string width for default font"
  {:arglists '([text font-size])}
  "MeasureText"
  [::mem/c-string ::mem/int] ::mem/int)
