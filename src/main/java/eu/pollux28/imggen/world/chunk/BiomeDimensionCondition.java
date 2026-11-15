package eu.pollux28.imggen.world.chunk;

import com.mojang.serialization.MapCodec;
import eu.pollux28.imggen.world.biome.BiomeDimension;
import eu.pollux28.imggen.world.biome.BiomeProfiles;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.function.Supplier;

/**
 * Surface rule condition that is true inside biomes of a dimension, as told by their tags or by an
 * override of the user. Vanilla can only test for a fixed list of biome names, which leaves out biomes
 * added by other mods.
 *
 * <p>Like the rest of the generator's private settings this is never written to disk, so it needs no
 * registered codec.
 *
 * @param profiles resolved on first use, because the generator's settings are built before the game is
 *                 ready to read biome tags
 */
record BiomeDimensionCondition(Supplier<BiomeProfiles> profiles, BiomeDimension dimension) implements SurfaceRules.ConditionSource {
    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        BiomeProfiles resolved = profiles.get();
        // The context already remembers the biome of the block it is working on.
        return () -> resolved.dimension(context.biome.get()) == dimension;
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return KeyDispatchDataCodec.of(MapCodec.unit(this));
    }
}
