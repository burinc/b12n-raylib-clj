(ns raylib.text.drawing
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem]
   [coffi.ffi :refer [defcfn]]))

(defcfn draw-fps!
  "Draw current FPS"
  {:arglists '([x y])}
  "DrawFPS"
  [::mem/int ::mem/int] ::mem/void)

(defcfn draw-text!
  "Draw text (using default font)"
  {:arglists '([text x y size color])}
  "DrawText"
  [::mem/c-string ::mem/int ::mem/int ::mem/int ::rs/color] ::mem/void)

(defmacro draw-text [{:keys [text x y size color]}]
  `(draw-text! ~text ~x ~y ~size ~color))

;; Moved from raylib-ext (2026-08-22 consolidation)
(defcfn measure-text
  "Measure string width for default font"
  {:arglists '([text font-size])}
  "MeasureText"
  [::mem/c-string ::mem/int] ::mem/int)

