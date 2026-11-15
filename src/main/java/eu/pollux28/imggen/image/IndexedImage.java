package eu.pollux28.imggen.image;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

/**
 * An image whose colours have been resolved to values once, up front. Pixels are stored as one or two
 * bytes pointing into the list of distinct values, which keeps large maps small in memory and makes
 * lookups during world generation plain array reads. The number of distinct colours does not matter, so
 * anti-aliased or photographic images work too.
 */
public final class IndexedImage<T> {
    private static final int MAX_VALUES = 1 << 16;

    private final ImageMapping mapping;
    private final List<T> values;
    @Nullable
    private final byte[] narrow;
    @Nullable
    private final short[] wide;

    private IndexedImage(ImageMapping mapping, List<T> values, @Nullable byte[] narrow, @Nullable short[] wide) {
        this.mapping = mapping;
        this.values = Collections.unmodifiableList(values);
        this.narrow = narrow;
        this.wide = wide;
    }

    /**
     * @param resolver turns an RGB colour into a value ({@code null}: nothing), called once per distinct colour
     */
    public static <T> IndexedImage<T> create(BufferedImage image, float scale, boolean repeat, boolean mirror, IntFunction<T> resolver) {
        int width = image.getWidth();
        int height = image.getHeight();
        if ((long) width * height > Integer.MAX_VALUE - 8) {
            throw new IllegalArgumentException("Image of " + width + "x" + height + " pixels is too large");
        }

        List<T> values = new ArrayList<>();
        Object2IntOpenHashMap<T> indexByValue = new Object2IntOpenHashMap<>();
        indexByValue.defaultReturnValue(-1);
        Int2IntOpenHashMap indexByColor = new Int2IntOpenHashMap();
        indexByColor.defaultReturnValue(-1);
        byte[] narrow = new byte[width * height];
        short[] wide = null;
        int[] row = new int[width];
        int lastColor = 0;
        int lastIndex = -1;

        for (int z = 0; z < height; z++) {
            image.getRGB(0, z, width, 1, row, 0, width);
            int offset = z * width;
            for (int x = 0; x < width; x++) {
                int color = row[x] & 0xFFFFFF;
                int index;
                if (lastIndex >= 0 && color == lastColor) {
                    index = lastIndex;
                } else {
                    index = indexByColor.get(color);
                    if (index < 0) {
                        T value = resolver.apply(color);
                        index = indexByValue.getInt(value);
                        if (index < 0) {
                            if (values.size() == MAX_VALUES) {
                                throw new IllegalArgumentException("Image maps to more than " + MAX_VALUES + " different values");
                            }
                            index = values.size();
                            values.add(value);
                            indexByValue.put(value, index);
                            if (index > 0xFF && wide == null) {
                                wide = widen(narrow, offset + x);
                                narrow = null;
                            }
                        }
                        indexByColor.put(color, index);
                    }
                    lastColor = color;
                    lastIndex = index;
                }

                if (wide != null) {
                    wide[offset + x] = (short) index;
                } else {
                    narrow[offset + x] = (byte) index;
                }
            }
        }

        return new IndexedImage<>(new ImageMapping(width, height, scale, repeat, mirror), values, narrow, wide);
    }

    public ImageMapping mapping() {
        return mapping;
    }

    /** The distinct values of the image; may contain one {@code null} for pixels that map to nothing. */
    public List<T> values() {
        return values;
    }

    /** @return index into {@link #values()} for the block column, or {@link ImageMapping#OUTSIDE} */
    public int valueIndex(int blockX, int blockZ) {
        int pixel = mapping.pixelIndex(blockX, blockZ);
        if (pixel == ImageMapping.OUTSIDE) {
            return ImageMapping.OUTSIDE;
        }
        return narrow != null ? narrow[pixel] & 0xFF : wide[pixel] & 0xFFFF;
    }

    /** @return the value for the block column; {@code null} outside the image or where nothing is mapped */
    @Nullable
    public T get(int blockX, int blockZ) {
        int index = valueIndex(blockX, blockZ);
        return index == ImageMapping.OUTSIDE ? null : values.get(index);
    }

    private static short[] widen(byte[] narrow, int filled) {
        short[] wide = new short[narrow.length];
        for (int i = 0; i < filled; i++) {
            wide[i] = (short) (narrow[i] & 0xFF);
        }
        return wide;
    }
}
