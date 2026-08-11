# The example catalog — 78 raylib examples

A map of the whole suite. Each example is one namespace under
`src/examples/` (or `src/examples/games/`, `src/examples/models/`),
runnable via `bb <name>` or `clj -M:<alias>`. `bb info` prints this
same grouping live from `bb/helpers.bb`'s `examples` registry — this
page is that registry rendered as a browsable table, plus (where known)
which official raylib C example a given Clojure example ports.

> **Full-size preview gallery:** not yet built. This project doesn't
> have a headless GIF-capture pipeline the way
> [`b12n-rljlt`](https://github.com/burinc/b12n-rljlt) does (its
> `RAYLIB_APP_AUTO_QUIT_MS`/`RAYLIB_APP_SHOT` env vars + `bb record`
> task — see that project's `docs/guide/headless-smoke-testing.md` for
> the recipe). Parked as follow-up work; this table is shaped so a
> preview column can be added later without restructuring.

Run one, or see them all grouped:
```sh
bb <name>       # e.g. bb asteroids   (opens a window)
bb examples     # flat list with descriptions
bb info         # this same grouping, printed from the terminal
```

## 🎮 Original games (9)

| `bb` name | title | shows | controls |
|---|---|---|---|
| `hello-world` | Hello World | Basic window test | Q to exit, F1 for debug stats |
| `pong` | Pong | Two-player paddle game | W/S, K/J, Enter |
| `asteroids` | Asteroids | Shoot asteroids | Arrows, Space |
| `asteroids2` | Asteroids 2 | Alternate version | Arrows, Space |
| `tetris` | Tetris | Block-stacking puzzle | Arrows, Space |
| `vampire-survivors` | Vampire Survivors | Survival action | WASD |
| `snake` | Snake | Classic snake game | Arrows, P, ENTER, Q |
| `floppy` | Floppy | Flappy bird clone | SPACE, P, ENTER, Q |
| `retro-maze-3d` | Retro Maze 3D | GameBoy-style maze escape | WASD, Mouse, SPACE, M, ENTER, Q |

## 📦 Core (23)

| `bb` name | title | shows | controls |
|---|---|---|---|
| `bouncing-ball` | Bouncing Ball | Physics demo | SPACE, G |
| `following-eyes` | Following Eyes | Mouse tracking | Move mouse |
| `screen-manager` | Screen Manager | State machine | ENTER |
| `input-keys` | Input Keys | Keyboard input | Arrow keys |
| `input-mouse` | Input Mouse | Mouse input | Click, move |
| `input-gamepad` | Input Gamepad | Gamepad demo | Connect gamepad |
| `mouse-wheel` | Mouse Wheel | Scroll input | Mouse wheel |
| `gestures-testbed` | Gestures Testbed | Touch gestures | Touch/click |
| `scissor-test` | Scissor Test | Scissor clipping | S, Mouse |
| `random-values` | Random Values | Random numbers | Watch |
| `camera-2d` | Camera 2D | 2D camera | Arrows, A/S, Wheel |
| `camera-3d-free` | Camera 3D Free | Free 3D camera | Mouse, Wheel |
| `split-screen-3d` | Split Screen 3D | Two-player 3D | W/S, UP/DOWN |
| `first-person-3d` | First Person 3D | FPS camera | WASD, Mouse, 1-4 |
| `camera-fps` | Camera FPS | FPS with physics | WASD, Space, Ctrl |
| `world-screen` | World Screen | 3D to 2D coords | Mouse, Wheel |
| `picking-3d` | Picking 3D | Ray casting | Click |
| `collision-area` | Collision Area | Collision detection | Mouse, SPACE |
| `colors-palette` | Colors Palette | Color showcase | Hover, SPACE |
| `logo-anim` | Logo Animation | Logo animation | R to replay |
| `window-should-close` | Window Should Close | Custom close confirmation | Y/N to confirm/cancel |
| `camera-2d-platformer` | Camera 2D Platformer | 5 camera follow modes | Arrows, SPACE, C, R, Wheel |
| `window-letterbox` | Window Letterbox | Resolution-independent rendering | SPACE, Resize window |

## 🔷 Shapes (15)

