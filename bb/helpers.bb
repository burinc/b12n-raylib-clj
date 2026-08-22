(ns bb.helpers
  "Shared helper functions for bb tasks"
  (:require [babashka.process :as p]
            [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]))

;; =============================================================================
;; Terminal Colors
;; =============================================================================

(def bold "\033[1m")
(def green "\033[0;32m")
(def yellow "\033[1;33m")
(def red "\033[0;31m")
(def cyan "\033[0;36m")
(def magenta "\033[0;35m")
(def blue "\033[0;34m")
(def reset "\033[0m")

(defn color [c text] (str c text reset))

;; =============================================================================
;; Output Helpers
;; =============================================================================

(defn step [n text] (println (color yellow (str "Step " n ":")) text))
(defn success [text] (println (color green (str "✅ " text))))
(defn error-msg [text] (println (color red (str "❌ " text))))
(defn info [text] (println (color cyan (str "ℹ️  " text))))
(defn warn [text] (println (color yellow (str "⚠️  " text))))

(defn header [text]
  (println)
  (println (color bold text))
  (println (apply str (repeat (count text) "="))))

(defn section [emoji title]
  (println)
  (println (color magenta (str emoji " " title))))

;; =============================================================================
;; Example Registry - Single Source of Truth
;; =============================================================================

(def example-categories
  "Categories with their emoji and descriptions"
  {:games {:emoji "🎮"
           :title "Original Games"}
   :core {:emoji "📦"
          :title "Core Examples"}
   :shapes {:emoji "🔷"
            :title "Shapes Examples"}
   :textures {:emoji "🖼️"
              :title "Textures Examples"}
   :audio {:emoji "🔊"
           :title "Audio Examples"}
   :shaders {:emoji "✨"
             :title "Shaders Examples"}
   :models {:emoji "🗿"
            :title "Models Examples"}
   :text {:emoji "📝"
          :title "Text Examples"}})

