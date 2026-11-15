package eu.pollux28.imggen.image;

import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.config.ImgGenConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

public final class ImageFiles {
    private ImageFiles() {
    }

    public static void createImageDirectory() {
        try {
            Files.createDirectories(ImgGenConfig.imageDirectory());
        } catch (IOException e) {
            ImgGen.LOGGER.error("Could not create the image folder {}", ImgGenConfig.imageDirectory(), e);
        }
    }

    /**
     * Reads {@code <game dir>/imggen/image/<fileName>} and converts it. Any problem is logged once and
     * reported as empty, so a broken image degrades the world instead of crashing the server.
     */
    public static <T> Optional<T> load(String fileName, String purpose, Function<BufferedImage, T> converter) {
        Path directory = ImgGenConfig.imageDirectory();
        Path path = directory.resolve(fileName).normalize();
        if (!path.startsWith(directory)) {
            ImgGen.LOGGER.error("The {} image '{}' must be inside {}", purpose, fileName, directory);
            return Optional.empty();
        }
        if (!Files.isRegularFile(path)) {
            ImgGen.LOGGER.error("The {} image {} does not exist", purpose, path);
            return Optional.empty();
        }

        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                ImgGen.LOGGER.error("The {} image {} is not in a supported format (use PNG)", purpose, path);
                return Optional.empty();
            }
            T converted = converter.apply(image);
            ImgGen.LOGGER.info("Loaded {} image {} ({}x{})", purpose, path.getFileName(), image.getWidth(), image.getHeight());
            return Optional.of(converted);
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            ImgGen.LOGGER.error("Could not load the {} image {}", purpose, path, e);
            return Optional.empty();
        }
    }
}
