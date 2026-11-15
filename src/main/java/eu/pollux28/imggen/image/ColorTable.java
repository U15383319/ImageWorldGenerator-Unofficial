package eu.pollux28.imggen.image;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.OptionalInt;

/** Colour to value lookup that falls back to the closest registered colour. */
public final class ColorTable<T> {
    private final Int2ObjectLinkedOpenHashMap<T> values = new Int2ObjectLinkedOpenHashMap<>();

    /** @return {@code false} if the colour was already taken; the first registration wins */
    public boolean register(int rgb, T value) {
        return values.putIfAbsent(rgb & 0xFFFFFF, value) == null;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Collection<T> values() {
        return values.values();
    }

    @Nullable
    public T exact(int rgb) {
        return values.get(rgb & 0xFFFFFF);
    }

    /** Value of the registered colour closest to {@code rgb}; ties go to the lowest colour code. */
    @Nullable
    public T nearest(int rgb) {
        int color = rgb & 0xFFFFFF;
        T exact = values.get(color);
        if (exact != null) {
            return exact;
        }

        T best = null;
        int bestColor = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (Int2ObjectMap.Entry<T> entry : values.int2ObjectEntrySet()) {
            int candidate = entry.getIntKey();
            int distance = distanceSquared(color, candidate);
            if (distance < bestDistance || (distance == bestDistance && candidate < bestColor)) {
                best = entry.getValue();
                bestColor = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Parses colours written like {@code 0x8DB360}, {@code #8DB360} or as a decimal number. */
    public static OptionalInt parseColor(String text) {
        try {
            return OptionalInt.of(Integer.decode(text.trim()) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    private static int distanceSquared(int first, int second) {
        int red = ((first >> 16) & 0xFF) - ((second >> 16) & 0xFF);
        int green = ((first >> 8) & 0xFF) - ((second >> 8) & 0xFF);
        int blue = (first & 0xFF) - (second & 0xFF);
        return red * red + green * green + blue * blue;
    }
}
