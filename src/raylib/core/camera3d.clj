(ns raylib.core.camera3d
  "3D Camera and rendering functions"
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem :refer [defalias]]
   [coffi.ffi :refer [defcfn]]))

;; Camera3D struct (also known as Camera in raylib)
;; typedef struct Camera3D {
;;     Vector3 position;       // Camera position
;;     Vector3 target;         // Camera target it looks-at
;;     Vector3 up;             // Camera up vector (rotation over its axis)
;;     float fovy;             // Camera field-of-view aperture in Y (degrees) in perspective, used as near plane width in orthographic
;;     int projection;         // Camera projection: CAMERA_PERSPECTIVE or CAMERA_ORTHOGRAPHIC
;; } Camera3D;
(defalias ::camera3d
  [::mem/struct
   [[:position ::rs/vector-3]
    [:target ::rs/vector-3]
    [:up ::rs/vector-3]
    [:fovy ::mem/float]
    [:projection ::mem/int]]])

;; Camera projection constants
(def CAMERA_PERSPECTIVE 0)
(def CAMERA_ORTHOGRAPHIC 1)

;; 3D Mode functions
(defcfn begin-mode-3d!
  "Begin 3D mode with custom camera (3D)"
  {:arglists '([camera])}
  "BeginMode3D"
  [::camera3d] ::mem/void)

(defcfn end-mode-3d!
  "Ends 3D mode and returns to default 2D orthographic mode"
  "EndMode3D"
  [] ::mem/void)

;; 3D Shape drawing functions
(defcfn draw-cube!
  "Draw cube"
  {:arglists '([position width height length color])}
  "DrawCube"
  [::rs/vector-3 ::mem/float ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-cube-v!
  "Draw cube (Vector version)"
  {:arglists '([position size color])}
  "DrawCubeV"
  [::rs/vector-3 ::rs/vector-3 ::rs/color] ::mem/void)

(defcfn draw-cube-wires!
  "Draw cube wires"
  {:arglists '([position width height length color])}
  "DrawCubeWires"
  [::rs/vector-3 ::mem/float ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-cube-wires-v!
  "Draw cube wires (Vector version)"
  {:arglists '([position size color])}
  "DrawCubeWiresV"
  [::rs/vector-3 ::rs/vector-3 ::rs/color] ::mem/void)

(defcfn draw-plane!
  "Draw a plane XZ"
  {:arglists '([center-pos size color])}
  "DrawPlane"
  [::rs/vector-3 ::rs/vector-2 ::rs/color] ::mem/void)

(defcfn draw-grid!
  "Draw a grid (centered at (0, 0, 0))"
  {:arglists '([slices spacing])}
  "DrawGrid"
  [::mem/int ::mem/float] ::mem/void)

(defcfn draw-sphere!
  "Draw sphere"
  {:arglists '([center-pos radius color])}
  "DrawSphere"
  [::rs/vector-3 ::mem/float ::rs/color] ::mem/void)

;; Camera mode constants (for UpdateCamera)
(def CAMERA_CUSTOM 0)
(def CAMERA_FREE 1)
(def CAMERA_ORBITAL 2)
(def CAMERA_FIRST_PERSON 3)
(def CAMERA_THIRD_PERSON 4)

;; Camera update function
(defcfn update-camera!
  "Update camera position for selected mode"
  {:arglists '([camera mode])}
  "UpdateCamera"
  [::mem/pointer ::mem/int] ::mem/void)

;; Cursor control functions
(defcfn disable-cursor!
  "Disables cursor (lock cursor)"
  "DisableCursor"
  [] ::mem/void)

(defcfn enable-cursor!
  "Enables cursor (unlock cursor)"
  "EnableCursor"
  [] ::mem/void)

(defcfn is-cursor-hidden?
  "Check if cursor is not visible"
  "IsCursorHidden"
  [] ::mem/byte)

(defcfn get-world-to-screen
  "Get the screen space position for a 3D world space position"
  {:arglists '([position camera])}
  "GetWorldToScreen"
  [::rs/vector-3 ::camera3d] ::rs/vector-2)
