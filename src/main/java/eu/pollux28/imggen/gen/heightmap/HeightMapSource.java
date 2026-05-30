package eu.pollux28.imggen.gen.heightmap;

import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.data.HeightDataProvider;
import eu.pollux28.imggen.data.HeightMapColorConverter;

public class HeightMapSource {
    private final HeightMapColorConverter heightMapColorConverter;
    private final HeightDataProvider heightMapDataProvider;

    public HeightMapSource() {
        ImgGen.ensureConfig();
        this.heightMapColorConverter = new HeightMapColorConverter(ImgGen.CONFIG.seaLevel);
        this.heightMapDataProvider = new HeightDataProvider(
                heightMapColorConverter,
                ImgGen.CONFIG.heightMapScale,
                ImgGen.CONFIG.heightMapName
        );
    }

    public int getHeight(int x, int z) {
        return heightMapDataProvider.GetData(x, z);
    }

    public boolean isInImage(int x, int z) {
        return heightMapDataProvider.isInImage(x, z);
    }

    public HeightMapColorConverter getHeightMapColorConverter() {
        return heightMapColorConverter;
    }

    public HeightDataProvider getHeightMapDataProvider() {
        return heightMapDataProvider;
    }
}
