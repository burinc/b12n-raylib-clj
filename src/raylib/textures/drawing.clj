(ns raylib.textures.drawing
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem]
   [coffi.ffi :as ffi]))

(def draw-texture!*
  (let [primfn (ffi/make-downcall "DrawTexture" [::rs/texture ::mem/int ::mem/int ::rs/color] ::mem/void)]
    (fn [texture x y tint]
      (primfn texture (int x) (int y) tint))))

(def draw-texture!
  (ffi/make-serde-wrapper draw-texture!* [::rs/texture ::mem/int ::mem/int ::rs/color] ::mem/void))

; ...
