# Getting Started

## Prerequisites

- **JDK 22 or newer** — required for the Foreign Function API (see
  [Coffi & Panama Internals](coffi-panama-internals.md) for why JDK 22+
  specifically)
- **Clojure CLI** (recommended) or Leiningen
- **Babashka** (optional, for task automation)

## Installing JDK 22+

Clojure runs on the JVM, so you need Java installed. This project requires JDK 22 or later because we use the new Foreign Function API to call native code.

On macOS with Homebrew:

```bash
brew install openjdk@22
```

On Linux (Ubuntu/Debian):

```bash
sudo apt install openjdk-22-jdk
```

Alternatively, you can use SDKMAN which works on macOS, Linux, and Windows (WSL):

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 22.0.2-open
```

## Installing the Clojure CLI

The Clojure CLI is the modern way to run Clojure projects using `deps.edn`.

On macOS with Homebrew:

```bash
brew install clojure/tools/clojure
```

On Linux:

```bash
curl -L -O https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh
chmod +x linux-install.sh
sudo ./linux-install.sh
```

Verify installation:

```bash
clj --version
```

## Installing Leiningen

If you prefer Leiningen over the Clojure CLI:

On macOS with Homebrew:

```bash
brew install leiningen
```

On Linux:

```bash
curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
chmod +x lein
sudo mv lein /usr/local/bin/
lein  # This will download the rest automatically
```

## Installing Babashka

Babashka provides fast task automation for Clojure projects.

On macOS with Homebrew:

```bash
brew install borkdude/brew/babashka
```

On Linux:

```bash
bash < <(curl -s https://raw.githubusercontent.com/babashka/babashka/master/install)
```

Verify installation:

```bash
bb --version
```

## Running examples

Clone this repository:

```bash
git clone https://github.com/burinc/b12n-raylib-clj.git
cd b12n-raylib-clj
```

### `bb <example>` (recommended)

If you have Babashka installed, running games is simple:

```bash
bb help              # Show all available commands
bb asteroids         # Run Asteroids game
bb tetris            # Run Tetris game
```

### Running by alias (macOS only)

```bash
clojure -M:asteroids     # Run Asteroids
clojure -M:tetris        # Run Tetris
clojure -M:pong          # Run Pong
clojure -M:hello-world   # Run Hello World
```

Two things to know about this path:

**Use `clojure`, not `clj`.** `clj` wraps the same launcher in `rlwrap` for
line editing, which interferes with a GUI app's event loop. Every `bb` task
here shells out to `clojure` for exactly this reason.

**These aliases only work on macOS.** Every example alias in `deps.edn`
carries `-XstartOnFirstThread`, which macOS requires to run OpenGL on the
main thread. It is a macOS-only flag, and the JVM treats an unrecognized
`-X` option as fatal — so on Linux the same command dies before it starts:

```
Unrecognized option: -XstartOnFirstThread
Error: Could not create the Java Virtual Machine.
```

On Linux, use `bb <name>` (below), which builds a flag-free command line for
you. If you'd rather not install Babashka, that command is:

```bash
clojure -J--enable-native-access=ALL-UNNAMED \
        -J-Djava.library.path=libs:libs/linux_amd64:/usr/local/lib:/usr/lib \
        -M -m examples.asteroids
```

### `lein run -m examples.<ns>`

```bash
lein run                        # Run default (Asteroids)
lein run -m examples.tetris     # Run Tetris
```

## Setting JAVA_HOME

If you have multiple Java versions installed, you may need to set JAVA_HOME:

```bash
export JAVA_HOME=/path/to/jdk-22
```

On macOS with Homebrew:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@22
```

## IDE Setup

Clojure development is best experienced with a good editor that supports REPL integration.

### VS Code with Calva

1. Install VS Code
2. Install the "Calva" extension
3. Open this project folder
4. Press `Ctrl+Alt+C` then `Ctrl+Alt+J` (or `Cmd` on macOS) to start a REPL
5. Select "deps.edn" when prompted

Calva provides syntax highlighting, inline evaluation, and a connected REPL. Evaluate code by placing your cursor on an expression and pressing `Ctrl+Enter`.

### IntelliJ IDEA with Cursive

1. Install IntelliJ IDEA (Community or Ultimate)
2. Install the "Cursive" plugin
3. Open this project folder
4. Cursive will detect `deps.edn` and set everything up

To start a REPL, right-click on `deps.edn` and select "Run REPL".

### Connecting to nREPL

**For live game development (recommended):**

```bash
bb asteroids   # Starts game + nREPL on port 7888
```

Then connect your editor to `localhost:7888`.

**For standalone REPL (non-GUI work):**

```bash
bb nrepl   # Starts nREPL on port 7999
```

Then connect your editor to `localhost:7999`.

See [REPL Workflow](repl-workflow.md) for the full live-development workflow, including hot-reloading running games from the connected REPL.
