# b12n-raylib-clj Guide

User-facing documentation for `b12n-raylib-clj`: a collection of
**[raylib](https://www.raylib.com/)** game-development examples in
**Clojure**, calling raylib's C library directly via
**[coffi](https://github.com/IGJoshua/coffi)** over JDK 22+'s Foreign
Function & Memory API (Project Panama). No wrapper library, no codegen:
`coffi`'s `defcfn` binds each raylib C function directly.

## Why this exists

One idea (a suite of raylib examples that reach the C library
directly, with no wrapper layer in between) explored on three Clojure
runtimes, one repo each.

This is the JVM one: JDK 22+'s Panama Foreign Function & Memory API via
`coffi`, where a binding is a `defcfn` form and a C struct arrives as a
plain Clojure map. [`b12n-raylib-jlt`](https://github.com/burinc/b12n-raylib-jlt)
does it on Chez Scheme through jolt's `jolt.ffi`, with no JVM at all.
[`b12n-raylib-jnk`](https://github.com/burinc/b12n-raylib-jnk) does it in
jank, which compiles through C++/LLVM to a native binary and so has no FFI
layer to speak of; it includes `raylib.h` and calls the C++ directly.

Reading them side by side is the interesting part: the same example, drawn
three ways, shows exactly where each runtime puts the boundary. The pages
below cover the JVM/Panama side: what `defcfn` actually does, how structs
and pointers cross, and how to add a new binding.

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
`src/raylib/`: a mix of original games and ports of official raylib C
examples across core/shapes/text/textures/shaders/audio/models
categories. See [`example-catalog.md`](example-catalog.md) for the
per-example breakdown of what's an original creation and what's
ported from which raylib C source file.

## Pages

### Orientation
- [`getting-started.md`](getting-started.md): install JDK 22+, the
  Clojure CLI, Babashka; running examples; IDE setup
- [`architecture.md`](architecture.md): module layout, the FFI/native
  library flow, bundled libraries

### FFI internals
- [`adding-ffi-bindings.md`](adding-ffi-bindings.md): `defcfn`/`defalias`,
  the C-to-coffi type table, pointer in/out params, a worked example
- [`coffi-panama-internals.md`](coffi-panama-internals.md): what
  happens under `defcfn` on the JDK Panama FFI, memory arenas, why
  JDK 22+

### Working with examples
- [`example-architecture-patterns.md`](example-architecture-patterns.md):
  the shared example skeleton, state-as-atom, `debug-stats`/embedded
  nREPL integration, the porting recipe
- [`repl-workflow.md`](repl-workflow.md): embedded vs standalone REPL,
  live game development
- [`example-catalog.md`](example-catalog.md): all 78 examples, grouped
  and tabulated
- [`demos.md`](demos.md): the full-size demo gallery (every example's
  animated GIF, one-line description)

### Support
- [`troubleshooting.md`](troubleshooting.md): common errors and fixes

## See also

The same suite on the other two Clojure runtimes:

- [`b12n-raylib-jlt`](https://github.com/burinc/b12n-raylib-jlt): in Jolt
  (native Clojure on Chez Scheme, no JVM), over `jolt.ffi`.
  [raylib-jlt.b12n.app](https://raylib-jlt.b12n.app)
- [`b12n-raylib-jnk`](https://github.com/burinc/b12n-raylib-jnk): in jank
  (native Clojure via C++/LLVM), calling raylib as ordinary C++ through
  `(:include "raylib.h")`, no FFI layer at all.
  [raylib-jnk.b12n.app](https://raylib-jnk.b12n.app)
