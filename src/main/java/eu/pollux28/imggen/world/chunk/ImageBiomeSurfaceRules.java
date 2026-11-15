package eu.pollux28.imggen.world.chunk;

import eu.pollux28.imggen.compat.TerraBlenderCompat;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

/** Surface rules for biomes that normally never appear in the overworld. */
final class ImageBiomeSurfaceRules {
    private static final SurfaceRules.RuleSource BEDROCK = blockRule(Blocks.BEDROCK);
    private static final SurfaceRules.RuleSource LAVA = blockRule(Blocks.LAVA);
    private static final SurfaceRules.RuleSource NETHERRACK = blockRule(Blocks.NETHERRACK);
    private static final SurfaceRules.RuleSource SOUL_SAND = blockRule(Blocks.SOUL_SAND);
    private static final SurfaceRules.RuleSource SOUL_SOIL = blockRule(Blocks.SOUL_SOIL);
    private static final SurfaceRules.RuleSource BASALT = blockRule(Blocks.BASALT);
    private static final SurfaceRules.RuleSource BLACKSTONE = blockRule(Blocks.BLACKSTONE);
    private static final SurfaceRules.RuleSource WARPED_WART_BLOCK = blockRule(Blocks.WARPED_WART_BLOCK);
    private static final SurfaceRules.RuleSource WARPED_NYLIUM = blockRule(Blocks.WARPED_NYLIUM);
    private static final SurfaceRules.RuleSource NETHER_WART_BLOCK = blockRule(Blocks.NETHER_WART_BLOCK);
    private static final SurfaceRules.RuleSource CRIMSON_NYLIUM = blockRule(Blocks.CRIMSON_NYLIUM);
    private static final SurfaceRules.RuleSource GRAVEL = blockRule(Blocks.GRAVEL);
    private static final SurfaceRules.RuleSource END_STONE = blockRule(Blocks.END_STONE);
    private static final SurfaceRules.RuleSource SCULK = blockRule(Blocks.SCULK);

    private ImageBiomeSurfaceRules() {
    }

