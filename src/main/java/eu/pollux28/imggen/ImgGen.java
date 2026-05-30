package eu.pollux28.imggen;

import eu.pollux28.imggen.command.Commands;
import eu.pollux28.imggen.config.Config;
import eu.pollux28.imggen.config.ConfigUtil;
import eu.pollux28.imggen.config.MainConfigData;
import eu.pollux28.imggen.gen.biomes.ImgGenBiomeSource;
import eu.pollux28.imggen.gen.chunk.ImgGenChunkGenerator;
import eu.pollux28.imggen.gen.density.ImageTerrainDensityFunction;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImgGen implements ModInitializer {
    public static final String MOD_ID = "imggen";
    public static final String VERSION = "2.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static MainConfigData CONFIG;

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Config.init();
        createImageDirectories();

        Registry.register(BuiltInRegistries.BIOME_SOURCE, id("imggen"), ImgGenBiomeSource.CODEC);
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("imggen"), ImgGenChunkGenerator.CODEC);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id("image_terrain"), ImageTerrainDensityFunction.CODEC.codec());

        Commands.init();
        ServerWorldEvents.LOAD.register((server, world) -> {
            ChunkGenerator chunkGenerator = world.getChunkSource().getGenerator();
            if (chunkGenerator instanceof ImgGenChunkGenerator imgGenChunkGenerator) {
                refreshConfig();
                imgGenChunkGenerator.reloadFromConfig();
            }
        });
    }

    public static void ensureConfig() {
        if (CONFIG == null) {
            Config.init();
        }
    }

    public static void refreshConfig() {
        CONFIG = ConfigUtil.getFromConfig(MainConfigData.class, Paths.get("", "config", "imggen.json"));
        if (CONFIG == null) {
            CONFIG = new MainConfigData();
        }
    }

    private void createImageDirectories() {
        try {
            Files.createDirectories(Paths.get("", "imggen", "image"));
            Files.createDirectories(Paths.get("", "imggen", "image", "gen"));
        } catch (IOException e) {
            LOGGER.error("Could not create Image World Generator folders", e);
        }
    }
}
