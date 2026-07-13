package eu.pollux28.imggen.gen.chunk;

import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

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

    private ImageBiomeSurfaceRules() {
    }

    // Replaces the old per-biome Biome#buildSurface dispatch before falling back to the overworld rule.
    static SurfaceRules.RuleSource create(SurfaceRules.RuleSource overworldRule, int seaLevel) {
        SurfaceRules.ConditionSource netherBiome = SurfaceRules.isBiome(
                Biomes.NETHER_WASTES,
                Biomes.SOUL_SAND_VALLEY,
                Biomes.CRIMSON_FOREST,
                Biomes.WARPED_FOREST,
                Biomes.BASALT_DELTAS
        );
        SurfaceRules.ConditionSource endBiome = SurfaceRules.isBiome(
                Biomes.THE_END,
                Biomes.SMALL_END_ISLANDS,
                Biomes.END_MIDLANDS,
                Biomes.END_HIGHLANDS,
                Biomes.END_BARRENS
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
                        netherBiome,
                        SurfaceRules.sequence(bedrockFloor, createNetherSurfaceRule(seaLevel))
                ),
                SurfaceRules.ifTrue(
                        endBiome,
                        SurfaceRules.sequence(bedrockFloor, END_STONE)
                ),
                overworldRule
        );
    }

    // Keeps vanilla 1.20.1 biome surface selection, but omits the Nether roof and makes heights sea-level-relative.
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