    /**
     * Puts the surface of Nether, End and deep dark biomes in front of the overworld rule, which only
     * knows the dimension it was written for.
     *
     * @param netherRule surface rule of the real Nether, used for Nether biomes of other mods
     * @param endRule    surface rule of the real End, used for End biomes of other mods
     */
    static SurfaceRules.RuleSource create(
            SurfaceRules.RuleSource overworldRule,
            @Nullable SurfaceRules.RuleSource netherRule,
            @Nullable SurfaceRules.RuleSource endRule,
            int seaLevel
    ) {
        SurfaceRules.ConditionSource vanillaNetherBiome = SurfaceRules.isBiome(
                Biomes.NETHER_WASTES,
                Biomes.SOUL_SAND_VALLEY,
                Biomes.CRIMSON_FOREST,
                Biomes.WARPED_FOREST,
                Biomes.BASALT_DELTAS
        );
        SurfaceRules.RuleSource bedrockFloor = SurfaceRules.ifTrue(
                SurfaceRules.verticalGradient(
                        "bedrock_floor",
                        VerticalAnchor.bottom(),
                        VerticalAnchor.aboveBottom(5)
                ),
                BEDROCK
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(
                        SurfaceRules.isBiome(Biomes.DEEP_DARK),
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SCULK)
                ),
                // The vanilla Nether rule is tied to the Nether's heights, so vanilla biomes get an adapted copy.
                SurfaceRules.ifTrue(
                        vanillaNetherBiome,
                        SurfaceRules.sequence(bedrockFloor, createNetherSurfaceRule(seaLevel))
                ),
                // Biomes of other mods bring their own surface inside the rule of their home dimension.
                SurfaceRules.ifTrue(
                        new BiomeTagCondition(BiomeTags.IS_NETHER),
                        sequence(
                                bedrockFloor,
                                netherRule == null ? null : TerraBlenderCompat.withModdedRules(TerraBlenderCompat.Dimension.NETHER, netherRule),
                                NETHERRACK
                        )
                ),
                SurfaceRules.ifTrue(
                        new BiomeTagCondition(BiomeTags.IS_END),
                        sequence(
                                bedrockFloor,
                                endRule == null ? null : TerraBlenderCompat.withModdedRules(TerraBlenderCompat.Dimension.END, endRule),
                                END_STONE
                        )
                ),
                TerraBlenderCompat.withModdedRules(TerraBlenderCompat.Dimension.OVERWORLD, overworldRule)
        );
    }

    private static SurfaceRules.RuleSource sequence(SurfaceRules.RuleSource... rules) {
        return SurfaceRules.sequence(Arrays.stream(rules).filter(Objects::nonNull).toArray(SurfaceRules.RuleSource[]::new));
    }

    /** Vanilla Nether surface rule without the bedrock roof, and with its fixed heights made relative to sea level. */
    private static SurfaceRules.RuleSource createNetherSurfaceRule(int seaLevel) {
        SurfaceRules.ConditionSource atOrAboveSeaLevelMinusOne = SurfaceRules.yBlockCheck(
                VerticalAnchor.absolute(seaLevel - 1),
                0
        );
        SurfaceRules.ConditionSource atOrAboveSeaLevel = SurfaceRules.yBlockCheck(
                VerticalAnchor.absolute(seaLevel),
                0
        );
        SurfaceRules.ConditionSource surfaceStartsNearSeaLevel = SurfaceRules.yStartCheck(
                VerticalAnchor.absolute(seaLevel - 2),
                0
        );
        SurfaceRules.ConditionSource surfaceStartsBelowSeaLevelCap = SurfaceRules.not(
                SurfaceRules.yStartCheck(VerticalAnchor.absolute(seaLevel + 3), 0)
        );
        SurfaceRules.ConditionSource hole = SurfaceRules.hole();
        SurfaceRules.ConditionSource soulSandLayer = SurfaceRules.noiseCondition(Noises.SOUL_SAND_LAYER, -0.012D);
        SurfaceRules.ConditionSource gravelLayer = SurfaceRules.noiseCondition(Noises.GRAVEL_LAYER, -0.012D);
        SurfaceRules.ConditionSource patch = SurfaceRules.noiseCondition(Noises.PATCH, -0.012D);
        SurfaceRules.ConditionSource netherrack = SurfaceRules.noiseCondition(Noises.NETHERRACK, 0.54D);
        SurfaceRules.ConditionSource netherWart = SurfaceRules.noiseCondition(Noises.NETHER_WART, 1.17D);
        SurfaceRules.ConditionSource stateSelector = SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, 0.0D);
        SurfaceRules.RuleSource gravelPatch = SurfaceRules.ifTrue(
                patch,
                SurfaceRules.ifTrue(
                        surfaceStartsNearSeaLevel,
                        SurfaceRules.ifTrue(surfaceStartsBelowSeaLevelCap, GRAVEL)
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(
                        SurfaceRules.isBiome(Biomes.BASALT_DELTAS),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.UNDER_CEILING, BASALT),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_FLOOR,
                                        SurfaceRules.sequence(
                                                gravelPatch,
                                                SurfaceRules.ifTrue(stateSelector, BASALT),
                                                BLACKSTONE
                                        )
                                )
                        )
                ),
                SurfaceRules.ifTrue(
                        SurfaceRules.isBiome(Biomes.SOUL_SAND_VALLEY),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_CEILING,
                                        SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(stateSelector, SOUL_SAND),
                                                SOUL_SOIL
                                        )
                                ),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_FLOOR,
                                        SurfaceRules.sequence(
                                                gravelPatch,
                                                SurfaceRules.ifTrue(stateSelector, SOUL_SAND),
                                                SOUL_SOIL
                                        )
                                )
                        )
                ),
                SurfaceRules.ifTrue(
                        SurfaceRules.ON_FLOOR,
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.not(atOrAboveSeaLevel),
                                        SurfaceRules.ifTrue(hole, LAVA)
                                ),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.isBiome(Biomes.WARPED_FOREST),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.not(netherrack),
                                                SurfaceRules.ifTrue(
                                                        atOrAboveSeaLevelMinusOne,
                                                        SurfaceRules.sequence(
                                                                SurfaceRules.ifTrue(netherWart, WARPED_WART_BLOCK),
                                                                WARPED_NYLIUM
                                                        )
                                                )
                                        )
                                ),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.isBiome(Biomes.CRIMSON_FOREST),
                                        SurfaceRules.ifTrue(
                                                SurfaceRules.not(netherrack),
                                                SurfaceRules.ifTrue(
                                                        atOrAboveSeaLevelMinusOne,
                                                        SurfaceRules.sequence(
                                                                SurfaceRules.ifTrue(netherWart, NETHER_WART_BLOCK),
                                                                CRIMSON_NYLIUM
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                SurfaceRules.ifTrue(
                        SurfaceRules.isBiome(Biomes.NETHER_WASTES),
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.UNDER_FLOOR,
                                        SurfaceRules.ifTrue(
                                                soulSandLayer,
                                                SurfaceRules.sequence(
                                                        SurfaceRules.ifTrue(
                                                                SurfaceRules.not(hole),
                                                                SurfaceRules.ifTrue(
                                                                        surfaceStartsNearSeaLevel,
                                                                        SurfaceRules.ifTrue(surfaceStartsBelowSeaLevelCap, SOUL_SAND)
                                                                )
                                                        ),
                                                        NETHERRACK
                                                )
                                        )
                                ),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.ON_FLOOR,
                                        SurfaceRules.ifTrue(
                                                atOrAboveSeaLevelMinusOne,
                                                SurfaceRules.ifTrue(
                                                        surfaceStartsBelowSeaLevelCap,
                                                        SurfaceRules.ifTrue(
                                                                gravelLayer,
                                                                SurfaceRules.sequence(
                                                                        SurfaceRules.ifTrue(atOrAboveSeaLevel, GRAVEL),
                                                                        SurfaceRules.ifTrue(SurfaceRules.not(hole), GRAVEL)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                NETHERRACK
        );
    }

    private static SurfaceRules.RuleSource blockRule(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}
