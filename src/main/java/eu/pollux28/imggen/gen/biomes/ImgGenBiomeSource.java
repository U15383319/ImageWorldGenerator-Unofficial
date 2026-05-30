package eu.pollux28.imggen.gen.biomes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.data.BiomeColorConverter;
import eu.pollux28.imggen.data.BiomeColors;
import eu.pollux28.imggen.data.BiomeDataProvider;
import eu.pollux28.imggen.util.BiomeIDAndRGBPair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class ImgGenBiomeSource extends BiomeSource {
    public static final Codec<ImgGenBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME),
            RegistryOps.retrieveGetter(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
    ).apply(instance, instance.stable(ImgGenBiomeSource::new)));

    private final HolderGetter<Biome> biomeLookup;
    private final BiomeSource fallbackSource;
    private Holder<Biome> defaultBiome;
    private BiomeColorConverter biomeColorConverter;
    private BiomeDataProvider biomeDataProvider;

    public ImgGenBiomeSource(HolderGetter<Biome> biomeLookup, HolderGetter<MultiNoiseBiomeSourceParameterList> parameterLookup) {
        this.biomeLookup = biomeLookup;
        this.fallbackSource = MultiNoiseBiomeSource.createFromPreset(parameterLookup.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        reloadFromConfig();
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        Stream<Holder<Biome>> imggenBiomes = Arrays.stream(BiomeColors.values())
                .map(biomeColor -> biomeLookup.get(biomeColor.getBiome()))
                .flatMap(Optional::stream);
        return Stream.concat(imggenBiomes, fallbackSource.possibleBiomes().stream()).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int biomeX, int biomeY, int biomeZ, Climate.Sampler sampler) {
        ensureLoaded();

        Holder<Biome> fallbackBiome = ImgGen.CONFIG.continuousGen
                ? fallbackSource.getNoiseBiome(biomeX, biomeY, biomeZ, sampler)
                : defaultBiome;

        int blockX = QuartPos.toBlock(biomeX);
        int blockZ = QuartPos.toBlock(biomeZ);
        return biomeDataProvider.getData(blockX, blockZ, fallbackBiome);
    }

    public void reloadFromConfig() {
        ImgGen.ensureConfig();
        this.defaultBiome = getDefaultBiome();
        this.biomeColorConverter = new BiomeColorConverter(defaultBiome);
        registerBiomes();
        this.biomeDataProvider = new BiomeDataProvider(
                biomeColorConverter,
                ImgGen.CONFIG.biomesImageScale,
                ImgGen.CONFIG.biomesImageName
        );
    }

    public Holder<Biome> getDefaultBiome() {
        ResourceLocation id = ResourceLocation.tryParse(ImgGen.CONFIG.defaultBiome);
        if (id == null) {
            return biomeLookup.getOrThrow(Biomes.OCEAN);
        }

        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
        return biomeLookup.get(key).orElseGet(() -> biomeLookup.getOrThrow(Biomes.OCEAN));
    }

    public BiomeDataProvider getBiomeDataProvider() {
        ensureLoaded();
        return biomeDataProvider;
    }

    private void ensureLoaded() {
        if (biomeDataProvider == null || biomeColorConverter == null || defaultBiome == null) {
            reloadFromConfig();
        }
    }

    private void registerBiomes() {
        for (BiomeColors biomeColor : BiomeColors.values()) {
            Optional<Holder.Reference<Biome>> biome = biomeLookup.get(biomeColor.getBiome());
            if (biome.isPresent()) {
                biomeColorConverter.RegisterBiome(biomeColor.getRGB(), biome.get());
                ImgGen.LOGGER.debug(
                        "Registered biome {} with color {}",
                        biomeColor.getBiome().location(),
                        Integer.toHexString(biomeColor.getRGB())
                );
            }
        }

        for (BiomeIDAndRGBPair biomeIDAndRGBPair : ImgGen.CONFIG.customBiomes) {
            if (isPlaceholderBiome(biomeIDAndRGBPair.biomeID)) {
                continue;
            }

            int rgb;
            try {
                rgb = Integer.decode(biomeIDAndRGBPair.RGB);
            } catch (NumberFormatException e) {
                ImgGen.LOGGER.error("Biome {} has an invalid color code {}", biomeIDAndRGBPair.biomeID, biomeIDAndRGBPair.RGB);
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(biomeIDAndRGBPair.biomeID);
            if (id == null) {
                ImgGen.LOGGER.error("Incorrect biome id format. Expected modid:biomeid, got {}", biomeIDAndRGBPair.biomeID);
                continue;
            }

            ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, id);
            Optional<Holder.Reference<Biome>> biome = biomeLookup.get(key);
            if (biome.isEmpty()) {
                ImgGen.LOGGER.error("Could not find biome {}", biomeIDAndRGBPair.biomeID);
                continue;
            }

            biomeColorConverter.RegisterBiome(rgb, biome.get());
            ImgGen.LOGGER.debug("Registered biome {} with color {}", id, Integer.toHexString(rgb));
        }
    }

    private boolean isPlaceholderBiome(String biomeId) {
        return "modid:biomeid".equalsIgnoreCase(biomeId);
    }
}
