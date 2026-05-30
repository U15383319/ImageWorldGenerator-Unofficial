package eu.pollux28.imggen.mixins;

import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractPiglin.class)
public class MixinAbstractPiglin {
    @Inject(method = "isConverting", at = @At("RETURN"), cancellable = true)
    private void imggen$protectPiglinInNetherBiome(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }

        AbstractPiglin piglin = (AbstractPiglin) (Object) this;
        if (piglin.level().getBiome(piglin.blockPosition()).is(BiomeTags.IS_NETHER)) {
            cir.setReturnValue(false);
        }
    }
}
