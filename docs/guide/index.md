# b12n-raylib-clj — Guide

User-facing documentation for `b12n-raylib-clj`: a collection of
**[raylib](https://www.raylib.com/)** game-development examples in
**Clojure**, calling raylib's C library directly via
**[coffi](https://github.com/IGJoshua/coffi)** over JDK 22+'s Foreign
Function & Memory API (Project Panama). No wrapper library, no codegen —
`coffi`'s `defcfn` binds each raylib C function directly.

## Why this exists

Same idea as [`b12n-rljlt`](https://github.com/burinc/b12n-rljlt) — a
suite of raylib examples that bind the C library directly over FFI,
with no wrapper layer in between — explored on a different runtime.
Here it's the JVM: JDK 22+'s Panama Foreign Function & Memory API via
`coffi`. `b12n-rljlt` does the same thing on Chez Scheme via jolt's
`jolt.ffi`, with no JVM at all. The pages below cover the JVM/Panama
side of that story: what `defcfn` actually does, how structs and
pointers cross the FFI boundary, and how to add a new binding.

## What b12n-raylib-clj is

A `.clj` (JVM Clojure) project:

```clojure
(require '[raylib.core.window :as rcw]
         '[raylib.core.drawing :as rcd]
         '[raylib.colors :as colors])

(rcw/init-window! 800 450 "Hello")
(loop []
  (when-not (rcw/window-should-close?)
    (rcd/begin-drawing!)
    (rcd/clear-background! colors/raywhite)
    (rcd/end-drawing!)
    (recur)))
(rcw/close-window!)
```

78 examples ship in `src/examples/` on top of the FFI bindings in
`src/raylib/` — a mix of original games and ports of official raylib C
examples across core/shapes/text/textures/shaders/audio/models
categories. See [`example-catalog.md`](example-catalog.md) for the
per-example breakdown of what's an original creation and what's
ported from which raylib C source file.

## Pages

### Orientation
- [`getting-started.md`](getting-started.md) — install JDK 22+, the
  Clojure CLI, Babashka; running examples; IDE setup
- [`architecture.md`](architecture.md) — module layout, the FFI/native
  library flow, bundled libraries

### FFI internals
- [`adding-ffi-bindings.md`](adding-ffi-bindings.md) — `defcfn`/`defalias`,
  the C-to-coffi type table, pointer in/out params, a worked example
- [`coffi-panama-internals.md`](coffi-panama-internals.md) — what
  happens under `defcfn` on the JDK Panama FFI, memory arenas, why
  JDK 22+

### Working with examples
- [`example-architecture-patterns.md`](example-architecture-patterns.md) —
  the shared example skeleton, state-as-atom, `debug-stats`/embedded
  nREPL integration, the porting recipe
- [`repl-workflow.md`](repl-workflow.md) — embedded vs standalone REPL,
  live game development
- [`example-catalog.md`](example-catalog.md) — all 78 examples, grouped
  and tabulated

### Support
- [`troubleshooting.md`](troubleshooting.md) — common errors and fixes

## See also
- [`b12n-rljlt`](https://github.com/burinc/b12n-rljlt) — the same idea
  in Jolt (native Clojure on Chez Scheme, no JVM) over `jolt.ffi`
