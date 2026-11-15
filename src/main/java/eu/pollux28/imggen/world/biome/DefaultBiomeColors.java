package eu.pollux28.imggen.world.biome;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.List;

/** Built-in colour of every vanilla biome. More can be added through {@code customBiomes} in the config. */
public final class DefaultBiomeColors {
    public record Entry(int rgb, ResourceKey<Biome> biome) {
    }

    public static final List<Entry> ENTRIES = List.of(
            new Entry(0x000070, Biomes.OCEAN),
            new Entry(0x8DB360, Biomes.PLAINS),
            new Entry(0xFA9418, Biomes.DESERT),
            new Entry(0x606060, Biomes.WINDSWEPT_HILLS),
            new Entry(0x056621, Biomes.FOREST),
            new Entry(0x0B6659, Biomes.TAIGA),
            new Entry(0x07F9B2, Biomes.SWAMP),
            new Entry(0x1D4E2A, Biomes.MANGROVE_SWAMP),
            new Entry(0x0000FF, Biomes.RIVER),
            new Entry(0xBF3B3B, Biomes.NETHER_WASTES),
            new Entry(0x8080FF, Biomes.THE_END),
            new Entry(0x7070FF, Biomes.SMALL_END_ISLANDS),
            new Entry(0x6060FF, Biomes.END_MIDLANDS),
            new Entry(0x5050FF, Biomes.END_HIGHLANDS),
            new Entry(0x9090FF, Biomes.END_BARRENS),
            new Entry(0x7070D6, Biomes.FROZEN_OCEAN),
            new Entry(0xA0A0FF, Biomes.FROZEN_RIVER),
            new Entry(0xFFFFFF, Biomes.SNOWY_PLAINS),
            new Entry(0xA0A0A0, Biomes.SNOWY_SLOPES),
            new Entry(0xFF00FF, Biomes.MUSHROOM_FIELDS),
            new Entry(0xFADE55, Biomes.BEACH),
            new Entry(0x22551C, Biomes.WINDSWEPT_GRAVELLY_HILLS),
            new Entry(0x507050, Biomes.WINDSWEPT_FOREST),
            new Entry(0x537B09, Biomes.JUNGLE),
            new Entry(0x628B17, Biomes.SPARSE_JUNGLE),
            new Entry(0x000030, Biomes.DEEP_OCEAN),
            new Entry(0xA2A284, Biomes.STONY_SHORE),
            new Entry(0xFAF0C0, Biomes.SNOWY_BEACH),
            new Entry(0x307444, Biomes.BIRCH_FOREST),
            new Entry(0x589C6C, Biomes.OLD_GROWTH_BIRCH_FOREST),
            new Entry(0x40511A, Biomes.DARK_FOREST),
            new Entry(0x31554A, Biomes.SNOWY_TAIGA),
            new Entry(0x596651, Biomes.OLD_GROWTH_PINE_TAIGA),
            new Entry(0x818E79, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
            new Entry(0xBDB25F, Biomes.SAVANNA),
            new Entry(0xA79D64, Biomes.SAVANNA_PLATEAU),
            new Entry(0xE5DA87, Biomes.WINDSWEPT_SAVANNA),
            new Entry(0xD94515, Biomes.BADLANDS),
            new Entry(0xB09765, Biomes.WOODED_BADLANDS),
            new Entry(0xFF6D3D, Biomes.ERODED_BADLANDS),
            new Entry(0x0000AC, Biomes.WARM_OCEAN),
            new Entry(0x000090, Biomes.LUKEWARM_OCEAN),
            new Entry(0x202070, Biomes.COLD_OCEAN),
            new Entry(0x000040, Biomes.DEEP_LUKEWARM_OCEAN),
            new Entry(0x202038, Biomes.DEEP_COLD_OCEAN),
            new Entry(0x404090, Biomes.DEEP_FROZEN_OCEAN),
            new Entry(0x000000, Biomes.THE_VOID),
            new Entry(0xB5DB88, Biomes.SUNFLOWER_PLAINS),
            new Entry(0x2D8E49, Biomes.FLOWER_FOREST),
            new Entry(0xB4DCDC, Biomes.ICE_SPIKES),
            new Entry(0x768E14, Biomes.BAMBOO_JUNGLE),
            new Entry(0x5E3830, Biomes.SOUL_SAND_VALLEY),
            new Entry(0xDD0808, Biomes.CRIMSON_FOREST),
            new Entry(0x49907B, Biomes.WARPED_FOREST),
            new Entry(0x403636, Biomes.BASALT_DELTAS),
            new Entry(0x60A060, Biomes.MEADOW),
            new Entry(0xFFB7C5, Biomes.CHERRY_GROVE),
            new Entry(0x8DA076, Biomes.GROVE),
            new Entry(0xB8D8E8, Biomes.FROZEN_PEAKS),
            new Entry(0xDCDCDC, Biomes.JAGGED_PEAKS),
            new Entry(0xB0A080, Biomes.STONY_PEAKS),
            new Entry(0x8A7765, Biomes.DRIPSTONE_CAVES),
            new Entry(0x4FA83D, Biomes.LUSH_CAVES),
            new Entry(0x0B0B16, Biomes.DEEP_DARK)
    );

    private DefaultBiomeColors() {
    }
}
