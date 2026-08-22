# Changelog

Notable changes to b12n-raylib-clj, newest first. The format follows
[babashka's changelog](https://github.com/babashka/babashka/blob/master/CHANGELOG.md):
one bullet per user-visible change, written as what a reader would
notice rather than what a commit did.

Sections are dated, not numbered. This is an example suite rather than a
released library, so "what changed, and when" is the useful question.

Examples read at <https://raylib-clj.b12n.app>.

## Unreleased

- 13 new example ports, taking the suite from 78 to **91**:
  `math-angle-rotation`, `ellipse-collision`, `camera-3d-mode`,
  `input-multitouch`, `delta-time`, `srcrec-dstrec`, `render-texture`,
  `smooth-pixelperfect`, `clock-of-clocks`, `input-gestures`,
  `camera-2d-split-screen`, `storage-values` and
  `input-virtual-controls`. The suite now groups as 32 core, 21 models,
  18 shapes, 9 games, 4 audio, 3 textures, 3 text and 1 shaders.
- `bb record:new` records only the examples that have never been
  captured; `bb record:status` reports what is missing and what has gone
  stale. Re-recording all 91 takes long enough that "just the new ones"
  is worth its own task.
- `raylib_ext` folded into the module namespaces, so an extension lives
  beside the module it extends rather than in a separate pile.
- `bb docs-sync` says which kind of deploy failure it hit — unreachable
  AWS, missing or expired credentials, a 403, or a genuinely absent
  bucket — instead of blaming missing infrastructure for all four and
  recommending `tofu:apply`. On a restricted laptop `HTTPS_PROXY` is the
  usual cause, and it now detects that and prints the unset-and-retry
  line.
- `bb docs-sync` exits non-zero when the deploy does not happen. It
  printed its complaint and exited 0 before, so nothing chaining off it
  could tell a publish from a no-op.
- Em-dashes removed from the docs.

**Known gap:** the docs still say 78 examples, and the demo gallery has
no GIF for the 13 new ports (`bb record:status` also reports 49 existing
GIFs as stale against changed sources). `bb record:new` and a docs pass
close both.

## 2026-08-21 — Public launch

Highlights:

- 78 raylib examples in JVM Clojure over
  [coffi](https://github.com/IGJoshua/coffi) and JDK 22+'s Panama Foreign
  Function & Memory API, spanning core, 3D models, shapes, games, audio,
  text, textures and shaders.
- This is the suite where the runtime does the marshalling for you. A
  binding is a single `defcfn` naming the C symbol and its types, and
  Panama builds the downcall from that description — so a by-value
  `Color` is just a `{:r 255 :g 0 :b 0 :a 255}` map, serialized from a
  `defalias` layout, rather than something hand-packed into a `:uint`.
- The seam shows where raylib mutates through a pointer: those calls
  allocate in a `confined-arena`, and the arena's lifetime is the one
  thing the abstraction will not hide from you.
- An animated GIF for every example, with a guide, an example catalog
  and a full gallery published at <https://raylib-clj.b12n.app>.

Other changes:

- Licensed EPL-2.0, matching what upstream declares. An earlier zlib
  header was a mistake and was corrected before launch; `LICENSE` is
  kept as canonical EPL-2.0 so GitHub detects it.
- `bb check` compiles every namespace rather than a subset, and the lint
  gate fails on errors rather than warnings.
- `bb docs-sync` builds, commits and publishes the site.
- CI enabled on pushes and pull requests.

## 2026-08-14

- Demo recording migrated to [screen-grab](https://github.com/burinc/b12n-screen-grab),
  replacing the repo's own hand-rolled batch capture script.

## 2026-08-11 — Documentation

- A full `docs/guide/`: getting started, architecture, adding FFI
  bindings, a coffi/Panama deep dive, example architecture patterns, the
  REPL workflow, troubleshooting, and an example catalog derived from the
  registry rather than hand-maintained.
- All 78 examples recorded as demo GIFs, with the catalog carrying
  preview thumbnails that link into a full-size gallery.
- `bb info`, a grouped cheat-sheet of the task list.

## Before 2026-08-11

The suite was built from 2026-01-08 onward, one port at a time, and
that history is in git. It predates the guide, the demo GIFs and the
published site.
