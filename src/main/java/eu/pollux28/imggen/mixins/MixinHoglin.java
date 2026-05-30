package eu.pollux28.imggen.mixins;

import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Hoglin.class)
public class MixinHoglin {
    @Inject(method = "isConverting", at = @At("RETURN"), cancellable = true)
    private void imggen$protectHoglinInNetherBiome(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }

        Hoglin hoglin = (Hoglin) (Object) this;
        if (hoglin.level().getBiome(hoglin.blockPosition()).is(BiomeTags.IS_NETHER)) {
            cir.setReturnValue(false);
        }
    }
}
