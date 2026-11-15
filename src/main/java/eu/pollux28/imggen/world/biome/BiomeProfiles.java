package eu.pollux28.imggen.world.biome;

import eu.pollux28.imggen.world.terrain.TerrainShape;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;

/**
 * How biomes behave in an image world: which terrain they get and which dimension's materials they use.
 * Both normally follow from the biome's tags; {@code imggen/biome_colors.json} can override either for
 * biomes whose mod did not tag them.
 */
public final class BiomeProfiles {
    /** No overrides: every biome behaves as its tags say. */
    public static final BiomeProfiles TAGS_ONLY = new BiomeProfiles(Map.of(), Map.of());

    private final Map<Holder<Biome>, TerrainShape> terrainOverrides;
    private final Map<Holder<Biome>, BiomeDimension> dimensionOverrides;

    public BiomeProfiles(Map<Holder<Biome>, TerrainShape> terrainOverrides, Map<Holder<Biome>, BiomeDimension> dimensionOverrides) {
        this.terrainOverrides = Map.copyOf(terrainOverrides);
        this.dimensionOverrides = Map.copyOf(dimensionOverrides);
    }

    public TerrainShape terrain(Holder<Biome> biome) {
        if (!terrainOverrides.isEmpty()) {
            TerrainShape override = terrainOverrides.get(biome);
            if (override != null) {
                return override;
            }
        }
        return TerrainShape.of(biome);
    }

    public BiomeDimension dimension(Holder<Biome> biome) {
        if (!dimensionOverrides.isEmpty()) {
            BiomeDimension override = dimensionOverrides.get(biome);
            if (override != null) {
                return override;
            }
        }
        return BiomeDimension.fromTags(biome);
    }
}
