package eu.pollux28.imggen.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

public enum StructureColors {
    PILLAGER_OUTPOST(0xB5DB88, BuiltinStructures.PILLAGER_OUTPOST),
    MINESHAFT(0x056621, BuiltinStructures.MINESHAFT),
    MINESHAFT_MESA(0xD94515, BuiltinStructures.MINESHAFT_MESA),
    MANSION(0x40511A, BuiltinStructures.WOODLAND_MANSION),
    JUNGLE_PYRAMID(0x537B09, BuiltinStructures.JUNGLE_TEMPLE),
    DESERT_PYRAMID(0xFFBC40, BuiltinStructures.DESERT_PYRAMID),
    IGLOO(0x243F36, BuiltinStructures.IGLOO),
    SHIPWRECK(0x000090, BuiltinStructures.SHIPWRECK),
    SHIPWRECK_BEACHED(0xFADE55, BuiltinStructures.SHIPWRECK_BEACHED),
    SWAMP_HUT(0x07F9B2, BuiltinStructures.SWAMP_HUT),
    STRONGHOLD(0x606060, BuiltinStructures.STRONGHOLD),
    MONUMENT(0x000070, BuiltinStructures.OCEAN_MONUMENT),
    OCEAN_RUIN_COLD(0x202070, BuiltinStructures.OCEAN_RUIN_COLD),
    OCEAN_RUIN_WARM(0x0000AC, BuiltinStructures.OCEAN_RUIN_WARM),
    FORTRESS(0x5E3830, BuiltinStructures.FORTRESS),
    NETHER_FOSSIL(0x49907B, BuiltinStructures.NETHER_FOSSIL),
    END_CITY(0x8080FF, BuiltinStructures.END_CITY),
    BURIED_TREASURE(0xFAF0C0, BuiltinStructures.BURIED_TREASURE),
    BASTION_REMNANT(0x403636, BuiltinStructures.BASTION_REMNANT),
    VILLAGE_PLAINS(0x8DB360, BuiltinStructures.VILLAGE_PLAINS),
    VILLAGE_DESERT(0xFA9418, BuiltinStructures.VILLAGE_DESERT),
    VILLAGE_SAVANNA(0xBDB25F, BuiltinStructures.VILLAGE_SAVANNA),
    VILLAGE_SNOWY(0xFFFFFF, BuiltinStructures.VILLAGE_SNOWY),
    VILLAGE_TAIGA(0x0B6659, BuiltinStructures.VILLAGE_TAIGA),
    RUINED_PORTAL(0xDD0808, BuiltinStructures.RUINED_PORTAL_STANDARD),
    RUINED_PORTAL_DESERT(0xD25F12, BuiltinStructures.RUINED_PORTAL_DESERT),
    RUINED_PORTAL_JUNGLE(0x2C4205, BuiltinStructures.RUINED_PORTAL_JUNGLE),
    RUINED_PORTAL_SWAMP(0x2FFFDA, BuiltinStructures.RUINED_PORTAL_SWAMP),
    RUINED_PORTAL_MOUNTAIN(0x789878, BuiltinStructures.RUINED_PORTAL_MOUNTAIN),
    RUINED_PORTAL_OCEAN(0x202038, BuiltinStructures.RUINED_PORTAL_OCEAN),
    RUINED_PORTAL_NETHER(0xBF3B3B, BuiltinStructures.RUINED_PORTAL_NETHER),
    ANCIENT_CITY(0x0B0B16, BuiltinStructures.ANCIENT_CITY),
    TRAIL_RUINS(0x8A7765, BuiltinStructures.TRAIL_RUINS),
    NONE(0x000000, null);

    private final int RGB;
    private final ResourceKey<Structure> structure;

    StructureColors(int RGB, ResourceKey<Structure> structure) {
        this.RGB = RGB;
        this.structure = structure;
    }

    public ResourceKey<Structure> getStructure() {
        return structure;
    }

    public int getRGB() {
        return RGB;
    }
}
