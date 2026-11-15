package eu.pollux28.imggen.image;

import net.minecraft.util.Mth;

/**
 * Maps world block coordinates onto the pixels of an image centred on the world origin.
 *
 * @param scale  blocks per pixel
 * @param repeat tile the image endlessly instead of ending at its border
 * @param mirror when repeating, flip every other tile so the edges line up
 */
public record ImageMapping(int width, int height, float scale, boolean repeat, boolean mirror) {
    public static final int OUTSIDE = -1;

    /** @return index of the pixel in a row-major array, or {@link #OUTSIDE} */
    public int pixelIndex(int blockX, int blockZ) {
        int pixelX = toPixel(blockX, width);
        if (pixelX < 0) {
            return OUTSIDE;
        }
        int pixelZ = toPixel(blockZ, height);
        if (pixelZ < 0) {
            return OUTSIDE;
        }
        return pixelZ * width + pixelX;
    }

    public boolean contains(int blockX, int blockZ) {
        return pixelIndex(blockX, blockZ) != OUTSIDE;
    }

    /** Extent of the image along X in blocks. */
    public double blockWidth() {
        return (double) width * scale;
    }

    /** Extent of the image along Z in blocks. */
    public double blockDepth() {
        return (double) height * scale;
    }

    /** Block X of the middle of the image; not exactly 0 for an odd number of pixels. */
    public double centerBlockX() {
        return (width - 2 * (width / 2)) * scale / 2.0;
    }

    public double centerBlockZ() {
        return (height - 2 * (height / 2)) * scale / 2.0;
    }

    public int pixelX(int pixelIndex) {
        return pixelIndex % width;
    }

    public int pixelZ(int pixelIndex) {
        return pixelIndex / width;
    }

    private int toPixel(int block, int size) {
        int pixel = (scale == 1.0F ? block : Mth.floor(block / scale)) + size / 2;
        if (pixel >= 0 && pixel < size) {
            return pixel;
        }
        if (!repeat) {
            return OUTSIDE;
        }
        if (!mirror) {
            return Math.floorMod(pixel, size);
        }
        int wrapped = Math.floorMod(pixel, size * 2);
        return wrapped < size ? wrapped : size * 2 - 1 - wrapped;
    }
}
