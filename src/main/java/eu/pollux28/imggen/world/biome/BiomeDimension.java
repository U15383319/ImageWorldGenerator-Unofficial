package eu.pollux28.imggen.world.biome;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

import javax.annotation.Nullable;
import java.util.Locale;

/** The dimension a biome belongs to by nature, which decides the blocks it is made of in an image world. */
public enum BiomeDimension {
    OVERWORLD,
    NETHER,
    END;

    public static BiomeDimension fromTags(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_NETHER)) {
            return NETHER;
        }
        return biome.is(BiomeTags.IS_END) ? END : OVERWORLD;
    }

    @Nullable
    public static BiomeDimension byName(String name) {
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
