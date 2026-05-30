package eu.pollux28.imggen.data;

import eu.pollux28.imggen.ImgGen;

public final class HeightDataProvider {
    private final ColorConverter<Integer> colorConverter;
    private final ImageManipulator imageManipulator;
    private final float scale;
    private final int width;
    private final int height;
    private final int widthHalf;
    private final int heightHalf;

    public HeightDataProvider(ColorConverter<Integer> colorConverter, float scale, String path) {
        this.colorConverter = colorConverter;
        this.imageManipulator = new ImageManipulator(path, "height");
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

    public int GetData(int x, int z) {
        int centeredX = (int) (x / scale) + widthHalf;
        int centeredZ = (int) (z / scale) + heightHalf;

        if (!isInImage(x, z)) {
            if (!ImgGen.CONFIG.repeatHeightMapImage) {
                return colorConverter.GetDefaultValue(centeredX, centeredZ);
            }

            centeredX = wrap(centeredX, width, ImgGen.CONFIG.repeatMirrorHeightMapImage);
            centeredZ = wrap(centeredZ, height, ImgGen.CONFIG.repeatMirrorHeightMapImage);
        }

        int rgb = imageManipulator.getRGB(centeredX, centeredZ);
        return colorConverter.GetValue(rgb);
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
