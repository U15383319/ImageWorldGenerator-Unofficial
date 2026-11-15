package eu.pollux28.imggen.world.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.config.ImgGenConfig;
import eu.pollux28.imggen.image.ColorTable;
import eu.pollux28.imggen.image.ImageFiles;
import eu.pollux28.imggen.image.IndexedImage;
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

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * Picks biomes from the colours of the biome image. Outside the image it either continues with the
 * vanilla overworld layout or returns the configured default biome.
 *
 * <p>Instances are created whenever the game decodes world presets, including on the world creation
 * screen, so nothing expensive happens before the first biome is actually requested.
 */
public final class ImgGenBiomeSource extends BiomeSource {
    public static final MapCodec<ImgGenBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME),
            RegistryOps.retrieveGetter(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
    ).apply(instance, instance.stable(ImgGenBiomeSource::new)));

    private final HolderGetter<Biome> biomes;
    private final BiomeSource vanillaSource;

    @Nullable
    private volatile ImgGenConfig config;
    @Nullable
    private volatile Colors colors;
    /** {@code null} until the first request; empty when there is no usable image. */
    @Nullable
    private volatile Optional<IndexedImage<Holder<Biome>>> image;

    public ImgGenBiomeSource(HolderGetter<Biome> biomes, HolderGetter<MultiNoiseBiomeSourceParameterList> parameterLists) {
        this.biomes = biomes;
        this.vanillaSource = MultiNoiseBiomeSource.createFromPreset(parameterLists.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
    }

    /** Lets the chunk generator share the config it was created with. Has no effect once the source is in use. */
    public void bindConfig(ImgGenConfig config) {
        if (this.config == null) {
            this.config = config;
        }
    }

    public ImgGenConfig config() {
        ImgGenConfig result = config;
        if (result == null) {
            synchronized (this) {
                result = config;
                if (result == null) {
                    result = ImgGenConfig.load();
                    config = result;
                }
            }
        }
        return result;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        Colors current = colors();
        return Stream.concat(
                Stream.concat(current.table.values().stream(), Stream.of(current.defaultBiome)),
                vanillaSource.possibleBiomes().stream()
        ).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        Holder<Biome> painted = paintedBiome(QuartPos.toBlock(quartX), QuartPos.toBlock(quartZ));
        return painted != null ? painted : vanillaSource.getNoiseBiome(quartX, quartY, quartZ, sampler);
    }

    /**
     * @return the biome this source dictates for the block column, or {@code null} where it defers to
     * vanilla generation (outside the image with {@code continuousGen} on)
     */
    @Nullable
    public Holder<Biome> paintedBiome(int blockX, int blockZ) {
        IndexedImage<Holder<Biome>> loaded = biomeImage();
        if (loaded != null) {
            Holder<Biome> biome = loaded.get(blockX, blockZ);
            if (biome != null) {
                return biome;
            }
        }
        return config().continuousGen ? null : colors().defaultBiome;
    }

    public Holder<Biome> defaultBiome() {
        return colors().defaultBiome;
    }

    /** @return the loaded biome image, or {@code null} if it is missing or unreadable */
    @Nullable
    public IndexedImage<Holder<Biome>> biomeImage() {
        Optional<IndexedImage<Holder<Biome>>> result = image;
        if (result == null) {
            synchronized (this) {
                result = image;
                if (result == null) {
                    result = loadImage();
                    image = result;
                }
            }
        }
        return result.orElse(null);
    }

    private Optional<IndexedImage<Holder<Biome>>> loadImage() {
        ImgGenConfig current = config();
        ColorTable<Holder<Biome>> table = colors().table;
        return ImageFiles.load(current.biomesImageName, "biome", source -> {
            int[] unknownColors = {0};
            IndexedImage<Holder<Biome>> indexed = IndexedImage.create(
                    source,
                    current.biomesImageScale,
                    current.repeatBiomeImage,
                    current.repeatMirrorBiomeImage,
                    rgb -> {
                        if (table.exact(rgb) == null) {
                            unknownColors[0]++;
                        }
                        return table.nearest(rgb);
                    }
            );
            if (unknownColors[0] > 0) {
                ImgGen.LOGGER.warn(
                        "The biome image uses {} colour(s) without a biome; they were mapped to the closest known colour",
                        unknownColors[0]
                );
            }
            return indexed;
        });
    }

    private Colors colors() {
        Colors result = colors;
        if (result == null) {
            synchronized (this) {
                result = colors;
                if (result == null) {
                    result = createColors();
                    colors = result;
                }
            }
        }
        return result;
    }

    private Colors createColors() {
        ImgGenConfig current = config();
        ColorTable<Holder<Biome>> table = new ColorTable<>();

        // Config entries go first so they can override a built-in colour.
        for (ImgGenConfig.BiomeColor entry : current.customBiomes) {
            if (entry.biomeID.equalsIgnoreCase("modid:biomeid")) {
                continue; // Placeholder written by old versions of the mod.
            }
            OptionalInt rgb = ColorTable.parseColor(entry.RGB);
            if (rgb.isEmpty()) {
                ImgGen.LOGGER.error("Custom biome {} has an invalid colour '{}'", entry.biomeID, entry.RGB);
                continue;
            }
            Optional<Holder.Reference<Biome>> biome = findBiome(entry.biomeID);
            if (biome.isEmpty()) {
                ImgGen.LOGGER.error("Custom biome '{}' does not exist", entry.biomeID);
                continue;
            }
            if (!table.register(rgb.getAsInt(), biome.get())) {
                ImgGen.LOGGER.warn("Colour {} is used by more than one custom biome; {} was ignored", entry.RGB, entry.biomeID);
            }
        }

        for (DefaultBiomeColors.Entry entry : DefaultBiomeColors.ENTRIES) {
            biomes.get(entry.biome()).ifPresent(biome -> table.register(entry.rgb(), biome));
        }

        Holder<Biome> defaultBiome = findBiome(current.defaultBiome).map(biome -> (Holder<Biome>) biome).orElseGet(() -> {
            ImgGen.LOGGER.error("Default biome '{}' does not exist, using minecraft:ocean", current.defaultBiome);
            return biomes.getOrThrow(Biomes.OCEAN);
        });
        return new Colors(table, defaultBiome);
    }

    private Optional<Holder.Reference<Biome>> findBiome(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? Optional.empty() : biomes.get(ResourceKey.create(Registries.BIOME, location));
    }

    private record Colors(ColorTable<Holder<Biome>> table, Holder<Biome> defaultBiome) {
    }
}
