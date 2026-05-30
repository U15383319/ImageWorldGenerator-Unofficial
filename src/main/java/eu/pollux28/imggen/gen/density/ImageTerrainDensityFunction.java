package eu.pollux28.imggen.gen.density;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.data.BiomeColors;
import eu.pollux28.imggen.data.ImageManipulator;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Comparator;
import java.util.Objects;

public record ImageTerrainDensityFunction(DensityFunction input) implements DensityFunction {
    public static final KeyDispatchDataCodec<ImageTerrainDensityFunction> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(ImageTerrainDensityFunction::input)
            ).apply(instance, ImageTerrainDensityFunction::new))
    );

    private static volatile TerrainImage terrainImage;

    @Override
    public double compute(FunctionContext context) {
        double baseDensity = input.compute(context);
        return applyImageContinentalness(baseDensity, context.blockX(), context.blockZ());
    }

    @Override
    public void fillArray(double[] values, ContextProvider contextProvider) {
        input.fillArray(values, contextProvider);
        for (int i = 0; i < values.length; i++) {
            FunctionContext context = contextProvider.forIndex(i);
            values[i] = applyImageContinentalness(values[i], context.blockX(), context.blockZ());
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ImageTerrainDensityFunction(input.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return input.minValue() - 1.0D;
    }

    @Override
    public double maxValue() {
        return input.maxValue() + 1.0D;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private static double applyImageContinentalness(double vanillaContinentalness, int x, int z) {
        ImgGen.ensureConfig();
        if (ImgGen.CONFIG.customHeightMap) {
            return vanillaContinentalness;
        }

        TerrainImage image = getTerrainImage();
        TerrainSample sample = image.sampleTerrain(x, z);
        if (!sample.valid()) {
            return vanillaContinentalness;
        }

        double target = sample.continentalness();
        if (target >= 0.0D) {
            return Mth.lerp(sample.strength(), vanillaContinentalness, Math.max(vanillaContinentalness, target));
        }

        return Mth.lerp(sample.strength(), vanillaContinentalness, Math.min(vanillaContinentalness, target));
    }

    private static TerrainImage getTerrainImage() {
        ImgGen.ensureConfig();
        String key = TerrainImage.keyFromConfig();
        TerrainImage current = terrainImage;
        if (current != null && current.key().equals(key)) {
            return current;
        }

        synchronized (ImageTerrainDensityFunction.class) {
            current = terrainImage;
            if (current == null || !current.key().equals(key)) {
                current = new TerrainImage(key);
                terrainImage = current;
            }
            return current;
        }
    }

    private static BiomeColors nearestBiomeColor(int color) {
        int rgb = color & 0x00FFFFFF;
        for (BiomeColors biomeColor : BiomeColors.values()) {
            if ((biomeColor.getRGB() & 0x00FFFFFF) == rgb) {
                return biomeColor;
            }
        }

        return java.util.Arrays.stream(BiomeColors.values())
                .min(Comparator.comparingDouble(biomeColor -> colorDistance(rgb, biomeColor.getRGB())))
                .orElse(BiomeColors.Plains);
    }

    private static double colorDistance(int first, int second) {
        int a = first & 0x00FFFFFF;
        int b = second & 0x00FFFFFF;
        return square((a & 0xFF) - (b & 0xFF))
                + square(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF))
                + square(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF));
    }

    private static int square(int value) {
        return value * value;
    }

    private record TerrainSample(double continentalness, double strength, boolean valid) {
        static TerrainSample missing() {
            return new TerrainSample(0.0D, 0.0D, false);
        }
    }

    private record TerrainTarget(double continentalness, double strength) {
        static TerrainTarget fromBiomeColor(BiomeColors color) {
            return switch (color) {
                case Deep_Ocean, Deep_Lukewarm_Ocean, Deep_Cold_Ocean, Deep_Frozen_Ocean ->
                        new TerrainTarget(-0.78D, 0.9D);
                case Ocean, Warm_Ocean, Lukewarm_Ocean, Cold_Ocean, Frozen_Ocean ->
                        new TerrainTarget(-0.48D, 0.75D);
                case River, Frozen_River -> new TerrainTarget(-0.12D, 0.45D);
                case Beach, Snowy_Beach, Stony_Shore -> new TerrainTarget(0.03D, 0.55D);
                case Swamp, Mangrove_Swamp -> new TerrainTarget(0.08D, 0.55D);
                case Mountains, Windswept_Forest, Grove, Snowy_Slopes -> new TerrainTarget(0.38D, 0.55D);
                case Jagged_Peaks, Frozen_Peaks, Stony_Peaks -> new TerrainTarget(0.55D, 0.6D);
                case Savanna_Plateau, Badlands, Wooded_Badlands, Eroded_Badlands -> new TerrainTarget(0.28D, 0.55D);
                case Mushroom_Fields, Meadow, Cherry_Grove -> new TerrainTarget(0.18D, 0.5D);
                case The_Void -> new TerrainTarget(-1.0D, 1.0D);
                default -> new TerrainTarget(0.16D, 0.55D);
            };
        }
    }

    private static final class TerrainImage {
        static final int OUTSIDE_IMAGE = Integer.MIN_VALUE;

        private final String key;
        private final ImageManipulator image;
        private final float scale;
        private final int width;
        private final int height;
        private final int widthHalf;
        private final int heightHalf;
        private final boolean repeat;
        private final boolean mirror;
        private final int smoothRadius;

        TerrainImage(String key) {
            this.key = key;
            this.image = new ImageManipulator(ImgGen.CONFIG.biomesImageName, "terrain");
            this.scale = ImgGen.CONFIG.biomesImageScale <= 0 ? 1 : ImgGen.CONFIG.biomesImageScale;
            this.width = image.getWidth();
            this.height = image.getHeight();
            this.widthHalf = width / 2;
            this.heightHalf = height / 2;
            this.repeat = ImgGen.CONFIG.repeatBiomeImage;
            this.mirror = ImgGen.CONFIG.repeatMirrorBiomeImage;
            this.smoothRadius = Math.max(8, Math.round(this.scale * 4.0F));
        }

        static String keyFromConfig() {
            return ImgGen.CONFIG.biomesImageName
                    + "|" + ImgGen.CONFIG.biomesImageScale
                    + "|" + ImgGen.CONFIG.repeatBiomeImage
                    + "|" + ImgGen.CONFIG.repeatMirrorBiomeImage;
        }

        String key() {
            return key;
        }

        TerrainSample sampleTerrain(int x, int z) {
            TerrainSample center = sampleAt(x, z);
            if (!center.valid() || smoothRadius <= 0) {
                return center;
            }

            TerrainSample north = sampleAt(x, z - smoothRadius);
            TerrainSample south = sampleAt(x, z + smoothRadius);
            TerrainSample west = sampleAt(x - smoothRadius, z);
            TerrainSample east = sampleAt(x + smoothRadius, z);

            double value = center.continentalness() * 4.0D;
            double strength = center.strength() * 4.0D;
            double weight = 4.0D;

            TerrainSample[] samples = {north, south, west, east};
            for (TerrainSample sample : samples) {
                if (sample.valid()) {
                    value += sample.continentalness();
                    strength += sample.strength();
                    weight += 1.0D;
                }
            }

            return new TerrainSample(value / weight, strength / weight, true);
        }

        private TerrainSample sampleAt(int x, int z) {
            int color = getColor(x, z);
            if (color == OUTSIDE_IMAGE) {
                return TerrainSample.missing();
            }

            TerrainTarget target = TerrainTarget.fromBiomeColor(nearestBiomeColor(color));
            return new TerrainSample(target.continentalness(), target.strength(), true);
        }

        private int getColor(int x, int z) {
            int centeredX = (int) (x / scale) + widthHalf;
            int centeredZ = (int) (z / scale) + heightHalf;

            if (!isInImage(centeredX, centeredZ)) {
                if (!repeat) {
                    return OUTSIDE_IMAGE;
                }

                centeredX = wrap(centeredX, width, mirror);
                centeredZ = wrap(centeredZ, height, mirror);
            }

            return image.getRGB(centeredX, centeredZ) & 0x00FFFFFF;
        }

        private boolean isInImage(int x, int z) {
            return x >= 0 && z >= 0 && x < width && z < height;
        }

        private int wrap(int value, int max, boolean mirror) {
            if (!mirror) {
                return Math.floorMod(value, max);
            }

            int period = max * 2;
            int wrapped = Math.floorMod(value, period);
            return wrapped < max ? wrapped : period - wrapped - 1;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TerrainImage image && Objects.equals(key, image.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
}
