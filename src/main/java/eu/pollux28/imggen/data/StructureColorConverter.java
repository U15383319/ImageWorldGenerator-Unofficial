package eu.pollux28.imggen.data;

import eu.pollux28.imggen.ImgGen;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StructureColorConverter implements ColorConverter<Holder<Structure>> {
    private final Map<Integer, Holder<Structure>> structureColorMap = new ConcurrentHashMap<>();

    @Override
    public Holder<Structure> GetValue(int color) {
        int finalColor = color & 0x00FFFFFF;
        Holder<Structure> structure = structureColorMap.get(finalColor);
        if (structure != null || finalColor == 0) {
            return structure;
        }

        if (structureColorMap.isEmpty()) {
            return null;
        }

        structure = structureColorMap.entrySet().stream().sequential()
                .min(Comparator.comparingDouble(entry -> getColorDiff(finalColor, entry.getKey())))
                .map(Map.Entry::getValue)
                .orElse(null);

        ImgGen.LOGGER.debug("Found unmapped structure color {}. Mapping it to the nearest known structure color.", Integer.toHexString(finalColor));
        RegisterStructure(finalColor, structure);
        return structure;
    }

    @Override
    public Holder<Structure> GetDefaultValue(int x, int z) {
        return null;
    }

    public boolean RegisterStructure(int color, Holder<Structure> structure) {
        int finalColor = color & 0x00FFFFFF;
        if (structureColorMap.containsKey(finalColor)) {
            ImgGen.LOGGER.warn("There has already been a structure registered for color {}", Integer.toHexString(finalColor));
            return false;
        }

        structureColorMap.put(finalColor, structure);
        return true;
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
