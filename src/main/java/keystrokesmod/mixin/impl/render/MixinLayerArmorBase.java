package keystrokesmod.mixin.impl.render;

import keystrokesmod.utility.GhostHandRenderState;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LayerArmorBase.class)
public abstract class MixinLayerArmorBase {

    @Shadow
    private boolean field_177193_i;

    @Redirect(method = "renderLayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/layers/LayerArmorBase;skipRenderGlint:Z"))
    private boolean ghostHand$skipGlint(LayerArmorBase<?> layer) {
        return this.field_177193_i || GhostHandRenderState.isActive();
    }
}
