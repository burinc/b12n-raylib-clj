(ns raylib.shapes.basic
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem]
   [coffi.ffi :refer [defcfn]]))

; ...

(defcfn draw-circle-v!
  "Draw a color-filled circle (Vector version)"
  {:arglists '([center radius color])}
  "DrawCircleV"
  [::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

; ...

(defcfn draw-rectangle!
  "Draw a color-filled rectangle"
  {:arglists '([x y width height color])}
  "DrawRectangle"
  [::mem/int ::mem/int ::mem/int ::mem/int ::rs/color] ::mem/void)

(defcfn draw-rectangle-lines!
  "Draw rectangle outline"
  {:arglists '([x y width height color])}
  "DrawRectangleLines"
  [::mem/int ::mem/int ::mem/int ::mem/int ::rs/color] ::mem/void)

(defcfn draw-rectangle-rec!
  "Draw a color-filled rectangle from Rectangle struct"
  {:arglists '([rec color])}
  "DrawRectangleRec"
  [::rs/rectangle ::rs/color] ::mem/void)

(defcfn draw-rectangle-lines-ex!
  "Draw rectangle outline with extended parameters"
  {:arglists '([rec line-thick color])}
  "DrawRectangleLinesEx"
  [::rs/rectangle ::mem/float ::rs/color] ::mem/void)

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

(defcfn draw-circle-gradient!
  "Draw a gradient-filled circle"
  {:arglists '([center-x center-y radius inner-color outer-color])}
  "DrawCircleGradient"
  [::mem/int ::mem/int ::mem/float ::rs/color ::rs/color] ::mem/void)

(defcfn draw-circle-lines!
  "Draw circle outline"
  {:arglists '([center-x center-y radius color])}
  "DrawCircleLines"
  [::mem/int ::mem/int ::mem/float ::rs/color] ::mem/void)

(defcfn draw-ellipse!
  "Draw ellipse"
  {:arglists '([center-x center-y radius-h radius-v color])}
  "DrawEllipse"
  [::mem/int ::mem/int ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-ellipse-lines!
  "Draw ellipse outline"
  {:arglists '([center-x center-y radius-h radius-v color])}
  "DrawEllipseLines"
  [::mem/int ::mem/int ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-rectangle-gradient-h!
  "Draw a horizontal-gradient-filled rectangle"
  {:arglists '([x y width height left-color right-color])}
  "DrawRectangleGradientH"
  [::mem/int ::mem/int ::mem/int ::mem/int ::rs/color ::rs/color] ::mem/void)

(defcfn draw-triangle!
  "Draw a color-filled triangle (vertex in counter-clockwise order!)"
  {:arglists '([v1 v2 v3 color])}
  "DrawTriangle"
  [::rs/vector-2 ::rs/vector-2 ::rs/vector-2 ::rs/color] ::mem/void)

(defcfn draw-triangle-lines!
  "Draw triangle outline (vertex in counter-clockwise order!)"
  {:arglists '([v1 v2 v3 color])}
  "DrawTriangleLines"
  [::rs/vector-2 ::rs/vector-2 ::rs/vector-2 ::rs/color] ::mem/void)

(defcfn draw-poly!
  "Draw a regular polygon (Vector version)"
  {:arglists '([center sides radius rotation color])}
  "DrawPoly"
  [::rs/vector-2 ::mem/int ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-poly-lines!
  "Draw a polygon outline of n sides"
  {:arglists '([center sides radius rotation color])}
  "DrawPolyLines"
  [::rs/vector-2 ::mem/int ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-poly-lines-ex!
  "Draw a polygon outline of n sides with extended parameters"
  {:arglists '([center sides radius rotation line-thick color])}
  "DrawPolyLinesEx"
  [::rs/vector-2 ::mem/int ::mem/float ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-line-bezier!
  "Draw line segment cubic-bezier in-out interpolation"
  {:arglists '([start-pos end-pos thick color])}
  "DrawLineBezier"
  [::rs/vector-2 ::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

(defcfn draw-circle-lines-v!
  "Draw circle outline (Vector version)"
  {:arglists '([center radius color])}
  "DrawCircleLinesV"
  [::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

