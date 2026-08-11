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
bb examples     # this same grouping, printed from the terminal
```

## 🎮 Original games (9)

| `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|
| `hello-world` | Hello World | Basic window test | Q to exit, F1 for debug stats | `core_basic_window.c` |
| `pong` | Pong | Two-player paddle game | W/S, K/J, Enter | — |
| `asteroids` | Asteroids | Shoot asteroids | Arrows, Space | — |
| `asteroids2` | Asteroids 2 | Alternate version | Arrows, Space | — |
| `tetris` | Tetris | Block-stacking puzzle | Arrows, Space | — |
| `vampire-survivors` | Vampire Survivors | Survival action | WASD | — |
| `snake` | Snake | Classic snake game | Arrows, P, ENTER, Q | `snake.c`¹ |
| `floppy` | Floppy | Flappy bird clone | SPACE, P, ENTER, Q | `floppy.c`¹ |
| `retro-maze-3d` | Retro Maze 3D | GameBoy-style maze escape | WASD, Mouse, SPACE, M, ENTER, Q | `retro_maze_3d.c`¹ |

¹ Ported from [`raysan5/raylib-games`](https://github.com/raysan5/raylib-games)
— a companion repo of classic-game clones and game-jam entries, separate
from `raysan5/raylib`'s own `examples/` tree that every other "ported
from" cell on this page cites. `snake.c` and `floppy.c` come from its
`classics/` collection; `retro_maze_3d.c` from its `retro_maze_3d/` GGJ
2021 entry (header comment: "GGJ 2021 - RETRO MAZE 3D … Copyright (c)
2021 Ramon Santamaria (@raysan5)", matching this example's own docstring
credit).

## 📦 Core (23)

| `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|
| `bouncing-ball` | Bouncing Ball | Physics demo | SPACE, G | `shapes_bouncing_ball.c` |
| `following-eyes` | Following Eyes | Mouse tracking | Move mouse | `shapes_following_eyes.c` |
| `screen-manager` | Screen Manager | State machine | ENTER | `core_basic_screen_manager.c` |
| `input-keys` | Input Keys | Keyboard input | Arrow keys | `core_input_keys.c` |
| `input-mouse` | Input Mouse | Mouse input | Click, move | `core_input_mouse.c` |
| `input-gamepad` | Input Gamepad | Gamepad demo | Connect gamepad | `core_input_gamepad.c` |
| `mouse-wheel` | Mouse Wheel | Scroll input | Mouse wheel | `core_input_mouse_wheel.c` |
| `gestures-testbed` | Gestures Testbed | Touch gestures | Touch/click | `core_input_gestures_testbed.c` |
| `scissor-test` | Scissor Test | Scissor clipping | S, Mouse | `core_scissor_test.c` |
| `random-values` | Random Values | Random numbers | Watch | `core_random_values.c` |
| `camera-2d` | Camera 2D | 2D camera | Arrows, A/S, Wheel | `core_2d_camera.c` |
| `camera-3d-free` | Camera 3D Free | Free 3D camera | Mouse, Wheel | `core_3d_camera_free.c` |
| `split-screen-3d` | Split Screen 3D | Two-player 3D | W/S, UP/DOWN | `core_3d_camera_split_screen.c` |
| `first-person-3d` | First Person 3D | FPS camera | WASD, Mouse, 1-4 | `core_3d_camera_first_person.c` |
| `camera-fps` | Camera FPS | FPS with physics | WASD, Space, Ctrl | `core_3d_camera_fps.c` |
| `world-screen` | World Screen | 3D to 2D coords | Mouse, Wheel | `core_world_screen.c` |
| `picking-3d` | Picking 3D | Ray casting | Click | `core_3d_picking.c` |
| `collision-area` | Collision Area | Collision detection | Mouse, SPACE | `shapes_collision_area.c` |
| `colors-palette` | Colors Palette | Color showcase | Hover, SPACE | `shapes_colors_palette.c` |
| `logo-anim` | Logo Animation | Logo animation | R to replay | `shapes_logo_raylib_anim.c` |
| `window-should-close` | Window Should Close | Custom close confirmation | Y/N to confirm/cancel | `core_window_should_close.c` |
| `camera-2d-platformer` | Camera 2D Platformer | 5 camera follow modes | Arrows, SPACE, C, R, Wheel | `core_2d_camera_platformer.c` |
| `window-letterbox` | Window Letterbox | Resolution-independent rendering | SPACE, Resize window | `core_window_letterbox.c` |

## 🔷 Shapes (15)

| `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|
| `logo-raylib` | Logo Raylib | Raylib logo drawn with shapes | ESC to exit | `shapes_logo_raylib.c` |
| `logo-raylib-anim` | Logo Raylib Anim | Animated logo construction | R to replay | `shapes_logo_raylib_anim.c` |
| `basic-shapes` | Basic Shapes | Circles, rectangles, triangles, polygons | ESC to exit | `shapes_basic_shapes.c` |
| `rectangle-scaling` | Rectangle Scaling | Drag to resize rectangle | Drag bottom-right corner | `shapes_rectangle_scaling.c` |
| `mouse-trail` | Mouse Trail | Circles following mouse cursor | Move mouse | `shapes_mouse_trail.c` |
| `lines-bezier` | Lines Bezier | Interactive bezier curve | Drag endpoints | `shapes_lines_bezier.c` |
| `easings-ball` | Easings Ball | Easing function animation | ENTER to replay | `shapes_easings_ball.c` |
| `ball-physics` | Ball Physics | Grab and throw balls | Click, Right-click, Wheel, Middle | `shapes_ball_physics.c` |
| `simple-particles` | Simple Particles | Water/smoke/fire effects | Arrows, Click | `shapes_simple_particles.c` |
| `dashed-line` | Dashed Line | Interactive dashed line | Arrows, C | `shapes_dashed_line.c` |
| `starfield-effect` | Starfield Effect | 3D starfield simulation | SPACE, Wheel | `shapes_starfield_effect.c` |
| `easings-box` | Easings Box | Box animation with easing functions | SPACE to reset | `shapes_easings_box.c` |
| `double-pendulum` | Double Pendulum | Chaotic pendulum simulation | ESC to exit | `shapes_double_pendulum.c` |
| `lines-drawing` | Lines Drawing | Draw rainbow lines on canvas | Click, Right-click, Wheel, Middle | `shapes_lines_drawing.c` |
| `easings-rectangles` | Easings Rectangles | Grid animation with easing functions | SPACE to replay | `shapes_easings_rectangles.c` |

## 📝 Text (3)

| `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|
| `writing-anim` | Writing Animation | Typewriter text effect | SPACE speed up, ENTER restart | `text_writing_anim.c` |
| `format-text` | Format Text | Formatted score/timer display | ESC to exit | `text_format_text.c` |
| `input-box` | Input Box | Text input field | Click, type, Backspace | `text_input_box.c` |

## 🖼️ Textures (2)

| `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|
| `background-scrolling` | Background Scrolling | Parallax demo | Watch | `textures_background_scrolling.c` |
| `sprite-animation` | Sprite Animation | Spritesheet | LEFT/RIGHT | `textures_sprite_animation.c` |

## ✨ Shaders (1)

| `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|
| `basic-lighting` | Basic Lighting | Dynamic lighting | Mouse, Y/R/G/B | `shaders_basic_lighting.c` |

## 🔊 Audio (4)

| `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|
| `audio-module` | Audio Module | Music visualization | SPACE, P, Arrows | `audio_module_playing.c` |
| `sound-loading` | Sound Loading | WAV/OGG playback | SPACE, ENTER | `audio_sound_loading.c` |
| `music-stream` | Music Stream | MP3 streaming | SPACE, P, Arrows | `audio_music_stream.c` |
| `sound-multi` | Sound Multi | Multiple sounds | SPACE | `audio_sound_multi.c` |

## 🗿 Models (21)

| `bb` name | title | shows | controls | ported from |
|---|---|---|---|---|
| `geometric-shapes` | Geometric Shapes | 3D primitives | Q to exit | `models_geometric_shapes.c` |
| `waving-cubes` | Waving Cubes | Animated cube wave | Q to exit | `models_waving_cubes.c` |
| `box-collisions` | Box Collisions | 3D collision detection | Arrow keys, Q | `models_box_collisions.c` |
| `orthographic-projection` | Orthographic Projection | Perspective vs orthographic | SPACE, Q | `models_orthographic_projection.c` |
| `tesseract-view` | Tesseract View | 4D hypercube | Q to exit | `models_tesseract_view.c` |
| `solar-system` | Solar System | Orbiting planets | Q to exit | `models_rlgl_solar_system.c` |
| `spinning-cubes` | Spinning Cubes | Color-cycling cubes | Q to exit | — |
| `point-cloud` | Point Cloud | Spherical points | UP/DOWN, Q | `models_point_rendering.c` |
| `wireframe-shapes` | Wireframe Shapes | Custom wireframes | SPACE, Q | — |
| `camera-modes` | Camera Modes | Free/Orbital/FPS cameras | 1/2/3, WASD, Q | — |
| `ray-picking` | Ray Picking | Click to select cubes | Click, Right-click, Q | `core_3d_picking.c` |
| `bouncing-spheres` | Bouncing Spheres | Physics in 3D box | SPACE, R, G, Q | — |
| `rotating-cube` | Rotating Cube | 3D rotation | Arrows, +/-, R, Q | `models_rotating_cube.c` |
| `particle-system` | Particle System | 3D particles | SPACE, G, W, R, Q | — |
| `dna-helix` | DNA Helix | Double helix | Arrows, SPACE, R, Q | — |
| `first-person-maze` | First Person Maze | Navigate 3D maze | WASD, Mouse, R, M, Q | `models_first_person_maze.c` |
| `yaw-pitch-roll` | Yaw Pitch Roll | 3D rotation demo | Arrows, SPACE, R, Q | `models_yaw_pitch_roll.c` |
| `lissajous-3d` | Lissajous 3D | Parametric curves | 1-5, Arrows, W/S, SPACE, Q | — |
| `lorenz-attractor` | Lorenz Attractor | Chaos theory | 1-3, Arrows, SPACE, R, Q | — |
| `terrain-generation` | Terrain Generation | Procedural terrain | 1-3, Arrows, G, W, SPACE, Q | — |
| `mesh-generation` | Mesh Generation | Procedural 3D shapes | Left/Right, Click, SPACE, R, Q | `models_mesh_generation.c` |

## Adding a new example

See [`example-architecture-patterns.md`](example-architecture-patterns.md#porting-a-new-raylib-c-example)
for the full recipe (source file, `deps.edn` alias, `bb.edn` task,
`bb/helpers.bb` registry entry).
