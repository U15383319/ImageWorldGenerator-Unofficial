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

    public BiomeColorConverter(Holder<Biome> defaultValue) {
        this.defaultValue = defaultValue;
        this.biomeColorMap = new ConcurrentHashMap<>();
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

        biome = biomeColorMap.entrySet().stream().sequential()
                .min(Comparator.comparingDouble(entry -> getColorDiff(finalColor, entry.getKey())))
                .map(Map.Entry::getValue)
                .orElse(defaultValue);

        ImgGen.LOGGER.debug("Found unmapped color code {}. Mapping it to the nearest known biome color.", Integer.toHexString(finalColor));
        RegisterBiome(finalColor, biome);

        return biome;
    }

    @Override
    public Holder<Biome> GetDefaultValue(int x, int z) {
        return defaultValue;
    }

    public void RegisterBiome(int color, Holder<Biome> biome) {
        int finalColor = color & 0x00FFFFFF;
        if (biomeColorMap.containsKey(finalColor)) {
            ImgGen.LOGGER.warn("There has already been a biome registered for color {}", Integer.toHexString(finalColor));
            return;
        }

        biomeColorMap.put(finalColor, biome);
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
