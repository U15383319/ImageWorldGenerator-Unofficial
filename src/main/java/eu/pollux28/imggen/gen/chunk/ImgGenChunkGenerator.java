package eu.pollux28.imggen.gen.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.gen.biomes.ImgGenBiomeSource;
import eu.pollux28.imggen.gen.heightmap.HeightMapSource;
import eu.pollux28.imggen.gen.density.ImageTerrainDensityFunction;
import eu.pollux28.imggen.gen.structures.StructuresSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ImgGenChunkGenerator extends ChunkGenerator {
    public static final Codec<ImgGenChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(generator -> generator.settings)
    ).apply(instance, instance.stable(ImgGenChunkGenerator::new)));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();

    private final Holder<NoiseGeneratorSettings> settings;
    private final Holder<NoiseGeneratorSettings> effectiveSettings;
    private final NoiseBasedChunkGenerator delegate;
    private HeightMapSource heightMapSource;
    private StructuresSource structuresSource;

    public ImgGenChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;
        this.effectiveSettings = Holder.direct(withImageTerrain(settings.value()));
        this.delegate = new NoiseBasedChunkGenerator(biomeSource, effectiveSettings);
        reloadFromConfig();
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public void reloadFromConfig() {
        ImgGen.ensureConfig();

        if (this.biomeSource instanceof ImgGenBiomeSource imgGenBiomeSource) {
            imgGenBiomeSource.reloadFromConfig();
        }

        this.heightMapSource = ImgGen.CONFIG.customHeightMap ? new HeightMapSource() : null;
        this.structuresSource = null;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> holderLookup, RandomState randomState, long seed) {
        return super.createState(holderLookup, randomState, seed);
    }

    @Override
    public void applyCarvers(
            WorldGenRegion worldGenRegion,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunkAccess,
            GenerationStep.Carving carving
    ) {
        delegate.applyCarvers(worldGenRegion, seed, randomState, biomeManager, structureManager, chunkAccess, carving);
    }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess) {
        delegate.buildSurface(worldGenRegion, structureManager, randomState, chunkAccess);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
        delegate.spawnOriginalMobs(worldGenRegion);
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor levelHeightAccessor) {
        return delegate.getSpawnHeight(levelHeightAccessor);
    }

    @Override
    public int getGenDepth() {
        return delegate.getGenDepth();
    }

    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(
            Holder<Biome> biome,
            StructureManager structureManager,
            MobCategory mobCategory,
            BlockPos blockPos
    ) {
        return delegate.getMobsAt(biome, structureManager, mobCategory, blockPos);
    }

    @Override
    public void createStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState chunkGeneratorStructureState,
            StructureManager structureManager,
            ChunkAccess chunkAccess,
            StructureTemplateManager structureTemplateManager
    ) {
        ImgGen.ensureConfig();
        StructuresSource source = (ImgGen.CONFIG.customStructures || ImgGen.CONFIG.useBlackList)
                ? getStructuresSource(registryAccess.registryOrThrow(Registries.STRUCTURE))
                : null;

        if (ImgGen.CONFIG.placeVanillaStructures) {
            if (ImgGen.CONFIG.useBlackList && (ImgGen.CONFIG.blackListApplication == 0 || ImgGen.CONFIG.blackListApplication == 2)) {
                createVanillaStructures(registryAccess, chunkGeneratorStructureState, structureManager, chunkAccess, structureTemplateManager, source);
            } else {
                super.createStructures(registryAccess, chunkGeneratorStructureState, structureManager, chunkAccess, structureTemplateManager);
            }
        }

        if (ImgGen.CONFIG.customStructures && source != null) {
            createImageStructures(registryAccess, chunkGeneratorStructureState, structureManager, chunkAccess, structureTemplateManager, source);
        }
    }

    @Override
    public void createReferences(WorldGenLevel worldGenLevel, StructureManager structureManager, ChunkAccess chunkAccess) {
        if (ImgGen.CONFIG == null || ImgGen.CONFIG.placeVanillaStructures || ImgGen.CONFIG.customStructures) {
            super.createReferences(worldGenLevel, structureManager, chunkAccess);
        }
    }

    private void createVanillaStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState chunkGeneratorStructureState,
            StructureManager structureManager,
            ChunkAccess chunkAccess,
            StructureTemplateManager structureTemplateManager,
            StructuresSource source
    ) {
        ChunkPos chunkPos = chunkAccess.getPos();
        SectionPos sectionPos = SectionPos.bottomOf(chunkAccess);
        RandomState randomState = chunkGeneratorStructureState.randomState();

        chunkGeneratorStructureState.possibleStructureSets().forEach(holder -> {
            StructurePlacement placement = holder.value().placement();
            List<StructureSet.StructureSelectionEntry> entries = holder.value().structures().stream()
                    .filter(entry -> shouldGenerateVanillaStructure(entry.structure().value(), source))
                    .toList();

            if (entries.isEmpty()) {
                return;
            }

            for (StructureSet.StructureSelectionEntry entry : entries) {
                StructureStart existing = structureManager.getStartForStructure(sectionPos, entry.structure().value(), chunkAccess);
                if (existing != null && existing.isValid()) {
                    return;
                }
            }

            if (!placement.isStructureChunk(chunkGeneratorStructureState, chunkPos.x, chunkPos.z)) {
                return;
            }

            if (entries.size() == 1) {
                StructureSet.StructureSelectionEntry entry = entries.get(0);
                tryGenerateStructure(
                        entry.structure(),
                        entry.structure().value().biomes()::contains,
                        registryAccess,
                        randomState,
                        structureManager,
                        structureTemplateManager,
                        chunkGeneratorStructureState.getLevelSeed(),
                        chunkAccess,
                        chunkPos,
                        sectionPos
                );
                return;
            }

            ArrayList<StructureSet.StructureSelectionEntry> weightedEntries = new ArrayList<>(entries);
            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
            random.setLargeFeatureSeed(chunkGeneratorStructureState.getLevelSeed(), chunkPos.x, chunkPos.z);
            int totalWeight = weightedEntries.stream().mapToInt(StructureSet.StructureSelectionEntry::weight).sum();

            while (!weightedEntries.isEmpty()) {
                int selectedWeight = random.nextInt(totalWeight);
                int selectedIndex = 0;

                for (StructureSet.StructureSelectionEntry entry : weightedEntries) {
                    selectedWeight -= entry.weight();
                    if (selectedWeight < 0) {
                        break;
                    }
                    selectedIndex++;
                }

                StructureSet.StructureSelectionEntry selected = weightedEntries.get(selectedIndex);
                if (tryGenerateStructure(
                        selected.structure(),
                        selected.structure().value().biomes()::contains,
                        registryAccess,
                        randomState,
                        structureManager,
                        structureTemplateManager,
                        chunkGeneratorStructureState.getLevelSeed(),
                        chunkAccess,
                        chunkPos,
                        sectionPos
                )) {
                    return;
                }

                weightedEntries.remove(selectedIndex);
                totalWeight -= selected.weight();
            }
        });
    }

    private void createImageStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState chunkGeneratorStructureState,
            StructureManager structureManager,
            ChunkAccess chunkAccess,
            StructureTemplateManager structureTemplateManager,
            StructuresSource source
    ) {
        ChunkPos chunkPos = chunkAccess.getPos();
        SectionPos sectionPos = SectionPos.bottomOf(chunkAccess);
        RandomState randomState = chunkGeneratorStructureState.randomState();

        for (Holder<Structure> structure : source.getStructuresInChunk(chunkPos)) {
            if (!shouldGenerateCustomStructure(structure.value(), source)) {
                continue;
            }

            StructureStart existing = structureManager.getStartForStructure(sectionPos, structure.value(), chunkAccess);
            if (existing != null && existing.isValid()) {
                continue;
            }

            tryGenerateStructure(
                    structure,
                    biome -> true,
                    registryAccess,
                    randomState,
                    structureManager,
                    structureTemplateManager,
                    chunkGeneratorStructureState.getLevelSeed(),
                    chunkAccess,
                    chunkPos,
                    sectionPos
            );
        }
    }

    private boolean tryGenerateStructure(
            Holder<Structure> structureHolder,
            Predicate<Holder<Biome>> biomePredicate,
            RegistryAccess registryAccess,
            RandomState randomState,
            StructureManager structureManager,
            StructureTemplateManager structureTemplateManager,
            long seed,
            ChunkAccess chunkAccess,
            ChunkPos chunkPos,
            SectionPos sectionPos
    ) {
        Structure structure = structureHolder.value();
        int references = fetchReferences(structureManager, chunkAccess, sectionPos, structure);
        StructureStart structureStart = structure.generate(
                registryAccess,
                this,
                this.biomeSource,
                randomState,
                structureTemplateManager,
                seed,
                chunkPos,
                references,
                chunkAccess,
                biomePredicate
        );

        if (structureStart.isValid()) {
            structureManager.setStartForStructure(sectionPos, structure, structureStart, chunkAccess);
            return true;
        }

        return false;
    }

    private static int fetchReferences(StructureManager structureManager, ChunkAccess chunkAccess, SectionPos sectionPos, Structure structure) {
        StructureStart structureStart = structureManager.getStartForStructure(sectionPos, structure, chunkAccess);
        return structureStart != null ? structureStart.getReferences() : 0;
    }

    private boolean shouldGenerateVanillaStructure(Structure structure, StructuresSource source) {
        return source == null || !source.isBlacklisted(structure);
    }

    private boolean shouldGenerateCustomStructure(Structure structure, StructuresSource source) {
        return !ImgGen.CONFIG.useBlackList
                || (ImgGen.CONFIG.blackListApplication != 1 && ImgGen.CONFIG.blackListApplication != 2)
                || !source.isBlacklisted(structure);
    }

    private StructuresSource getStructuresSource(Registry<Structure> structures) {
        if (structuresSource == null) {
            structuresSource = new StructuresSource(structures);
            structuresSource.registerStructures();
            if (ImgGen.CONFIG.useBlackList) {
                structuresSource.registerBlackList();
            }
        }

        return structuresSource;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Executor executor,
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunkAccess
    ) {
        return delegate.fillFromNoise(executor, blender, randomState, structureManager, chunkAccess)
                .thenApply(generatedChunk -> {
                    if (ImgGen.CONFIG.customHeightMap) {
                        applyImageHeightmap(generatedChunk);
                    } else {
                        applyImageBiomeTerrain(generatedChunk);
                    }
                    return generatedChunk;
                });
    }

    @Override
    public int getSeaLevel() {
        ImgGen.ensureConfig();
        return ImgGen.CONFIG.customHeightMap ? ImgGen.CONFIG.seaLevel : settings.value().seaLevel();
    }

    @Override
    public int getMinY() {
        return delegate.getMinY();
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        OptionalInt imageHeight = getImageHeight(x, z, levelHeightAccessor);
        if (imageHeight.isPresent()) {
            int terrainTop = imageHeight.getAsInt() + 1;
            if (countsFluidForHeight(type)) {
                return Math.max(terrainTop, getSeaLevel());
            }
            return terrainTop;
        }

        return delegate.getBaseHeight(x, z, type, levelHeightAccessor, randomState);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        OptionalInt imageHeight = getImageHeight(x, z, levelHeightAccessor);
        if (imageHeight.isEmpty()) {
            return delegate.getBaseColumn(x, z, levelHeightAccessor, randomState);
        }

        int minY = levelHeightAccessor.getMinBuildHeight();
        BlockState[] states = new BlockState[levelHeightAccessor.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = minY + i;
            states[i] = getStateForHeight(y, imageHeight.getAsInt(), minY);
        }

        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos blockPos) {
        delegate.addDebugScreenInfo(lines, randomState, blockPos);
    }

    public boolean hasCustomHeightMapAt(int x, int z) {
        ImgGen.ensureConfig();
        return heightMapSource != null && (heightMapSource.isInImage(x, z) || ImgGen.CONFIG.repeatHeightMapImage);
    }

    public int getImageHeight(int x, int z) {
        if (heightMapSource == null) {
            return getSeaLevel();
        }
        return heightMapSource.getHeight(x, z);
    }

    public HeightMapSource getHeightMapSource() {
        return heightMapSource;
    }

    public Holder<NoiseGeneratorSettings> generatorSettings() {
        return effectiveSettings;
    }

    private static NoiseGeneratorSettings withImageTerrain(NoiseGeneratorSettings baseSettings) {
        NoiseRouter router = baseSettings.noiseRouter();
        NoiseRouter imageRouter = new NoiseRouter(
                router.barrierNoise(),
                router.fluidLevelFloodednessNoise(),
                router.fluidLevelSpreadNoise(),
                router.lavaNoise(),
                router.temperature(),
                router.vegetation(),
                new ImageTerrainDensityFunction(router.continents()),
                router.erosion(),
                router.depth(),
                router.ridges(),
                router.initialDensityWithoutJaggedness(),
                router.finalDensity(),
                router.veinToggle(),
                router.veinRidged(),
                router.veinGap()
        );

        return new NoiseGeneratorSettings(
                baseSettings.noiseSettings(),
                baseSettings.defaultBlock(),
                baseSettings.defaultFluid(),
                imageRouter,
                baseSettings.surfaceRule(),
                baseSettings.spawnTarget(),
                baseSettings.seaLevel(),
                baseSettings.disableMobGeneration(),
                baseSettings.isAquifersEnabled(),
                baseSettings.oreVeinsEnabled(),
                baseSettings.useLegacyRandomSource()
        );
    }

    private OptionalInt getImageHeight(int x, int z, LevelHeightAccessor levelHeightAccessor) {
        if (!hasCustomHeightMapAt(x, z)) {
            return OptionalInt.empty();
        }

        int minY = levelHeightAccessor.getMinBuildHeight();
        int maxY = levelHeightAccessor.getMaxBuildHeight() - 1;
        return OptionalInt.of(Mth.clamp(heightMapSource.getHeight(x, z), minY, maxY));
    }

    private void applyImageHeightmap(ChunkAccess chunkAccess) {
        ImgGen.ensureConfig();
        if (heightMapSource == null) {
            return;
        }

        ChunkPos chunkPos = chunkAccess.getPos();
        int minY = chunkAccess.getMinBuildHeight();
        int maxY = chunkAccess.getMaxBuildHeight();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkPos.getMinBlockX() + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkPos.getMinBlockZ() + localZ;
                OptionalInt imageHeight = getImageHeight(worldX, worldZ, chunkAccess);
                if (imageHeight.isEmpty()) {
                    continue;
                }

                int height = imageHeight.getAsInt();
                for (int y = minY; y < maxY; y++) {
                    mutable.set(worldX, y, worldZ);
                    chunkAccess.setBlockState(mutable, getStateForHeight(y, height, minY), false);
                }
            }
        }

        Heightmap.primeHeightmaps(chunkAccess, EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
    }

    private void applyImageBiomeTerrain(ChunkAccess chunkAccess) {
        if (!(biomeSource instanceof ImgGenBiomeSource imgGenBiomeSource)) {
            return;
        }

        ChunkPos chunkPos = chunkAccess.getPos();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean changed = false;

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkPos.getMinBlockX() + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkPos.getMinBlockZ() + localZ;
                if (!ImgGen.CONFIG.repeatBiomeImage
                        && !imgGenBiomeSource.getBiomeDataProvider().isInImage(worldX, worldZ)) {
                    continue;
                }

                int currentSurface = chunkAccess.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, localX, localZ);
                OptionalInt minimumSurface = getMinimumLandSurface(
                        imgGenBiomeSource,
                        worldX,
                        worldZ,
                        currentSurface
                );
                if (minimumSurface.isEmpty()) {
                    continue;
                }

                int targetSurface = Mth.clamp(
                        minimumSurface.getAsInt(),
                        chunkAccess.getMinBuildHeight() + 1,
                        chunkAccess.getMaxBuildHeight() - 1
                );
                if (currentSurface >= targetSurface) {
                    continue;
                }

                for (int y = currentSurface + 1; y <= targetSurface; y++) {
                    mutable.set(worldX, y, worldZ);
                    BlockState state = chunkAccess.getBlockState(mutable);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        chunkAccess.setBlockState(mutable, settings.value().defaultBlock(), false);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            Heightmap.primeHeightmaps(chunkAccess, EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG));
        }
    }

    private OptionalInt getMinimumLandSurface(
            ImgGenBiomeSource imgGenBiomeSource,
            int x,
            int z,
            int currentSurface
    ) {
        Holder<Biome> centerBiome = imgGenBiomeSource.getImageBiome(x, z);
        if (!isLandBiome(centerBiome)) {
            return OptionalInt.empty();
        }

        int landSurface = getSeaLevel() + 1;
        if (currentSurface >= landSurface) {
            return OptionalInt.empty();
        }

        int radius = Math.max(8, Math.round(ImgGen.CONFIG.biomesImageScale * 8.0F));
        int[][] neighbors = {
                {-radius, 0, 2},
                {radius, 0, 2},
                {0, -radius, 2},
                {0, radius, 2},
                {-radius, -radius, 1},
                {-radius, radius, 1},
                {radius, -radius, 1},
                {radius, radius, 1}
        };
        int landWeight = 4;
        int totalWeight = 4;

        for (int[] neighbor : neighbors) {
            int sampleX = x + neighbor[0];
            int sampleZ = z + neighbor[1];
            int weight = neighbor[2];
            totalWeight += weight;
            if (hasImageBiomeAt(imgGenBiomeSource, sampleX, sampleZ)
                    && isLandBiome(imgGenBiomeSource.getImageBiome(sampleX, sampleZ))) {
                landWeight += weight;
            }
        }

        double landRatio = (double) landWeight / totalWeight;
        if (landRatio <= 0.5D) {
            return OptionalInt.empty();
        }

        double blend = Mth.clamp((landRatio - 0.5D) * 2.0D, 0.0D, 1.0D);
        blend = blend * blend * (3.0D - 2.0D * blend);
        int targetSurface = currentSurface + (int) Math.ceil((landSurface - currentSurface) * blend);
        return OptionalInt.of(targetSurface);
    }

    private boolean hasImageBiomeAt(ImgGenBiomeSource imgGenBiomeSource, int x, int z) {
        return ImgGen.CONFIG.repeatBiomeImage || imgGenBiomeSource.getBiomeDataProvider().isInImage(x, z);
    }

    private boolean isLandBiome(Holder<Biome> biome) {
        return !isOceanOrRiver(biome) && !biome.is(Biomes.THE_VOID);
    }

    private boolean isOceanOrRiver(Holder<Biome> biome) {
        return biome.is(Biomes.OCEAN)
                || biome.is(Biomes.WARM_OCEAN)
                || biome.is(Biomes.LUKEWARM_OCEAN)
                || biome.is(Biomes.COLD_OCEAN)
                || biome.is(Biomes.FROZEN_OCEAN)
                || biome.is(Biomes.DEEP_OCEAN)
                || biome.is(Biomes.DEEP_LUKEWARM_OCEAN)
                || biome.is(Biomes.DEEP_COLD_OCEAN)
                || biome.is(Biomes.DEEP_FROZEN_OCEAN)
                || biome.is(Biomes.RIVER)
                || biome.is(Biomes.FROZEN_RIVER);
    }

    private BlockState getStateForHeight(int y, int height, int minY) {
        if (y == minY) {
            return BEDROCK;
        }

        if (y <= height) {
            return settings.value().defaultBlock();
        }

        if (y < getSeaLevel()) {
            return settings.value().defaultFluid();
        }

        return AIR;
    }

    private boolean countsFluidForHeight(Heightmap.Types type) {
        return type == Heightmap.Types.WORLD_SURFACE
                || type == Heightmap.Types.WORLD_SURFACE_WG
                || type == Heightmap.Types.MOTION_BLOCKING
                || type == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
    }
}
