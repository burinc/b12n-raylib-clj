# Example architecture patterns

## The shared skeleton

Most examples follow the same shape: start the embedded nREPL, open a
window, loop until the user closes it, clean up (86 of 97; 11
examples, including `pong`, `camera-2d`, and `music-stream`, skip the
embedded nREPL; `grep -rL "nrepl/start" src/examples/*.clj
src/examples/*/*.clj` lists them). Here's
[`src/examples/asteroids.clj`](../../src/examples/asteroids.clj)'s
`-main` (around line 523), verbatim:

```clojure
(defn -main [& args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick (update-fps @game-atom))]
      (when-not (rcw/window-should-close?)
        (reset! game-atom game)
        (draw game)
        (recur))))
  ;; Cleanup
  (when @render-target
    (ext/unload-render-texture! @render-target))
  (rcw/close-window!))
```

`nrepl/start` runs first, before the window even opens, so you can
connect a REPL to a game that's still starting up. `(init)` does the
one-time setup (`init-window!`, config flags, and, in asteroids'
case, allocating the letterboxed render texture and calling
`debug-stats/enable!`). Then the loop: compute the next game state
(`tick`), check `window-should-close?`, and, while the window is
still open, commit the new state to `game-atom` and draw the frame,
before recurring. When the loop exits (the user closed the window),
asteroids releases its render texture and calls `close-window!`.

Most examples are a variation on this shape: start nREPL once, init
the window once, loop `update -> draw -> check-close` until the window
closes, then clean up. Simpler examples skip the parts specific to
asteroids (the render texture, the letterboxing) but follow the same
overall skeleton, except for the 11 examples noted above, which skip
the nREPL step entirely.

## State as an atom

Asteroids keeps its entire game state in one atom,
[`game-atom`](../../src/examples/asteroids.clj), seeded from
`initial-state`:

```clojure
(defn initial-state []
  {:dt 0
   :time (System/nanoTime)
   :time-acc [1]
   :frame-counter -1
   :screen :title
   :ship (make-ship WIDTH HEIGHT)
   :asteroids (map (fn [_] (make-asteroid)) (range INITIAL_ASTEROIDS))
   :bullets []
   :alive true})

(def game-atom (atom (initial-state)))
```

Ship, asteroids, bullets, and the current screen all live in this one
map. The `-main` loop above reads it, computes a new value with
`tick`, and `reset!`s it back; the atom is the single source of
truth for "what's happening right now."

The functions that compute the *next* state are pure (deterministic,
no game-state mutation) even where they lean on an FFI call
underneath. `vector-add` and `check-point-circle` are two the README
calls out as testable straight from a standalone REPL:

```clojure
(defn vector-add [v1 v2]
  [(+ (v1 0) (v2 0))
   (+ (v1 1) (v2 1))])
```

`vector-add` is plain Clojure arithmetic; `check-point-circle`
delegates its actual geometry to `ext/check-collision-point-circle?`
(an FFI-backed call) but is still deterministic and doesn't touch
`game-atom` or draw anything; you can call either at a REPL with
made-up arguments and get the same answer every time. The *draw*
phase is the opposite: `draw` calls
`rcd/begin-drawing!`, a sequence of raylib draw calls, and
`rcd/end-drawing!`; every one of those is a side effect (it writes
pixels to the screen), and calling `draw` twice with the same game
state does not give you back a value to compare, it paints a frame.
Keeping the state-update functions pure is what makes them REPL- and
test-friendly; the draw phase can't be, because rendering is
inherently a side effect.

## Plugging in `debug-stats`

[`src/debug_stats.clj`](../../src/debug_stats.clj) is an optional F1
overlay plugin. Its own docstring is the usage guide, verbatim:

