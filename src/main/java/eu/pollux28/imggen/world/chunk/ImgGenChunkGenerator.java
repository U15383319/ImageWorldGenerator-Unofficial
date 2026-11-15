package eu.pollux28.imggen.world.chunk;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pollux28.imggen.config.ImgGenConfig;
import eu.pollux28.imggen.image.HeightImage;
import eu.pollux28.imggen.image.ImageFiles;
import eu.pollux28.imggen.world.biome.ImgGenBiomeSource;
import eu.pollux28.imggen.world.structure.ImageStructures;
import eu.pollux28.imggen.world.terrain.ImageClimateFunction;
import eu.pollux28.imggen.world.terrain.ImageTerrain;
import eu.pollux28.imggen.world.terrain.TerrainShape;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The vanilla noise generator with three additions driven by the config:
 * <ul>
 *     <li>terrain that follows the biome image (see {@link ImageTerrain}), overridden by the height map where one is set,</li>
 *     <li>materials for Nether, End and void biomes painted into the overworld,</li>
 *     <li>structures placed from the structure image and a structure blacklist.</li>
 * </ul>
 *
 * <p>The extra behaviour is injected through a private copy of the noise settings handed to the vanilla
 * generator, so caves, aquifers, carvers, features and mob spawning all run unchanged vanilla code. The
 * world itself keeps storing the plain settings it was created with.
 */
