package eu.pollux28.imggen.data;

import eu.pollux28.imggen.ImgGen;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class StructureDataProvider {
    private final ColorConverter<Holder<Structure>> colorConverter;
    private final ImageManipulator imageManipulator;
    private final int width;
    private final int height;
    private final int widthHalf;
    private final int heightHalf;

    public StructureDataProvider(ColorConverter<Holder<Structure>> colorConverter, String path) {
        this.colorConverter = colorConverter;
        this.imageManipulator = new ImageManipulator(path, "structure");
        this.width = imageManipulator.getWidth();
        this.height = imageManipulator.getHeight();
        this.widthHalf = width / 2;
        this.heightHalf = height / 2;
    }

    public boolean isInImage(int x, int z) {
        int centeredX = x + widthHalf;
        int centeredZ = z + heightHalf;
        return centeredX >= 0 && centeredZ >= 0 && centeredX < width && centeredZ < height;
    }

    public Holder<Structure> GetData(int x, int z) {
        int centeredX = x + widthHalf;
        int centeredZ = z + heightHalf;

        if (!isInImage(x, z)) {
            if (!ImgGen.CONFIG.repeatStructureImage) {
                return colorConverter.GetDefaultValue(centeredX, centeredZ);
            }

            centeredX = wrap(centeredX, width, ImgGen.CONFIG.repeatMirrorStructureImage);
            centeredZ = wrap(centeredZ, height, ImgGen.CONFIG.repeatMirrorStructureImage);
        }

        int rgb = imageManipulator.getRGB(centeredX, centeredZ);
        return colorConverter.GetValue(rgb);
    }

    private int wrap(int value, int max, boolean mirror) {
        if (!mirror) {
            return Math.floorMod(value, max);
        }

        int period = max * 2;
        int wrapped = Math.floorMod(value, period);
        return wrapped < max ? wrapped : period - wrapped - 1;
    }
}
