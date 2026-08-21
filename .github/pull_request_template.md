## What this changes

<!-- One or two sentences. If it adds an example, name it — and its upstream
     raylib source (e.g. shapes/shapes_bouncing_ball) if it's a port. -->

## Gate

- [ ] `bb check` passes — **0 errors** from clj-kondo, and all namespaces compile

<!-- Step 1 requires every namespace under src/, so it catches a broken require
     that running your own example would not. Step 2's warning count is a
     standing backlog (~69, mostly unused bindings in example code); please
     don't add to it, but you don't have to fix it either. -->

## If this adds an example

<!-- Skip this section otherwise. All four touchpoints are required, or the
     example won't run, won't be listed, or won't be compile-checked.
     See CONTRIBUTING.md § Adding an example. -->

- [ ] `src/examples/<name>.clj` — namespace docstring names the upstream C
      source if it's a port
- [ ] `:<name>` alias in `deps.edn`
- [ ] `bb <name>` task in `bb.edn`, calling `(h/run-example! "<name>")`
- [ ] Row in `bb/helpers.bb`'s `examples` registry (`:alias` `:category`
      `:title` `:desc` `:controls`)
- [ ] Ran it in a real window and it looks right

## If this adds anything to `resources/`

<!-- Skip if not. Those assets are NOT covered by the project license. -->

- [ ] Row added to `resources/LICENSE.md` with author and license
- [ ] The license is permissive (CC0 / CC-BY / zlib) — flag it in the notes
      below if it is share-alike or non-commercial

## Environment you tested on

- OS / arch (`uname -sm`):
- JDK (`java -version`):

## Notes for the reviewer

<!-- Anything surprising, any deliberate deviation, anything you're unsure about. -->