(def examples
  "Registry of all examples with metadata."
  [;; Original Games
   {:alias "hello-world"
    :category :games
    :title "Hello World"
    :desc "Basic window test"
    :controls "Q to exit, F1 for debug stats"}
   {:alias "pong"
    :category :games
    :title "Pong"
    :desc "Two-player paddle game"
    :controls "W/S, K/J, Enter"}
   {:alias "asteroids"
    :category :games
    :title "Asteroids"
    :desc "Shoot asteroids"
    :controls "Arrows, Space"}
   {:alias "asteroids2"
    :category :games
    :title "Asteroids 2"
    :desc "Alternate version"
    :controls "Arrows, Space"}
   {:alias "tetris"
    :category :games
    :title "Tetris"
    :desc "Block-stacking puzzle"
    :controls "Arrows, Space"}
   {:alias "vampire-survivors"
    :category :games
    :title "Vampire Survivors"
    :desc "Survival action"
    :controls "WASD"}

   ;; Core Examples
   {:alias "bouncing-ball"
    :category :core
    :title "Bouncing Ball"
    :desc "Physics demo"
    :controls "SPACE, G"}
   {:alias "following-eyes"
    :category :core
    :title "Following Eyes"
    :desc "Mouse tracking"
    :controls "Move mouse"}
   {:alias "screen-manager"
    :category :core
    :title "Screen Manager"
    :desc "State machine"
    :controls "ENTER"}
   {:alias "input-keys"
    :category :core
    :title "Input Keys"
    :desc "Keyboard input"
    :controls "Arrow keys"}
   {:alias "input-mouse"
    :category :core
    :title "Input Mouse"
    :desc "Mouse input"
    :controls "Click, move"}
   {:alias "input-gamepad"
    :category :core
    :title "Input Gamepad"
    :desc "Gamepad demo"
    :controls "Connect gamepad"}
   {:alias "mouse-wheel"
    :category :core
    :title "Mouse Wheel"
    :desc "Scroll input"
    :controls "Mouse wheel"}
   {:alias "gestures-testbed"
    :category :core
    :title "Gestures Testbed"
    :desc "Touch gestures"
    :controls "Touch/click"}
   {:alias "scissor-test"
    :category :core
    :title "Scissor Test"
    :desc "Scissor clipping"
    :controls "S, Mouse"}
   {:alias "random-values"
    :category :core
    :title "Random Values"
    :desc "Random numbers"
    :controls "Watch"}
   {:alias "camera-2d"
    :category :core
    :title "Camera 2D"
    :desc "2D camera"
    :controls "Arrows, A/S, Wheel"}
   {:alias "camera-3d-free"
    :category :core
    :title "Camera 3D Free"
    :desc "Free 3D camera"
    :controls "Mouse, Wheel"}
   {:alias "split-screen-3d"
    :category :core
    :title "Split Screen 3D"
    :desc "Two-player 3D"
    :controls "W/S, UP/DOWN"}
   {:alias "first-person-3d"
    :category :core
    :title "First Person 3D"
    :desc "FPS camera"
    :controls "WASD, Mouse, 1-4"}
   {:alias "camera-fps"
    :category :core
    :title "Camera FPS"
    :desc "FPS with physics"
    :controls "WASD, Space, Ctrl"}
   {:alias "world-screen"
    :category :core
    :title "World Screen"
    :desc "3D to 2D coords"
    :controls "Mouse, Wheel"}
   {:alias "picking-3d"
    :category :core
    :title "Picking 3D"
    :desc "Ray casting"
    :controls "Click"}
   {:alias "collision-area"
    :category :core
    :title "Collision Area"
    :desc "Collision detection"
    :controls "Mouse, SPACE"}
   {:alias "colors-palette"
    :category :core
    :title "Colors Palette"
    :desc "Color showcase"
    :controls "Hover, SPACE"}
   {:alias "logo-anim"
    :category :core
    :title "Logo Animation"
    :desc "Logo animation"
    :controls "R to replay"}

   ;; Textures
   {:alias "background-scrolling"
    :category :textures
    :title "Background Scrolling"
    :desc "Parallax demo"
    :controls "Watch"}
   {:alias "sprite-animation"
    :category :textures
    :title "Sprite Animation"
    :desc "Spritesheet"
    :controls "LEFT/RIGHT"}

   ;; Audio
   {:alias "audio-module"
    :category :audio
    :title "Audio Module"
    :desc "Music visualization"
    :controls "SPACE, P, Arrows"}
   {:alias "sound-loading"
    :category :audio
    :title "Sound Loading"
    :desc "WAV/OGG playback"
    :controls "SPACE, ENTER"}
   {:alias "music-stream"
    :category :audio
    :title "Music Stream"
    :desc "MP3 streaming"
    :controls "SPACE, P, Arrows"}
   {:alias "sound-multi"
    :category :audio
    :title "Sound Multi"
    :desc "Multiple sounds"
    :controls "SPACE"}

   ;; Shaders
   {:alias "basic-lighting"
    :category :shaders
    :title "Basic Lighting"
    :desc "Dynamic lighting"
    :controls "Mouse, Y/R/G/B"}

   ;; Models
   {:alias "geometric-shapes"
    :category :models
    :title "Geometric Shapes"
    :desc "3D primitives"
    :controls "Q to exit"}
   {:alias "waving-cubes"
    :category :models
    :title "Waving Cubes"
    :desc "Animated cube wave"
    :controls "Q to exit"}
   {:alias "box-collisions"
    :category :models
    :title "Box Collisions"
    :desc "3D collision detection"
    :controls "Arrow keys, Q"}
   {:alias "orthographic-projection"
    :category :models
    :title "Orthographic Projection"
    :desc "Perspective vs orthographic"
    :controls "SPACE, Q"}
   {:alias "tesseract-view"
    :category :models
    :title "Tesseract View"
    :desc "4D hypercube"
    :controls "Q to exit"}
   {:alias "solar-system"
    :category :models
    :title "Solar System"
    :desc "Orbiting planets"
    :controls "Q to exit"}
   {:alias "spinning-cubes"
    :category :models
    :title "Spinning Cubes"
    :desc "Color-cycling cubes"
    :controls "Q to exit"}
   {:alias "point-cloud"
    :category :models
    :title "Point Cloud"
    :desc "Spherical points"
    :controls "UP/DOWN, Q"}
   {:alias "wireframe-shapes"
    :category :models
    :title "Wireframe Shapes"
    :desc "Custom wireframes"
    :controls "SPACE, Q"}
   {:alias "camera-modes"
    :category :models
    :title "Camera Modes"
    :desc "Free/Orbital/FPS cameras"
    :controls "1/2/3, WASD, Q"}
   {:alias "ray-picking"
    :category :models
    :title "Ray Picking"
    :desc "Click to select cubes"
    :controls "Click, Right-click, Q"}
   {:alias "bouncing-spheres"
    :category :models
    :title "Bouncing Spheres"
    :desc "Physics in 3D box"
    :controls "SPACE, R, G, Q"}
   {:alias "rotating-cube"
    :category :models
    :title "Rotating Cube"
    :desc "3D rotation"
    :controls "Arrows, +/-, R, Q"}
   {:alias "particle-system"
    :category :models
    :title "Particle System"
    :desc "3D particles"
    :controls "SPACE, G, W, R, Q"}
   {:alias "dna-helix"
    :category :models
    :title "DNA Helix"
    :desc "Double helix"
    :controls "Arrows, SPACE, R, Q"}
   {:alias "first-person-maze"
    :category :models
    :title "First Person Maze"
    :desc "Navigate 3D maze"
    :controls "WASD, Mouse, R, M, Q"}
   {:alias "yaw-pitch-roll"
    :category :models
    :title "Yaw Pitch Roll"
    :desc "3D rotation demo"
    :controls "Arrows, SPACE, R, Q"}
   {:alias "lissajous-3d"
    :category :models
    :title "Lissajous 3D"
    :desc "Parametric curves"
    :controls "1-5, Arrows, W/S, SPACE, Q"}
   {:alias "lorenz-attractor"
    :category :models
    :title "Lorenz Attractor"
    :desc "Chaos theory"
    :controls "1-3, Arrows, SPACE, R, Q"}
   {:alias "terrain-generation"
    :category :models
    :title "Terrain Generation"
    :desc "Procedural terrain"
    :controls "1-3, Arrows, G, W, SPACE, Q"}
   {:alias "mesh-generation"
    :category :models
    :title "Mesh Generation"
    :desc "Procedural 3D shapes"
    :controls "Left/Right, Click, SPACE, R, Q"}
   ;; Classic Games
   {:alias "snake"
    :category :games
    :title "Snake"
    :desc "Classic snake game"
    :controls "Arrows, P, ENTER, Q"}
   {:alias "floppy"
    :category :games
    :title "Floppy"
    :desc "Flappy bird clone"
    :controls "SPACE, P, ENTER, Q"}
   {:alias "retro-maze-3d"
    :category :games
    :title "Retro Maze 3D"
    :desc "GameBoy-style maze escape"
    :controls "WASD, Mouse, SPACE, M, ENTER, Q"}

   ;; Shapes
   {:alias "logo-raylib"
    :category :shapes
    :title "Logo Raylib"
    :desc "Raylib logo drawn with shapes"
    :controls "ESC to exit"}
   {:alias "logo-raylib-anim"
    :category :shapes
    :title "Logo Raylib Anim"
    :desc "Animated logo construction"
    :controls "R to replay"}
   {:alias "basic-shapes"
    :category :shapes
    :title "Basic Shapes"
    :desc "Circles, rectangles, triangles, polygons"
    :controls "ESC to exit"}
   {:alias "rectangle-scaling"
    :category :shapes
    :title "Rectangle Scaling"
    :desc "Drag to resize rectangle"
    :controls "Drag bottom-right corner"}
   {:alias "mouse-trail"
    :category :shapes
    :title "Mouse Trail"
    :desc "Circles following mouse cursor"
    :controls "Move mouse"}
   {:alias "lines-bezier"
    :category :shapes
    :title "Lines Bezier"
    :desc "Interactive bezier curve"
    :controls "Drag endpoints"}
   {:alias "easings-ball"
    :category :shapes
    :title "Easings Ball"
    :desc "Easing function animation"
    :controls "ENTER to replay"}
   {:alias "ball-physics"
    :category :shapes
    :title "Ball Physics"
    :desc "Grab and throw balls"
    :controls "Click, Right-click, Wheel, Middle"}
   {:alias "simple-particles"
    :category :shapes
    :title "Simple Particles"
    :desc "Water/smoke/fire effects"
    :controls "Arrows, Click"}
   {:alias "dashed-line"
    :category :shapes
    :title "Dashed Line"
    :desc "Interactive dashed line"
    :controls "Arrows, C"}
   {:alias "starfield-effect"
    :category :shapes
    :title "Starfield Effect"
    :desc "3D starfield simulation"
    :controls "SPACE, Wheel"}
   {:alias "easings-box"
    :category :shapes
    :title "Easings Box"
    :desc "Box animation with easing functions"
    :controls "SPACE to reset"}
   {:alias "double-pendulum"
    :category :shapes
    :title "Double Pendulum"
    :desc "Chaotic pendulum simulation"
    :controls "ESC to exit"}
   {:alias "penrose-tile"
    :category :shapes
    :title "Penrose Tile"
    :desc "L-system Penrose tiling drawn by turtle"
    :controls "UP/DOWN change generations, Q to exit"}
   {:alias "bullet-hell"
    :category :shapes
    :title "Bullet Hell"
    :desc "Throughput test firing rows of bullets from a rotating circle"
    :controls "RIGHT/LEFT rows, UP/DOWN speed, Z/X cooldown, SPACE angle, ENTER draw method, C clear"}
   {:alias "viewport-scaling"
    :category :core
    :title "Viewport Scaling"
    :desc "Six ways to fit a fixed-resolution game onto a resizable window"
    :controls "Click < > to change resolution and viewport type, resize the window"}
   {:alias "keyboard-testbed"
    :category :core
    :title "Keyboard Testbed"
    :desc "On-screen ENG-US keyboard showing what raylib reports per key"
    :controls "Press any key to light it up, hover to highlight, close via title bar"}
   {:alias "easings-testbed"
    :category :shapes
    :title "Easings Testbed"
    :desc "All 28 easing curves, one per axis"
    :controls "LEFT/RIGHT x-curve, UP/DOWN y-curve, ENTER play, SPACE restart, Q/W/A/S duration, T bound"}
   {:alias "undo-redo"
    :category :core
    :title "Undo Redo"
    :desc "Ring-buffer undo history on a grid"
    :controls "Arrows move, SPACE colour, CTRL+Z undo, CTRL+Y redo, Q exit"}
   {:alias "storage-values"
    :category :core
    :title "Storage Values"
    :desc "Save and load scores to a file"
    :controls "R random, ENTER save, SPACE load, Q to exit"}
   {:alias "input-virtual-controls"
    :category :core
    :title "Input Virtual Controls"
    :desc "On-screen D-pad"
    :controls "Click/touch the D-pad, Q to exit"}
   {:alias "input-gestures"
    :category :core
    :title "Input Gestures"
    :desc "Log of detected touch gestures"
    :controls "Click/drag/flick in the test area, Q to exit"}
   {:alias "camera-2d-split-screen"
    :category :core
    :title "Camera 2D Split Screen"
    :desc "Two players, two cameras, one grid"
    :controls "P1 W/S/A/D, P2 arrows, Q to exit"}
   {:alias "clock-of-clocks"
    :category :shapes
    :title "Clock Of Clocks"
    :desc "Digits drawn from a grid of analogue clocks"
    :controls "Space to toggle 12/24h, Q to exit"}
   {:alias "render-texture"
    :category :core
    :title "Render Texture"
    :desc "Bouncing ball drawn into an offscreen target"
    :controls "Q to exit"}
   {:alias "smooth-pixelperfect"
    :category :core
    :title "Smooth Pixelperfect"
    :desc "Pixel-aligned world, sub-pixel smooth camera"
    :controls "Q to exit"}
   {:alias "srcrec-dstrec"
    :category :textures
    :title "Srcrec Dstrec"
    :desc "Source/destination rects with rotation"
    :controls "Q to exit"}
   {:alias "delta-time"
    :category :core
    :title "Delta Time"
    :desc "Frame-rate independent vs fixed-step motion"
    :controls "Wheel to change FPS, R to reset, Q to exit"}
   {:alias "camera-3d-mode"
    :category :core
    :title "Camera 3D Mode"
    :desc "Minimal 3D scene - cube on a grid"
    :controls "Q to exit"}
   {:alias "input-multitouch"
    :category :core
    :title "Input Multitouch"
    :desc "Numbered circle per touch point"
    :controls "Touch/click, Q to exit"}
   {:alias "ellipse-collision"
    :category :shapes
    :title "Ellipse Collision"
    :desc "Steer one ellipse into another"
    :controls "A/B to switch control, Mouse to move, Q to exit"}
   {:alias "math-angle-rotation"
    :category :shapes
    :title "Math Angle Rotation"
    :desc "Fixed and sweeping angle lines"
    :controls "Q to exit"}
   {:alias "lines-drawing"
    :category :shapes
    :title "Lines Drawing"
    :desc "Draw rainbow lines on canvas"
    :controls "Click, Right-click, Wheel, Middle"}
   {:alias "easings-rectangles"
    :category :shapes
    :title "Easings Rectangles"
    :desc "Grid animation with easing functions"
    :controls "SPACE to replay"}

   ;; Text
   {:alias "writing-anim"
    :category :text
    :title "Writing Animation"
    :desc "Typewriter text effect"
    :controls "SPACE speed up, ENTER restart"}
   {:alias "format-text"
    :category :text
    :title "Format Text"
    :desc "Formatted score/timer display"
    :controls "ESC to exit"}
   {:alias "input-box"
    :category :text
    :title "Input Box"
    :desc "Text input field"
    :controls "Click, type, Backspace"}

   ;; Additional Core
   {:alias "window-should-close"
    :category :core
    :title "Window Should Close"
    :desc "Custom close confirmation"
    :controls "Y/N to confirm/cancel"}
   {:alias "camera-2d-platformer"
    :category :core
    :title "Camera 2D Platformer"
    :desc "5 camera follow modes"
    :controls "Arrows, SPACE, C, R, Wheel"}
   {:alias "window-letterbox"
    :category :core
    :title "Window Letterbox"
    :desc "Resolution-independent rendering"
    :controls "SPACE, Resize window"}])

