package eu.pollux28.imggen.world.terrain;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * The kind of landscape a biome needs, expressed as the ranges of the vanilla climate parameters
 * (continentalness, erosion, weirdness) that produce it. Since 1.18 terrain no longer follows biomes, so
 * {@link ImageTerrain} pushes those parameters into the ranges below wherever the image paints a biome.
 * A {@code null} range leaves the vanilla noise of that parameter untouched.
 *
 * <p>The numbers follow the bands used by vanilla's overworld biome layout: continentalness below -0.19
 * is ocean and above -0.11 inland, low erosion means mountains and high erosion flat land, and weirdness
 * near 0 carves river valleys while about 0.67 makes peaks.
 */
public enum TerrainShape {
    /** Not painted, or a biome that should keep vanilla terrain. */
    UNSHAPED(null, null, null, 1),
    /** Columns of this shape are emptied completely after generation. */
    VOID(null, null, null, 1),
    DEEP_OCEAN(range(-0.95F, -0.60F), null, null, 1),
    OCEAN(range(-0.42F, -0.25F), null, null, 1),
    RIVER(range(-0.05F, 0.20F), range(0.10F, 0.50F), range(0.0F, 0.0F), 3),
    SHORE(range(-0.14F, -0.11F), range(0.20F, 0.50F), range(0.08F, 0.25F), 2),
    WETLAND(range(-0.05F, 0.20F), range(0.60F, 0.90F), range(0.08F, 0.30F), 1),
    LOWLAND(range(-0.05F, 0.30F), range(-0.15F, 0.50F), range(0.08F, 0.50F), 1),
    HILLS(range(0.10F, 0.50F), range(-0.45F, -0.10F), range(0.30F, 0.60F), 1),
    PLATEAU(range(0.15F, 0.60F), range(-0.75F, -0.40F), range(0.15F, 0.40F), 1),
    SLOPE(range(0.20F, 0.70F), range(-0.80F, -0.45F), range(0.40F, 0.56F), 1),
    PEAK(range(0.35F, 0.90F), range(-1.00F, -0.60F), range(0.57F, 0.76F), 1);

    /** Inclusive range a climate parameter is squeezed into. */
    public record Range(float min, float max) {
    }

    @Nullable
    private final Range continentalness;
    @Nullable
    private final Range erosion;
    @Nullable
    private final Range weirdness;
    private final int priority;

    TerrainShape(@Nullable Range continentalness, @Nullable Range erosion, @Nullable Range weirdness, int priority) {
        this.continentalness = continentalness;
        this.erosion = erosion;
        this.weirdness = weirdness;
        this.priority = priority;
    }

    @Nullable
    Range range(ImageTerrain.Parameter parameter) {
        return switch (parameter) {
            case CONTINENTALNESS -> continentalness;
            case EROSION -> erosion;
            case WEIRDNESS -> weirdness;
        };
    }

    /** Weight multiplier when blending with neighbours, so narrow features such as rivers survive the blur. */
    int priority() {
        return priority;
    }

    /**
     * The shape a biome gets from its tags. Works for modded biomes too, as long as they carry the usual
     * vanilla or common biome tags; {@code BiomeProfiles} adds the overrides of the user on top.
     */
    public static TerrainShape of(Holder<Biome> biome) {
        if (biome.is(Biomes.THE_VOID) || biome.is(Tags.Biomes.IS_VOID)) {
            return VOID;
        }
        if (biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(Tags.Biomes.IS_DEEP_OCEAN)) {
            return DEEP_OCEAN;
        }
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(Tags.Biomes.IS_OCEAN)) {
            return OCEAN;
        }
        if (biome.is(BiomeTags.IS_RIVER) || biome.is(Tags.Biomes.IS_RIVER)) {
            return RIVER;
        }
        if (biome.is(BiomeTags.IS_BEACH) || biome.is(Tags.Biomes.IS_BEACH) || biome.is(Tags.Biomes.IS_STONY_SHORES)) {
            return SHORE;
        }
        if (biome.is(Tags.Biomes.IS_SWAMP)) {
            return WETLAND;
        }
        if (biome.is(Tags.Biomes.IS_MOUNTAIN_PEAK)) {
            return PEAK;
        }
        if (biome.is(Tags.Biomes.IS_MOUNTAIN_SLOPE)) {
            return SLOPE;
        }
        if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_BADLANDS) || biome.is(Tags.Biomes.IS_PLATEAU)) {
            return PLATEAU;
        }
        if (biome.is(BiomeTags.IS_HILL) || biome.is(Tags.Biomes.IS_WINDSWEPT) || biome.is(Tags.Biomes.IS_HILL)) {
            return HILLS;
        }
        return LOWLAND;
    }

    /** @return the shape with that name, ignoring case, or {@code null} */
    @Nullable
    public static TerrainShape byName(String name) {
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Range range(float min, float max) {
        return new Range(min, max);
    }
}
