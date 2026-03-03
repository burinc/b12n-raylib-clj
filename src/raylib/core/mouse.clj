(ns raylib.core.mouse
  (:require
   [raylib.core]
   [raylib.internals :as ri]
   [raylib.structs :as rs]
   [coffi.mem :as mem]
   [coffi.ffi :refer [defcfn]]))

(defcfn is-mouse-button-pressed?
  "Check if a mouse button has been pressed once"
  {:arglists '([button])}
  "IsMouseButtonPressed"
  [::mem/int] ::ri/bool)

(defcfn is-mouse-button-down?
  "Check if a mouse button is being pressed"
  {:arglists '([button])}
  "IsMouseButtonDown"
  [::mem/int] ::ri/bool)

(defcfn is-mouse-button-released?
  "Check if a mouse button has been released once"
  {:arglists '([button])}
  "IsMouseButtonReleased"
  [::mem/int] ::ri/bool)

(defcfn is-mouse-button-up?
  "Check if a mouse button is NOT being pressed"
  {:arglists '([button])}
  "IsMouseButtonUp"
  [::mem/int] ::ri/bool)

(defcfn get-mouse-x
  "Get mouse position X"
  "GetMouseX"
  [] ::mem/int)

(defcfn get-mouse-y
  "Get mouse position Y"
  "GetMouseY"
  [] ::mem/int)

(defcfn get-mouse-position
  "Get mouse position XY"
  "GetMousePosition"
  [] ::rs/vector-2)

(defcfn get-mouse-delta
  "Get mouse delta between frams"
  "GetMouseDelta"
  [] ::rs/vector-2)

(defcfn get-mouse-wheel-move
  "Get mouse wheel movement for X or Y, whichever is larger"
  "GetMouseWheelMove"
  [] ::mem/float)

(defcfn set-mouse-cursor!
  "Set mouse cursor shape"
  {:arglists '([cursor])}
  "SetMouseCursor"
  [::mem/int] ::mem/void)

(defcfn get-mouse-wheel-move-v
  "Get mouse wheel movement for both X and Y"
  "GetMouseWheelMoveV"
  [] ::rs/vector-2)

; ...
