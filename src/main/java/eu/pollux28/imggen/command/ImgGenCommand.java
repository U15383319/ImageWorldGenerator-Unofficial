package eu.pollux28.imggen.command;

import com.mojang.brigadier.CommandDispatcher;
import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.config.ImgGenConfig;
import eu.pollux28.imggen.image.HeightImage;
import eu.pollux28.imggen.image.ImageMapping;
import eu.pollux28.imggen.image.IndexedImage;
import eu.pollux28.imggen.world.biome.BiomeColorExport;
import eu.pollux28.imggen.world.biome.ImgGenBiomeSource;
import eu.pollux28.imggen.world.chunk.ImgGenChunkGenerator;
import eu.pollux28.imggen.world.terrain.TerrainShape;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biome;

import java.io.IOException;

/**
 * {@code /imggen debug [pos]} shows what the images say about a position;
 * {@code /imggen colors} assigns colours to biomes that have none and writes the colour reference sheet.
 */
public final class ImgGenCommand {
    private ImgGenCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(ImgGen.MOD_ID)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("debug")
                        .executes(context -> debug(context.getSource(), BlockPos.containing(context.getSource().getPosition())))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> debug(context.getSource(), BlockPosArgument.getBlockPos(context, "pos")))))
                .then(Commands.literal("colors")
                        .executes(context -> colors(context.getSource()))));
    }

    private static int colors(CommandSourceStack source) {
        try {
            BiomeColorExport.Result result = BiomeColorExport.run(
                    source.registryAccess().registryOrThrow(Registries.BIOME),
                    ImgGenConfig.load()
            );
            source.sendSuccess(() -> Component.translatable(
                    "commands.imggen.colors.done", result.added(), result.total(), result.colorFile().toString(), result.referenceSheet().toString()
            ), true);
            return result.total();
        } catch (IOException e) {
            ImgGen.LOGGER.error("Could not update the biome colour file", e);
            source.sendFailure(Component.translatable("commands.imggen.colors.failed", String.valueOf(e.getMessage())));
            return 0;
        }
    }

    private static int debug(CommandSourceStack source, BlockPos pos) {
        ImgGenChunkGenerator generator = ImgGen.imageGenerator(source.getLevel());
        if (generator == null) {
            source.sendFailure(Component.translatable("commands.imggen.not_image_world"));
            return 0;
        }

        int x = pos.getX();
        int z = pos.getZ();
        Holder<Biome> biome = source.getLevel().getBiome(pos);
        String biomeName = biome.unwrapKey().map(key -> key.location().toString()).orElse("?");
        source.sendSuccess(() -> Component.translatable("commands.imggen.debug.biome", x, z, biomeName), false);

        if (generator.getBiomeSource() instanceof ImgGenBiomeSource biomeSource) {
            IndexedImage<Holder<Biome>> image = biomeSource.biomeImage();
            // Biomes are stored per 4x4 cell and read from the image at the corner of the cell.
            int cellX = x & ~3;
            int cellZ = z & ~3;
            int pixel = image == null ? ImageMapping.OUTSIDE : image.mapping().pixelIndex(cellX, cellZ);
            if (pixel == ImageMapping.OUTSIDE) {
                source.sendSuccess(() -> Component.translatable("commands.imggen.debug.outside_image"), false);
            } else {
                ImageMapping mapping = image.mapping();
                Holder<Biome> painted = image.get(cellX, cellZ);
                String paintedName = painted == null ? "?" : painted.unwrapKey().map(key -> key.location().toString()).orElse("?");
                source.sendSuccess(() -> Component.translatable(
                        "commands.imggen.debug.pixel", mapping.pixelX(pixel), mapping.pixelZ(pixel), paintedName
                ), false);
            }
        }

        TerrainShape shape = generator.terrainShapeAt(x, z);
        if (shape != null && shape != TerrainShape.UNSHAPED) {
            source.sendSuccess(() -> Component.translatable("commands.imggen.debug.terrain", shape.name()), false);
        }

        int height = generator.heightMapHeightAt(x, z);
        if (height != HeightImage.OUTSIDE) {
            source.sendSuccess(() -> Component.translatable("commands.imggen.debug.height", height), false);
        }
        return 1;
    }
}
