package eu.pollux28.imggen.data;

public class HeightMapColorConverter implements ColorConverter<Integer> {
    private final int defaultHeight;

    public HeightMapColorConverter(int defaultHeight) {
        this.defaultHeight = defaultHeight;
    }

    @Override
    public Integer GetValue(int color) {
        return color & 0xFF;
    }

    @Override
    public Integer GetDefaultValue(int x, int z) {
        return defaultHeight;
    }
}
