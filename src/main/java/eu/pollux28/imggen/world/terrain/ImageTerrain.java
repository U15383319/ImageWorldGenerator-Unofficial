package eu.pollux28.imggen.world.terrain;

import eu.pollux28.imggen.image.ImageMapping;
import eu.pollux28.imggen.image.IndexedImage;
import eu.pollux28.imggen.world.biome.ImgGenBiomeSource;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Works out, for any block column, how strongly and towards which values the vanilla climate parameters
 * have to be bent so the terrain fits the biomes painted around that column.
 *
 * <p>The answer is a weighted average of the {@link TerrainShape}s in a small neighbourhood, which turns
 * the hard biome borders of the image into slopes instead of cliffs. Columns away from the image blend
 * back into untouched vanilla terrain.
 */
public final class ImageTerrain {
    public enum Parameter {
        CONTINENTALNESS,
        EROSION,
        WEIRDNESS;

        private final int offset = ordinal() * VALUES_PER_PARAMETER;
    }

    /** Blocks between two neighbourhood samples; one biome cell. */
    private static final int SAMPLE_SPACING = 4;
    /** Binomial weights of a 7x7 neighbourhood, reaching 12 blocks in every direction. */
    private static final int[] KERNEL = {1, 6, 15, 20, 15, 6, 1};
    private static final int KERNEL_RADIUS = KERNEL.length / 2;
    /**
     * Rivers are often painted only a few pixels wide, less than the neighbourhood, so they count this
     * many times when the valley is carved. Only the valley gets the bonus: giving it to the other
     * parameters as well would flatten the land along both banks.
     */
    private static final int RIVER_WEIGHT = 3;
    /** Share of river in the neighbourhood from which a valley starts to form, and where it is complete. */
    private static final float RIVER_START = 0.2F;
    private static final float RIVER_FULL = 0.5F;
    /** How much of a parameter's range the vanilla noise sweeps; keeps natural variation inside a biome. */
    private static final double NOISE_SPREAD = 0.6;

    private static final int VALUES_PER_PARAMETER = 5;
    private static final int LOW = 0;
    private static final int HIGH = 1;
    private static final int STRENGTH = 2;
    private static final int LIMIT_MIN = 3;
    private static final int LIMIT_MAX = 4;
    private static final int RIVER = 3 * VALUES_PER_PARAMETER;
    private static final float[] NO_INFLUENCE = withoutLimits(new float[RIVER + 1]);
    private static final int CACHE_SIZE = 256;

    private final ImgGenBiomeSource biomeSource;
    private final ThreadLocal<Cache> cache = ThreadLocal.withInitial(Cache::new);
    @Nullable
    private volatile Shapes shapes;

    public ImageTerrain(ImgGenBiomeSource biomeSource) {
        this.biomeSource = biomeSource;
    }

    /** Shape of the biome painted exactly at the column, without blending. */
    public TerrainShape shapeAt(int blockX, int blockZ) {
        return shapes().at(blockX, blockZ);
    }

    /** Bends one climate parameter of a column towards what the surrounding image biomes ask for. */
    public double apply(Parameter parameter, double vanilla, int blockX, int blockZ) {
        float[] blend = blend(blockX, blockZ);
        double value = vanilla;

        float strength = blend[parameter.offset + STRENGTH];
        if (strength > 0) {
            double position = Mth.clamp(0.5 + vanilla * NOISE_SPREAD, 0.0, 1.0);
            double target = Mth.lerp(position, blend[parameter.offset + LOW], blend[parameter.offset + HIGH]);
            value = Mth.lerp(strength, vanilla, target);
        }

        // The blend may not carry a column across what its own biome stands for, such as the shoreline.
        value = Mth.clamp(value, blend[parameter.offset + LIMIT_MIN], blend[parameter.offset + LIMIT_MAX]);

        if (parameter == Parameter.WEIRDNESS && blend[RIVER] > RIVER_START) {
            // Weirdness 0 is what carves a river valley; force it wherever river dominates the neighbourhood.
            // This comes last on purpose: the banks of a river have to be allowed to dip towards it.
            float t = Mth.clamp((blend[RIVER] - RIVER_START) / (RIVER_FULL - RIVER_START), 0.0F, 1.0F);
            value *= 1.0 - t * t * (3.0 - 2.0 * t);
        }
        return value;
    }

    private float[] blend(int blockX, int blockZ) {
        Cache local = cache.get();
        int slot = (blockX * 31 + blockZ) >>> 2 & (CACHE_SIZE - 1);
        if (local.blends[slot] != null && local.x[slot] == blockX && local.z[slot] == blockZ) {
            return local.blends[slot];
        }

        float[] blend = computeBlend(shapes(), blockX, blockZ);
        local.x[slot] = blockX;
        local.z[slot] = blockZ;
        local.blends[slot] = blend;
        return blend;
    }

