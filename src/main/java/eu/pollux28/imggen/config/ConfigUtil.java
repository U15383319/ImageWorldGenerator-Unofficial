package eu.pollux28.imggen.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.pollux28.imggen.ImgGen;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigUtil {
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static <T> Constructor<T> getConstructor(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            ImgGen.LOGGER.error("Config class has no default constructor", e);
            return null;
        }
    }

    private static <T> T create(Constructor<T> constructor) {
        if (constructor == null) {
            return null;
        }

        try {
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            ImgGen.LOGGER.error("Failed to create config instance", e);
            return null;
        }
    }

    public static <T> T getFromConfig(Class<T> configClass, Path path) {
        T config = create(getConstructor(configClass));

        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            if (Files.exists(path)) {
                T readConfig = ConfigUtil.gson.fromJson(new FileReader(path.toFile()), configClass);
                if (readConfig != null) {
                    config = readConfig;
                }

                String version = (String) config.getClass().getField("configVersion").get(config);
                if (!version.equals(ImgGen.VERSION)) {
                    config.getClass().getField("configVersion").set(config, ImgGen.VERSION);
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
                        writer.write(ConfigUtil.gson.toJson(config));
                    }
                }
            } else {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
                    writer.write(ConfigUtil.gson.toJson(config));
                }
            }
        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            ImgGen.LOGGER.error("Failed to read config {}", path, e);
        }

        return config;
    }
}
