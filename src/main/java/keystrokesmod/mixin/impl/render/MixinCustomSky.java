package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.ModuleManager;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.optifine.CustomSky", remap = false)
public abstract class MixinCustomSky {

    @ModifyVariable(method = "renderSky", at = @At("STORE"))
    @Dynamic
    private static long changeWorldTime(long time) {
        return ModuleManager.weather != null && ModuleManager.weather.isEnabled() ? (long) MathHelper.func_151237_a(ModuleManager.weather.time.getInput() * 1000.0, 0.0, 23999.0) : time;
    }
}
