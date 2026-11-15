package eu.pollux28.imggen.world.chunk;

import com.mojang.serialization.MapCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Surface rule condition that is true inside biomes carrying a tag. Vanilla can only test for a fixed
 * list of biome names, which leaves out biomes added by other mods.
 *
 * <p>Like the rest of the generator's private settings this is never written to disk, so it needs no
 * registered codec.
 */
record BiomeTagCondition(TagKey<Biome> tag) implements SurfaceRules.ConditionSource {
    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        // The context already remembers the biome of the block it is working on.
        return () -> context.biome.get().is(tag);
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return KeyDispatchDataCodec.of(MapCodec.unit(this));
    }
}
