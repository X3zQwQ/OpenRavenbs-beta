package keystrokesmod.mixin.impl.render;

import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer implements IMixinItemRenderer {

    private ItemStack originalItemToRender;

    @Shadow
    private ItemStack field_78453_b;

    public boolean cancelUpdate = false;

    public boolean cancelReset = false;

    private boolean renderItemInUse;

    @Shadow
    private float field_78454_c;

    @Shadow
    private float field_78451_d;

    @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"))
    private void modifyRenderItemPre(float p_renderItemInFirstPerson_1_, CallbackInfo info) {
        this.originalItemToRender = this.field_78453_b;
        this.field_78453_b = Utils.getSpoofedItem(this.originalItemToRender);
    }

    @Inject(method = "renderItemInFirstPerson", at = @At("RETURN"))
    private void modifyRenderItemPost(float p_renderItemInFirstPerson_1_, CallbackInfo info) {
        this.field_78453_b = this.originalItemToRender;
    }

    @Redirect(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;getItemInUseCount()I"))
    private int getItemInUseCountForRender(AbstractClientPlayer player) {
        int actualCount = player.func_71052_bv();
        if (actualCount <= 0 && this.renderItemInUse && this.field_78453_b != null) {
            return this.field_78453_b.func_77975_n() == EnumAction.BLOCK ? 1 : actualCount;
        } else {
            return actualCount;
        }
    }

    @Inject(method = "updateEquippedItem", at = @At("HEAD"), cancellable = true)
    private void onUpdateEquippedItem(CallbackInfo ci) {
        if (this.cancelUpdate) {
            this.cancelUpdate = false;
            this.field_78454_c = 1.0F;
            this.field_78451_d = 1.0F;
            ci.cancel();
        }
    }

    @Inject(method = "resetEquippedProgress", at = @At("HEAD"), cancellable = true)
    public void injectResetEquippedProgress(CallbackInfo ci) {
        if (this.cancelReset) {
            this.cancelReset = false;
            this.field_78454_c = 1.0F;
            this.field_78451_d = 1.0F;
            ci.cancel();
        }
    }

    @Inject(method = "resetEquippedProgress2", at = @At("HEAD"), cancellable = true)
    public void injectResetEquippedProgress2(CallbackInfo ci) {
        if (this.cancelReset) {
            this.cancelReset = false;
            this.field_78454_c = 1.0F;
            this.field_78451_d = 1.0F;
            ci.cancel();
        }
    }

    @Override
    public void setCancelUpdate(boolean cancel) {
        this.cancelUpdate = cancel;
    }

    @Override
    public void setCancelReset(boolean reset) {
        this.cancelReset = reset;
    }

    @Override
    public boolean isRenderItemInUse() {
        return this.renderItemInUse;
    }

    @Override
    public void setRenderItemInUse(boolean renderItemInUse) {
        this.renderItemInUse = renderItemInUse;
    }
}