```
Debug stats overlay plugin.

Usage:
1. Require this namespace in your game ns
2. Call (debug-stats/enable!) once at startup
3. Call (debug-stats/update!) in your game tick function
4. Call (debug-stats/draw!) at the end of your draw function (inside begin/end-drawing)
5. Press F1 to toggle the stats overlay

Example:
(ns my-game
  (:require [debug-stats]))

(defn init []
  (debug-stats/enable!))

(defn tick [game]
  (debug-stats/update!)
  ;; ... your game logic
  )

(defn draw [game]
  (rcd/begin-drawing!)
  ;; ... your drawing code
  (debug-stats/draw!)
  (rcd/end-drawing!))
```

`asteroids.clj` follows this exactly: `(debug-stats/enable!)` at the
end of `init`, `(debug-stats/update!)` in its tick function, and
`(debug-stats/draw!)` as the last call inside each
`begin-drawing!`/`end-drawing!` pair.

## Plugging in the embedded nREPL

[`src/raylib/nrepl.clj`](../../src/raylib/nrepl.clj) wraps
`nrepl.server/start-server`:

```clojure
(defn start
  "Start a network repl for debugging on specified port followed by
  an optional parameters map. The :bind, :transport-fn, :handler,
  :ack-port and :greeting-fn will be forwarded to
  nrepl.server/start-server as they are.

  If the port is already in use, logs a warning and returns nil
  instead of throwing - this allows games to still run when another
  nREPL server is already using the port."
  [{:keys [port bind transport-fn handler ack-port greeting-fn]}]
  (try
    (log/info "starting nREPL server on port" port)
    (nrepl/start-server :port port
                        :bind bind
                        :transport-fn transport-fn
                        :handler handler
                        :ack-port ack-port
                        :greeting-fn greeting-fn)

    (catch java.net.BindException e
      (log/warn (str "nREPL port " port " already in use - continuing without embedded nREPL. "
                     "You can connect to the existing nREPL server if one is running."))
      nil)
    (catch Throwable t
      (log/error t "failed to start nREPL")
      (throw t))))
```

Called once in `-main` as `(nrepl/start {:port 7888})`. The
`BindException` catch is what makes port 7888 safe to reuse: if
another example (or another instance of the same one) is already
listening there, `start` logs a warning and returns `nil` instead of
crashing; the second game still runs, it just doesn't get its own
nREPL server. Any other exception during startup is logged and
re-thrown.

## Porting a new raylib C example

The recipe, as a numbered list:

1. Find the C source in raylib's
   [`examples/`](https://github.com/raysan5/raylib/tree/master/examples)
   tree.
2. Create `src/examples/<name>.clj` following the shared skeleton
   above.
3. Add a `deps.edn` alias, mirroring any existing one:

   ```clojure
   :my-example
   {:jvm-opts ["--enable-native-access=ALL-UNNAMED"
               "-XstartOnFirstThread"
               "-Djava.library.path=libs:libs/macos:..."]
    :main-opts ["-m" "examples.my-example"]}
   ```

4. Add a `bb.edn` task. Every task calls the shared `h/run-example!`
   helper, which looks up the example's title, description, and
    controls from the registry (step 5) and prints them itself, so the
   task body stays a single line. The real `asteroids` task:

   ```clojure
   asteroids {:doc "🎮 Asteroids - shoot asteroids and survive"
              :task (h/run-example! "asteroids")}
   ```

   Because `run-example!` looks the example up by alias, this task
   only prints the right header/controls text once the registry entry
   in step 5 exists.
5. Add the example's entry to
    [`bb/helpers.bb`](../../bb/helpers.bb)'s `examples` registry; this
   is what makes `bb examples`, `run-example!`'s header text, and this
   guide's own `example-catalog.md` pick it up. One real entry, as the
   shape to copy:

   ```clojure
   {:alias "asteroids"
    :category :games
    :title "Asteroids"
    :desc "Shoot asteroids"
    :controls "Arrows, Space"}
   ```

## See also
- [`example-catalog.md`](example-catalog.md): every example this
  pattern produced, in one table
