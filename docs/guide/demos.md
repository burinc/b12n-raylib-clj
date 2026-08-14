# Full-size demo gallery

Every example at full size — linked from
[the example catalog](example-catalog.md)'s preview thumbnails.
Recorded via [cgevent](https://github.com/burinc/b12n-cgevent), driven by
the [`screen-grab`](https://github.com/burinc/b12n-screen-grab) CLI and
configured by
[`scripts/demo_manifest.edn`](../../scripts/demo_manifest.edn). Regenerate
with `bb record`.

## 🎮 Original games (9)

### hello-world

the minimal raylib window + text (Q exits, F1 toggles debug stats)

![hello-world](../demos/hello-world.gif)

### pong

two-paddle classic, P1 (W/S) vs P2 (K/J)

![pong](../demos/pong.gif)

### asteroids

the classic vector shooter (rotate/thrust/fire)

![asteroids](../demos/asteroids.gif)

### asteroids2

an alternate asteroids build (rotate/thrust/fire)

![asteroids2](../demos/asteroids2.gif)

### tetris

the block-stacking puzzle (move/rotate/drop)

![tetris](../demos/tetris.gif)

### vampire-survivors

auto-fire survival: move (WASD), waves chase you

![vampire-survivors](../demos/vampire-survivors.gif)

### snake

the classic snake (arrow keys, grow, don't crash)

![snake](../demos/snake.gif)

### floppy

flap through the pipe gaps (SPACE)

![floppy](../demos/floppy.gif)

### retro-maze-3d

a GameBoy-style 3D maze escape (WASD + mouse look)

![retro-maze-3d](../demos/retro-maze-3d.gif)


## 📦 Core (23)

### bouncing-ball

a ball bouncing around the window (SPACE pauses, G toggles gravity)

![bouncing-ball](../demos/bouncing-ball.gif)

### following-eyes

two eyes track the mouse

![following-eyes](../demos/following-eyes.gif)

### screen-manager

a LOGO/TITLE/GAMEPLAY/ENDING state flow (ENTER advances)

![screen-manager](../demos/screen-manager.gif)

### input-keys

steer a ball with the arrow keys

![input-keys](../demos/input-keys.gif)

### input-mouse

a ball follows the mouse; click to recolor

![input-mouse](../demos/input-mouse.gif)

### input-gamepad

a live gamepad axis/button readout

![input-gamepad](../demos/input-gamepad.gif)

### mouse-wheel

scroll a box with the mouse wheel

![mouse-wheel](../demos/mouse-wheel.gif)

### gestures-testbed

a testbed for raylib's touch/click gesture detection

![gestures-testbed](../demos/gestures-testbed.gif)

### scissor-test

a scissor rectangle clips a grid (S toggles, mouse moves it)

![scissor-test](../demos/scissor-test.gif)

### random-values

a new random value every two seconds

![random-values](../demos/random-values.gif)

### camera-2d

a 2D camera over a scene (arrows pan, A/S rotate, wheel zooms)

![camera-2d](../demos/camera-2d.gif)

### camera-3d-free

a free-orbit 3D camera (mouse look, wheel zoom)

![camera-3d-free](../demos/camera-3d-free.gif)

### split-screen-3d

two 3D viewports, one per player (W/S, UP/DOWN)

![split-screen-3d](../demos/split-screen-3d.gif)

### first-person-3d

a first-person camera walkthrough (WASD + mouse, 1-4 switch modes)

![first-person-3d](../demos/first-person-3d.gif)

### camera-fps

an FPS camera with jump/crouch physics (WASD, Space, Ctrl)

![camera-fps](../demos/camera-fps.gif)

### world-screen

project 3D world points to 2D screen space (mouse, wheel)

![world-screen](../demos/world-screen.gif)

### picking-3d

click to raycast and pick a 3D cube

![picking-3d](../demos/picking-3d.gif)

### collision-area

AABB collision between two boxes (mouse moves, SPACE)

![collision-area](../demos/collision-area.gif)

### colors-palette

every named raylib color in a grid (hover, SPACE)

![colors-palette](../demos/colors-palette.gif)

### logo-anim

the raylib logo animating in (R replays)

![logo-anim](../demos/logo-anim.gif)

### window-should-close

intercept the close button with a Y/N confirm

![window-should-close](../demos/window-should-close.gif)

### camera-2d-platformer

5 platformer camera-follow styles (SPACE cycles, C/R/wheel)

![camera-2d-platformer](../demos/camera-2d-platformer.gif)

### window-letterbox

resolution-independent rendering via letterboxing (SPACE, resize)

![window-letterbox](../demos/window-letterbox.gif)


## 🔷 Shapes (15)

### logo-raylib

the raylib logo built from rectangles + text

![logo-raylib](../demos/logo-raylib.gif)

### logo-raylib-anim

the logo animating together, piece by piece (R replays)

![logo-raylib-anim](../demos/logo-raylib-anim.gif)

### basic-shapes

circles, rectangles, triangles, polygons + an rlgl triangle

![basic-shapes](../demos/basic-shapes.gif)

### rectangle-scaling

drag the bottom-right corner to resize a rectangle

![rectangle-scaling](../demos/rectangle-scaling.gif)

### mouse-trail

a fading trail follows the cursor

![mouse-trail](../demos/mouse-trail.gif)

### lines-bezier

a cubic bezier curve — drag the endpoints

![lines-bezier](../demos/lines-bezier.gif)

### easings-ball

a ball animating along an easing curve (ENTER replays)

![easings-ball](../demos/easings-ball.gif)

### ball-physics

grab, throw, and resize bouncing balls (click, right-click, wheel)

![ball-physics](../demos/ball-physics.gif)

### simple-particles

water/smoke/fire particle effects (arrows switch, click emits)

![simple-particles](../demos/simple-particles.gif)

### dashed-line

a dashed line follows the mouse (arrows, C)

![dashed-line](../demos/dashed-line.gif)

### starfield-effect

a 3D starfield flying toward the camera (SPACE, wheel)

![starfield-effect](../demos/starfield-effect.gif)

### easings-box

a grid of boxes, each on a different easing curve (SPACE resets)

![easings-box](../demos/easings-box.gif)

### double-pendulum

chaotic double-pendulum motion + trail

![double-pendulum](../demos/double-pendulum.gif)

### lines-drawing

click-drag to paint a rainbow fan of thick lines

![lines-drawing](../demos/lines-drawing.gif)

### easings-rectangles

a grid of rectangles animating on easing curves (SPACE replays)

![easings-rectangles](../demos/easings-rectangles.gif)


## 📝 Text (3)

### writing-anim

a message types itself out (SPACE speeds up, ENTER restarts)

![writing-anim](../demos/writing-anim.gif)

### format-text

padded score + MM:SS timer readouts

![format-text](../demos/format-text.gif)

### input-box

type into a text box (click to focus, Backspace to edit)

![input-box](../demos/input-box.gif)


## 🖼️ Textures (2)

### background-scrolling

a parallax-scrolling background

![background-scrolling](../demos/background-scrolling.gif)

### sprite-animation

a spritesheet character walk cycle (LEFT/RIGHT)

![sprite-animation](../demos/sprite-animation.gif)


## ✨ Shaders (1)

### basic-lighting

dynamic per-pixel lighting (mouse moves it, Y/R/G/B toggle lights)

![basic-lighting](../demos/basic-lighting.gif)


## 🔊 Audio (4)

### audio-module

a music-driven waveform/spectrum visualizer

![audio-module](../demos/audio-module.gif)

### sound-loading

load and play a WAV/OGG sound effect (SPACE, ENTER)

![sound-loading](../demos/sound-loading.gif)

### music-stream

stream an MP3 with play/pause/seek (SPACE, P, arrows)

![music-stream](../demos/music-stream.gif)

### sound-multi

fire the same sound multiple times, overlapping (SPACE)

![sound-multi](../demos/sound-multi.gif)


## 🗿 Models (21)

### geometric-shapes

3D primitive shapes on display

![geometric-shapes](../demos/geometric-shapes.gif)

### waving-cubes

an NxN grid of cubes rippling in 3D

![waving-cubes](../demos/waving-cubes.gif)

### box-collisions

a player cube colliding with 3D boxes (arrow keys)

![box-collisions](../demos/box-collisions.gif)

### orthographic-projection

perspective vs orthographic (SPACE toggles)

![orthographic-projection](../demos/orthographic-projection.gif)

### tesseract-view

a rotating 4D hypercube projected to 2D

![tesseract-view](../demos/tesseract-view.gif)

### solar-system

Sun/Earth/Moon orbiting via the rlgl matrix stack

![solar-system](../demos/solar-system.gif)

### spinning-cubes

a row of cubes, each spinning with a color-cycling phase offset

![spinning-cubes](../demos/spinning-cubes.gif)

### point-cloud

~1500 points forming a rotating sphere (UP/DOWN adjusts)

![point-cloud](../demos/point-cloud.gif)

### wireframe-shapes

pyramid/octahedron/torus/helix in 3D lines (SPACE cycles)

![wireframe-shapes](../demos/wireframe-shapes.gif)

### camera-modes

Free/Orbital/FPS camera modes (1/2/3 switches, WASD)

![camera-modes](../demos/camera-modes.gif)

### ray-picking

click to select cubes via ray-picking

![ray-picking](../demos/ray-picking.gif)

### bouncing-spheres

spheres bouncing inside a 3D box (SPACE, R, G)

![bouncing-spheres](../demos/bouncing-spheres.gif)

### rotating-cube

a single cube spinning via the rlgl matrix stack (arrows, +/-, R)

![rotating-cube](../demos/rotating-cube.gif)

### particle-system

a 3D particle emitter (SPACE bursts, G toggles gravity, W/R reset)

![particle-system](../demos/particle-system.gif)

### dna-helix

a rotating double-helix structure (arrows, SPACE, R)

![dna-helix](../demos/dna-helix.gif)

### first-person-maze

walk a 3D maze in first person (WASD + mouse, M for map)

![first-person-maze](../demos/first-person-maze.gif)

### yaw-pitch-roll

yaw/pitch/roll rotation on a 3D model (arrows, SPACE, R)

![yaw-pitch-roll](../demos/yaw-pitch-roll.gif)

### lissajous-3d

3D Lissajous parametric curves (1-5 picks a pattern, W/S, SPACE)

![lissajous-3d](../demos/lissajous-3d.gif)

### lorenz-attractor

the Lorenz attractor's chaotic butterfly path (1-3, arrows, SPACE, R)

![lorenz-attractor](../demos/lorenz-attractor.gif)

### terrain-generation

procedurally generated terrain (1-3 picks an algorithm, G, W, SPACE)

![terrain-generation](../demos/terrain-generation.gif)

### mesh-generation

procedurally generated 3D meshes (Left/Right cycles, click, SPACE, R)

![mesh-generation](../demos/mesh-generation.gif)
