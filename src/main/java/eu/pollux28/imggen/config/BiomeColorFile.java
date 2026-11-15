package eu.pollux28.imggen.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Contents of {@code imggen/biome_colors.json}: colours for biomes that have none built in, mostly those
 * of other mods. The file is created and extended by {@code /imggen colors} and only has an effect once
 * it exists. Entries are never changed by the mod after they were written, so images painted with these
 * colours stay valid when mods are added or removed.
 */
public final class BiomeColorFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public List<Entry> biomes = new ArrayList<>();

    public static final class Entry {
        public String biome = "";
        /** Colour like {@code 0x7A5C3E}; may be left out for an entry that only overrides behaviour. */
        @Nullable
        public String color;
        /** Optional name of a terrain shape; without it the biome's tags decide. */
        @Nullable
        public String terrain;
        /** Optional {@code overworld}, {@code nether} or {@code end}; without it the biome's tags decide. */
        @Nullable
        public String dimension;
    }

    public static Path path() {
        return FMLPaths.GAMEDIR.get().resolve("imggen").resolve("biome_colors.json");
    }

    /**
     * @return the parsed file, or {@code null} if it does not exist
     * @throws IOException if the file exists but cannot be read or is not valid
     */
    @Nullable
    public static BiomeColorFile load() throws IOException {
        Path path = path();
        if (!Files.isRegularFile(path)) {
            return null;
        }

        try {
            BiomeColorFile file = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), BiomeColorFile.class);
            if (file == null) {
                file = new BiomeColorFile();
            }
            if (file.biomes == null) {
                file.biomes = new ArrayList<>();
            }
            file.biomes.removeIf(entry -> entry == null || entry.biome == null);
            return file;
        } catch (JsonParseException e) {
            throw new IOException("Not valid JSON: " + e.getMessage(), e);
        }
    }

    public void save() throws IOException {
        Path path = path();
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
    }
}
