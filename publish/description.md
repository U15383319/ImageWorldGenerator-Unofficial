# Image World Generator: Repainted

**Turn a picture into a world.** This mod generates a Minecraft world from an image: every colour becomes a biome and the terrain follows it. Oceans are deep, rivers cut valleys, beaches sit at the waterline and peaks become mountains. Optional images set exact heights and place structures.

> This is an **unofficial continuation** of [ImageWorldGenerator](https://github.com/28Pollux28/ImageWorldGenerator) by 28Pollux28 (Fabric, Minecraft 1.16.5), ported to NeoForge 1.21.1 and reworked for the new world generation. It is not affiliated with the original author. Licensed under LGPL-3.0, like the original.

## Features

- **Any image works.** Every pixel is matched to the biome with the closest colour. All vanilla biomes have a built-in colour, and you can add or override colours in the config.
- **Terrain that fits the picture.** The shoreline follows the image, while the land itself is shaped by the game's own terrain noise, so it looks natural and keeps its caves, aquifers and ores.
- **Height map (optional).** A greyscale image sets the exact surface height of every column.
- **Structure image (optional).** Place villages, temples, strongholds and other structures where you want them, and blacklist the ones you do not want at all.
- **Biomes of other mods.** `/imggen colors` gives every modded biome a colour and writes a reference sheet you can open in a browser. With TerraBlender installed, biomes from mods built on it keep their own surface blocks.
- **Nether, End and void biomes** can appear in the overworld and bring their own blocks. The void leaves a hole.
- **A world that ends with the image.** Outside the image there is nothing by default. Prefer an endless sea or vanilla terrain out there? One config option. A world border around the image is available too.

## Getting started

1. Start the game once. This creates `config/imggen.json` and the folder `imggen/image/` in the game directory.
2. Put your image into `imggen/image/` and set its file name as `biomesImageName` in the config.
3. Create a world with the world type **Image World**. On a dedicated server, set `level-type=imggen\:imggen` in `server.properties`.

The image is centred on the world origin, and one pixel is one block unless you change `biomesImageScale`. `/imggen debug` shows which pixel and biome apply where you stand.

## Good to know

- The config and the images are **not stored in the world save**. If you change them after creating a world, newly generated chunks will not line up with the old ones.
- Only the overworld is generated from images; the Nether and the End are vanilla.
- The height map covers Y 0 to 255.
