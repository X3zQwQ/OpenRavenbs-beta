package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.impl.render.ChestESP;
import net.minecraft.client.renderer.tileentity.TileEntityEnderChestRenderer;
import net.minecraft.tileentity.TileEntityEnderChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityEnderChestRenderer.class)
public class MixinTileEntityEnderChestRenderer {

    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityEnderChest;DDDFI)V", at = @At("HEAD"))
    private void raven$enderChestChamsPre(TileEntityEnderChest te, double x, double y, double z, float partialTicks, int destroyStage, CallbackInfo ci) {
        ChestESP.onRenderChestPre(te);
    }

    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityEnderChest;DDDFI)V", at = @At("RETURN"))
    private void raven$enderChestChamsPost(TileEntityEnderChest te, double x, double y, double z, float partialTicks, int destroyStage, CallbackInfo ci) {
        ChestESP.onRenderChestPost();
    }
}
