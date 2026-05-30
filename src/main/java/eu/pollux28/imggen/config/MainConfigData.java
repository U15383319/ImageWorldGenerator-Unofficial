package eu.pollux28.imggen.config;

import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.util.BiomeIDAndRGBPair;
import eu.pollux28.imggen.util.StructureAndRGBPair;

import java.util.Arrays;
import java.util.List;

public class MainConfigData {
    public String configVersion = ImgGen.VERSION;
    public String biomesImageName = "biomeImage.png";
    public float biomesImageScale = 1;
    public boolean repeatBiomeImage = false;
    public boolean repeatMirrorBiomeImage = false;
    public String defaultBiome = "minecraft:ocean";
    public boolean continuousGen = true;
    public boolean largeBiomes = false;
    public List<BiomeIDAndRGBPair> customBiomes = Arrays.asList();
    public boolean customHeightMap = false;
    public String heightMapName = "HeightMap.png";
    public float heightMapScale = 1;
    public boolean repeatHeightMapImage = false;
    public boolean repeatMirrorHeightMapImage = false;
    public int seaLevel = 63;
    public boolean placeVanillaStructures = true;
    public boolean customStructures = false;
    public String customStructuresImage = "structureMap.png";
    public boolean repeatStructureImage = false;
    public boolean repeatMirrorStructureImage = false;
    public boolean useBlackList = false;
    public byte blackListApplication = 2;
    public String[] structureBlacklist = {};
    public List<StructureAndRGBPair> customStructuresRGB = Arrays.asList();
    public float lakeFormationPercentChance = 0.5f;

    public int segmentImageWidth = 512;
    public int segmentImageHeight = 512;
    public int imageCacheSize = 4;
}
