package eu.pollux28.imggen.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.pollux28.imggen.ImgGen;
import eu.pollux28.imggen.gen.biomes.ImgGenBiomeSource;
import eu.pollux28.imggen.gen.chunk.ImgGenChunkGenerator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.core.Holder;

public class Commands {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<CommandSourceStack> root = net.minecraft.commands.Commands.literal("imggen")
                    .requires(source -> source.hasPermission(2));

            root.then(net.minecraft.commands.Commands.literal("debug")
                    .then(net.minecraft.commands.Commands.argument("location", Vec3Argument.vec3())
                            .executes(context -> execute(context.getSource(), BlockPos.containing(Vec3Argument.getVec3(context, "location"))))));

            dispatcher.register(root);
        });
    }

    private static int execute(CommandSourceStack source, BlockPos blockPos) {
        ChunkGenerator chunkGenerator = source.getLevel().getChunkSource().getGenerator();
        if (!(chunkGenerator instanceof ImgGenChunkGenerator imgGenChunkGenerator)) {
            source.sendSuccess(() -> Component.literal("Not in an Image World Generator world!"), false);
            return 1;
        }

        Holder<Biome> biome = source.getLevel().getBiome(blockPos);
        ResourceLocation biomeId = source.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getKey(biome.value());
        String biomeText = biomeId == null ? "unknown" : biomeId.toString();

        StringBuilder message = new StringBuilder()
                .append("Biome at ")
                .append(blockPos.getX()).append(" ")
                .append(blockPos.getZ())
                .append(" is ")
                .append(biomeText);

        if (imgGenChunkGenerator.getBiomeSource() instanceof ImgGenBiomeSource imgGenBiomeSource) {
            int[] imageData = imgGenBiomeSource.getBiomeDataProvider().getImageCoordAndValue(blockPos.getX(), blockPos.getZ());
            message.append(". Image rgb at ")
                    .append(imageData[0]).append(" ")
                    .append(imageData[1])
                    .append(" is ")
                    .append(Integer.toHexString(imageData[2]));
        }

        if (ImgGen.CONFIG.customHeightMap && imgGenChunkGenerator.hasCustomHeightMapAt(blockPos.getX(), blockPos.getZ())) {
            message.append(". Height is ")
                    .append(imgGenChunkGenerator.getImageHeight(blockPos.getX(), blockPos.getZ()));
        }

        source.sendSuccess(() -> Component.literal(message.toString()), false);
        return 1;
    }
}