public final class ImgGenChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<ImgGenChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(generator -> generator.baseSettings)
    ).apply(instance, instance.stable(ImgGenChunkGenerator::new)));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
    private static final BlockState NETHERRACK = Blocks.NETHERRACK.defaultBlockState();
    private static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();
    /** Horizontal size in blocks of one biome cell. */
    private static final int CELL_SIZE = QuartPos.SIZE;

    private final Holder<NoiseGeneratorSettings> baseSettings;
    private final ImgGenConfig config;
    /** {@code null} when another mod or data pack combined this generator with a foreign biome source. */
    @Nullable
    private final ImgGenBiomeSource imageBiomes;
    @Nullable
    private final ImageTerrain terrain;

    /** {@code null} until the first request; empty when the height map is off or unusable. */
    @Nullable
    private volatile Optional<HeightImage> heightImage;
    @Nullable
    private volatile ImageStructures imageStructures;

    public ImgGenChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        this(biomeSource, settings, ImgGenConfig.load());
    }

    private ImgGenChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, ImgGenConfig config) {
        this(biomeSource, settings, config, createTerrain(biomeSource, config));
    }

    private ImgGenChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings, ImgGenConfig config, @Nullable ImageTerrain terrain) {
        super(biomeSource, Holder.direct(createEffectiveSettings(settings.value(), config, terrain)));
        this.baseSettings = settings;
        this.config = config;
        this.imageBiomes = biomeSource instanceof ImgGenBiomeSource source ? source : null;
        this.terrain = terrain;
    }

    @Nullable
    private static ImageTerrain createTerrain(BiomeSource biomeSource, ImgGenConfig config) {
        if (biomeSource instanceof ImgGenBiomeSource source) {
            source.bindConfig(config);
            return new ImageTerrain(source);
        }
        return null;
    }

    private static NoiseGeneratorSettings createEffectiveSettings(NoiseGeneratorSettings base, ImgGenConfig config, @Nullable ImageTerrain terrain) {
        int seaLevel = config.customHeightMap ? config.seaLevel : base.seaLevel();
        NoiseRouter router = terrain != null ? ImageClimateFunction.steer(base.noiseRouter(), terrain) : base.noiseRouter();

        return new NoiseGeneratorSettings(
                base.noiseSettings(),
                base.defaultBlock(),
                base.defaultFluid(),
                router,
                ImageBiomeSurfaceRules.create(base.surfaceRule(), seaLevel),
                base.spawnTarget(),
                seaLevel,
                base.disableMobGeneration(),
                base.aquifersEnabled(),
                base.oreVeinsEnabled(),
                base.useLegacyRandomSource()
        );
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public ImgGenConfig config() {
        return config;
    }

    /** @return the terrain shape painted at the column, or {@code null} without an image biome source */
    @Nullable
    public TerrainShape terrainShapeAt(int blockX, int blockZ) {
        return terrain == null ? null : terrain.shapeAt(blockX, blockZ);
    }

    /** @return the surface Y the height map dictates for the column, or {@link HeightImage#OUTSIDE} */
    public int heightMapHeightAt(int blockX, int blockZ) {
        HeightImage heights = heightImage();
        return heights == null ? HeightImage.OUTSIDE : heights.get(blockX, blockZ);
    }

    // ---------------------------------------------------------------- terrain

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        HeightImage heights = heightImage();
        ChunkPos chunkPos = chunk.getPos();

        if (heights != null && heights.coversChunk(chunkPos.getMinBlockX(), chunkPos.getMinBlockZ())) {
            // The image decides every column of the chunk: computing noise first would be wasted work.
            return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("wgen_fill_height_map", () -> {
                fillFromHeightMap(chunk, heights, false);
                applyBiomeMaterials(chunk);
                return chunk;
            }), Util.backgroundExecutor());
        }

        return super.fillFromNoise(blender, randomState, structureManager, chunk).thenApply(filled -> {
            if (heights != null) {
                fillFromHeightMap(filled, heights, true);
            }
            applyBiomeMaterials(filled);
            return filled;
        });
    }

    /**
     * @param replaceExisting whether the chunk already holds noise terrain that has to be overwritten
     */
    private void fillFromHeightMap(ChunkAccess chunk, HeightImage heights, boolean replaceExisting) {
        NoiseGeneratorSettings settings = generatorSettings().value();
        BlockState solid = settings.defaultBlock();
        BlockState fluid = settings.defaultFluid();
        int seaLevel = settings.seaLevel();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                int height = heights.get(x, z);
                if (height == HeightImage.OUTSIDE) {
                    continue;
                }

                int surface = Mth.clamp(height, minY, maxY - 1);
                int top = replaceExisting ? maxY : Math.max(surface + 1, seaLevel);
                for (int y = minY; y < top; y++) {
                    chunk.setBlockState(pos.set(x, y, z), y <= surface ? solid : y < seaLevel ? fluid : AIR, false);
                }
            }
        }
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        super.buildSurface(level, structureManager, randomState, chunk);
        replaceOverworldStone(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        if (isVoidColumn(x, z)) {
            return level.getMinBuildHeight();
        }

        int height = heightMapHeightAt(x, z);
        if (height == HeightImage.OUTSIDE) {
            return super.getBaseHeight(x, z, type, level, randomState);
        }

        NoiseGeneratorSettings settings = generatorSettings().value();
        int surface = Mth.clamp(height, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1);
        boolean countsFluid = type.isOpaque().test(settings.defaultFluid());
        return countsFluid ? Math.max(surface + 1, settings.seaLevel()) : surface + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        boolean isVoid = isVoidColumn(x, z);
        int height = isVoid ? HeightImage.OUTSIDE : heightMapHeightAt(x, z);
        if (!isVoid && height == HeightImage.OUTSIDE) {
            return super.getBaseColumn(x, z, level, randomState);
        }

        NoiseGeneratorSettings settings = generatorSettings().value();
        int minY = level.getMinBuildHeight();
        int surface = isVoid ? minY - 1 : Mth.clamp(height, minY, level.getMaxBuildHeight() - 1);
        int seaLevel = isVoid ? minY : settings.seaLevel();
        BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = minY + i;
            states[i] = y <= surface ? settings.defaultBlock() : y < seaLevel ? settings.defaultFluid() : AIR;
        }
        return new NoiseColumn(minY, states);
    }

    private boolean isVoidColumn(int blockX, int blockZ) {
        return terrain != null && terrain.shapeAt(blockX, blockZ) == TerrainShape.VOID;
    }

    @Nullable
    private HeightImage heightImage() {
        Optional<HeightImage> result = heightImage;
        if (result == null) {
            synchronized (this) {
                result = heightImage;
                if (result == null) {
                    result = !config.customHeightMap
                            ? Optional.empty()
                            : ImageFiles.load(config.heightMapName, "height map", image -> HeightImage.create(
                                    image,
                                    config.heightMapScale,
                                    config.repeatHeightMapImage,
                                    config.repeatMirrorHeightMapImage
                            ));
                    heightImage = result;
                }
            }
        }
        return result.orElse(null);
    }

    // ------------------------------------------- out-of-place biome materials

    /**
     * Right after the terrain is shaped: empties void biomes and turns the water of Nether biomes into
     * lava. Works per biome cell, matching the resolution biomes are stored at.
     */
    private void applyBiomeMaterials(ChunkAccess chunk) {
        if (imageBiomes == null) {
            return;
        }

        BlockState fluid = generatorSettings().value().defaultFluid();
        boolean replaceFluid = !fluid.getFluidState().isEmpty() && !fluid.is(Blocks.LAVA);
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int cellX = chunkPos.getMinBlockX(); cellX <= chunkPos.getMaxBlockX(); cellX += CELL_SIZE) {
            for (int cellZ = chunkPos.getMinBlockZ(); cellZ <= chunkPos.getMaxBlockZ(); cellZ += CELL_SIZE) {
                Holder<Biome> biome = imageBiomes.paintedBiome(cellX, cellZ);
                if (biome == null) {
                    continue;
                }

                if (TerrainShape.of(biome) == TerrainShape.VOID) {
                    forEachBlockInCell(chunk, cellX, cellZ, pos, state -> state.isAir() ? null : AIR);
                } else if (replaceFluid && biome.is(BiomeTags.IS_NETHER)) {
                    forEachBlockInCell(chunk, cellX, cellZ, pos, state -> state == fluid ? LAVA : null);
                }
            }
        }
    }

    /**
     * After the surface rules ran: they only touch the default block, so the other stone types and ore
     * veins of the overworld noise are still inside Nether and End biomes.
     */
    private void replaceOverworldStone(ChunkAccess chunk) {
        if (imageBiomes == null) {
            return;
        }

        BlockState defaultBlock = generatorSettings().value().defaultBlock();
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int cellX = chunkPos.getMinBlockX(); cellX <= chunkPos.getMaxBlockX(); cellX += CELL_SIZE) {
            for (int cellZ = chunkPos.getMinBlockZ(); cellZ <= chunkPos.getMaxBlockZ(); cellZ += CELL_SIZE) {
                Holder<Biome> biome = imageBiomes.paintedBiome(cellX, cellZ);
                if (biome == null) {
                    continue;
                }

                BlockState replacement = biome.is(BiomeTags.IS_NETHER) ? NETHERRACK : biome.is(BiomeTags.IS_END) ? END_STONE : null;
                if (replacement != null) {
                    forEachBlockInCell(chunk, cellX, cellZ, pos, state -> isOverworldStone(state, defaultBlock) ? replacement : null);
                }
            }
        }
    }

    private static boolean isOverworldStone(BlockState state, BlockState defaultBlock) {
        return state == defaultBlock
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.RAW_IRON_BLOCK)
                || state.is(Blocks.COPPER_ORE)
                || state.is(Blocks.DEEPSLATE_COPPER_ORE)
                || state.is(Blocks.RAW_COPPER_BLOCK);
    }

    private interface BlockReplacer {
        /** @return the state to place instead, or {@code null} to keep the block */
        @Nullable
        BlockState replace(BlockState state);
    }

    private static void forEachBlockInCell(ChunkAccess chunk, int cellX, int cellZ, BlockPos.MutableBlockPos pos, BlockReplacer replacer) {
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        for (int x = cellX; x < cellX + CELL_SIZE; x++) {
            for (int z = cellZ; z < cellZ + CELL_SIZE; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockState replacement = replacer.replace(chunk.getBlockState(pos.set(x, y, z)));
                    if (replacement != null) {
                        chunk.setBlockState(pos, replacement, false);
                        if (!replacement.getFluidState().isEmpty()) {
                            chunk.markPosForPostprocessing(pos);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------- structures

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets, RandomState randomState, long seed) {
        if (config.placeVanillaStructures && !config.blacklistAppliesToVanilla()) {
            return super.createState(structureSets, randomState, seed);
        }

        List<Holder<StructureSet>> allowed = List.of();
        if (config.placeVanillaStructures) {
            Set<ResourceKey<Structure>> blacklist = ImageStructures.blacklist(config);
            Set<Holder<Biome>> possibleBiomes = biomeSource.possibleBiomes();
            allowed = structureSets.listElements()
                    .<Holder<StructureSet>>flatMap(set -> ImageStructures.withoutBlacklisted(set, blacklist).stream())
                    .filter(set -> set.value().structures().stream()
                            .flatMap(entry -> entry.structure().value().biomes().stream())
                            .anyMatch(possibleBiomes::contains))
                    .toList();
        }
        // Same as the vanilla state, just built from the structure sets that are still allowed.
        return new ChunkGeneratorStructureState(randomState, biomeSource, seed, seed, allowed);
    }

    @Override
    public void createStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager templateManager
    ) {
        super.createStructures(registryAccess, structureState, structureManager, chunk, templateManager);
        if (!config.customStructures) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        SectionPos sectionPos = SectionPos.bottomOf(chunk);
        for (Holder<Structure> holder : imageStructures(registryAccess).inChunk(chunkPos)) {
            Structure structure = holder.value();
            StructureStart existing = structureManager.getStartForStructure(sectionPos, structure, chunk);
            if (existing != null && existing.isValid()) {
                continue;
            }

            // The image explicitly asks for the structure here, so its usual biome requirement is waived.
            StructureStart start = structure.generate(
                    registryAccess,
                    this,
                    biomeSource,
                    structureState.randomState(),
                    templateManager,
                    structureState.getLevelSeed(),
                    chunkPos,
                    existing != null ? existing.getReferences() : 0,
                    chunk,
                    biome -> true
            );
            if (start.isValid()) {
                structureManager.setStartForStructure(sectionPos, structure, start, chunk);
            }
        }
    }

    private ImageStructures imageStructures(RegistryAccess registryAccess) {
        ImageStructures result = imageStructures;
        if (result == null) {
            synchronized (this) {
                result = imageStructures;
                if (result == null) {
                    result = ImageStructures.load(config, registryAccess.registryOrThrow(Registries.STRUCTURE));
                    imageStructures = result;
                }
            }
        }
        return result;
    }
}