    private static float[] computeBlend(Shapes shapes, int blockX, int blockZ) {
        Parameter[] parameters = Parameter.values();
        float[] sums = new float[RIVER + 1];
        float totalWeight = 0;
        float totalValleyWeight = 0;
        boolean influenced = false;

        for (int offsetX = -KERNEL_RADIUS; offsetX <= KERNEL_RADIUS; offsetX++) {
            for (int offsetZ = -KERNEL_RADIUS; offsetZ <= KERNEL_RADIUS; offsetZ++) {
                TerrainShape shape = shapes.at(blockX + offsetX * SAMPLE_SPACING, blockZ + offsetZ * SAMPLE_SPACING);
                float weight = KERNEL[offsetX + KERNEL_RADIUS] * KERNEL[offsetZ + KERNEL_RADIUS];
                float valleyWeight = shape == TerrainShape.RIVER ? weight * RIVER_WEIGHT : weight;
                totalWeight += weight;
                totalValleyWeight += valleyWeight;

                for (Parameter parameter : parameters) {
                    TerrainShape.Range target = shape.target(parameter);
                    if (target != null) {
                        float parameterWeight = parameter == Parameter.WEIRDNESS ? valleyWeight : weight;
                        sums[parameter.offset + LOW] += parameterWeight * target.min();
                        sums[parameter.offset + HIGH] += parameterWeight * target.max();
                        sums[parameter.offset + STRENGTH] += parameterWeight;
                        influenced = true;
                    }
                }
                if (shape == TerrainShape.RIVER) {
                    sums[RIVER] += valleyWeight;
                }
            }
        }

        if (!influenced) {
            return NO_INFLUENCE;
        }

        TerrainShape own = shapes.at(blockX, blockZ);
        for (Parameter parameter : parameters) {
            float weight = sums[parameter.offset + STRENGTH];
            if (weight > 0) {
                sums[parameter.offset + LOW] /= weight;
                sums[parameter.offset + HIGH] /= weight;
                sums[parameter.offset + STRENGTH] = weight / (parameter == Parameter.WEIRDNESS ? totalValleyWeight : totalWeight);
            }
            TerrainShape.Range limit = own.limit(parameter);
            sums[parameter.offset + LIMIT_MIN] = limit.min();
            sums[parameter.offset + LIMIT_MAX] = limit.max();
        }
        sums[RIVER] /= totalValleyWeight;
        return sums;
    }

    private static float[] withoutLimits(float[] blend) {
        for (Parameter parameter : Parameter.values()) {
            blend[parameter.offset + LIMIT_MIN] = Float.NEGATIVE_INFINITY;
            blend[parameter.offset + LIMIT_MAX] = Float.POSITIVE_INFINITY;
        }
        return blend;
    }

    private Shapes shapes() {
        Shapes result = shapes;
        if (result == null) {
            synchronized (this) {
                result = shapes;
                if (result == null) {
                    result = Shapes.create(biomeSource);
                    shapes = result;
                }
            }
        }
        return result;
    }

    /** Terrain shape per distinct biome of the image, so sampling is two array reads. */
    private record Shapes(@Nullable IndexedImage<Holder<Biome>> image, TerrainShape[] byValueIndex, TerrainShape outside) {
        static Shapes create(ImgGenBiomeSource biomeSource) {
            IndexedImage<Holder<Biome>> image = biomeSource.biomeImage();
            TerrainShape outside = biomeSource.config().continuousGen
                    ? TerrainShape.UNSHAPED
                    : biomeSource.profiles().terrain(biomeSource.defaultBiome());
            if (image == null) {
                return new Shapes(null, new TerrainShape[0], outside);
            }

            List<Holder<Biome>> biomes = image.values();
            TerrainShape[] byValueIndex = new TerrainShape[biomes.size()];
            for (int i = 0; i < byValueIndex.length; i++) {
                Holder<Biome> biome = biomes.get(i);
                byValueIndex[i] = biome == null ? TerrainShape.UNSHAPED : biomeSource.profiles().terrain(biome);
            }
            return new Shapes(image, byValueIndex, outside);
        }

        TerrainShape at(int blockX, int blockZ) {
            if (image == null) {
                return outside;
            }
            int index = image.valueIndex(blockX, blockZ);
            return index == ImageMapping.OUTSIDE ? outside : byValueIndex[index];
        }
    }

    /** Per-thread memo: the three climate parameters of a column are computed right after each other. */
    private static final class Cache {
        final int[] x = new int[CACHE_SIZE];
        final int[] z = new int[CACHE_SIZE];
        final float[][] blends = new float[CACHE_SIZE][];
    }
}
