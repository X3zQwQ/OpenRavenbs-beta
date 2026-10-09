package keystrokesmod.mixin.impl.render;

import keystrokesmod.event.PostMouseSelectionEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.mixin.interfaces.ISaturationRenderer;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.AimAssist;
import keystrokesmod.module.impl.movement.Timer;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.impl.render.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer implements ISaturationRenderer {

    @Shadow
    private ShaderGroup field_147707_d;

    @Unique
    private ShaderGroup raven$saturationShader;

    @Inject(method = "isShaderActive", at = @At("HEAD"), cancellable = true)
    private void onIsShaderActive(CallbackInfoReturnable<Boolean> cir) {
        if (this.raven$saturationShader != null && OpenGlHelper.field_148824_g) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getShaderGroup", at = @At("HEAD"), cancellable = true)
    private void onGetShaderGroup(CallbackInfoReturnable<ShaderGroup> cir) {
        if (this.raven$saturationShader != null && OpenGlHelper.field_148824_g && this.field_147707_d == null) {
            cir.setReturnValue(this.raven$saturationShader);
        }
    }

    @Inject(method = "updateShaderGroupSize", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;createBindEntityOutlineFbs(II)V"))
    private void onUpdateShaderGroupSize(int width, int height, CallbackInfo ci) {
        if (this.raven$saturationShader != null) {
            this.raven$saturationShader.func_148026_a(width, height);
        }
    }

    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntityOutlineFramebuffer()V", shift = At.Shift.AFTER))
    private void onRenderSaturation(float partialTicks, long nanoTime, CallbackInfo ci) {
        if (this.raven$saturationShader != null) {
            GlStateManager.func_179128_n(5890);
            GlStateManager.func_179094_E();
            GlStateManager.func_179096_D();
            this.raven$saturationShader.func_148018_a(partialTicks);
            GlStateManager.func_179121_F();
        }
    }

    @ModifyArg(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorld(FJ)V"), index = 0)
    private float raven$timerWorldPartialTicks(float partialTicks) {
        return Timer.getWorldRenderPartialTicks(partialTicks);
    }

    @Override
    public ShaderGroup raven$getSaturationShader() {
        return this.raven$saturationShader;
    }

    @Override
    public void raven$setSaturationShader(ShaderGroup shader) {
        this.raven$saturationShader = shader;
    }

    @Redirect(method = "hurtCameraEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V"))
    public void injectNoHurtCam(float angle, float x, float y, float z) {
        if (ModuleManager.noHurtCam != null && ModuleManager.noHurtCam.isEnabled()) {
            angle = (float) (angle / 14.0F * ModuleManager.noHurtCam.multiplier.getInput());
        }
        GlStateManager.func_179114_b(angle, x, y, z);
    }

    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"))
    public double injectNoCameraClip(Vec3 raytrace, Vec3 original) {
        if (ModuleManager.noCameraClip != null && ModuleManager.noCameraClip.isEnabled()) {
            return ModuleManager.extendCamera != null && ModuleManager.extendCamera.isEnabled() ? ModuleManager.extendCamera.distance.getInput() : 4.0;
        } else {
            return raytrace.func_72438_d(original);
        }
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F"))
    private float freelookRotationYaw(Entity entity) {
        if (ModuleManager.freelook != null && ModuleManager.freelook.isEnabled() && Freelook.perspectiveToggled) {
            return Freelook.cameraYaw;
        } else {
            AimAssist aimAssist = ModuleManager.aimAssist;
            return aimAssist != null && aimAssist.isDetachedCameraActive() ? aimAssist.getCameraYaw() : entity.field_70177_z;
        }
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationYaw:F"))
    private float freelookPrevRotationYaw(Entity entity) {
        if (ModuleManager.freelook != null && ModuleManager.freelook.isEnabled() && Freelook.perspectiveToggled) {
            return Freelook.cameraYaw;
        } else {
            AimAssist aimAssist = ModuleManager.aimAssist;
            return aimAssist != null && aimAssist.isDetachedCameraActive() ? aimAssist.getPrevCameraYaw() : entity.field_70126_B;
        }
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F"))
    private float freelookRotationPitch(Entity entity) {
        if (ModuleManager.freelook != null && ModuleManager.freelook.isEnabled() && Freelook.perspectiveToggled) {
            return Freelook.cameraPitch;
        } else {
            AimAssist aimAssist = ModuleManager.aimAssist;
            return aimAssist != null && aimAssist.isDetachedCameraActive() ? aimAssist.getCameraPitch() : entity.field_70125_A;
        }
    }

    @Redirect(method = "orientCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationPitch:F"))
    private float freelookPrevRotationPitch(Entity entity) {
        if (ModuleManager.freelook != null && ModuleManager.freelook.isEnabled() && Freelook.perspectiveToggled) {
            return Freelook.cameraPitch;
        } else {
            AimAssist aimAssist = ModuleManager.aimAssist;
            return aimAssist != null && aimAssist.isDetachedCameraActive() ? aimAssist.getPrevCameraPitch() : entity.field_70127_C;
        }
    }

    @Redirect(method = "updateCameraAndRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;inGameHasFocus:Z"))
    private boolean freelookOverrideMouse(Minecraft mc) {
        return Freelook.overrideMouse(mc);
    }

    @Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setAngles(FF)V"))
    private void freecamMouseInput(EntityPlayerSP player, float yaw, float pitch) {
        if (Freecam.freeEntity != null) {
            Freecam.freeEntity.func_70082_c(yaw, pitch);
            Freecam.freeEntity.field_70759_as = Freecam.freeEntity.field_70177_z;
            Freecam.freeEntity.field_70758_at = Freecam.freeEntity.field_70177_z;
        } else {
            AimAssist aimAssist = ModuleManager.aimAssist;
            if (aimAssist == null || !aimAssist.handleMouseInput(player, yaw, pitch)) {
                player.func_70082_c(yaw, pitch);
            }
        }
    }

    @Redirect(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectSetupFog(EntityLivingBase entity, Potion potion) {
        return entity == Minecraft.func_71410_x().field_71439_g && ModuleManager.antiDebuff != null && ModuleManager.antiDebuff.canRemoveBlindness(potion) ? false : entity.func_70644_a(potion);
    }

    @Redirect(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectFogColor(EntityLivingBase entity, Potion potion) {
        return entity == Minecraft.func_71410_x().field_71439_g && ModuleManager.antiDebuff != null && ModuleManager.antiDebuff.canRemoveBlindness(potion) ? false : entity.func_70644_a(potion);
    }

    @Redirect(method = "setupCameraTransform", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean redirectSetupCameraTransform(EntityPlayerSP entity, Potion potion) {
        return entity == Minecraft.func_71410_x().field_71439_g && ModuleManager.antiDebuff != null && ModuleManager.antiDebuff.canRemoveNausea(potion) ? false : entity.func_70644_a(potion);
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;getMouseOver(F)V", shift = At.Shift.AFTER))
    private void onRenderWorld(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PostMouseSelectionEvent());
    }

    @Inject(method = "getMouseOver", at = @At("HEAD"))
    private void onGetMouseOverHead(float partialTicks, CallbackInfo ci) {
        RotationHelper rh = RotationHelper.get();
        if (!rh.swappedForMouseOver) {
            Minecraft mc = Minecraft.func_71410_x();
            Entity view = mc.func_175606_aa();
            if (view != null && rh.isActive()) {
                Float yaw = rh.getServerYaw();
                Float pitch = rh.getServerPitch();
                if (yaw != null && !yaw.isNaN() && pitch != null && !pitch.isNaN()) {
                    rh.beginSwap(view, yaw, pitch, true);
                    rh.swappedForMouseOver = true;
                }
            }
        }
    }

    @Inject(method = "getMouseOver", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endSection()V", shift = At.Shift.BEFORE))
    private void onGetMouseOverBeforeEndSection(float partialTicks, CallbackInfo ci) {
        if (ModuleManager.bedAura != null && ModuleManager.bedAura.shouldOverrideMouseOver()) {
            ModuleManager.bedAura.modifyMouseOverFromGetMouseOver(partialTicks);
        } else if (ModuleManager.killAura != null && ModuleManager.killAura.shouldOverrideMouseOver()) {
            ModuleManager.killAura.modifyMouseOverFromGetMouseOver(partialTicks);
        } else if (ModuleManager.ghostHand != null && ModuleManager.ghostHand.shouldOverrideMouseOver()) {
            ModuleManager.ghostHand.modifyMouseOverFromGetMouseOver(partialTicks);
        } else {
            if (ModuleManager.piercing != null && ModuleManager.piercing.shouldOverrideMouseOver()) {
                ModuleManager.piercing.modifyMouseOverFromGetMouseOver(partialTicks);
            }
        }
    }

    @Inject(method = "getMouseOver", at = @At("RETURN"))
    private void onGetMouseOverReturn(float partialTicks, CallbackInfo ci) {
        RotationHelper rh = RotationHelper.get();
        if (rh.swappedForMouseOver) {
            Entity view = Minecraft.func_71410_x().func_175606_aa();
            if (view != null) {
                rh.endSwap(view);
            }
            rh.swappedForMouseOver = false;
        }
    }
}