;; =============================================================================
;; Example Lookup Functions
;; =============================================================================

(defn examples-by-category [category]
  (filter #(= category (:category %)) examples))

(defn find-example [alias]
  (first (filter #(= alias (:alias %)) examples)))

;; =============================================================================
;; Example Runner
;; =============================================================================

(def macos? (str/starts-with? (System/getProperty "os.name") "Mac"))

(defn- alias->namespace
  "Extract the main namespace from a deps.edn alias by reading the file."
  [alias]
  (let [deps (read-string (slurp "deps.edn"))
        alias-key (keyword alias)
        alias-data (get-in deps [:aliases alias-key])
        main-opts (:main-opts alias-data)]
    (when main-opts
      (second (drop-while #(not= "-m" %) main-opts)))))

(defn run-example! [alias]
  (if-let [example (find-example alias)]
    (let [{:keys [title desc controls category]} example
          {:keys [emoji]} (get example-categories category)]
      (header (str emoji " Running " title))
      (when desc (info desc))
      (when controls (info (str "Controls: " controls)))
      (println)
      ;; -XstartOnFirstThread is macOS-only (required for OpenGL main thread).
      ;; On Linux it causes "Unrecognized option" error, so we skip it.
      (if macos?
        (p/shell "clojure" (str "-M:" alias))
        (let [ns-name (alias->namespace alias)]
          (p/shell "clojure"
                   "-J--enable-native-access=ALL-UNNAMED"
                   "-J-Djava.library.path=libs:libs/linux_amd64:/usr/local/lib:/usr/lib"
                   "-M" "-m" ns-name))))
    (do
      (error-msg (str "Unknown example: " alias))
      (info "Run 'bb examples' to see available examples")
      (System/exit 1))))

;; =============================================================================
;; Help Generation
;; =============================================================================

(defn print-examples-help []
  (println)
  (println (color bold "🎮 Raylib Clojure Playground - Examples"))
  (println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
  (println)

  (doseq [[cat-key {:keys [emoji title]}] example-categories]
    (let [cat-examples (examples-by-category cat-key)]
      (when (seq cat-examples)
        (println (color magenta (str emoji " " title ":")))
        (doseq [{:keys [alias desc]} cat-examples]
          (println (str "   bb " (color cyan alias))
                   (when desc (str "  " desc))))
        (println))))

  (println (color yellow "Run any example:"))
  (println "   bb <example-name>")
  (println)
  (println (color cyan "Examples:"))
  (println "   bb asteroids")
  (println "   bb camera-2d")
  (println "   bb basic-lighting"))

;; =============================================================================
;; Demo recording status
;; =============================================================================
;;
;; screen-grab decides what to re-record by hashing each example's SOURCE file
;; and comparing against docs/demos/ledger.edn. That is the right default for
;; a maintainer keeping GIFs honest, but it conflates two very different
;; things:
;;
;;   MISSING - no GIF exists. The example has never been recorded.
;;   STALE   - a GIF exists, but the source changed since it was recorded.
;;
;; A refactor that touches many example files - renaming a namespace, say -
;; marks dozens STALE without altering a single pixel they draw. After the
;; 2026-08-22 raylib_ext consolidation, 49 examples went stale that way while
;; only 13 were genuinely missing. Re-recording all 62 costs an hour of
;; window-grabbing to reproduce 49 identical files.
;;
;; So `demo-status` separates the two, and `bb record:new` records only the
;; MISSING set.

(defn example-src
  "Path to an example's source file, derived the same way scripts do: from
   the namespace in deps.edn's :main-opts, NOT from the alias string.
   Deriving it differently would compute different shas and invalidate the
   whole ledger."
  [aliases alias]
  (let [main-opts (:main-opts (get aliases (keyword alias)))
        ns-str (str (second (drop-while (fn [x] (not= "-m" x)) main-opts)))]
    (str "src/" (str/replace (str/replace ns-str "." "/") "-" "_") ".clj")))

(defn- sha256 [path]
  (when (fs/exists? path)
    (let [md (java.security.MessageDigest/getInstance "SHA-256")]
      (apply str (map (fn [b] (format "%02x" b)) (.digest md (fs/read-all-bytes path)))))))

(defn demo-status
  "Classify every registered example as :missing, :stale or :current.

   Reads docs/demos/ledger.edn and deps.edn; needs neither screen-grab nor a
   window, so a contributor without the capture tool can still run it."
  []
  (let [ledger (if (fs/exists? "docs/demos/ledger.edn")
                 (edn/read-string (slurp "docs/demos/ledger.edn"))
                 {})
        aliases (:aliases (edn/read-string (slurp "deps.edn")))]
    (for [{:keys [alias]} examples]
      (let [gif (str "docs/demos/" alias ".gif")
            entry (get ledger alias)
            state (cond
                    (not (fs/exists? gif)) :missing
                    (not= (:sha entry) (sha256 (example-src aliases alias))) :stale
                    :else :current)]
        {:alias alias :state state}))))
