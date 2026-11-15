package eu.pollux28.imggen.mixin;

import eu.pollux28.imggen.world.chunk.ImgGenChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Thins out lakes in image worlds, where they tend to punch holes into carefully painted maps. */
@Mixin(LakeFeature.class)
public abstract class LakeFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void imggen$skipSomeLakes(FeaturePlaceContext<LakeFeature.Configuration> context, CallbackInfoReturnable<Boolean> cir) {
        if (context.chunkGenerator() instanceof ImgGenChunkGenerator generator
                && context.random().nextFloat() < generator.config().lakeFormationPercentChance) {
            cir.setReturnValue(false);
        }
    }
}
