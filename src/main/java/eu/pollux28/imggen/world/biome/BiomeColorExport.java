package eu.pollux28.imggen.world.biome;

import eu.pollux28.imggen.config.BiomeColorFile;
import eu.pollux28.imggen.config.ImgGenConfig;
import eu.pollux28.imggen.image.ColorTable;
import eu.pollux28.imggen.world.terrain.TerrainShape;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Behind {@code /imggen colors}: gives every biome that has no colour yet one of its own, stores the
 * result in {@code imggen/biome_colors.json} and writes a reference sheet of all biome colours.
 */
public final class BiomeColorExport {
    /** How far apart two colours have to be, so pixels cannot be mistaken for a neighbouring biome. */
    private static final int MIN_DISTANCE = 12;
    private static final int COLOR_STEP = 0x3D4F29;

    public record Result(int added, int total, Path colorFile, Path referenceSheet) {
    }

    private enum Source {
        BUILT_IN("built-in"),
        CONFIG("customBiomes"),
        FILE("biome_colors.json");

        private final String label;

        Source(String label) {
            this.label = label;
        }
    }

    private record Row(String id, int rgb, Source source, TerrainShape terrain, BiomeDimension dimension) {
    }

    private BiomeColorExport() {
    }

    /**
     * @throws IOException if the existing colour file is unreadable (it is left untouched) or writing fails
     */
    public static Result run(Registry<Biome> biomes, ImgGenConfig config) throws IOException {
        BiomeColorFile file = BiomeColorFile.load();
        if (file == null) {
            file = new BiomeColorFile();
        }

        // Colours in order of precedence, exactly as the biome source resolves them.
        Map<ResourceLocation, Integer> colors = new HashMap<>();
        Map<ResourceLocation, Source> sources = new HashMap<>();
        IntList taken = new IntArrayList();
        for (ImgGenConfig.BiomeColor entry : config.customBiomes) {
            claim(colors, sources, taken, ResourceLocation.tryParse(entry.biomeID), ColorTable.parseColor(entry.RGB), Source.CONFIG);
        }
        for (DefaultBiomeColors.Entry entry : DefaultBiomeColors.ENTRIES) {
            claim(colors, sources, taken, entry.biome().location(), OptionalInt.of(entry.rgb()), Source.BUILT_IN);
        }
        Map<ResourceLocation, BiomeColorFile.Entry> fileEntries = new HashMap<>();
        for (BiomeColorFile.Entry entry : file.biomes) {
            ResourceLocation id = ResourceLocation.tryParse(entry.biome);
            if (id != null) {
                fileEntries.putIfAbsent(id, entry);
                claim(colors, sources, taken, id, entry.color == null ? OptionalInt.empty() : ColorTable.parseColor(entry.color), Source.FILE);
            }
        }

        List<Holder.Reference<Biome>> all = biomes.holders()
                .sorted(Comparator.comparing(holder -> holder.key().location()))
                .toList();
        int added = 0;
        for (Holder.Reference<Biome> holder : all) {
            ResourceLocation id = holder.key().location();
            if (colors.containsKey(id)) {
                continue;
            }

            int rgb = pickColor(id, taken);
            BiomeColorFile.Entry entry = fileEntries.get(id);
            if (entry == null) {
                entry = new BiomeColorFile.Entry();
                entry.biome = id.toString();
                file.biomes.add(entry);
                fileEntries.put(id, entry);
            }
            entry.color = String.format("0x%06X", rgb);
            claim(colors, sources, taken, id, OptionalInt.of(rgb), Source.FILE);
            added++;
        }
        file.save();

        List<Row> rows = new ArrayList<>();
        for (Holder.Reference<Biome> holder : all) {
            ResourceLocation id = holder.key().location();
            BiomeColorFile.Entry entry = fileEntries.get(id);
            TerrainShape terrain = entry == null || entry.terrain == null ? null : TerrainShape.byName(entry.terrain);
            BiomeDimension dimension = entry == null || entry.dimension == null ? null : BiomeDimension.byName(entry.dimension);
            rows.add(new Row(
                    id.toString(),
                    colors.get(id),
                    sources.get(id),
                    terrain != null ? terrain : TerrainShape.of(holder),
                    dimension != null ? dimension : BiomeDimension.fromTags(holder)
            ));
        }
        Path sheet = BiomeColorFile.path().resolveSibling("biome_colors.html");
        Files.writeString(sheet, renderSheet(rows), StandardCharsets.UTF_8);
        return new Result(added, all.size(), BiomeColorFile.path(), sheet);
    }

