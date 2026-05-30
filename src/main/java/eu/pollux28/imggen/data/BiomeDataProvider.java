package eu.pollux28.imggen.data;

import eu.pollux28.imggen.ImgGen;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public final class BiomeDataProvider {
    private final ColorConverter<Holder<Biome>> colorConverter;
    private final ImageManipulator imageManipulator;
    private final float scale;
    private final int width;
    private final int height;
    private final int widthHalf;
    private final int heightHalf;

    public BiomeDataProvider(ColorConverter<Holder<Biome>> colorConverter, float scale, String path) {
        this.colorConverter = colorConverter;
        this.imageManipulator = new ImageManipulator(path, "biome");
        this.scale = scale <= 0 ? 1 : scale;
        this.width = imageManipulator.getWidth();
        this.height = imageManipulator.getHeight();
        this.widthHalf = width / 2;
        this.heightHalf = height / 2;
    }

    public boolean isInImage(int x, int z) {
        int centeredX = (int) (x / scale) + widthHalf;
        int centeredZ = (int) (z / scale) + heightHalf;

        return centeredX >= 0 && centeredZ >= 0 && centeredX < width && centeredZ < height;
    }

    public Holder<Biome> getData(int x, int z, Holder<Biome> defaultBiome) {
        int centeredX = (int) (x / scale) + widthHalf;
        int centeredZ = (int) (z / scale) + heightHalf;

        if (!isInImage(x, z)) {
            if (!ImgGen.CONFIG.repeatBiomeImage) {
                return defaultBiome;
            }

            centeredX = wrap(centeredX, width, ImgGen.CONFIG.repeatMirrorBiomeImage);
            centeredZ = wrap(centeredZ, height, ImgGen.CONFIG.repeatMirrorBiomeImage);
        }

        int rgb = imageManipulator.getRGB(centeredX, centeredZ);
        return colorConverter.GetValue(rgb);
    }

    public int[] getImageCoordAndValue(int x, int z) {
        int centeredX = (int) (x / scale) + widthHalf;
        int centeredZ = (int) (z / scale) + heightHalf;

        if (!isInImage(x, z)) {
            if (!ImgGen.CONFIG.repeatBiomeImage) {
                return new int[]{centeredX, centeredZ, 0};
            }

            centeredX = wrap(centeredX, width, ImgGen.CONFIG.repeatMirrorBiomeImage);
            centeredZ = wrap(centeredZ, height, ImgGen.CONFIG.repeatMirrorBiomeImage);
        }

        int rgb = imageManipulator.getRGB(centeredX, centeredZ) & 0x00FFFFFF;
        return new int[]{centeredX, centeredZ, rgb};
    }

    private int wrap(int value, int max, boolean mirror) {
        if (!mirror) {
            return Math.floorMod(value, max);
        }

        int period = max * 2;
        int wrapped = Math.floorMod(value, period);
        return wrapped < max ? wrapped : period - wrapped - 1;
    }
}
