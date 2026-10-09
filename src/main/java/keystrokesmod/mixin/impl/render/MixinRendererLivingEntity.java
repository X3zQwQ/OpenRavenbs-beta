package keystrokesmod.mixin.impl.render;

import java.awt.Color;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.other.NameHider;
import keystrokesmod.module.impl.render.DamageTint;
import keystrokesmod.module.impl.render.MobESP;
import keystrokesmod.module.impl.render.PlayerESP;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.utility.GhostHandRenderState;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> extends Render<T> {

    @Shadow
    protected boolean field_177098_i;

    @Unique
    private boolean ghostHand$renderingTransparent;

    @Unique
    private EntityLivingBase nameHider$renderNameEntity;

    @Unique
    private EntityLivingBase damageTint$entity;

    @Shadow
    protected abstract void func_177091_f();

    protected MixinRendererLivingEntity(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;setDoRenderBrightness(Lnet/minecraft/entity/EntityLivingBase;F)Z"))
    private void ghostHand$beginTransparency(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (!this.field_177098_i && !PlayerESP.renderingOutlinePass && !MobESP.renderingOutlinePass && ModuleManager.ghostHand != null) {
            this.ghostHand$renderingTransparent = GhostHandRenderState.begin(ModuleManager.ghostHand.getRenderOpacity(entity, partialTicks));
        }
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = { @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableCull()V"), @At("RETURN") })
    private void ghostHand$endTransparency(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (this.ghostHand$renderingTransparent) {
            GhostHandRenderState.end();
            this.ghostHand$renderingTransparent = false;
        }
    }

    @Unique
    private boolean shouldRender() {
        return ModuleManager.playerESP != null && ModuleManager.playerESP.isEnabled() && ModuleManager.playerESP.outline.isToggled();
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;setScoreTeamColor(Lnet/minecraft/entity/EntityLivingBase;)Z"))
    private boolean setOutlineColor(RendererLivingEntity instance, T entityLivingBaseIn) {
        int i = 16777215;
        boolean drawOutline = this.shouldRender() && (entityLivingBaseIn != Minecraft.func_71410_x().field_71439_g && !AntiBot.isBot(entityLivingBaseIn) || entityLivingBaseIn == Minecraft.func_71410_x().field_71439_g && ModuleManager.playerESP.renderSelf.isToggled());
        if (drawOutline && !ModuleManager.playerESP.teamColor.isToggled()) {
            if (ModuleManager.playerESP.rainbow.isToggled()) {
                i = Utils.getChroma(2L, 0L);
            } else {
                i = ModuleManager.playerESP.color.getColor();
            }
        } else if (entityLivingBaseIn instanceof EntityPlayer) {
            ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam) entityLivingBaseIn.func_96124_cp();
            if (scoreplayerteam != null) {
                String s = FontRenderer.func_78282_e(scoreplayerteam.func_96668_e());
                if (s.length() >= 2) {
                    i = this.func_76983_a().func_175064_b(s.charAt(1));
                }
            }
        }
        if (drawOutline && ModuleManager.playerESP.redOnDamage.isToggled() && entityLivingBaseIn.field_70737_aN != 0) {
            i = Color.RED.getRGB();
        }
        if (drawOutline) {
            return false;
        } else {
            float f1 = (i >> 16 & 0xFF) / 255.0F;
            float f2 = (i >> 8 & 0xFF) / 255.0F;
            float f = (i & 0xFF) / 255.0F;
            GlStateManager.func_179140_f();
            GlStateManager.func_179138_g(OpenGlHelper.field_77478_a);
            GlStateManager.func_179131_c(f1, f2, f, 1.0F);
            GlStateManager.func_179090_x();
            GlStateManager.func_179138_g(OpenGlHelper.field_77476_b);
            GlStateManager.func_179090_x();
            GlStateManager.func_179138_g(OpenGlHelper.field_77478_a);
            return true;
        }
    }

    @ModifyVariable(method = "renderModel", at = @At("STORE"), ordinal = 0)
    private boolean modifyInvisibleFlag(boolean flag) {
        return flag || this.field_177098_i && this.shouldRender() && ModuleManager.playerESP.showInvis.isToggled();
    }

    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private void suppressNameDuringOutlinePass(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (PlayerESP.renderingOutlinePass || MobESP.renderingOutlinePass) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("HEAD"))
    private void mobEsp$chamsPre(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof EntityPlayer)) {
            MobESP.onRenderMobPre(entity);
        }
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("RETURN"))
    private void mobEsp$chamsPost(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof EntityPlayer)) {
            MobESP.onRenderMobPost();
        }
    }

    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At("HEAD"))
    private void nameHider$captureRenderEntity(T entity, double x, double y, double z, CallbackInfo ci) {
        this.nameHider$renderNameEntity = entity;
    }

    @ModifyVariable(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At("STORE"), ordinal = 0)
    private String nameHider$hideRenderedName(String displayName) {
        if (displayName == null || ModuleManager.nameHider == null || !ModuleManager.nameHider.isEnabled()) {
            return displayName;
        } else if (this.nameHider$renderNameEntity instanceof EntityPlayer) {
            EntityPlayer entityPlayer = (EntityPlayer) (Object) this.nameHider$renderNameEntity;
            return NameHider.getPlayerDisplayName(entityPlayer, entityPlayer.func_145748_c_()).func_150254_d();
        } else {
            return NameHider.getFakeName(displayName);
        }
    }

    @Inject(method = "renderName(Lnet/minecraft/entity/EntityLivingBase;DDD)V", at = @At("RETURN"))
    private void nameHider$clearRenderEntity(T entity, double x, double y, double z, CallbackInfo ci) {
        this.nameHider$renderNameEntity = null;
    }

    @Inject(method = "setBrightness", at = @At("HEAD"))
    private void damageTint$captureEntity(T entitylivingbaseIn, float partialTicks, boolean combineTextures, CallbackInfoReturnable<Boolean> cir) {
        this.damageTint$entity = entitylivingbaseIn;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 0))
    private float damageTint$modifyRed(float f) {
        return DamageTint.instance != null ? DamageTint.instance.color.getRed() / 255.0F : f;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 1))
    private float damageTint$modifyGreen(float f) {
        return DamageTint.instance != null ? DamageTint.instance.color.getGreen() / 255.0F : f;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 2))
    private float damageTint$modifyBlue(float f) {
        return DamageTint.instance != null ? DamageTint.instance.color.getBlue() / 255.0F : f;
    }

    @ModifyArg(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 3))
    private float damageTint$modifyAlpha(float f) {
        return DamageTint.instance != null ? DamageTint.computeAlpha(this.damageTint$entity) : f;
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("RETURN"))
    private void damageTint$restoreRenderState(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (DamageTint.instance != null) {
            this.func_177091_f();
            GlStateManager.func_179138_g(OpenGlHelper.field_77476_b);
            GlStateManager.func_179098_w();
            GlStateManager.func_179138_g(OpenGlHelper.field_77478_a);
            GlStateManager.func_179098_w();
            GlStateManager.func_179141_d();
            GlStateManager.func_179092_a(516, 0.1F);
            GlStateManager.func_179084_k();
            GlStateManager.func_179145_e();
            GlStateManager.func_179126_j();
            GlStateManager.func_179132_a(true);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            this.damageTint$entity = null;
        }
    }
}
