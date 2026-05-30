package eu.pollux28.imggen.config;

import eu.pollux28.imggen.ImgGen;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    public static void init() {
        MainConfigData configData = null;

        try {
            Path configPath = Paths.get("", "config", "imggen.json");
            Files.createDirectories(configPath.getParent());

            if (Files.exists(configPath)) {
                configData = ConfigUtil.gson.fromJson(new FileReader(configPath.toFile()), MainConfigData.class);
                if (configData == null) {
                    configData = new MainConfigData();
                }

                if (!configData.configVersion.equals(ImgGen.VERSION)) {
                    configData.configVersion = ImgGen.VERSION;
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(configPath.toFile()))) {
                        writer.write(ConfigUtil.gson.toJson(configData));
                    }
                }
            } else {
                configData = new MainConfigData();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(configPath.toFile()))) {
                    writer.write(ConfigUtil.gson.toJson(configData));
                }
            }
        } catch (IOException e) {
            ImgGen.LOGGER.error("Failed to load Image World Generator config", e);
        }

        ImgGen.CONFIG = configData == null ? new MainConfigData() : configData;
    }
}
