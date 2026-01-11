(ns raylib.core.collision
  "Ray casting and collision detection functions"
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem :refer [defalias]]
   [coffi.ffi :refer [defcfn]]))

;; Ray struct: position (Vector3) + direction (Vector3) = 24 bytes
(defalias ::ray
  [::mem/struct
   [[:position ::rs/vector-3]
    [:direction ::rs/vector-3]]])

;; RayCollision struct: hit (bool/4 bytes padded) + distance (float) + point (Vector3) + normal (Vector3) = 32 bytes
;; bool is typically 1 byte but padded to 4 for alignment
(defalias ::ray-collision
  [::mem/struct
   [[:hit ::mem/int] ; bool as int (4 bytes)
    [:distance ::mem/float]
    [:point ::rs/vector-3]
    [:normal ::rs/vector-3]]])

;; BoundingBox struct: min (Vector3) + max (Vector3) = 24 bytes
(defalias ::bounding-box
  [::mem/struct
   [[:min ::rs/vector-3]
    [:max ::rs/vector-3]]])

(defcfn get-screen-to-world-ray
  "Get a ray trace from screen position (i.e. mouse)"
  {:arglists '([position camera])}
  "GetScreenToWorldRay"
  [::rs/vector-2 :raylib.core.camera3d/camera3d] ::ray)

(defcfn get-ray-collision-box
  "Get collision info between ray and box"
  {:arglists '([ray box])}
  "GetRayCollisionBox"
  [::ray ::bounding-box] ::ray-collision)

(defcfn draw-ray!
  "Draw a ray line"
  {:arglists '([ray color])}
  "DrawRay"
  [::ray ::rs/color] ::mem/void)

(defcfn get-ray-collision-sphere
  "Get collision info between ray and sphere"
  {:arglists '([ray center radius])}
  "GetRayCollisionSphere"
  [::ray ::rs/vector-3 ::mem/float] ::ray-collision)

(defcfn get-ray-collision-triangle
  "Get collision info between ray and triangle"
  {:arglists '([ray p1 p2 p3])}
  "GetRayCollisionTriangle"
  [::ray ::rs/vector-3 ::rs/vector-3 ::rs/vector-3] ::ray-collision)

(defcfn get-ray-collision-quad
  "Get collision info between ray and quad"
  {:arglists '([ray p1 p2 p3 p4])}
  "GetRayCollisionQuad"
  [::ray ::rs/vector-3 ::rs/vector-3 ::rs/vector-3 ::rs/vector-3] ::ray-collision)

;; Helper to create a bounding box from position and size
(defn make-bounding-box
  "Create a bounding box from center position and size"
  [{px :x py :y pz :z} {sx :x sy :y sz :z}]
  {:min {:x (- px (/ sx 2)) :y (- py (/ sy 2)) :z (- pz (/ sz 2))}
   :max {:x (+ px (/ sx 2)) :y (+ py (/ sy 2)) :z (+ pz (/ sz 2))}})
