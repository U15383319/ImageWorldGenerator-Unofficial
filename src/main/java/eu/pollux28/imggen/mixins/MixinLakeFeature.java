package eu.pollux28.imggen.mixins;

import eu.pollux28.imggen.ImgGen;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LakeFeature.class)
public class MixinLakeFeature {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void imggen$reduceLakeGeneration(FeaturePlaceContext<LakeFeature.Configuration> context, CallbackInfoReturnable<Boolean> cir) {
        ImgGen.ensureConfig();
        if (context.random().nextFloat() < ImgGen.CONFIG.lakeFormationPercentChance) {
            cir.setReturnValue(false);
        }
    }
}
