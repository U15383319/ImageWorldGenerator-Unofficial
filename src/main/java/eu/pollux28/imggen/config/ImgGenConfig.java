package eu.pollux28.imggen.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import eu.pollux28.imggen.ImgGen;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Contents of {@code config/imggen.json}. Field names are the JSON keys and are kept identical to the
 * original mod so existing config files keep working.
 *
 * <p>The file is read every time a world's chunk generator is created, so edits apply to the next world
 * load without restarting the game. A loaded instance is never mutated afterwards.
 */
public final class ImgGenConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "imggen.json";

    // Biome image
    public String biomesImageName = "biomeImage.png";
    public float biomesImageScale = 1;
    public boolean repeatBiomeImage = false;
    public boolean repeatMirrorBiomeImage = false;
    /** Biome used outside the image when {@link #continuousGen} is off. The void means: the world ends there. */
    public String defaultBiome = "minecraft:the_void";
    /** Outside the image: {@code true} continues with vanilla generation, {@code false} uses {@link #defaultBiome}. */
    public boolean continuousGen = false;
    /**
     * Puts the vanilla world border around the biome image when the world is created. It can be changed
     * afterwards with {@code /worldborder} like in any other world.
     */
    public boolean limitWorldToImage = false;
    public List<BiomeColor> customBiomes = new ArrayList<>();

    // Height map image
    public boolean customHeightMap = false;
    public String heightMapName = "HeightMap.png";
    public float heightMapScale = 1;
    public boolean repeatHeightMapImage = false;
    public boolean repeatMirrorHeightMapImage = false;
    /** Sea level used while {@link #customHeightMap} is on. */
    public int seaLevel = 63;

    // Structures
    public boolean placeVanillaStructures = true;
    public boolean customStructures = false;
    public String customStructuresImage = "structureMap.png";
    public boolean repeatStructureImage = false;
    public boolean repeatMirrorStructureImage = false;
    public boolean useBlackList = false;
    /** 0: the blacklist applies to vanilla placement, 1: to image placement, 2: to both. */
    public byte blackListApplication = 2;
    public List<String> structureBlacklist = new ArrayList<>();
    public List<StructureColor> customStructuresRGB = new ArrayList<>();

    /** Chance in [0, 1] that a lake feature is skipped. */
    public float lakeFormationPercentChance = 0.5f;

    public static final class BiomeColor {
        public String biomeID = "";
        public String RGB = "";
    }

    public static final class StructureColor {
        public String structureID = "";
        public String RGB = "";
    }

    public boolean blacklistAppliesToVanilla() {
        return useBlackList && blackListApplication != 1;
    }

    public boolean blacklistAppliesToImage() {
        return useBlackList && blackListApplication != 0;
    }

    public static Path imageDirectory() {
        return FMLPaths.GAMEDIR.get().resolve(ImgGen.MOD_ID).resolve("image");
    }

    /** Reads the config file, creating or normalising it on disk when needed. Never returns {@code null}. */
    public static ImgGenConfig load() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        ImgGenConfig config = null;
        String existing = null;

        if (Files.isRegularFile(path)) {
            try {
                existing = Files.readString(path, StandardCharsets.UTF_8);
                config = GSON.fromJson(existing, ImgGenConfig.class);
            } catch (IOException | JsonParseException e) {
                ImgGen.LOGGER.error("Could not read {}, using default settings for this session", path, e);
                return new ImgGenConfig();
            }
        }

        if (config == null) {
            config = new ImgGenConfig();
        }
        config.sanitize();

        String serialized = GSON.toJson(config);
        if (!serialized.equals(existing)) {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, serialized, StandardCharsets.UTF_8);
            } catch (IOException e) {
                ImgGen.LOGGER.error("Could not write {}", path, e);
            }
        }
        return config;
    }

    /** Gson leaves fields {@code null} when the file says so; restore sane values instead of failing later. */
    private void sanitize() {
        ImgGenConfig defaults = new ImgGenConfig();
        if (biomesImageName == null) biomesImageName = defaults.biomesImageName;
        if (defaultBiome == null) defaultBiome = defaults.defaultBiome;
        if (heightMapName == null) heightMapName = defaults.heightMapName;
        if (customStructuresImage == null) customStructuresImage = defaults.customStructuresImage;
        if (customBiomes == null) customBiomes = new ArrayList<>();
        if (structureBlacklist == null) structureBlacklist = new ArrayList<>();
        if (customStructuresRGB == null) customStructuresRGB = new ArrayList<>();
        if (!(biomesImageScale > 0)) biomesImageScale = 1;
        if (!(heightMapScale > 0)) heightMapScale = 1;
        customBiomes.removeIf(entry -> entry == null || entry.biomeID == null || entry.RGB == null);
        customStructuresRGB.removeIf(entry -> entry == null || entry.structureID == null || entry.RGB == null);
        structureBlacklist.removeIf(entry -> entry == null);
    }
}
