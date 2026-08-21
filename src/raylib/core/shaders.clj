(ns raylib.core.shaders
  "Shader loading and management functions"
  (:require
   [raylib.core]
   [coffi.mem :as mem :refer [defalias]]
   [coffi.ffi :refer [defcfn]]))

;; Shader uniform types
(def SHADER_UNIFORM_FLOAT 0)
(def SHADER_UNIFORM_VEC2 1)
(def SHADER_UNIFORM_VEC3 2)
(def SHADER_UNIFORM_VEC4 3)
(def SHADER_UNIFORM_INT 4)
(def SHADER_UNIFORM_IVEC2 5)
(def SHADER_UNIFORM_IVEC3 6)
(def SHADER_UNIFORM_IVEC4 7)
(def SHADER_UNIFORM_SAMPLER2D 8)

;; Shader location indices
(def SHADER_LOC_VERTEX_POSITION 0)
(def SHADER_LOC_VERTEX_TEXCOORD01 1)
(def SHADER_LOC_VERTEX_TEXCOORD02 2)
(def SHADER_LOC_VERTEX_NORMAL 3)
(def SHADER_LOC_VERTEX_TANGENT 4)
(def SHADER_LOC_VERTEX_COLOR 5)
(def SHADER_LOC_MATRIX_MVP 6)
(def SHADER_LOC_MATRIX_VIEW 7)
(def SHADER_LOC_MATRIX_PROJECTION 8)
(def SHADER_LOC_MATRIX_MODEL 9)
(def SHADER_LOC_MATRIX_NORMAL 10)
(def SHADER_LOC_VECTOR_VIEW 11)
(def SHADER_LOC_COLOR_DIFFUSE 12)
(def SHADER_LOC_COLOR_SPECULAR 13)
(def SHADER_LOC_COLOR_AMBIENT 14)
(def SHADER_LOC_MAP_ALBEDO 15)
(def SHADER_LOC_MAP_METALNESS 16)
(def SHADER_LOC_MAP_NORMAL 17)
(def SHADER_LOC_MAP_ROUGHNESS 18)
(def SHADER_LOC_MAP_OCCLUSION 19)
(def SHADER_LOC_MAP_EMISSION 20)
(def SHADER_LOC_MAP_HEIGHT 21)
(def SHADER_LOC_MAP_CUBEMAP 22)
(def SHADER_LOC_MAP_IRRADIANCE 23)
(def SHADER_LOC_MAP_PREFILTER 24)
(def SHADER_LOC_MAP_BRDF 25)

;; Shader struct: { unsigned int id; int *locs; }
;; On 64-bit: id (4) + padding (4) + locs pointer (8) = 16 bytes
;; We treat this as an opaque 16-byte struct to avoid alignment issues
(defalias ::shader
  [::mem/struct
   [[:id ::mem/int]
    [:_pad ::mem/int] ; padding for 8-byte alignment
    [:locs-lo ::mem/int] ; pointer as two ints
    [:locs-hi ::mem/int]]])

(defcfn load-shader
  "Load shader from files and bind default locations"
  {:arglists '([vs-filename fs-filename])}
  "LoadShader"
  [::mem/c-string ::mem/c-string] ::shader)

(defcfn unload-shader!
  "Unload shader from GPU memory (VRAM)"
  {:arglists '([shader])}
  "UnloadShader"
  [::shader] ::mem/void)

(defcfn get-shader-location
  "Get shader uniform location"
  {:arglists '([shader uniform-name])}
  "GetShaderLocation"
  [::shader ::mem/c-string] ::mem/int)

(defcfn get-shader-location-attrib
  "Get shader attribute location"
  {:arglists '([shader attrib-name])}
  "GetShaderLocationAttrib"
  [::shader ::mem/c-string] ::mem/int)

;; SetShaderValue needs special handling for different value types
;; We'll create helper functions for each type

(defcfn set-shader-value-raw!
  "Set shader uniform value (internal)"
  {:arglists '([shader loc-index value uniform-type])}
  "SetShaderValue"
  [::shader ::mem/int ::mem/pointer ::mem/int] ::mem/void)

(defn set-shader-value-float!
  "Set shader uniform float value"
  [shader loc-index value]
  (let [buf (mem/alloc 4)] ; 4 bytes for float
    (mem/write-float buf 0 (float value))
    (set-shader-value-raw! shader loc-index buf SHADER_UNIFORM_FLOAT)))

(defn set-shader-value-vec3!
  "Set shader uniform vec3 value"
  [shader loc-index [x y z]]
  (let [buf (mem/alloc 12)] ; 3 floats = 12 bytes
    (mem/write-float buf 0 (float x))
    (mem/write-float (mem/slice buf 4) 0 (float y))
    (mem/write-float (mem/slice buf 8) 0 (float z))
    (set-shader-value-raw! shader loc-index buf SHADER_UNIFORM_VEC3)))

(defn set-shader-value-vec4!
  "Set shader uniform vec4 value"
  [shader loc-index [x y z w]]
  (let [buf (mem/alloc 16)] ; 4 floats = 16 bytes
    (mem/write-float buf 0 (float x))
    (mem/write-float (mem/slice buf 4) 0 (float y))
    (mem/write-float (mem/slice buf 8) 0 (float z))
    (mem/write-float (mem/slice buf 12) 0 (float w))
    (set-shader-value-raw! shader loc-index buf SHADER_UNIFORM_VEC4)))

(defn set-shader-value-int!
  "Set shader uniform int value"
  [shader loc-index value]
  (let [buf (mem/alloc 4)] ; 4 bytes for int
    (mem/write-int buf 0 (int value))
    (set-shader-value-raw! shader loc-index buf SHADER_UNIFORM_INT)))

(defcfn begin-shader-mode!
  "Begin custom shader drawing"
  {:arglists '([shader])}
  "BeginShaderMode"
  [::shader] ::mem/void)

(defcfn end-shader-mode!
  "End custom shader drawing (use default shader)"
  {:arglists '([])}
  "EndShaderMode"
  [] ::mem/void)
