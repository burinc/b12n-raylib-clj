(ns raylib.core.camera2d
  "2D Camera functions"
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem :refer [defalias]]
   [coffi.ffi :refer [defcfn]]))

;; Camera2D struct (24 bytes)
;; { Vector2 offset; Vector2 target; float rotation; float zoom; }
(defalias ::camera-2d
  [::mem/struct
   [[:offset ::rs/vector-2] ; 8 bytes - screen space offset
    [:target ::rs/vector-2] ; 8 bytes - world space target
    [:rotation ::mem/float] ; 4 bytes - rotation in degrees
    [:zoom ::mem/float]]]) ; 4 bytes - zoom/scale

(defcfn begin-mode-2d!
  "Begin 2D mode with custom camera (2D)"
  {:arglists '([camera])}
  "BeginMode2D"
  [::camera-2d] ::mem/void)

(defcfn end-mode-2d!
  "Ends 2D mode with custom camera"
  "EndMode2D"
  [] ::mem/void)

;; Helper to create a Camera2D map
(defn make-camera-2d
  "Create a Camera2D map with default or specified values.
   Args:
   - target: {:x x :y y} - world position camera looks at
   - offset: {:x x :y y} - screen position where target is drawn (usually center)
   - rotation: degrees (default 0)
   - zoom: scale factor (default 1.0, must not be 0)"
  ([target offset]
   (make-camera-2d target offset 0.0 1.0))
  ([target offset rotation zoom]
   {:offset offset
    :target target
    :rotation (float rotation)
    :zoom (float zoom)}))
