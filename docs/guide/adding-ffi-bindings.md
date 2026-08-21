# Adding a new FFI binding

## The `defcfn` pattern

Every raylib binding is a `defcfn` form. Here are three real ones from
[`src/raylib/core/window.clj`](../../src/raylib/core/window.clj) lines 1-21:

```clojure
(ns raylib.core.window
  (:require
   [raylib.core]
   [raylib.internals :as ri]
   [coffi.mem :as mem]
   [coffi.ffi :refer [defcfn]]))

(defcfn init-window!
  "Initialize window and OpenGL context"
  {:arglists '([width height title])}
  "InitWindow"
  [::mem/int ::mem/int ::mem/c-string] ::mem/void)

(defcfn window-should-close?
  "Check if KEY_ESCAPE pressed or Close icon pressed"
  "WindowShouldClose"
  [] ::ri/bool)

(defcfn close-window!
  "Close window and unload OpenGL context"
  "CloseWindow" [] ::mem/void)
```

`defcfn` (from `coffi.ffi`) always has the same four-part shape:

1. **Docstring**: describes what the C function does. Copy it from
   `raylib.h`'s comment on the same line as the function signature.
2. **`{:arglists '(...)}`**: optional. Only needed when the function
   takes arguments, since `defcfn`'s own parameter vector is a list of
   *types*, not names; `:arglists` is what makes `(doc init-window!)`
   and editor autocomplete show meaningful argument names instead of
   `[arg0 arg1 arg2]`. `window-should-close?` and `close-window!` both
   take no arguments, so they skip it.
3. **C function name string**: the exact symbol raylib exports (e.g.
   `"InitWindow"`), used to look up the function in the shared
   library.
4. **`[param-types] return-type`**: the parameter types vector
   followed by the return type, both from the type-mapping table
   below.

Every binding namespace requires `raylib.core` first (see the `:require`
above); that namespace is what loads the native `libraylib` shared
library, and a `defcfn` can't resolve its C symbol until the library is
loaded.

## C -> coffi type mapping

| C Type | Coffi Type |
|--------|------------|
| `int` | `::mem/int` |
| `float` | `::mem/float` |
| `double` | `::mem/double` |
| `bool` | `::ri/bool` (returns 0/1) |
| `unsigned char` | `::ri/ubyte` |
| `const char*` | `::mem/c-string` |
| `void` | `::mem/void` |
| `Color` | `::rs/color` |
| `Vector2` | `::rs/vector-2` |
| `Vector3` | `::rs/vector-3` |
| `Rectangle` | `::rs/rectangle` |
| `Camera2D` | `::rc2d/camera-2d` |
| `Camera3D` | `::rc3d/camera3d` |
| Pointer (in/out param) | `::mem/pointer` |

`::ri/bool` and `::ri/ubyte` are **not** built-in coffi types; coffi
only ships primitive types like `::mem/int` and `::mem/byte` out of the
box. They're custom types this project defines in
[`src/raylib/internals.clj`](../../src/raylib/internals.clj):

```clojure
(ns raylib.internals
  (:require [coffi.mem :as mem]))

;; ubyte
(defmethod mem/primitive-type ::ubyte
  [_type]
  ::mem/byte)

(defmethod mem/serialize* ::ubyte
  [obj _type _scope]
  (unchecked-byte obj))

(defmethod mem/deserialize* ::ubyte
  [obj _type]
  (Byte/toUnsignedLong obj))

;; bool
(defmethod mem/primitive-type ::bool
  [_type]
  ::mem/byte)

(defmethod mem/serialize* ::bool
  [obj _type _scope]
  (byte (if obj 1 0)))

(defmethod mem/deserialize* ::bool
  [obj _type]
  (not (zero? obj)))
```

Raylib's C `bool` and `unsigned char` both travel over the FFI boundary
as a single byte; that's just how they're laid out in memory. coffi's
`mem/primitive-type` multimethod tells coffi which real primitive
(`::mem/byte`) to use on the wire for a custom type. `mem/serialize*`
and `mem/deserialize*` then teach coffi how to box and unbox that raw
byte into something Clojure-friendly: `::ubyte` deserializes to an
unsigned `Byte/toUnsignedLong` value (so a byte like `-1` reads back
as `255`, not `-1`), and `::bool` serializes a Clojure truthy/falsy
value to `1`/`0` and deserializes `0`/non-`0` back to `false`/`true`.
Without these three multimethod overrides, `::ri/bool` and `::ri/ubyte`
wouldn't exist as usable coffi types at all.

