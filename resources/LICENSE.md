# Media asset licenses

**These assets are not covered by this project's EPL-2.0 license** (see
`../LICENSE`). Every one of them is a raylib example asset, redistributed here
unmodified so the examples run out of the box. Their terms are reproduced
from raylib's own `examples/*/resources/LICENSE.md`, where stated - one asset
is not listed there at all, and is marked as such below.

Read the table before reusing anything here. Two rows need attention:

- **`scarfy.png` and `fudesumi.png` are CC-BY-NC**: non-commercial only, which
  is *more restrictive* than the code around them.
- **`patterns.png` has no stated licence.** raylib redistributes it without a
  licence-table entry, and still does at current upstream, so its terms are
  genuinely unknown rather than merely uncited here. It is reproduced with
  that unknown carried forward rather than being silently assumed permissive.

| File | Author | License | Used by | Source |
|------|--------|---------|---------|--------|
| `scarfy.png` | [Eiden Marsal](https://www.artstation.com/marshall_z) | [**CC-BY-NC 4.0**](https://creativecommons.org/licenses/by-nc/4.0/legalcode), non-commercial | `sprite-animation` | raylib `examples/textures/resources` |
| `cyberpunk_street_background.png` | [Luis Zuno](http://ansimuz.com/site/) | [CC-BY 3.0](http://creativecommons.org/licenses/by/3.0/) | `background-scrolling` | [Cyberpunk Street Environment](https://ansimuz.itch.io/cyberpunk-street-environment) |
| `cyberpunk_street_midground.png` | [Luis Zuno](http://ansimuz.com/site/) | [CC-BY 3.0](http://creativecommons.org/licenses/by/3.0/) | `background-scrolling` | [Cyberpunk Street Environment](https://ansimuz.itch.io/cyberpunk-street-environment) |
| `cyberpunk_street_foreground.png` | [Luis Zuno](http://ansimuz.com/site/) | [CC-BY 3.0](http://creativecommons.org/licenses/by/3.0/) | `background-scrolling` | [Cyberpunk Street Environment](https://ansimuz.itch.io/cyberpunk-street-environment) |
| `ps3.png` | [@raysan5](https://github.com/raysan5) | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) | `input-gamepad` | raylib `examples/core/resources` |
| `xbox.png` | [@raysan5](https://github.com/raysan5) | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) | `input-gamepad` | raylib `examples/core/resources` |
| `country.mp3` | [@emegeme](https://github.com/emegeme) | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) | `music-stream` | raylib `examples/audio/resources` |
| `target.ogg` | [@emegeme](https://github.com/emegeme) | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) | `sound-loading` | raylib `examples/audio/resources` |
| `sound.wav` | [@raysan5](https://github.com/raysan5) | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) | `sound-loading`, `sound-multi` | Made with [rFXGen](https://raylibtech.itch.io/rfxgen) |
| `mini1111.xm` | [tPORt](https://modarchive.org/index.php?request=view_by_moduleid&query=51891) | [Mod Archive Distribution license](https://modarchive.org/index.php?terms-upload) | `audio-module` | raylib `examples/audio/resources` |
| `raysan.png` | [@raysan5](https://github.com/raysan5) | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) | `ascii-rendering` | raylib `examples/shaders/resources` |
| `fudesumi.png` | [Eiden Marsal](https://www.artstation.com/marshall_z) | [**CC-BY-NC 4.0**](https://creativecommons.org/licenses/by-nc/4.0/), non-commercial | `ascii-rendering` | raylib `examples/shaders/resources` |
| `patterns.png` | **unknown** | **unstated** | `tiled-drawing` | raylib `examples/textures/resources` |
| `billboard.png` | [@raysan5](https://github.com/raysan5) | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) | `billboard-rendering` | raylib `examples/models/resources` |
| `cubicmap.png` | [@raysan5](https://github.com/raysan5) | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) | `cubicmap-rendering` | raylib `examples/models/resources` |
| `cubicmap_atlas.png` | [@raysan5](https://github.com/raysan5) | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) | `cubicmap-rendering` | raylib `examples/models/resources` |
| `heightmap.png` | [@raysan5](https://github.com/raysan5) | [CC0](https://creativecommons.org/publicdomain/zero/1.0/) | `heightmap-rendering` | raylib `examples/models/resources` |
| `shaders/glsl330/lighting.vs` | [@raysan5](https://github.com/raysan5) | zlib/libpng | `basic-lighting` | raylib `examples/shaders/resources/shaders/glsl330` |
| `shaders/glsl330/lighting.fs` | [@raysan5](https://github.com/raysan5) | zlib/libpng | `basic-lighting` | raylib `examples/shaders/resources/shaders/glsl330` |
| `shaders/glsl330/palette_switch.fs` | [@raysan5](https://github.com/raysan5) | zlib/libpng | `palette-switch` | raylib `examples/shaders/resources/shaders/glsl330` |
| `shaders/glsl330/ascii.fs` | [@raysan5](https://github.com/raysan5) | zlib/libpng | `ascii-rendering` | raylib `examples/shaders/resources/shaders/glsl330` |

## If the restricted assets are a problem for you

Each is confined to a single example, so each can be dropped independently:

| File | Restriction | Example to drop with it |
|------|-------------|-------------------------|
| `scarfy.png` | CC-BY-NC, non-commercial | `sprite-animation` |
| `fudesumi.png` | CC-BY-NC, non-commercial | `ascii-rendering` |
| `patterns.png` | licence unknown | `tiled-drawing` |

Removing all three files and their three examples leaves the rest of the tree
uniformly permissive and fully attributed: CC0, CC-BY 3.0, zlib, and one Mod
Archive module (none of them share-alike).

`raysan.png` is also used by `ascii-rendering`, but it is CC0 and can stay
either way.
