package eu.pollux28.imggen.mixins;

import com.mojang.datafixers.DataFixer;
import eu.pollux28.imggen.gen.chunk.ImgGenChunkGenerator;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public abstract class MixinChunkMap {
    @Mutable
    @Final
    @Shadow
    private RandomState randomState;

    @Mutable
    @Final
    @Shadow
    private ChunkGeneratorStructureState chunkGeneratorState;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void imggen$useImgGenNoiseSettings(
            ServerLevel serverLevel,
            LevelStorageSource.LevelStorageAccess levelStorageAccess,
            DataFixer dataFixer,
            StructureTemplateManager structureTemplateManager,
            Executor executor,
            BlockableEventLoop<Runnable> mainThreadExecutor,
            LightChunkGetter lightChunkGetter,
            ChunkGenerator chunkGenerator,
            ChunkProgressListener chunkProgressListener,
            ChunkStatusUpdateListener chunkStatusUpdateListener,
            Supplier<DimensionDataStorage> overworldDataStorage,
            int viewDistance,
            boolean sync,
            CallbackInfo ci
    ) {
        if (chunkGenerator instanceof ImgGenChunkGenerator imgGenChunkGenerator) {
            RegistryAccess registryAccess = serverLevel.registryAccess();
            long seed = serverLevel.getSeed();
            this.randomState = RandomState.create(
                    imgGenChunkGenerator.generatorSettings().value(),
                    registryAccess.lookupOrThrow(Registries.NOISE),
                    seed
            );
            this.chunkGeneratorState = chunkGenerator.createState(
                    registryAccess.lookupOrThrow(Registries.STRUCTURE_SET),
                    this.randomState,
                    seed
            );
        }
    }
}
