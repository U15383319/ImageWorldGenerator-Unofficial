package eu.pollux28.imggen.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pollux28.imggen.ImgGen;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Piglins and hoglins stay themselves inside Nether biomes painted into an image world. */
@Mixin({AbstractPiglin.class, Hoglin.class})
public abstract class ZombificationMixin {
    @ModifyReturnValue(method = "isConverting", at = @At("RETURN"))
    private boolean imggen$safeInPaintedNether(boolean converting) {
        if (!converting) {
            return false;
        }
        Entity self = (Entity) (Object) this;
        return ImgGen.imageGenerator(self.level()) == null
                || !self.level().getBiome(self.blockPosition()).is(BiomeTags.IS_NETHER);
    }
}
