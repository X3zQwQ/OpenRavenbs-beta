package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.impl.render.ChestESP;
import net.minecraft.client.renderer.tileentity.TileEntityChestRenderer;
import net.minecraft.tileentity.TileEntityChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityChestRenderer.class)
public class MixinTileEntityChestRenderer {

    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityChest;DDDFI)V", at = @At("HEAD"))
    private void raven$chestChamsPre(TileEntityChest te, double x, double y, double z, float partialTicks, int destroyStage, CallbackInfo ci) {
        ChestESP.onRenderChestPre(te);
    }

    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityChest;DDDFI)V", at = @At("RETURN"))
    private void raven$chestChamsPost(TileEntityChest te, double x, double y, double z, float partialTicks, int destroyStage, CallbackInfo ci) {
        ChestESP.onRenderChestPost();
    }
}
