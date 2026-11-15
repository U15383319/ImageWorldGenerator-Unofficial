package eu.pollux28.imggen.world.terrain;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * The kind of landscape a biome needs, expressed through the vanilla climate parameters (continentalness,
 * erosion, weirdness) that produce it. Since 1.18 terrain no longer follows biomes, so {@link ImageTerrain}
 * bends those parameters wherever the image paints a biome. Every shape has two sets of numbers:
 * <ul>
 *     <li><b>targets</b>: the range a parameter is pulled into. Targets of neighbouring shapes are blended,
 *     which is what makes borders slope instead of step. {@code null} leaves the vanilla noise alone.</li>
 *     <li><b>limits</b>: bounds the parameter may never leave on a column painted with this shape, applied
 *     after blending. They keep water under water and land dry right up to the painted border, however
 *     narrow the feature and whatever lies next to it.</li>
 * </ul>
 *
 * <p>The numbers come from vanilla's terrain spline. Roughly: continentalness up to -0.18 is sea floor and
 * from -0.16 on is land; low erosion means mountains and high erosion flat land; weirdness near 0 carves a
 * river valley, about 0.2 is plain ground and about 0.67 makes peaks. Erosion above 0.5 or weirdness below
 * 0.12 puts land at or just under sea level, which is right for a swamp and wrong for everything else.
 */
public enum TerrainShape {
    /** Not painted, or a biome that should keep vanilla terrain. */
    UNSHAPED(null, null, null, Limits.ANY, Limits.ANY, Limits.ANY),
    /** Columns of this shape are emptied completely after generation. */
    VOID(null, null, null, Limits.ANY, Limits.ANY, Limits.ANY),
    DEEP_OCEAN(range(-0.95F, -0.60F), null, null, atMost(-0.50F), Limits.ANY, Limits.ANY),
    OCEAN(range(-0.42F, -0.25F), null, null, atMost(-0.19F), Limits.ANY, Limits.ANY),
    RIVER(range(-0.05F, 0.10F), range(0.00F, 0.30F), range(0.0F, 0.0F), range(-0.15F, 0.10F), range(-0.25F, 0.35F), range(-0.04F, 0.04F)),
    SHORE(range(-0.14F, -0.10F), range(0.00F, 0.30F), range(0.20F, 0.30F), range(-0.15F, -0.05F), range(-0.10F, 0.40F), range(0.15F, 0.35F)),
    WETLAND(range(-0.05F, 0.20F), range(0.60F, 0.90F), range(0.12F, 0.30F), atLeast(-0.15F), atLeast(0.45F), atLeast(0.10F)),
    LOWLAND(range(0.00F, 0.30F), range(-0.15F, 0.35F), range(0.20F, 0.50F), Limits.DRY_LAND, atMost(0.40F), Limits.NO_VALLEY),
    HILLS(range(0.10F, 0.50F), range(-0.45F, -0.10F), range(0.30F, 0.60F), Limits.DRY_LAND, atMost(0.40F), Limits.NO_VALLEY),
    PLATEAU(range(0.15F, 0.60F), range(-0.75F, -0.40F), range(0.20F, 0.40F), Limits.DRY_LAND, atMost(0.40F), Limits.NO_VALLEY),
    SLOPE(range(0.20F, 0.70F), range(-0.80F, -0.45F), range(0.40F, 0.56F), Limits.DRY_LAND, atMost(0.40F), Limits.NO_VALLEY),
    PEAK(range(0.35F, 0.90F), range(-1.00F, -0.60F), range(0.57F, 0.76F), Limits.DRY_LAND, atMost(0.40F), Limits.NO_VALLEY);

    /** Inclusive range of a climate parameter. */
    public record Range(float min, float max) {
    }

    @Nullable
    private final Range continentalness;
    @Nullable
    private final Range erosion;
    @Nullable
    private final Range weirdness;
    private final Range continentalnessLimit;
    private final Range erosionLimit;
    private final Range weirdnessLimit;

    TerrainShape(
            @Nullable Range continentalness,
            @Nullable Range erosion,
            @Nullable Range weirdness,
            Range continentalnessLimit,
            Range erosionLimit,
            Range weirdnessLimit
    ) {
        this.continentalness = continentalness;
        this.erosion = erosion;
        this.weirdness = weirdness;
        this.continentalnessLimit = continentalnessLimit;
        this.erosionLimit = erosionLimit;
        this.weirdnessLimit = weirdnessLimit;
    }

    /** @return the range the parameter is pulled into, or {@code null} to leave the vanilla noise alone */
    @Nullable
    Range target(ImageTerrain.Parameter parameter) {
        return switch (parameter) {
            case CONTINENTALNESS -> continentalness;
            case EROSION -> erosion;
            case WEIRDNESS -> weirdness;
        };
    }

    /** @return the bounds the parameter must stay within on a column of this shape; unbounded where it does not matter */
    Range limit(ImageTerrain.Parameter parameter) {
        return switch (parameter) {
            case CONTINENTALNESS -> continentalnessLimit;
            case EROSION -> erosionLimit;
            case WEIRDNESS -> weirdnessLimit;
        };
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
        // Mountains without the finer peak or slope tag: tall, but not jagged.
        if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(Tags.Biomes.IS_MOUNTAIN)
                || biome.is(BiomeTags.IS_BADLANDS) || biome.is(Tags.Biomes.IS_PLATEAU)) {
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

    private static Range atLeast(float min) {
        return new Range(min, Float.POSITIVE_INFINITY);
    }

    private static Range atMost(float max) {
        return new Range(Float.NEGATIVE_INFINITY, max);
    }

    /** Enum constants cannot refer to static fields of their own class, hence the holder. */
    private static final class Limits {
        static final Range ANY = new Range(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        /** Continentalness from which the vanilla spline produces land. */
        static final Range DRY_LAND = new Range(-0.15F, Float.POSITIVE_INFINITY);
        /** Weirdness high enough to stay clear of the river valley around 0. */
        static final Range NO_VALLEY = new Range(0.20F, Float.POSITIVE_INFINITY);
    }
}
