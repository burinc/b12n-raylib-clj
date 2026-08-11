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

## Build tools comparison

This project supports both modern Clojure CLI and traditional Leiningen:

| Feature | Clojure CLI (`deps.edn`) | Leiningen (`project.clj`) |
|---------|--------------------------|---------------------------|
| Run game | `clj -M:asteroids` | `lein run -m examples.asteroids` |
| Start REPL | `clj` | `lein repl` |
| Start nREPL | `clj -M:dev` | `lein repl` |
| With Babashka | `bb asteroids` | N/A |
