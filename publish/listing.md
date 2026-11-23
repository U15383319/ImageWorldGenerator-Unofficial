# Listing details

Everything a project page asks for besides the long description, which is in `description.md`.

## Name

Image World Generator: Repainted

Mod id: `imggen` (unchanged from the original mod)

## Summary

Generate a Minecraft world from images: paint biomes, heights and structures as colours and the terrain
follows your map. Unofficial NeoForge continuation of ImageWorldGenerator.

## Categories

- CurseForge: World Gen, with the subcategories Biomes and Structures
- Modrinth: `worldgen`, optionally `adventure`

## Environment

- Server: required
- Client: required in single player. Not verified for clients joining a server that has the mod; without
  it the feedback of the `/imggen` command shows untranslated keys.

## Dependencies

| | |
| --- | --- |
| Required | Minecraft 1.21.1, NeoForge 21.1.x, Java 21 |
| Optional | TerraBlender 4.1 or newer |

Built and tested against NeoForge 21.1.228.

## License

LGPL-3.0, the license of the original mod.

## Files

- `logo.png`: 1000x1000 project icon, in the style of the original mod's icon
- `images/`: pictures for the gallery of the project page. The description cannot refer to local files, so
  it contains none; upload them to the gallery instead.
  - `golden-apple-overview.png`: in-game screenshot of a world generated from the golden apple item texture,
    scaled up 32 times (`biomesImageScale: 32`), seen from straight above in spectator mode
  - `painted-map.png`: a biome image made for testing. There is no in-game screenshot of its world yet, so
    it is only useful next to one.

## Links

Entered in the link fields of the project page, not in the description:

- Source: https://github.com/U15383319/ImageWorldGenerator-Unofficial
- Issues: https://github.com/U15383319/ImageWorldGenerator-Unofficial/issues

## Changelog

### 2.0.0 for NeoForge 1.21.1

First release of this continuation. Compared to ImageWorldGenerator 1.3.2 (Fabric 1.16.5):

- Ported to NeoForge for Minecraft 1.21.1.
- Terrain follows the painted biomes: oceans, rivers, beaches, hills and peaks are shaped from the image.
- The world type is now a regular world preset ("Image World"); on servers use `level-type=imggen\:imggen`.
- Biomes of other mods: `/imggen colors` assigns colours and writes a reference sheet; TerraBlender based
  mods keep their own surface blocks.
- Nether, End and void biomes can be painted into the overworld.
- Outside the image the world is void by default. New option `limitWorldToImage` adds a world border
  around the image.
- Players spawn on land inside the image.
- Images are loaded once into a compact form; any number of colours is supported.
- Removed options: `largeBiomes`, `segmentImageWidth`, `segmentImageHeight`, `imageCacheSize`.
- Existing config files keep working; `continuousGen` and `defaultBiome` only change for newly created
  configs.
