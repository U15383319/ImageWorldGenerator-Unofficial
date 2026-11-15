package eu.pollux28.imggen;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import eu.pollux28.imggen.command.ImgGenCommand;
import eu.pollux28.imggen.config.ImgGenConfig;
import eu.pollux28.imggen.image.ImageFiles;
import eu.pollux28.imggen.world.biome.ImgGenBiomeSource;
import eu.pollux28.imggen.world.chunk.ImgGenChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import javax.annotation.Nullable;

@Mod(ImgGen.MOD_ID)
public final class ImgGen {
    public static final String MOD_ID = "imggen";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, MOD_ID);
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID);

    static {
        BIOME_SOURCES.register(MOD_ID, () -> ImgGenBiomeSource.CODEC);
        CHUNK_GENERATORS.register(MOD_ID, () -> ImgGenChunkGenerator.CODEC);
    }

    public ImgGen(IEventBus modBus) {
        BIOME_SOURCES.register(modBus);
        CHUNK_GENERATORS.register(modBus);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> ImgGenCommand.register(event.getDispatcher()));

        // Create the config file and image folder up front so players find them before making a world.
        ImgGenConfig.load();
        ImageFiles.createImageDirectory();
    }

    /** @return the generator of the level if it is an image world on the server side, otherwise {@code null} */
    @Nullable
    public static ImgGenChunkGenerator imageGenerator(Level level) {
        return level instanceof ServerLevel serverLevel
                && serverLevel.getChunkSource().getGenerator() instanceof ImgGenChunkGenerator generator ? generator : null;
    }
}
