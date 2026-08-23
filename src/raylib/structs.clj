(ns raylib.structs
  (:require
   [coffi.mem :as mem :refer [defalias]]
   [raylib.internals :as ri]))

(defalias ::color
  [::mem/struct
   [[:r ::ri/ubyte]
    [:g ::ri/ubyte]
    [:b ::ri/ubyte]
    [:a ::ri/ubyte]]])

(defalias ::vector-2
  [::mem/struct
   [[:x ::mem/float]
    [:y ::mem/float]]])

(defalias ::vector-3
  [::mem/struct
   [[:x ::mem/float]
    [:y ::mem/float]
    [:z ::mem/float]]])

(defalias ::vector-4
  [::mem/struct
   [[:x ::mem/float]
    [:y ::mem/float]
    [:z ::mem/float]
    [:w ::mem/float]]])

(defalias ::texture
  [::mem/struct
   [[:id ::mem/int]
    [:width ::mem/int]
    [:height ::mem/int]
    [:mipmaps ::mem/int]
    [:format ::mem/int]]])

(defalias ::render-texture
  [::mem/struct
   [[:id ::mem/int]
    [:texture ::texture]
    [:depth ::texture]]])

;; Matrix: 4x4, stored COLUMN-major the way raylib writes it - the field
;; names run m0 m4 m8 m12 across the first row, not m0 m1 m2 m3. Laid out
;; here in declaration order so the bytes match; do not "tidy" the order.
(defalias ::matrix
  [::mem/struct
   [[:m0 ::mem/float] [:m4 ::mem/float] [:m8 ::mem/float] [:m12 ::mem/float]
    [:m1 ::mem/float] [:m5 ::mem/float] [:m9 ::mem/float] [:m13 ::mem/float]
    [:m2 ::mem/float] [:m6 ::mem/float] [:m10 ::mem/float] [:m14 ::mem/float]
    [:m3 ::mem/float] [:m7 ::mem/float] [:m11 ::mem/float] [:m15 ::mem/float]]])

;; Moved here from core/collision so models can use it without depending on
;; the collision namespace.
(defalias ::bounding-box
  [::mem/struct
   [[:min ::vector-3]
    [:max ::vector-3]]])

;; Camera3D (raylib also calls it Camera). Lives here rather than in
;; core/camera3d because the models billboards take one by value too.
(defalias ::camera-3d
  [::mem/struct
   [[:position ::vector-3]
    [:target ::vector-3]
    [:up ::vector-3]
    [:fovy ::mem/float]
    [:projection ::mem/int]]])

;; Image: CPU-side pixel data, as opposed to Texture which lives on the GPU.
;; `data` is an opaque pointer here - raylib owns the allocation and
;; unload-image! frees it.
(defalias ::image
  [::mem/struct
   [[:data ::mem/pointer]
    [:width ::mem/int]
    [:height ::mem/int]
    [:mipmaps ::mem/int]
    [:format ::mem/int]]])

(defalias ::rectangle
  [::mem/struct
   [[:x ::mem/float]
    [:y ::mem/float]
    [:width ::mem/float]
    [:height ::mem/float]]])
