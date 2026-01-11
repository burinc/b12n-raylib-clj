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

; ...