    private static void claim(
            Map<ResourceLocation, Integer> colors,
            Map<ResourceLocation, Source> sources,
            IntList taken,
            ResourceLocation id,
            OptionalInt rgb,
            Source source
    ) {
        if (id == null || rgb.isEmpty()) {
            return;
        }
        taken.add(rgb.getAsInt());
        if (!colors.containsKey(id)) {
            colors.put(id, rgb.getAsInt());
            sources.put(id, source);
        }
    }

    /**
     * Starts from a colour derived from the biome's name, so the same biome tends to get the same colour
     * everywhere, and walks through the colour space until it is clear of every colour already in use.
     */
    private static int pickColor(ResourceLocation id, IntList taken) {
        int candidate = (id.toString().hashCode() * 0x9E3779B1) >>> 8;
        int minDistance = MIN_DISTANCE;
        for (int attempt = 1; ; attempt++) {
            if (isClear(candidate, taken, minDistance)) {
                return candidate;
            }
            candidate = (candidate + COLOR_STEP) & 0xFFFFFF;
            if (attempt % 4096 == 0 && minDistance > 1) {
                minDistance /= 2; // An enormous number of biomes: accept colours that sit closer together.
            }
        }
    }

    private static boolean isClear(int candidate, IntList taken, int minDistance) {
        int limit = minDistance * minDistance;
        for (int i = 0; i < taken.size(); i++) {
            int other = taken.getInt(i);
            int red = ((candidate >> 16) & 0xFF) - ((other >> 16) & 0xFF);
            int green = ((candidate >> 8) & 0xFF) - ((other >> 8) & 0xFF);
            int blue = (candidate & 0xFF) - (other & 0xFF);
            if (red * red + green * green + blue * blue < limit) {
                return false;
            }
        }
        return true;
    }

    private static String renderSheet(List<Row> rows) {
        StringBuilder html = new StringBuilder("""
                <!doctype html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <title>Image World Generator - biome colours</title>
                <style>
                body { font-family: system-ui, sans-serif; margin: 2rem; background: #fafafa; color: #222; }
                table { border-collapse: collapse; }
                th, td { padding: .3rem .8rem; text-align: left; border-bottom: 1px solid #ddd; }
                td.swatch { width: 4rem; border: 1px solid #888; }
                code { font-size: 1rem; }
                </style>
                </head>
                <body>
                <h1>Biome colours</h1>
                <p>Paint these exact colours into the biome image. Terrain and dimension show how each biome
                currently behaves; both can be overridden per biome in <code>biome_colors.json</code>.</p>
                <table>
                <tr><th></th><th>Colour</th><th>Biome</th><th>Terrain</th><th>Dimension</th><th>Colour set by</th></tr>
                """);
        for (Row row : rows) {
            String hex = String.format("%06X", row.rgb());
            html.append("<tr><td class=\"swatch\" style=\"background:#").append(hex).append("\"></td><td><code>#")
                    .append(hex).append("</code></td><td><code>").append(row.id()).append("</code></td><td>")
                    .append(row.terrain().name().toLowerCase(java.util.Locale.ROOT)).append("</td><td>")
                    .append(row.dimension().name().toLowerCase(java.util.Locale.ROOT)).append("</td><td>")
                    .append(row.source().label).append("</td></tr>\n");
        }
        return html.append("</table>\n</body>\n</html>\n").toString();
    }
}
