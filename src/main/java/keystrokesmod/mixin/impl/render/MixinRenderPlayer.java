package keystrokesmod.mixin.impl.render;

import keystrokesmod.utility.BlockAnimationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("HEAD"))
    private void onDoRenderHead(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        BlockAnimationUtils.beginRender(entity);
    }

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("RETURN"))
    private void onDoRenderReturn(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        BlockAnimationUtils.endRender(entity);
    }

    @Redirect(method = "setModelVisibilities(Lnet/minecraft/client/entity/AbstractClientPlayer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack redirectGetCurrentItem(InventoryPlayer inventory) {
        return Minecraft.func_71410_x().field_71474_y.field_74320_O == 0 && inventory.field_70458_d == Minecraft.func_71410_x().field_71439_g ? Utils.getSpoofedItem(inventory.func_70448_g()) : inventory.func_70448_g();
    }

    @Redirect(method = "setModelVisibilities(Lnet/minecraft/client/entity/AbstractClientPlayer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;getItemInUseCount()I"))
    private int redirectGetItemInUseCount(AbstractClientPlayer clientPlayer) {
        int actualCount = clientPlayer.func_71052_bv();
        if (actualCount > 0) {
            return actualCount;
        } else {
            ItemStack itemStack = Minecraft.func_71410_x().field_71474_y.field_74320_O == 0 ? Utils.getSpoofedItem(clientPlayer.field_71071_by.func_70448_g()) : clientPlayer.field_71071_by.func_70448_g();
            return BlockAnimationUtils.shouldForceBlockAnimation(clientPlayer, itemStack) ? 1 : actualCount;
        }
    }
}