## Struct definitions with `defalias`

C structs are defined with `defalias` in
[`src/raylib/structs.clj`](../../src/raylib/structs.clj). The full list
at time of writing (`grep -n "defalias" src/raylib/structs.clj`):
Color, Vector2, Vector3, Vector4, Texture, RenderTexture, Rectangle.

```clojure
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
```

Once defined, a struct behaves as a plain Clojure map:
`{:x 100.0 :y 200.0}` for a `::vector-2`, `{:r 255 :g 0 :b 0 :a 255}`
for a `::color`.

**Field order must match the C struct layout exactly.** coffi lays out
the native memory segment for a `::mem/struct` field-by-field, in the
order you list them; it has no way to know raylib's real field order
except from what you tell it. If `raylib.h` declares `Color` as
`r, g, b, a` and you write the `defalias` fields in a different order,
every read and write against that struct silently misaligns.

## Pointer in/out parameters

Some raylib functions take a struct **pointer** and mutate it in place
rather than returning a new struct: `UpdateCamera(Camera3D *camera, int mode)`
is one. The worked example for this pattern is `update-camera` in
[`src/raylib/core/camera3d.clj`](../../src/raylib/core/camera3d.clj):

```clojure
;; Camera update function
(defcfn update-camera!
  "Update camera position for selected mode"
  {:arglists '([camera mode])}
  "UpdateCamera"
  [::mem/pointer ::mem/int] ::mem/void)

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
```

The raw `defcfn` (`update-camera!`) takes `::mem/pointer`; coffi
can't serialize a Clojure map directly as a pointer argument, so the
wrapper function (`update-camera`) does the pointer dance by hand:

1. **`mem/confined-arena`**: creates an arena that owns the lifetime
   of any native memory allocated from it, scoped to this block.
2. **`mem/alloc-instance`**: allocates a native memory segment inside
   that arena, sized and laid out for the `::camera3d` struct.
3. **`mem/serialize-into`**: writes the Clojure `camera` map's fields
   into that segment, following the struct's field layout.
4. **`update-camera!`**: the raw `defcfn` call passes the segment as
   the pointer argument; raylib's `UpdateCamera` mutates the segment's
   bytes in place.
5. **`mem/deserialize-from`**: reads the (now-mutated) segment back
   out into a fresh Clojure map, which becomes the wrapper's return
   value.

See [Coffi & Panama Internals](coffi-panama-internals.md#memory-arenas)
for what a confined arena actually is and when you need one at all.

## Worked example: adding a new raylib function end-to-end

`DrawRectangleGradientV` is a real raylib C function that isn't bound
anywhere in this repo yet, confirmed with:

```bash
grep -rn "DrawRectangleGradientV" src/raylib/
```

which returns nothing. It's a good pick for a worked example: a plain
draw call with two `Color` arguments and no pointer trickery.

1. **Find the C signature in `raylib.h`:**

   ```c
   void DrawRectangleGradientV(int posX, int posY, int width, int height, Color color1, Color color2);
   ```

2. **Pick the target namespace.** This is a shape-drawing call, so it
   belongs in [`src/raylib/shapes/basic.clj`](../../src/raylib/shapes/basic.clj)
   (confirmed to exist) alongside its sibling `draw-rectangle!`, which
   already uses the same `int int int int Color` argument shape.

3. **Write the `defcfn` form**, following the type-mapping table above:

   ```clojure
   (defcfn draw-rectangle-gradient-v!
     "Draw a vertical-gradient-filled rectangle"
     {:arglists '([pos-x pos-y width height color-1 color-2])}
     "DrawRectangleGradientV"
     [::mem/int ::mem/int ::mem/int ::mem/int ::rs/color ::rs/color] ::mem/void)
   ```

4. **Verify it.** Start any example (`bb basic-shapes`, which already
   connects an embedded nREPL on port 7888), connect your editor, and
   `clj-nrepl-eval` a call like
   `(raylib.shapes.basic/draw-rectangle-gradient-v! 100 100 200 100 colors/red colors/blue)`
   inside the running game's draw loop to confirm it renders instead
   of throwing.

## See also
- [`coffi-panama-internals.md`](coffi-panama-internals.md): what
  happens underneath `defcfn` when Clojure calls the resulting function
