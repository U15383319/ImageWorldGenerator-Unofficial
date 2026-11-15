package eu.pollux28.imggen.world.structure;

import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.config.ImgGenConfig;
import eu.pollux28.imggen.image.ColorTable;
import eu.pollux28.imggen.image.ImageFiles;
import eu.pollux28.imggen.image.IndexedImage;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/** Structure placement dictated by the config: the structure image and the structure blacklist. */
public final class ImageStructures {
    @Nullable
    private final IndexedImage<Holder<Structure>> image;

    private ImageStructures(@Nullable IndexedImage<Holder<Structure>> image) {
        this.image = image;
    }

    /** Loads the structure image; one pixel is one block, and any non-black pixel places its structure in that chunk. */
    public static ImageStructures load(ImgGenConfig config, Registry<Structure> structures) {
        Set<ResourceKey<Structure>> blacklist = config.blacklistAppliesToImage() ? blacklist(config) : Set.of();
        ColorTable<Holder<Structure>> table = new ColorTable<>();

        // Config entries go first so they can override a built-in colour.
        for (ImgGenConfig.StructureColor entry : config.customStructuresRGB) {
            if (entry.structureID.equalsIgnoreCase("modid:structureid")) {
                continue; // Placeholder written by old versions of the mod.
            }
            OptionalInt rgb = ColorTable.parseColor(entry.RGB);
            if (rgb.isEmpty()) {
                ImgGen.LOGGER.error("Custom structure {} has an invalid colour '{}'", entry.structureID, entry.RGB);
                continue;
            }
            ResourceLocation location = ResourceLocation.tryParse(entry.structureID);
            Optional<Holder.Reference<Structure>> structure = location == null
                    ? Optional.empty()
                    : structures.getHolder(ResourceKey.create(Registries.STRUCTURE, location));
            if (structure.isEmpty()) {
                ImgGen.LOGGER.error("Custom structure '{}' does not exist", entry.structureID);
                continue;
            }
            if (!table.register(rgb.getAsInt(), structure.get())) {
                ImgGen.LOGGER.warn("Colour {} is used by more than one custom structure; {} was ignored", entry.RGB, entry.structureID);
            }
        }

        for (DefaultStructureColors.Entry entry : DefaultStructureColors.ENTRIES) {
            structures.getHolder(entry.structure()).ifPresent(structure -> table.register(entry.rgb(), structure));
        }

        IndexedImage<Holder<Structure>> image = ImageFiles.<IndexedImage<Holder<Structure>>>load(
                config.customStructuresImage,
                "structure",
                source -> IndexedImage.create(source, 1.0F, config.repeatStructureImage, config.repeatMirrorStructureImage, rgb -> {
                    Holder<Structure> structure = rgb == 0 ? null : table.nearest(rgb);
                    boolean blacklisted = structure != null && structure.unwrapKey().filter(blacklist::contains).isPresent();
                    return blacklisted ? null : structure;
                })
        ).orElse(null);
        if (image != null && image.values().stream().allMatch(Objects::isNull)) {
            image = null; // Nothing to place: skip scanning it for every chunk.
        }
        return new ImageStructures(image);
    }

    /** @return the distinct structures painted anywhere inside the chunk */
    public Set<Holder<Structure>> inChunk(ChunkPos chunkPos) {
        if (image == null) {
            return Set.of();
        }

        Set<Holder<Structure>> result = Set.of();
        int lastIndex = -1;
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                int index = image.valueIndex(x, z);
                if (index < 0 || index == lastIndex) {
                    continue;
                }
                lastIndex = index;

                Holder<Structure> structure = image.values().get(index);
                if (structure != null) {
                    if (result.isEmpty()) {
                        result = new LinkedHashSet<>();
                    }
                    result.add(structure);
                }
            }
        }
        return result;
    }

    public static Set<ResourceKey<Structure>> blacklist(ImgGenConfig config) {
        Set<ResourceKey<Structure>> blacklist = new HashSet<>();
        for (String id : config.structureBlacklist) {
            if (id.equalsIgnoreCase("modid:structureid")) {
                continue;
            }
            ResourceLocation location = ResourceLocation.tryParse(id);
            if (location == null) {
                ImgGen.LOGGER.error("Blacklisted structure '{}' is not a valid id", id);
            } else {
                blacklist.add(ResourceKey.create(Registries.STRUCTURE, location));
            }
        }
        return blacklist;
    }

    /**
     * @return the structure set without its blacklisted structures; the same holder if none are affected,
     * empty if nothing is left
     */
    public static Optional<Holder<StructureSet>> withoutBlacklisted(Holder<StructureSet> holder, Set<ResourceKey<Structure>> blacklist) {
        StructureSet set = holder.value();
        List<StructureSet.StructureSelectionEntry> kept = new ArrayList<>(set.structures().size());
        for (StructureSet.StructureSelectionEntry entry : set.structures()) {
            if (entry.structure().unwrapKey().filter(blacklist::contains).isEmpty()) {
                kept.add(entry);
            }
        }

        if (kept.size() == set.structures().size()) {
            return Optional.of(holder);
        }
        return kept.isEmpty() ? Optional.empty() : Optional.of(Holder.direct(new StructureSet(kept, set.placement())));
    }
}
