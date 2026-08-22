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

(defcfn draw-sphere-ex!
  "Draw sphere with extended parameters"
  {:arglists '([center-pos radius rings slices color])}
  "DrawSphereEx"
  [::rs/vector-3 ::mem/float ::mem/int ::mem/int ::rs/color] ::mem/void)

(defcfn draw-sphere-wires!
  "Draw sphere wires"
  {:arglists '([center-pos radius rings slices color])}
  "DrawSphereWires"
  [::rs/vector-3 ::mem/float ::mem/int ::mem/int ::rs/color] ::mem/void)

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

(defcfn get-world-to-screen
  "Get the screen space position for a 3D world space position"
  {:arglists '([position camera])}
  "GetWorldToScreen"
  [::rs/vector-3 ::camera3d] ::rs/vector-2)

;; Helper function that updates camera and returns the new state
(defn update-camera
  "Update camera position for selected mode. Returns updated camera map.
   mode: CAMERA_FREE, CAMERA_ORBITAL, CAMERA_FIRST_PERSON, CAMERA_THIRD_PERSON"
  [camera mode]
  (let [arena (mem/confined-arena)
        seg (mem/alloc-instance ::camera3d arena)]
    (mem/serialize-into camera ::camera3d seg arena)
    (update-camera! seg mode)
    (mem/deserialize-from seg ::camera3d)))

;; Additional 3D shape drawing functions

(defcfn draw-cylinder!
  "Draw a cylinder/cone"
  {:arglists '([position radius-top radius-bottom height slices color])}
  "DrawCylinder"
  [::rs/vector-3 ::mem/float ::mem/float ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-cylinder-ex!
  "Draw a cylinder with base at startPos and top at endPos"
  {:arglists '([start-pos end-pos start-radius end-radius sides color])}
  "DrawCylinderEx"
  [::rs/vector-3 ::rs/vector-3 ::mem/float ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-cylinder-wires!
  "Draw a cylinder/cone wires"
  {:arglists '([position radius-top radius-bottom height slices color])}
  "DrawCylinderWires"
  [::rs/vector-3 ::mem/float ::mem/float ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-cylinder-wires-ex!
  "Draw a cylinder wires with base at startPos and top at endPos"
  {:arglists '([start-pos end-pos start-radius end-radius sides color])}
  "DrawCylinderWiresEx"
  [::rs/vector-3 ::rs/vector-3 ::mem/float ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-capsule!
  "Draw a capsule with the center of its sphere caps at startPos and endPos"
  {:arglists '([start-pos end-pos radius slices rings color])}
  "DrawCapsule"
  [::rs/vector-3 ::rs/vector-3 ::mem/float ::mem/int ::mem/int ::rs/color] ::mem/void)

(defcfn draw-capsule-wires!
  "Draw a capsule wireframe with the center of its sphere caps at startPos and endPos"
  {:arglists '([start-pos end-pos radius slices rings color])}
  "DrawCapsuleWires"
  [::rs/vector-3 ::rs/vector-3 ::mem/float ::mem/int ::mem/int ::rs/color] ::mem/void)

;; Line and circle 3D drawing

(defcfn draw-line-3d!
  "Draw a line in 3D world space"
  {:arglists '([start-pos end-pos color])}
  "DrawLine3D"
  [::rs/vector-3 ::rs/vector-3 ::rs/color] ::mem/void)

(defcfn draw-point-3d!
  "Draw a point in 3D space"
  {:arglists '([position color])}
  "DrawPoint3D"
  [::rs/vector-3 ::rs/color] ::mem/void)

(defcfn draw-circle-3d!
  "Draw a circle in 3D world space"
  {:arglists '([center radius rotation-axis rotation-angle color])}
  "DrawCircle3D"
  [::rs/vector-3 ::mem/float ::rs/vector-3 ::mem/float ::rs/color] ::mem/void)

(defcfn draw-triangle-3d!
  "Draw a triangle in 3D space"
  {:arglists '([v1 v2 v3 color])}
  "DrawTriangle3D"
  [::rs/vector-3 ::rs/vector-3 ::rs/vector-3 ::rs/color] ::mem/void)

;; Ray drawing (useful for picking/debugging)

(defalias ::ray
  [::mem/struct
   [[:position ::rs/vector-3]
    [:direction ::rs/vector-3]]])

(defcfn draw-ray!
  "Draw a ray line"
  {:arglists '([ray color])}
  "DrawRay"
  [::ray ::rs/color] ::mem/void)

