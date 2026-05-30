package eu.pollux28.imggen.data;

import eu.pollux28.imggen.ImgGen;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageManipulator {
    private final String path;
    private final BufferedImage image;

    public ImageManipulator(String path, String imageType) {
        this.path = path;
        this.image = loadImage(path, imageType);
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public int getRGB(int x, int y) {
        int clampedX = Math.max(0, Math.min(x, getWidth() - 1));
        int clampedY = Math.max(0, Math.min(y, getHeight() - 1));
        return image.getRGB(clampedX, clampedY);
    }

    public String getPath() {
        return path;
    }

    private BufferedImage loadImage(String imagePath, String imageType) {
        Path configPath = Paths.get("", "imggen", "image", imagePath);
        try {
            BufferedImage loaded = ImageIO.read(configPath.toFile());
            if (loaded != null) {
                ImgGen.LOGGER.info("Loaded {} image {} ({}x{})", imageType, imagePath, loaded.getWidth(), loaded.getHeight());
                return loaded;
            }
        } catch (IOException e) {
            ImgGen.LOGGER.warn("Could not load image at /imggen/image/{}", imagePath);
        }

        BufferedImage fallback = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        fallback.setRGB(0, 0, 0xFF000000);
        return fallback;
    }
}
