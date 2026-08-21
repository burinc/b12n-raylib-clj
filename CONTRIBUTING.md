# Contributing

Thanks for taking an interest. This is a suite of
[raylib](https://github.com/raysan5/raylib) examples written in Clojure,
calling the real `libraylib` over its C ABI through
[coffi](https://github.com/IGJoshua/coffi) and JDK 22+'s Foreign Function &
Memory API.

New examples are very welcome — the suite is deliberately mechanical to grow.

## Setting up

You need a JDK 22 or newer; everything else is optional.

```sh
java -version     # must be 22+, for the Foreign Function & Memory API
clojure --version # Clojure CLI
bb --version      # babashka — optional, but every example has a `bb <name>` task
```

You do **not** need to install raylib. Prebuilt 5.5.0 binaries for macOS,
Linux, and Windows ship under `libs/` and are selected by OS/arch at load time
(see `src/raylib/core.clj`). If you'd rather link a system raylib, put it
anywhere on the `-Djava.library.path` list in `deps.edn`.

On macOS, Gatekeeper may quarantine the bundled dylib. If a run dies on a
signature error:

```sh
codesign --force --sign - libs/macos/libraylib.5.5.0.dylib
```

## Before you open a PR

Run the gate. It is fast and opens no window:

```sh
bb check          # compile every namespace under src/, then clj-kondo
```

Step 1 requires all 104 namespaces — this is the check that catches a new
example whose requires are broken, which is the easiest mistake to make and
the one a passing `bb <your-example>` will not reveal (you only ran one).

Step 2 is clj-kondo. **It must report 0 errors.** Warnings are a different
matter: there is a standing backlog of about 69, nearly all `unused binding`
in example code. Don't feel obliged to fix them, but don't add to them either.

If clj-kondo reports an `Unresolved var` for something you know exists, that
is a bug in `.clj-kondo/hooks/raylib_ffi.clj`, not in your code — please say so
in the PR rather than working around it. That hook is what teaches clj-kondo to
see through `coffi.ffi/defcfn`; without it the suite reports 776 false
positives.

## Adding an example

One new example touches exactly four places. The full recipe — with the shared
skeleton, the naming rules, and the state-as-atom pattern — is in the guide:

**[docs/guide/example-architecture-patterns.md § Porting a new raylib C example](docs/guide/example-architecture-patterns.md#porting-a-new-raylib-c-example)**

In short:

1. `src/examples/<name>.clj` — the namespace, following the shared skeleton.
2. A `deps.edn` alias, so `clojure -M:<name>` runs it.
3. A `bb.edn` task — a one-liner calling `(h/run-example! "<name>")`.
4. A row in `bb/helpers.bb`'s `examples` registry (`:alias`, `:category`,
   `:title`, `:desc`, `:controls`). This is what `bb examples`, the task's own
   header text, and the docs catalog all read from — skip it and your example
   is invisible to every one of them.

Two conventions worth knowing before you write any code:

- **Side-effecting functions end in `!`, predicates in `?`.** The binding layer
  is consistent about this and the examples read much better for it.
- **Write against `src/raylib/`, not raw coffi.** Add a new `defcfn` there only
  if your example genuinely needs a raylib call nothing else uses — and put it
  in the namespace matching raylib's own module split (`core/`, `shapes/`,
  `text/`, `textures/`).

## Adding an FFI binding

If you're touching the binding layer rather than adding an example, read
[`docs/guide/adding-ffi-bindings.md`](docs/guide/adding-ffi-bindings.md) first.
It has the C-to-coffi type table and, importantly, the recipe for pointer
in/out parameters — raylib passes several structs that way and the arena
handling is not obvious.

[`docs/guide/coffi-panama-internals.md`](docs/guide/coffi-panama-internals.md)
covers what actually happens under `defcfn`, if you want the layer below that.

## Demo GIFs

You don't need to record anything. Every GIF under `docs/demos/` is committed.
`bb record` drives a screen-capture tool that is not publicly released, so it
is maintainer-only — the task says so and exits cleanly rather than failing
obscurely. If your example would look better with a specific input sequence in
its demo, add an `:overrides` entry for it in `scripts/demo_manifest.edn` and
mention it in your PR; a maintainer will record it.

## Licensing

This project is released under the zlib/libpng license — the same license as
raylib itself. By contributing, you agree your contribution is licensed under
those terms.

If your example is a port of an upstream raylib example, name the original in
its namespace docstring (`Based on: shapes/shapes_bouncing_ball.c`) so the
attribution stays traceable — every ported example here does this already.

**Don't add media to `resources/`** without flagging it. Those assets are not
covered by this project's license and each carries its own terms; see
[`resources/LICENSE.md`](resources/LICENSE.md). A new asset needs a row there,
and anything non-commercial or share-alike needs discussion first.
