# Changelog

Notable changes to b12n-raylib-clj, newest first. The format follows
[babashka's changelog](https://github.com/babashka/babashka/blob/master/CHANGELOG.md):
one bullet per user-visible change, written as what a reader would
notice rather than what a commit did.

Sections are dated, not numbered. This is an example suite rather than a
released library, so "what changed, and when" is the useful question.

Examples read at <https://raylib-clj.b12n.app>.

## Unreleased

- 19 new example ports, taking the suite from 78 to **97**:
  `math-angle-rotation`, `ellipse-collision`, `camera-3d-mode`,
  `input-multitouch`, `delta-time`, `srcrec-dstrec`, `render-texture`,
  `smooth-pixelperfect`, `clock-of-clocks`, `input-gestures`,
  `camera-2d-split-screen`, `storage-values`,
  `input-virtual-controls`, `penrose-tile`, `easings-testbed`,
  `undo-redo`, `bullet-hell`, `viewport-scaling` and
  `keyboard-testbed`. The suite now groups as 35 core, 21 models,
  21 shapes, 9 games, 4 audio, 3 textures, 3 text and 1 shaders.
- Three of those ports depart from their C original where the C was
  working around a constraint Clojure does not have, and each says so in
  its own docstring rather than quietly diverging. `bullet-hell` drops
  off-screen bullets instead of flagging them in a 500,000-slot array,
  which also removes the periodic mass-vanish when that array fills.
  `undo-redo` keeps its ring buffer, because the example draws the 26
  slots. `keyboard-testbed` replaces a 100-case keycode-to-label switch
  with a layout table, so a key's code, caption and width are declared
  once.
- `viewport-scaling` documents a finding about the upstream example: it
  offers six named scaling modes but only four distinct behaviours.
  `KEEP_HEIGHT_INTEGER` and `KEEP_HEIGHT` compute identical rectangles,
  as do the two `KEEP_WIDTH` variants; only `KEEP_ASPECT_INTEGER` really
  differs. Checked across 100 window and game size combinations.
- `bb record:new` records only the examples that have never been
  captured; `bb record:status` reports what is missing and what has gone
  stale. Re-recording all 97 takes long enough that "just the new ones"
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

- Every example now has a demo GIF, and the gallery, catalog and counts
  across the docs match the registry: 97 entries, 97 GIFs, no broken
  preview links or anchors. Four of the new demos gained input timelines
  in `scripts/demo_manifest.edn`, since they show nothing until something
  happens. `keyboard-testbed` steers by mouse hover rather than
  keypresses: a key lights only while held, and a synthetic press
  releases too fast to reliably land inside a captured frame.

**Known gap:** `bb record:status` reports 49 existing GIFs as stale
against changed sources. Those are the `raylib_ext` consolidation's
call-site renames, which changed no pixels, so they have deliberately
not been re-recorded.

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
