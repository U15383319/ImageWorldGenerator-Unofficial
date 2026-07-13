package eu.pollux28.imggen.data;

import eu.pollux28.imggen.ImgGen;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BiomeColorConverter implements ColorConverter<Holder<Biome>> {
    private final Holder<Biome> defaultValue;
    private final Map<Integer, Holder<Biome>> biomeColorMap;
    private final Map<Integer, Holder<Biome>> resolvedColorCache;

    public BiomeColorConverter(Holder<Biome> defaultValue) {
        this.defaultValue = defaultValue;
        this.biomeColorMap = new ConcurrentHashMap<>();
        this.resolvedColorCache = new ConcurrentHashMap<>();
    }

    @Override
    public Holder<Biome> GetValue(int color) {
        int finalColor = color & 0x00FFFFFF;
        Holder<Biome> biome = biomeColorMap.get(finalColor);

        if (biome != null) {
            return biome;
        }

        if (biomeColorMap.isEmpty()) {
            return defaultValue;
        }

        return resolvedColorCache.computeIfAbsent(finalColor, this::findNearestBiome);
    }

    @Override
    public Holder<Biome> GetDefaultValue(int x, int z) {
        return defaultValue;
    }

    public void RegisterBiome(int color, Holder<Biome> biome) {
        int finalColor = color & 0x00FFFFFF;
        if (biomeColorMap.putIfAbsent(finalColor, biome) != null) {
            ImgGen.LOGGER.warn("There has already been a biome registered for color {}", Integer.toHexString(finalColor));
            return;
        }

        resolvedColorCache.clear();
    }

    private Holder<Biome> findNearestBiome(int color) {
        Holder<Biome> biome = biomeColorMap.entrySet().stream()
                .min(Comparator
                        .<Map.Entry<Integer, Holder<Biome>>>comparingDouble(entry -> getColorDiff(color, entry.getKey()))
                        .thenComparingInt(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .orElse(defaultValue);

        ImgGen.LOGGER.debug(
                "Found unmapped color code {}. Mapping it to the nearest known biome color.",
                Integer.toHexString(color)
        );
        return biome;
    }

    private double getColorDiff(int rgb, int mappedRgb) {
        return square((rgb % 256) - (mappedRgb % 256))
                + square(((rgb >> 8) % 256) - ((mappedRgb >> 8) % 256))
                + square(((rgb >> 16) % 256) - ((mappedRgb >> 16) % 256));
    }

    private float square(float value) {
        return value * value;
    }
}
