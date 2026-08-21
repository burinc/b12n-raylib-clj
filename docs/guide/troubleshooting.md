# Troubleshooting

## "Library not found" error

Make sure you're running from the project root directory where `libs/` folder exists.

## macOS security warning

Run `bb macos:sign-lib` or manually sign the library:

```bash
codesign --force --sign - libs/macos/libraylib.5.5.0.dylib
```

See [Architecture: macOS code signing](architecture.md#macos-code-signing)
for more detail.

## "No matching method" or FFI errors

Ensure you're using JDK 22 or newer:

```bash
java -version  # Should show 22.x.x or higher
```

## Window doesn't appear on macOS

The `-XstartOnFirstThread` flag is required. This is already configured in `deps.edn` and `project.clj`.

See [Coffi & Panama Internals](coffi-panama-internals.md#why-macos-needs--xstartonfirstthread) for why this flag is necessary and what it does.

## `Unrecognized option: -XstartOnFirstThread` (Linux)

```
Unrecognized option: -XstartOnFirstThread
Error: Could not create the Java Virtual Machine.
```

You ran `clojure -M:<alias>` on Linux. Every example alias in `deps.edn`
carries `-XstartOnFirstThread` because macOS requires it to run OpenGL on the
main thread, but it is a macOS-only flag, and the JVM treats any
unrecognized `-X` option as fatal rather than ignoring it.

Use `bb <name>` instead. It detects the platform and builds a flag-free
command line on Linux. The equivalent raw command, if you'd rather not
install Babashka:

```bash
clojure -J--enable-native-access=ALL-UNNAMED \
        -J-Djava.library.path=libs:libs/linux_amd64:/usr/local/lib:/usr/lib \
        -M -m examples.asteroids
```

## A GUI example misbehaves when launched with `clj`

Use `clojure`, not `clj`. `clj` wraps the same launcher in `rlwrap` for
line editing, which does not play well with a GUI app holding the main
thread. Every `bb` task here shells out to `clojure` for this reason, and
`deps.edn` carries the same note above its example aliases.

`clj` remains the better choice for a plain REPL, where the line editing is
what you want.

## `WARNING: A restricted method in java.lang.foreign.Linker has been called`

Harmless in itself, but it tells you something: **you are running without the
project's JVM flags.** Every alias in `deps.edn` passes
`--enable-native-access=ALL-UNNAMED`, which suppresses this warning entirely.
Seeing it means you invoked a bare `clojure -e ...` or a plain REPL instead.

The full block looks like this, and appears only when the flag is missing:

```
WARNING: A restricted method in java.lang.foreign.Linker has been called
WARNING: java.lang.foreign.Linker::downcallHandle has been called by
         coffi.ffi$downcall_handle in an unnamed module
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for
         callers in this module
WARNING: Restricted methods will be blocked in a future release unless
         native access is enabled
```

Nothing breaks today, but note the last line. A future JDK will *block*
these calls rather than warn, so get the flag onto your command line rather
than learning to ignore the message:

```bash
clojure -J--enable-native-access=ALL-UNNAMED -J-Djava.library.path=libs:libs/macos ...
```

## Build tools comparison

This project supports both the Clojure CLI and Leiningen:

| Feature | Clojure CLI (`deps.edn`) | Leiningen (`project.clj`) |
|---------|--------------------------|---------------------------|
| Run game (macOS) | `clojure -M:asteroids` | `lein run -m examples.asteroids` |
| Run game (any OS) | `bb asteroids` | N/A |
| Start REPL | `clj` | `lein repl` |
| Start nREPL | `bb nrepl` (or `clojure -M:dev`) | `lein repl` |

`clj` is fine for a plain REPL; the `rlwrap` line editing it adds is useful
there. It is only GUI examples that need `clojure`.
