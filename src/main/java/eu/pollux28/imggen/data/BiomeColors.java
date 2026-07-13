package eu.pollux28.imggen.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public enum BiomeColors {
    Ocean(0x000070, Biomes.OCEAN),
    Plains(0x8DB360, Biomes.PLAINS),
    Desert(0xFA9418, Biomes.DESERT),
    Windswept_Hills(0x606060, Biomes.WINDSWEPT_HILLS),
    Forest(0x056621, Biomes.FOREST),
    Taiga(0x0B6659, Biomes.TAIGA),
    Swamp(0x07F9B2, Biomes.SWAMP),
    Mangrove_Swamp(0x1D4E2A, Biomes.MANGROVE_SWAMP),
    River(0x0000FF, Biomes.RIVER),
    Nether_Wastes(0xBF3B3B, Biomes.NETHER_WASTES),
    The_End(0x8080FF, Biomes.THE_END),
    Small_End_Islands(0x7070FF, Biomes.SMALL_END_ISLANDS),
    End_Midlands(0x6060FF, Biomes.END_MIDLANDS),
    End_Highlands(0x5050FF, Biomes.END_HIGHLANDS),
    End_Barrens(0x9090FF, Biomes.END_BARRENS),
    Frozen_Ocean(0x7070D6, Biomes.FROZEN_OCEAN),
    Frozen_River(0xA0A0FF, Biomes.FROZEN_RIVER),
    Snowy_Plains(0xFFFFFF, Biomes.SNOWY_PLAINS),
    Snowy_Slopes(0xA0A0A0, Biomes.SNOWY_SLOPES),
    Mushroom_Fields(0xFF00FF, Biomes.MUSHROOM_FIELDS),
    Beach(0xFADE55, Biomes.BEACH),
    Windswept_Gravelly_Hills(0x22551C, Biomes.WINDSWEPT_GRAVELLY_HILLS),
    Windswept_Forest(0x507050, Biomes.WINDSWEPT_FOREST),
    Jungle(0x537B09, Biomes.JUNGLE),
    Sparse_Jungle(0x628B17, Biomes.SPARSE_JUNGLE),
    Deep_Ocean(0x000030, Biomes.DEEP_OCEAN),
    Stony_Shore(0xA2A284, Biomes.STONY_SHORE),
    Snowy_Beach(0xFAF0C0, Biomes.SNOWY_BEACH),
    Birch_Forest(0x307444, Biomes.BIRCH_FOREST),
    Old_Growth_Birch_Forest(0x589C6C, Biomes.OLD_GROWTH_BIRCH_FOREST),
    Dark_Forest(0x40511A, Biomes.DARK_FOREST),
    Snowy_Taiga(0x31554A, Biomes.SNOWY_TAIGA),
    Old_Growth_Pine_Taiga(0x596651, Biomes.OLD_GROWTH_PINE_TAIGA),
    Old_Growth_Spruce_Taiga(0x818E79, Biomes.OLD_GROWTH_SPRUCE_TAIGA),
    Savanna(0xBDB25F, Biomes.SAVANNA),
    Savanna_Plateau(0xA79D64, Biomes.SAVANNA_PLATEAU),
    Windswept_Savanna(0xE5DA87, Biomes.WINDSWEPT_SAVANNA),
    Badlands(0xD94515, Biomes.BADLANDS),
    Wooded_Badlands(0xB09765, Biomes.WOODED_BADLANDS),
    Eroded_Badlands(0xFF6D3D, Biomes.ERODED_BADLANDS),
    Warm_Ocean(0x0000AC, Biomes.WARM_OCEAN),
    Lukewarm_Ocean(0x000090, Biomes.LUKEWARM_OCEAN),
    Cold_Ocean(0x202070, Biomes.COLD_OCEAN),
    Deep_Lukewarm_Ocean(0x000040, Biomes.DEEP_LUKEWARM_OCEAN),
    Deep_Cold_Ocean(0x202038, Biomes.DEEP_COLD_OCEAN),
    Deep_Frozen_Ocean(0x404090, Biomes.DEEP_FROZEN_OCEAN),
    The_Void(0x000000, Biomes.THE_VOID),
    Sunflower_Plains(0xB5DB88, Biomes.SUNFLOWER_PLAINS),
    Flower_Forest(0x2D8E49, Biomes.FLOWER_FOREST),
    Ice_Spikes(0xB4DCDC, Biomes.ICE_SPIKES),
    Bamboo_Jungle(0x768E14, Biomes.BAMBOO_JUNGLE),
    Soul_Sand_Valley(0x5E3830, Biomes.SOUL_SAND_VALLEY),
    Crimson_Forest(0xDD0808, Biomes.CRIMSON_FOREST),
    Warped_Forest(0x49907B, Biomes.WARPED_FOREST),
    Basalt_Deltas(0x403636, Biomes.BASALT_DELTAS),
    Meadow(0x60A060, Biomes.MEADOW),
    Cherry_Grove(0xFFB7C5, Biomes.CHERRY_GROVE),
    Grove(0x8DA076, Biomes.GROVE),
    Frozen_Peaks(0xB8D8E8, Biomes.FROZEN_PEAKS),
    Jagged_Peaks(0xDCDCDC, Biomes.JAGGED_PEAKS),
    Stony_Peaks(0xB0A080, Biomes.STONY_PEAKS),
    Dripstone_Caves(0x8A7765, Biomes.DRIPSTONE_CAVES),
    Lush_Caves(0x4FA83D, Biomes.LUSH_CAVES),
    Deep_Dark(0x0B0B16, Biomes.DEEP_DARK);

    private final int RGB;
    private final ResourceKey<Biome> biome;

    BiomeColors(int RGB, ResourceKey<Biome> biome) {
        this.RGB = RGB;
        this.biome = biome;
    }

    public ResourceKey<Biome> getBiome() {
        return biome;
    }

    public int getRGB() {
        return RGB;
    }
}
