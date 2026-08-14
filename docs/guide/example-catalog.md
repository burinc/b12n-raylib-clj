# The example catalog — 78 raylib examples

A map of the whole suite. Each example is one namespace under
`src/examples/` (or `src/examples/games/`, `src/examples/models/`),
runnable via `bb <name>` or `clj -M:<alias>`. `bb examples` prints this
same grouping live from `bb/helpers.bb`'s `examples` registry — this
page is that registry rendered as a browsable table, plus (where known)
which official raylib C example a given Clojure example ports. Nearly
every "ported from" cell cites a file in
[`raysan5/raylib`](https://github.com/raysan5/raylib)'s own `examples/`
tree; the 3 cells marked ¹ instead cite the companion
[`raysan5/raylib-games`](https://github.com/raysan5/raylib-games) repo
— see the note under the games table below.

> **Full-size preview gallery:** [`demos.md`](demos.md) — every example
> at full size, one-line description included. The `preview` column
> below thumbnails straight from the same recorded GIFs
> (`docs/demos/*.gif`, via [cgevent](https://github.com/burinc/b12n-cgevent)
> and `bb record`, shelled out to the
> [`screen-grab`](https://github.com/burinc/b12n-screen-grab) CLI and
> configured by
> [`scripts/demo_manifest.edn`](../../scripts/demo_manifest.edn)).

Run one, or see them all grouped:
```sh
bb <name>       # e.g. bb asteroids   (opens a window)
bb examples     # this same grouping, printed from the terminal
```

## 🎮 Original games (9)

| preview | `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|---|
| [<img src="../demos/hello-world.gif" width="80">](demos.md#hello-world) | `hello-world` | Hello World | Basic window test | Q to exit, F1 for debug stats | `core_basic_window.c` |
| [<img src="../demos/pong.gif" width="80">](demos.md#pong) | `pong` | Pong | Two-player paddle game | W/S, K/J, Enter | — |
| [<img src="../demos/asteroids.gif" width="80">](demos.md#asteroids) | `asteroids` | Asteroids | Shoot asteroids | Arrows, Space | — |
| [<img src="../demos/asteroids2.gif" width="80">](demos.md#asteroids2) | `asteroids2` | Asteroids 2 | Alternate version | Arrows, Space | — |
| [<img src="../demos/tetris.gif" width="80">](demos.md#tetris) | `tetris` | Tetris | Block-stacking puzzle | Arrows, Space | — |
| [<img src="../demos/vampire-survivors.gif" width="80">](demos.md#vampire-survivors) | `vampire-survivors` | Vampire Survivors | Survival action | WASD | — |
| [<img src="../demos/snake.gif" width="80">](demos.md#snake) | `snake` | Snake | Classic snake game | Arrows, P, ENTER, Q | `snake.c`¹ |
| [<img src="../demos/floppy.gif" width="80">](demos.md#floppy) | `floppy` | Floppy | Flappy bird clone | SPACE, P, ENTER, Q | `floppy.c`¹ |
| [<img src="../demos/retro-maze-3d.gif" width="80">](demos.md#retro-maze-3d) | `retro-maze-3d` | Retro Maze 3D | GameBoy-style maze escape | WASD, Mouse, SPACE, M, ENTER, Q | `retro_maze_3d.c`¹ |

¹ Ported from [`raysan5/raylib-games`](https://github.com/raysan5/raylib-games)
— a companion repo of classic-game clones and game-jam entries, separate
from `raysan5/raylib`'s own `examples/` tree that every other "ported
from" cell on this page cites. `snake.c` and `floppy.c` come from its
`classics/` collection; `retro_maze_3d.c` from its `retro_maze_3d/` GGJ
2021 entry (header comment: "GGJ 2021 - RETRO MAZE 3D … Copyright (c)
2021 Ramon Santamaria (@raysan5)", matching this example's own docstring
credit).

## 📦 Core (23)

| preview | `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|---|
| [<img src="../demos/bouncing-ball.gif" width="80">](demos.md#bouncing-ball) | `bouncing-ball` | Bouncing Ball | Physics demo | SPACE, G | `shapes_bouncing_ball.c` |
| [<img src="../demos/following-eyes.gif" width="80">](demos.md#following-eyes) | `following-eyes` | Following Eyes | Mouse tracking | Move mouse | `shapes_following_eyes.c` |
| [<img src="../demos/screen-manager.gif" width="80">](demos.md#screen-manager) | `screen-manager` | Screen Manager | State machine | ENTER | `core_basic_screen_manager.c` |
| [<img src="../demos/input-keys.gif" width="80">](demos.md#input-keys) | `input-keys` | Input Keys | Keyboard input | Arrow keys | `core_input_keys.c` |
| [<img src="../demos/input-mouse.gif" width="80">](demos.md#input-mouse) | `input-mouse` | Input Mouse | Mouse input | Click, move | `core_input_mouse.c` |
| [<img src="../demos/input-gamepad.gif" width="80">](demos.md#input-gamepad) | `input-gamepad` | Input Gamepad | Gamepad demo | Connect gamepad | `core_input_gamepad.c` |
| [<img src="../demos/mouse-wheel.gif" width="80">](demos.md#mouse-wheel) | `mouse-wheel` | Mouse Wheel | Scroll input | Mouse wheel | `core_input_mouse_wheel.c` |
| [<img src="../demos/gestures-testbed.gif" width="80">](demos.md#gestures-testbed) | `gestures-testbed` | Gestures Testbed | Touch gestures | Touch/click | `core_input_gestures_testbed.c` |
| [<img src="../demos/scissor-test.gif" width="80">](demos.md#scissor-test) | `scissor-test` | Scissor Test | Scissor clipping | S, Mouse | `core_scissor_test.c` |
| [<img src="../demos/random-values.gif" width="80">](demos.md#random-values) | `random-values` | Random Values | Random numbers | Watch | `core_random_values.c` |
| [<img src="../demos/camera-2d.gif" width="80">](demos.md#camera-2d) | `camera-2d` | Camera 2D | 2D camera | Arrows, A/S, Wheel | `core_2d_camera.c` |
| [<img src="../demos/camera-3d-free.gif" width="80">](demos.md#camera-3d-free) | `camera-3d-free` | Camera 3D Free | Free 3D camera | Mouse, Wheel | `core_3d_camera_free.c` |
| [<img src="../demos/split-screen-3d.gif" width="80">](demos.md#split-screen-3d) | `split-screen-3d` | Split Screen 3D | Two-player 3D | W/S, UP/DOWN | `core_3d_camera_split_screen.c` |
| [<img src="../demos/first-person-3d.gif" width="80">](demos.md#first-person-3d) | `first-person-3d` | First Person 3D | FPS camera | WASD, Mouse, 1-4 | `core_3d_camera_first_person.c` |
| [<img src="../demos/camera-fps.gif" width="80">](demos.md#camera-fps) | `camera-fps` | Camera FPS | FPS with physics | WASD, Space, Ctrl | `core_3d_camera_fps.c` |
| [<img src="../demos/world-screen.gif" width="80">](demos.md#world-screen) | `world-screen` | World Screen | 3D to 2D coords | Mouse, Wheel | `core_world_screen.c` |
| [<img src="../demos/picking-3d.gif" width="80">](demos.md#picking-3d) | `picking-3d` | Picking 3D | Ray casting | Click | `core_3d_picking.c` |
| [<img src="../demos/collision-area.gif" width="80">](demos.md#collision-area) | `collision-area` | Collision Area | Collision detection | Mouse, SPACE | `shapes_collision_area.c` |
| [<img src="../demos/colors-palette.gif" width="80">](demos.md#colors-palette) | `colors-palette` | Colors Palette | Color showcase | Hover, SPACE | `shapes_colors_palette.c` |
| [<img src="../demos/logo-anim.gif" width="80">](demos.md#logo-anim) | `logo-anim` | Logo Animation | Logo animation | R to replay | `shapes_logo_raylib_anim.c` |
| [<img src="../demos/window-should-close.gif" width="80">](demos.md#window-should-close) | `window-should-close` | Window Should Close | Custom close confirmation | Y/N to confirm/cancel | `core_window_should_close.c` |
| [<img src="../demos/camera-2d-platformer.gif" width="80">](demos.md#camera-2d-platformer) | `camera-2d-platformer` | Camera 2D Platformer | 5 camera follow modes | Arrows, SPACE, C, R, Wheel | `core_2d_camera_platformer.c` |
| [<img src="../demos/window-letterbox.gif" width="80">](demos.md#window-letterbox) | `window-letterbox` | Window Letterbox | Resolution-independent rendering | SPACE, Resize window | `core_window_letterbox.c` |

## 🔷 Shapes (15)

| preview | `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|---|
| [<img src="../demos/logo-raylib.gif" width="80">](demos.md#logo-raylib) | `logo-raylib` | Logo Raylib | Raylib logo drawn with shapes | ESC to exit | `shapes_logo_raylib.c` |
| [<img src="../demos/logo-raylib-anim.gif" width="80">](demos.md#logo-raylib-anim) | `logo-raylib-anim` | Logo Raylib Anim | Animated logo construction | R to replay | `shapes_logo_raylib_anim.c` |
| [<img src="../demos/basic-shapes.gif" width="80">](demos.md#basic-shapes) | `basic-shapes` | Basic Shapes | Circles, rectangles, triangles, polygons | ESC to exit | `shapes_basic_shapes.c` |
| [<img src="../demos/rectangle-scaling.gif" width="80">](demos.md#rectangle-scaling) | `rectangle-scaling` | Rectangle Scaling | Drag to resize rectangle | Drag bottom-right corner | `shapes_rectangle_scaling.c` |
| [<img src="../demos/mouse-trail.gif" width="80">](demos.md#mouse-trail) | `mouse-trail` | Mouse Trail | Circles following mouse cursor | Move mouse | `shapes_mouse_trail.c` |
| [<img src="../demos/lines-bezier.gif" width="80">](demos.md#lines-bezier) | `lines-bezier` | Lines Bezier | Interactive bezier curve | Drag endpoints | `shapes_lines_bezier.c` |
| [<img src="../demos/easings-ball.gif" width="80">](demos.md#easings-ball) | `easings-ball` | Easings Ball | Easing function animation | ENTER to replay | `shapes_easings_ball.c` |
| [<img src="../demos/ball-physics.gif" width="80">](demos.md#ball-physics) | `ball-physics` | Ball Physics | Grab and throw balls | Click, Right-click, Wheel, Middle | `shapes_ball_physics.c` |
| [<img src="../demos/simple-particles.gif" width="80">](demos.md#simple-particles) | `simple-particles` | Simple Particles | Water/smoke/fire effects | Arrows, Click | `shapes_simple_particles.c` |
| [<img src="../demos/dashed-line.gif" width="80">](demos.md#dashed-line) | `dashed-line` | Dashed Line | Interactive dashed line | Arrows, C | `shapes_dashed_line.c` |
| [<img src="../demos/starfield-effect.gif" width="80">](demos.md#starfield-effect) | `starfield-effect` | Starfield Effect | 3D starfield simulation | SPACE, Wheel | `shapes_starfield_effect.c` |
| [<img src="../demos/easings-box.gif" width="80">](demos.md#easings-box) | `easings-box` | Easings Box | Box animation with easing functions | SPACE to reset | `shapes_easings_box.c` |
| [<img src="../demos/double-pendulum.gif" width="80">](demos.md#double-pendulum) | `double-pendulum` | Double Pendulum | Chaotic pendulum simulation | ESC to exit | `shapes_double_pendulum.c` |
| [<img src="../demos/lines-drawing.gif" width="80">](demos.md#lines-drawing) | `lines-drawing` | Lines Drawing | Draw rainbow lines on canvas | Click, Right-click, Wheel, Middle | `shapes_lines_drawing.c` |
| [<img src="../demos/easings-rectangles.gif" width="80">](demos.md#easings-rectangles) | `easings-rectangles` | Easings Rectangles | Grid animation with easing functions | SPACE to replay | `shapes_easings_rectangles.c` |

## 📝 Text (3)

| preview | `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|---|
| [<img src="../demos/writing-anim.gif" width="80">](demos.md#writing-anim) | `writing-anim` | Writing Animation | Typewriter text effect | SPACE speed up, ENTER restart | `text_writing_anim.c` |
| [<img src="../demos/format-text.gif" width="80">](demos.md#format-text) | `format-text` | Format Text | Formatted score/timer display | ESC to exit | `text_format_text.c` |
| [<img src="../demos/input-box.gif" width="80">](demos.md#input-box) | `input-box` | Input Box | Text input field | Click, type, Backspace | `text_input_box.c` |

## 🖼️ Textures (2)

| preview | `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|---|
| [<img src="../demos/background-scrolling.gif" width="80">](demos.md#background-scrolling) | `background-scrolling` | Background Scrolling | Parallax demo | Watch | `textures_background_scrolling.c` |
| [<img src="../demos/sprite-animation.gif" width="80">](demos.md#sprite-animation) | `sprite-animation` | Sprite Animation | Spritesheet | LEFT/RIGHT | `textures_sprite_animation.c` |

## ✨ Shaders (1)

| preview | `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|---|
| [<img src="../demos/basic-lighting.gif" width="80">](demos.md#basic-lighting) | `basic-lighting` | Basic Lighting | Dynamic lighting | Mouse, Y/R/G/B | `shaders_basic_lighting.c` |

## 🔊 Audio (4)

| preview | `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|---|
| [<img src="../demos/audio-module.gif" width="80">](demos.md#audio-module) | `audio-module` | Audio Module | Music visualization | SPACE, P, Arrows | `audio_module_playing.c` |
| [<img src="../demos/sound-loading.gif" width="80">](demos.md#sound-loading) | `sound-loading` | Sound Loading | WAV/OGG playback | SPACE, ENTER | `audio_sound_loading.c` |
| [<img src="../demos/music-stream.gif" width="80">](demos.md#music-stream) | `music-stream` | Music Stream | MP3 streaming | SPACE, P, Arrows | `audio_music_stream.c` |
| [<img src="../demos/sound-multi.gif" width="80">](demos.md#sound-multi) | `sound-multi` | Sound Multi | Multiple sounds | SPACE | `audio_sound_multi.c` |

## 🗿 Models (21)

| preview | `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|---|
| [<img src="../demos/geometric-shapes.gif" width="80">](demos.md#geometric-shapes) | `geometric-shapes` | Geometric Shapes | 3D primitives | Q to exit | `models_geometric_shapes.c` |
| [<img src="../demos/waving-cubes.gif" width="80">](demos.md#waving-cubes) | `waving-cubes` | Waving Cubes | Animated cube wave | Q to exit | `models_waving_cubes.c` |
| [<img src="../demos/box-collisions.gif" width="80">](demos.md#box-collisions) | `box-collisions` | Box Collisions | 3D collision detection | Arrow keys, Q | `models_box_collisions.c` |
| [<img src="../demos/orthographic-projection.gif" width="80">](demos.md#orthographic-projection) | `orthographic-projection` | Orthographic Projection | Perspective vs orthographic | SPACE, Q | `models_orthographic_projection.c` |
| [<img src="../demos/tesseract-view.gif" width="80">](demos.md#tesseract-view) | `tesseract-view` | Tesseract View | 4D hypercube | Q to exit | `models_tesseract_view.c` |
| [<img src="../demos/solar-system.gif" width="80">](demos.md#solar-system) | `solar-system` | Solar System | Orbiting planets | Q to exit | `models_rlgl_solar_system.c` |
| [<img src="../demos/spinning-cubes.gif" width="80">](demos.md#spinning-cubes) | `spinning-cubes` | Spinning Cubes | Color-cycling cubes | Q to exit | — |
| [<img src="../demos/point-cloud.gif" width="80">](demos.md#point-cloud) | `point-cloud` | Point Cloud | Spherical points | UP/DOWN, Q | `models_point_rendering.c` |
| [<img src="../demos/wireframe-shapes.gif" width="80">](demos.md#wireframe-shapes) | `wireframe-shapes` | Wireframe Shapes | Custom wireframes | SPACE, Q | — |
| [<img src="../demos/camera-modes.gif" width="80">](demos.md#camera-modes) | `camera-modes` | Camera Modes | Free/Orbital/FPS cameras | 1/2/3, WASD, Q | — |
| [<img src="../demos/ray-picking.gif" width="80">](demos.md#ray-picking) | `ray-picking` | Ray Picking | Click to select cubes | Click, Right-click, Q | `core_3d_picking.c` |
| [<img src="../demos/bouncing-spheres.gif" width="80">](demos.md#bouncing-spheres) | `bouncing-spheres` | Bouncing Spheres | Physics in 3D box | SPACE, R, G, Q | — |
| [<img src="../demos/rotating-cube.gif" width="80">](demos.md#rotating-cube) | `rotating-cube` | Rotating Cube | 3D rotation | Arrows, +/-, R, Q | `models_rotating_cube.c` |
| [<img src="../demos/particle-system.gif" width="80">](demos.md#particle-system) | `particle-system` | Particle System | 3D particles | SPACE, G, W, R, Q | — |
| [<img src="../demos/dna-helix.gif" width="80">](demos.md#dna-helix) | `dna-helix` | DNA Helix | Double helix | Arrows, SPACE, R, Q | — |
| [<img src="../demos/first-person-maze.gif" width="80">](demos.md#first-person-maze) | `first-person-maze` | First Person Maze | Navigate 3D maze | WASD, Mouse, R, M, Q | `models_first_person_maze.c` |
| [<img src="../demos/yaw-pitch-roll.gif" width="80">](demos.md#yaw-pitch-roll) | `yaw-pitch-roll` | Yaw Pitch Roll | 3D rotation demo | Arrows, SPACE, R, Q | `models_yaw_pitch_roll.c` |
| [<img src="../demos/lissajous-3d.gif" width="80">](demos.md#lissajous-3d) | `lissajous-3d` | Lissajous 3D | Parametric curves | 1-5, Arrows, W/S, SPACE, Q | — |
| [<img src="../demos/lorenz-attractor.gif" width="80">](demos.md#lorenz-attractor) | `lorenz-attractor` | Lorenz Attractor | Chaos theory | 1-3, Arrows, SPACE, R, Q | — |
| [<img src="../demos/terrain-generation.gif" width="80">](demos.md#terrain-generation) | `terrain-generation` | Terrain Generation | Procedural terrain | 1-3, Arrows, G, W, SPACE, Q | — |
| [<img src="../demos/mesh-generation.gif" width="80">](demos.md#mesh-generation) | `mesh-generation` | Mesh Generation | Procedural 3D shapes | Left/Right, Click, SPACE, R, Q | `models_mesh_generation.c` |

## Adding a new example

See [`example-architecture-patterns.md`](example-architecture-patterns.md#porting-a-new-raylib-c-example)
for the full recipe (source file, `deps.edn` alias, `bb.edn` task,
`bb/helpers.bb` registry entry).
