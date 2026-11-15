package eu.pollux28.imggen.world;

import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.image.ImageMapping;
import eu.pollux28.imggen.image.IndexedImage;
import eu.pollux28.imggen.world.biome.BiomeDimension;
import eu.pollux28.imggen.world.biome.BiomeProfiles;
import eu.pollux28.imggen.world.biome.ImgGenBiomeSource;
import eu.pollux28.imggen.world.chunk.ImgGenChunkGenerator;
import eu.pollux28.imggen.world.terrain.TerrainShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.event.level.LevelEvent;

import javax.annotation.Nullable;

/**
 * One-time setup of a freshly created image world: where players start, and optionally a world border
 * around the image. Runs only when the world is created, so later changes by the player are respected.
 */
public final class ImageWorldSetup {
    /** Distance in blocks between two candidate spawn points. */
    private static final int SPAWN_SEARCH_STEP = 8;

    private ImageWorldSetup() {
    }

    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ImgGenChunkGenerator generator = ImgGen.imageGenerator(level);
        if (generator == null || !(generator.getBiomeSource() instanceof ImgGenBiomeSource biomeSource)) {
            return;
        }
        IndexedImage<Holder<Biome>> image = biomeSource.biomeImage();
        if (image == null) {
            return; // No usable image: the world is plain vanilla or all default biome, leave it to vanilla.
        }

        if (generator.config().limitWorldToImage) {
            applyWorldBorder(level, image.mapping(), event.getSettings());
        }

        BlockPos spawn = findSpawn(level, generator, biomeSource, image);
        if (spawn != null) {
            event.getSettings().setSpawn(spawn, 0.0F);
            event.setCanceled(true); // Skip the vanilla search, which knows nothing about the image.
            ImgGen.LOGGER.info("World spawn set to {}, {}, {} inside the biome image", spawn.getX(), spawn.getY(), spawn.getZ());
        } else {
            ImgGen.LOGGER.warn("Found no dry land inside the biome image to spawn on, leaving the spawn point to vanilla");
        }
    }

    /**
     * The vanilla border is always a square, so a rectangular image gets the square around its longer
     * side; the strips left over along the shorter side lie outside the image.
     */
    private static void applyWorldBorder(ServerLevel level, ImageMapping mapping, ServerLevelData levelData) {
        if (mapping.repeat()) {
            ImgGen.LOGGER.warn("limitWorldToImage is ignored because the biome image repeats endlessly");
            return;
        }

        WorldBorder border = level.getWorldBorder();
        double size = Math.max(mapping.blockWidth(), mapping.blockDepth());
        border.setCenter(mapping.centerBlockX(), mapping.centerBlockZ());
        border.setSize(Mth.clamp(size, 1.0, border.getAbsoluteMaxSize()));
        // The server re-applies the stored border right after this event, so it has to be stored as well.
        levelData.setWorldBorder(border.createSettings());
        ImgGen.LOGGER.info(
                "World border set to {} blocks around {}, {} to fit the biome image",
                (long) border.getSize(), border.getCenterX(), border.getCenterZ()
        );
    }

    /**
     * Looks for the dry spot closest to the centre of the image, walking outwards in square rings.
     * Ordinary overworld land is preferred; anything that is not water or void is the fallback.
     */
    @Nullable
    private static BlockPos findSpawn(ServerLevel level, ImgGenChunkGenerator generator, ImgGenBiomeSource biomeSource, IndexedImage<Holder<Biome>> image) {
        ImageMapping mapping = image.mapping();
        BiomeProfiles profiles = biomeSource.profiles();
        RandomState randomState = level.getChunkSource().randomState();
        int centerX = Mth.floor(mapping.centerBlockX());
        int centerZ = Mth.floor(mapping.centerBlockZ());
        int maxRadius = Mth.ceil(Math.max(mapping.blockWidth(), mapping.blockDepth()) / 2);
        BlockPos fallback = null;

        for (int radius = 0; radius <= maxRadius; radius += SPAWN_SEARCH_STEP) {
            for (int offsetX = -radius; offsetX <= radius; offsetX += SPAWN_SEARCH_STEP) {
                boolean edgeColumn = Math.abs(offsetX) == radius;
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ += edgeColumn || radius == 0 ? SPAWN_SEARCH_STEP : radius * 2) {
                    int x = centerX + offsetX;
                    int z = centerZ + offsetZ;
                    Holder<Biome> biome = image.get(x, z);
                    if (biome == null) {
                        continue;
                    }

                    TerrainShape shape = profiles.terrain(biome);
                    if (shape == TerrainShape.VOID || shape == TerrainShape.DEEP_OCEAN || shape == TerrainShape.OCEAN || shape == TerrainShape.RIVER) {
                        continue;
                    }
                    boolean preferred = shape != TerrainShape.PEAK && profiles.dimension(biome) == BiomeDimension.OVERWORLD;
                    if (!preferred && fallback != null) {
                        continue;
                    }

                    int y = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
                    if (y < generator.getSeaLevel()) {
                        continue; // Painted as land, but this spot ended up under water.
                    }
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (preferred) {
                        return candidate;
                    }
                    fallback = candidate;
                }
            }
        }
        return fallback;
    }
}
