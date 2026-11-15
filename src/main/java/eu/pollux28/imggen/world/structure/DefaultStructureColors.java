package eu.pollux28.imggen.world.structure;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;

/**
 * Built-in colour of every vanilla structure for the structure image; black means "nothing here".
 * More can be added through {@code customStructuresRGB} in the config.
 */
public final class DefaultStructureColors {
    public record Entry(int rgb, ResourceKey<Structure> structure) {
    }

    public static final List<Entry> ENTRIES = List.of(
            new Entry(0xB5DB88, BuiltinStructures.PILLAGER_OUTPOST),
            new Entry(0x056621, BuiltinStructures.MINESHAFT),
            new Entry(0xD94515, BuiltinStructures.MINESHAFT_MESA),
            new Entry(0x40511A, BuiltinStructures.WOODLAND_MANSION),
            new Entry(0x537B09, BuiltinStructures.JUNGLE_TEMPLE),
            new Entry(0xFFBC40, BuiltinStructures.DESERT_PYRAMID),
            new Entry(0x243F36, BuiltinStructures.IGLOO),
            new Entry(0x000090, BuiltinStructures.SHIPWRECK),
            new Entry(0xFADE55, BuiltinStructures.SHIPWRECK_BEACHED),
            new Entry(0x07F9B2, BuiltinStructures.SWAMP_HUT),
            new Entry(0x606060, BuiltinStructures.STRONGHOLD),
            new Entry(0x000070, BuiltinStructures.OCEAN_MONUMENT),
            new Entry(0x202070, BuiltinStructures.OCEAN_RUIN_COLD),
            new Entry(0x0000AC, BuiltinStructures.OCEAN_RUIN_WARM),
            new Entry(0x5E3830, BuiltinStructures.FORTRESS),
            new Entry(0x49907B, BuiltinStructures.NETHER_FOSSIL),
            new Entry(0x8080FF, BuiltinStructures.END_CITY),
            new Entry(0xFAF0C0, BuiltinStructures.BURIED_TREASURE),
            new Entry(0x403636, BuiltinStructures.BASTION_REMNANT),
            new Entry(0x8DB360, BuiltinStructures.VILLAGE_PLAINS),
            new Entry(0xFA9418, BuiltinStructures.VILLAGE_DESERT),
            new Entry(0xBDB25F, BuiltinStructures.VILLAGE_SAVANNA),
            new Entry(0xFFFFFF, BuiltinStructures.VILLAGE_SNOWY),
            new Entry(0x0B6659, BuiltinStructures.VILLAGE_TAIGA),
            new Entry(0xDD0808, BuiltinStructures.RUINED_PORTAL_STANDARD),
            new Entry(0xD25F12, BuiltinStructures.RUINED_PORTAL_DESERT),
            new Entry(0x2C4205, BuiltinStructures.RUINED_PORTAL_JUNGLE),
            new Entry(0x2FFFDA, BuiltinStructures.RUINED_PORTAL_SWAMP),
            new Entry(0x789878, BuiltinStructures.RUINED_PORTAL_MOUNTAIN),
            new Entry(0x202038, BuiltinStructures.RUINED_PORTAL_OCEAN),
            new Entry(0xBF3B3B, BuiltinStructures.RUINED_PORTAL_NETHER),
            new Entry(0x0B0B16, BuiltinStructures.ANCIENT_CITY),
            new Entry(0x8A7765, BuiltinStructures.TRAIL_RUINS),
            new Entry(0xC87533, BuiltinStructures.TRIAL_CHAMBERS)
    );

    private DefaultStructureColors() {
    }
}
