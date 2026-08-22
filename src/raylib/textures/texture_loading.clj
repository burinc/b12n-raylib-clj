(ns raylib.textures.texture-loading
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem]
   [coffi.ffi :refer [defcfn]]))

(defcfn load-texture!
  "Load texture from file into GPU memory (VRAM)"
  {:arglists '([filename])}
  "LoadTexture"
  [::mem/c-string] ::rs/texture)

; ...

;; Moved from raylib-ext (2026-08-22 consolidation)
(defcfn load-render-texture!
  "Load texture for rendering (framebuffer)"
  {:arglists '([width height])}
  "LoadRenderTexture"
  [::mem/int ::mem/int] ::rs/render-texture)

(defcfn unload-render-texture!
  "Unload render texture from GPU memory (VRAM)"
  {:arglists '([target])}
  "UnloadRenderTexture"
  [::rs/render-texture] ::mem/void)

(defcfn begin-texture-mode!
  "Begin drawing to render texture"
  {:arglists '([target])}
  "BeginTextureMode"
  [::rs/render-texture] ::mem/void)

(defcfn end-texture-mode!
  "End drawing to render texture"
  "EndTextureMode"
  [] ::mem/void)

;; Advanced texture drawing

(defcfn draw-texture-pro!
  "Draw a part of a texture defined by a rectangle with 'pro' parameters"
  {:arglists '([texture source dest origin rotation tint])}
  "DrawTexturePro"
  [::rs/texture ::rs/rectangle ::rs/rectangle ::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

(defcfn draw-texture-rec!
  "Draw a part of a texture defined by a rectangle"
  {:arglists '([texture source position tint])}
  "DrawTextureRec"
  [::rs/texture ::rs/rectangle ::rs/vector-2 ::rs/color] ::mem/void)

(defcfn draw-texture-ex!
  "Draw a texture with extended parameters"
  {:arglists '([texture position rotation scale tint])}
  "DrawTextureEx"
  [::rs/texture ::rs/vector-2 ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn unload-texture!
  "Unload texture from GPU memory (VRAM)"
  {:arglists '([texture])}
  "UnloadTexture"
  [::rs/texture] ::mem/void)

(defcfn set-texture-filter!
  "Set texture scaling filter mode"
  {:arglists '([texture filter])}
  "SetTextureFilter"
  [::rs/texture ::mem/int] ::mem/void)
