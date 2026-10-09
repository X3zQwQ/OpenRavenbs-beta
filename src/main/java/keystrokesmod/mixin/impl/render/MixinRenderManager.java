package keystrokesmod.mixin.impl.render;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.AimAssist;
import keystrokesmod.module.impl.render.Freelook;
import keystrokesmod.utility.RotationUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(RenderManager.class)
public class MixinRenderManager {

    @Shadow
    private float field_78732_j;

    @Shadow
    private float field_78735_i;

    @Unique
    private float cachedPrevRotationPitch;

    @Unique
    private float cachedRotationPitch;

    @Inject(method = "renderEntityStatic", at = @At("HEAD"))
    public void renderEntityStaticPre(Entity entity, float n, boolean b, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (entity instanceof EntityPlayerSP && PreMotionEvent.setRenderYaw()) {
            EntityPlayerSP player = (EntityPlayerSP) entity;
            this.cachedRotationPitch = player.field_70125_A;
            this.cachedPrevRotationPitch = player.field_70127_C;
            player.field_70127_C = RotationUtils.prevRenderPitch;
            player.field_70125_A = RotationUtils.renderPitch;
        }
    }

    @Inject(method = "renderEntityStatic", at = @At("RETURN"))
    public void renderEntityStaticPost(Entity entity, float n, boolean b, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (entity instanceof EntityPlayerSP && PreMotionEvent.setRenderYaw()) {
            EntityPlayerSP player = (EntityPlayerSP) entity;
            player.field_70127_C = this.cachedPrevRotationPitch;
            player.field_70125_A = this.cachedRotationPitch;
        }
    }

    @Redirect(method = "cacheActiveRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/RenderManager;playerViewX:F", opcode = 181))
    private void freelookRedirectPlayerViewX(RenderManager rm, float value) {
        if (ModuleManager.freelook != null && ModuleManager.freelook.isEnabled() && Freelook.perspectiveToggled) {
            this.field_78732_j = Freelook.cameraPitch;
        } else {
            AimAssist aimAssist = ModuleManager.aimAssist;
            this.field_78732_j = aimAssist != null && aimAssist.isDetachedCameraActive() ? aimAssist.getCameraPitch() : value;
        }
    }

    @Redirect(method = "cacheActiveRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/RenderManager;playerViewY:F", opcode = 181))
    private void freelookRedirectPlayerViewY(RenderManager rm, float value) {
        if (ModuleManager.freelook != null && ModuleManager.freelook.isEnabled() && Freelook.perspectiveToggled) {
            this.field_78735_i = Freelook.cameraYaw;
        } else {
            AimAssist aimAssist = ModuleManager.aimAssist;
            this.field_78735_i = aimAssist != null && aimAssist.isDetachedCameraActive() ? aimAssist.getCameraYaw() : value;
        }
    }
}