| `bb` name | title | shows | controls |
|---|---|---|---|
| `logo-raylib` | Logo Raylib | Raylib logo drawn with shapes | ESC to exit |
| `logo-raylib-anim` | Logo Raylib Anim | Animated logo construction | R to replay |
| `basic-shapes` | Basic Shapes | Circles, rectangles, triangles, polygons | ESC to exit |
| `rectangle-scaling` | Rectangle Scaling | Drag to resize rectangle | Drag bottom-right corner |
| `mouse-trail` | Mouse Trail | Circles following mouse cursor | Move mouse |
| `lines-bezier` | Lines Bezier | Interactive bezier curve | Drag endpoints |
| `easings-ball` | Easings Ball | Easing function animation | ENTER to replay |
| `ball-physics` | Ball Physics | Grab and throw balls | Click, Right-click, Wheel, Middle |
| `simple-particles` | Simple Particles | Water/smoke/fire effects | Arrows, Click |
| `dashed-line` | Dashed Line | Interactive dashed line | Arrows, C |
| `starfield-effect` | Starfield Effect | 3D starfield simulation | SPACE, Wheel |
| `easings-box` | Easings Box | Box animation with easing functions | SPACE to reset |
| `double-pendulum` | Double Pendulum | Chaotic pendulum simulation | ESC to exit |
| `lines-drawing` | Lines Drawing | Draw rainbow lines on canvas | Click, Right-click, Wheel, Middle |
| `easings-rectangles` | Easings Rectangles | Grid animation with easing functions | SPACE to replay |

## 📝 Text (3)

| `bb` name | title | shows | controls |
|---|---|---|---|
| `writing-anim` | Writing Animation | Typewriter text effect | SPACE speed up, ENTER restart |
| `format-text` | Format Text | Formatted score/timer display | ESC to exit |
| `input-box` | Input Box | Text input field | Click, type, Backspace |

## 🖼️ Textures (2)

| `bb` name | title | shows | controls |
|---|---|---|---|
| `background-scrolling` | Background Scrolling | Parallax demo | Watch |
| `sprite-animation` | Sprite Animation | Spritesheet | LEFT/RIGHT |

## ✨ Shaders (1)

| `bb` name | title | shows | controls |
|---|---|---|---|
| `basic-lighting` | Basic Lighting | Dynamic lighting | Mouse, Y/R/G/B |

## 🔊 Audio (4)

| `bb` name | title | shows | controls |
|---|---|---|---|
| `audio-module` | Audio Module | Music visualization | SPACE, P, Arrows |
| `sound-loading` | Sound Loading | WAV/OGG playback | SPACE, ENTER |
| `music-stream` | Music Stream | MP3 streaming | SPACE, P, Arrows |
| `sound-multi` | Sound Multi | Multiple sounds | SPACE |

## 🗿 Models (21)

| `bb` name | title | shows | controls |
|---|---|---|---|
| `geometric-shapes` | Geometric Shapes | 3D primitives | Q to exit |
| `waving-cubes` | Waving Cubes | Animated cube wave | Q to exit |
| `box-collisions` | Box Collisions | 3D collision detection | Arrow keys, Q |
| `orthographic-projection` | Orthographic Projection | Perspective vs orthographic | SPACE, Q |
| `tesseract-view` | Tesseract View | 4D hypercube | Q to exit |
| `solar-system` | Solar System | Orbiting planets | Q to exit |
| `spinning-cubes` | Spinning Cubes | Color-cycling cubes | Q to exit |
| `point-cloud` | Point Cloud | Spherical points | UP/DOWN, Q |
| `wireframe-shapes` | Wireframe Shapes | Custom wireframes | SPACE, Q |
| `camera-modes` | Camera Modes | Free/Orbital/FPS cameras | 1/2/3, WASD, Q |
| `ray-picking` | Ray Picking | Click to select cubes | Click, Right-click, Q |
| `bouncing-spheres` | Bouncing Spheres | Physics in 3D box | SPACE, R, G, Q |
| `rotating-cube` | Rotating Cube | 3D rotation | Arrows, +/-, R, Q |
| `particle-system` | Particle System | 3D particles | SPACE, G, W, R, Q |
| `dna-helix` | DNA Helix | Double helix | Arrows, SPACE, R, Q |
| `first-person-maze` | First Person Maze | Navigate 3D maze | WASD, Mouse, R, M, Q |
| `yaw-pitch-roll` | Yaw Pitch Roll | 3D rotation demo | Arrows, SPACE, R, Q |
| `lissajous-3d` | Lissajous 3D | Parametric curves | 1-5, Arrows, W/S, SPACE, Q |
| `lorenz-attractor` | Lorenz Attractor | Chaos theory | 1-3, Arrows, SPACE, R, Q |
| `terrain-generation` | Terrain Generation | Procedural terrain | 1-3, Arrows, G, W, SPACE, Q |
| `mesh-generation` | Mesh Generation | Procedural 3D shapes | Left/Right, Click, SPACE, R, Q |

## Adding a new example

See [`example-architecture-patterns.md`](example-architecture-patterns.md#porting-a-new-raylib-c-example)
for the full recipe (source file, `deps.edn` alias, `bb.edn` task,
`bb/helpers.bb` registry entry).
