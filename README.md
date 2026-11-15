# ImageWorldGenerator-Unofficial

A NeoForge mod for Minecraft 1.21.1 that generates a world from images: one image paints the biomes (and
with them the terrain), optional ones set exact heights and place structures.

This project is an unofficial port of [ImageWorldGenerator](https://github.com/28Pollux28/ImageWorldGenerator),
originally developed by 28Pollux28 for Minecraft 1.16.5 (Fabric). The unfinished Fabric 1.20.1 port lives on
the [`1.20.1-fabric`](../../tree/1.20.1-fabric) branch.

## Usage

1. Start the game once. This creates `config/imggen.json` and the folder `imggen/image/` in the game directory.
2. Put your images into `imggen/image/` and adjust the config (it is re-read every time a world is loaded).
3. Create a world with the world type **Image World**. On a dedicated server set `level-type=imggen\:imggen`
   in `server.properties`.

Images are centred on the world origin.

Commands (operators only):

- `/imggen debug [pos]` shows which pixel, painted biome, actual biome and terrain shape apply to a position.
- `/imggen colors` gives every biome without a colour one of its own, see [Other mods](#other-mods).

### Biome image

Every pixel colour selects a biome; one pixel covers `biomesImageScale` blocks. The built-in colours are
listed in [`DefaultBiomeColors`](src/main/java/eu/pollux28/imggen/world/biome/DefaultBiomeColors.java), and
`customBiomes` adds or overrides entries, modded biomes included:

```json
"customBiomes": [ { "biomeID": "minecraft:cherry_grove", "RGB": "0xFFB7C5" } ]
```

Colours that are not in the table fall back to the closest known colour, so save maps without
anti-aliasing. Outside the image the world continues with vanilla generation (`continuousGen: true`) or
consists of `defaultBiome` only.

The terrain follows the painted biomes: oceans are deep, rivers cut valleys, peaks become mountains. Biomes
are classified by their tags, so modded biomes get fitting terrain as well. Nether, End and void biomes can
be painted too and bring their own blocks; `minecraft:the_void` leaves a hole.

### Height map

With `customHeightMap: true` the image `heightMapName` dictates the surface exactly: the grey level of a
pixel (0-255) is the surface Y, and everything below `seaLevel` is flooded.

### Structures

- `placeVanillaStructures`: keep normal structure generation.
- `customStructures`: additionally place structures from `customStructuresImage`, where a non-black pixel
  puts its structure into that chunk (one pixel is one block). Built-in colours are listed in
  [`DefaultStructureColors`](src/main/java/eu/pollux28/imggen/world/structure/DefaultStructureColors.java);
  `customStructuresRGB` adds more (`{ "structureID": "...", "RGB": "..." }`).
- `useBlackList` with `structureBlacklist`: structures that must not generate. `blackListApplication`
  selects where it applies: `0` vanilla placement, `1` the structure image, `2` both.

### Other mods

- Biomes of other mods have no built-in colour. Either list them in `customBiomes`, or run `/imggen colors`
  in any world: it assigns a colour to every biome that has none and writes two files into `imggen/`:
  - `biome_colors.html`, a sheet of all biomes with their colours to pick from while painting;
  - `biome_colors.json`, the assigned colours. From then on it is read on every world load, after
    `customBiomes` and the built-in colours. Existing entries are never changed, running the command again
    only adds biomes of newly installed mods. Edit a colour by hand if you like.

  Note that more known colours also means that inexact colours of an image (anti-aliasing, JPEG artefacts)
  may now land on one of these biomes instead of a vanilla one.
- The terrain of a biome is chosen from its biome tags, and so is whether it counts as a Nether or End
  biome. A biome without the usual tags is treated as ordinary overworld lowland. Each entry of
  `biome_colors.json` can override both with two optional fields:

  ```json
  { "biome": "somemod:crag", "color": "0x7A5C3E", "terrain": "peak", "dimension": "overworld" }
  ```

  `terrain` is one of `deep_ocean`, `ocean`, `river`, `shore`, `wetland`, `lowland`, `hills`, `plateau`,
  `slope`, `peak`, `void` or `unshaped` (leave the vanilla terrain alone); `dimension` is `overworld`,
  `nether` or `end`. Entries may be added for vanilla biomes too, and `color` can be left out there.
- With [TerraBlender](https://github.com/Glitchfiend/TerraBlender) installed, biomes of mods built on it
  (Biomes O' Plenty and others) get the surface blocks those mods define. Without TerraBlender nothing
  changes; it is optional.
- Nether and End biomes of other mods use the surface rules of the real Nether and End, so their own
  blocks appear when painted into the overworld.
- Outside the image, vanilla generation only produces vanilla biomes.

### Other options

| Option | Meaning |
| --- | --- |
| `repeat…Image` / `repeatMirror…Image` | Tile the image endlessly, optionally mirroring every other tile. |
| `lakeFormationPercentChance` | Chance (0-1) that a lake feature is skipped in image worlds. |

## Building

```
./gradlew build
```

Requires JDK 21. The jar is written to `build/libs/`.

## License

Licensed under [GNU LGPL-3.0](LICENSE), the same license as the original mod.
