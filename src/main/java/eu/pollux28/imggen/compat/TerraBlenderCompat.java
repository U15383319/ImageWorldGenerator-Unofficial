package eu.pollux28.imggen.compat;

import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.fml.ModList;
import terrablender.api.SurfaceRuleManager;

/**
 * Optional TerraBlender support. Mods built on TerraBlender (Biomes O' Plenty and many others) register
 * the surface of their biomes with it instead of putting it into the noise settings, and TerraBlender only
 * hands those rules to worlds using the vanilla biome source. Image worlds therefore ask for them directly.
 */
public final class TerraBlenderCompat {
    private static final boolean LOADED = ModList.get().isLoaded("terrablender");

    public enum Dimension {
        OVERWORLD,
        NETHER,
        END
    }

    private TerraBlenderCompat() {
    }

    /**
     * @return {@code rules} with the surface rules other mods registered for their own biomes placed in
     * front of it, or {@code rules} unchanged when TerraBlender is not installed
     */
    public static SurfaceRules.RuleSource withModdedRules(Dimension dimension, SurfaceRules.RuleSource rules) {
        // The TerraBlender classes are only touched behind this check, in a class of their own.
        return LOADED ? Bridge.withModdedRules(dimension, rules) : rules;
    }

    private static final class Bridge {
        static SurfaceRules.RuleSource withModdedRules(Dimension dimension, SurfaceRules.RuleSource rules) {
            SurfaceRuleManager.RuleCategory category = switch (dimension) {
                case OVERWORLD -> SurfaceRuleManager.RuleCategory.OVERWORLD;
                case NETHER -> SurfaceRuleManager.RuleCategory.NETHER;
                case END -> SurfaceRuleManager.RuleCategory.END;
            };
            return SurfaceRuleManager.getNamespacedRules(category, rules);
        }
    }
}
