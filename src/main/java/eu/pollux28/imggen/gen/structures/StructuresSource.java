package eu.pollux28.imggen.gen.structures;

import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.data.StructureColorConverter;
import eu.pollux28.imggen.data.StructureColors;
import eu.pollux28.imggen.data.StructureDataProvider;
import eu.pollux28.imggen.util.StructureAndRGBPair;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class StructuresSource {
    private final Registry<Structure> structures;
    private final Set<Structure> structureBlacklist = new LinkedHashSet<>();
    private StructureColorConverter structureColorConverter;
    private StructureDataProvider structureDataProvider;

    public StructuresSource(Registry<Structure> structures) {
        this.structures = structures;
    }

    public void registerStructures() {
        this.structureColorConverter = new StructureColorConverter();
        for (StructureColors structureColor : StructureColors.values()) {
            if (structureColor.getStructure() == null) {
                continue;
            }

            getStructureByKey(structureColor.getStructure())
                    .ifPresent(structure -> structureColorConverter.RegisterStructure(structureColor.getRGB(), structure));
        }

        for (StructureAndRGBPair structureIDAndRGBPair : ImgGen.CONFIG.customStructuresRGB) {
            if (isPlaceholderStructure(structureIDAndRGBPair.structureID)) {
                continue;
            }

            int rgb;
            try {
                rgb = Integer.decode(structureIDAndRGBPair.RGB);
            } catch (NumberFormatException e) {
                ImgGen.LOGGER.error("Structure {} has an invalid color code {}", structureIDAndRGBPair.structureID, structureIDAndRGBPair.RGB);
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(structureIDAndRGBPair.structureID);
            if (id == null) {
                ImgGen.LOGGER.error("Incorrect structure id format. Expected modid:structureid, got {}", structureIDAndRGBPair.structureID);
                continue;
            }

            Optional<Holder.Reference<Structure>> structure = getStructureById(id);
            if (structure.isEmpty()) {
                ImgGen.LOGGER.error("Could not find structure {}", structureIDAndRGBPair.structureID);
                continue;
            }

            structureColorConverter.RegisterStructure(rgb, structure.get());
            ImgGen.LOGGER.debug("Registered structure {} with color {}", id, Integer.toHexString(rgb));
        }

        this.structureDataProvider = new StructureDataProvider(structureColorConverter, ImgGen.CONFIG.customStructuresImage);
    }

    public void registerBlackList() {
        structureBlacklist.clear();
        for (String idText : ImgGen.CONFIG.structureBlacklist) {
            if (isPlaceholderStructure(idText)) {
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(idText);
            if (id == null) {
                continue;
            }

            getStructureById(id).ifPresent(structure -> structureBlacklist.add(structure.value()));
        }
    }

    public boolean isBlacklisted(Structure structure) {
        return structureBlacklist.contains(structure);
    }

    public Set<Holder<Structure>> getStructuresInChunk(ChunkPos chunkPos) {
        Set<Holder<Structure>> result = new LinkedHashSet<>();
        if (structureDataProvider == null) {
            return result;
        }

        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                Holder<Structure> structure = structureDataProvider.GetData(x, z);
                if (structure != null) {
                    result.add(structure);
                }
            }
        }

        return result;
    }

    private Optional<Holder.Reference<Structure>> getStructureById(ResourceLocation structureId) {
        return getStructureByKey(ResourceKey.create(Registries.STRUCTURE, structureId));
    }

    private Optional<Holder.Reference<Structure>> getStructureByKey(ResourceKey<Structure> key) {
        return structures.getHolder(key);
    }

    private boolean isPlaceholderStructure(String structureId) {
        return "modid:structureid".equalsIgnoreCase(structureId);
    }
}
