package eu.pollux28.imggen.image;

import java.awt.image.BufferedImage;

/** Height map: the blue channel of a pixel (the grey level of a greyscale image) is the surface Y. */
public final class HeightImage {
    public static final int OUTSIDE = Integer.MIN_VALUE;

    private final ImageMapping mapping;
    private final byte[] heights;

    private HeightImage(ImageMapping mapping, byte[] heights) {
        this.mapping = mapping;
        this.heights = heights;
    }

    public static HeightImage create(BufferedImage image, float scale, boolean repeat, boolean mirror) {
        int width = image.getWidth();
        int height = image.getHeight();
        if ((long) width * height > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("Image of " + width + "x" + height + " pixels is too large");
        }

        byte[] heights = new byte[width * height];
        int[] row = new int[width];
        for (int z = 0; z < height; z++) {
            image.getRGB(0, z, width, 1, row, 0, width);
            int offset = z * width;
            for (int x = 0; x < width; x++) {
                heights[offset + x] = (byte) row[x];
            }
        }
        return new HeightImage(new ImageMapping(width, height, scale, repeat, mirror), heights);
    }

    /** @return surface Y (0-255) of the block column, or {@link #OUTSIDE} */
    public int get(int blockX, int blockZ) {
        int pixel = mapping.pixelIndex(blockX, blockZ);
        return pixel == ImageMapping.OUTSIDE ? OUTSIDE : heights[pixel] & 0xFF;
    }

    /** Whether every column of the 16x16 area starting at the given block lies on the image. */
    public boolean coversChunk(int minBlockX, int minBlockZ) {
        if (mapping.repeat()) {
            return true;
        }
        return mapping.contains(minBlockX, minBlockZ)
                && mapping.contains(minBlockX + 15, minBlockZ)
                && mapping.contains(minBlockX, minBlockZ + 15)
                && mapping.contains(minBlockX + 15, minBlockZ + 15);
    }
}
