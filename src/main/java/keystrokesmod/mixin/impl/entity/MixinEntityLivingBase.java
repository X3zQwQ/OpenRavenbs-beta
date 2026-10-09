package keystrokesmod.mixin.impl.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PrePlayerMovementInputEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.utility.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {

    private final Map<Integer, PotionEffect> activePotionsMap = Maps.newHashMap();

    @Shadow
    public float field_70759_as;

    @Shadow
    public float field_70761_aq;

    @Shadow
    public float field_70733_aJ;

    public MixinEntityLivingBase(World worldIn) {
        super(worldIn);
    }

    @Shadow
    public PotionEffect func_70660_b(Potion potionIn) {
        return this.activePotionsMap.get(potionIn.field_76415_H);
    }

    @Shadow
    public boolean func_70644_a(Potion potionIn) {
        return this.activePotionsMap.containsKey(potionIn.field_76415_H);
    }

    @Inject(method = { "updateDistance", "func_110146_f" }, at = @At("HEAD"), cancellable = true)
    protected void injectUpdateDistance(float p_110146_1_, float p_110146_2_, CallbackInfoReturnable<Float> cir) {
        float rotationYaw = this.field_70177_z;
        if (Settings.fullBody != null && Settings.rotateBody != null && !Settings.fullBody.isToggled() && Settings.rotateBody.isToggled() && (EntityLivingBase) (Object) this instanceof EntityPlayerSP && PreMotionEvent.setRenderYaw()) {
            if (this.field_70733_aJ > 0.0F) {
                p_110146_1_ = RotationUtils.renderYaw;
            }
            rotationYaw = RotationUtils.renderYaw;
            this.field_70759_as = RotationUtils.renderYaw;
        }
        float f = MathHelper.func_76142_g(p_110146_1_ - this.field_70761_aq);
        this.field_70761_aq += f * 0.3F;
        float f1 = MathHelper.func_76142_g(rotationYaw - this.field_70761_aq);
        boolean flag = f1 < 90.0F || f1 >= 90.0F;
        if (f1 < -75.0F) {
            f1 = -75.0F;
        }
        if (f1 >= 75.0F) {
            f1 = 75.0F;
        }
        this.field_70761_aq = rotationYaw - f1;
        if (f1 * f1 > 2500.0F) {
            this.field_70761_aq += f1 * 0.2F;
        }
        if (flag) {
            p_110146_2_ *= -1.0F;
        }
        cir.setReturnValue(p_110146_2_);
    }

    @Shadow
    protected float func_175134_bD() {
        return 0.42F;
    }

    @Overwrite
    protected void func_70664_aZ() {
        JumpEvent jumpEvent = new JumpEvent((EntityLivingBase) (Object) this, this.func_175134_bD(), this.field_70177_z, this.func_70051_ag());
        MinecraftForge.EVENT_BUS.post(jumpEvent);
        if (!jumpEvent.isCanceled()) {
            this.field_70181_x = jumpEvent.getMotionY();
            if (this.func_70644_a(Potion.field_76430_j)) {
                this.field_70181_x = this.field_70181_x + (this.func_70660_b(Potion.field_76430_j).func_76458_c() + 1) * 0.1F;
            }
            if (jumpEvent.applySprint()) {
                float f = jumpEvent.getYaw() * (float) (Math.PI / 180.0);
                this.field_70159_w = this.field_70159_w - MathHelper.func_76126_a(f) * 0.2F;
                this.field_70179_y = this.field_70179_y + MathHelper.func_76134_b(f) * 0.2F;
            }
            this.field_70160_al = true;
            ForgeHooks.onLivingJump((EntityLivingBase) (Object) this);
        }
    }

    @Inject(method = "isPotionActive(Lnet/minecraft/potion/Potion;)Z", at = @At("HEAD"), cancellable = true)
    private void isPotionActive(Potion p_isPotionActive_1_, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if ((Object) this == Minecraft.func_71410_x().field_71439_g) {
            if (ModuleManager.antiDebuff != null && ModuleManager.antiDebuff.isEnabled() && (p_isPotionActive_1_ == Potion.field_76431_k && ModuleManager.antiDebuff.removeNausea.isToggled() || p_isPotionActive_1_ == Potion.field_76440_q && ModuleManager.antiDebuff.removeBlindness.isToggled()) && ModuleManager.antiDebuff.removeSideEffects.isToggled()) {
                callbackInfoReturnable.setReturnValue(false);
            }
        }
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;moveEntityWithHeading(FF)V"))
    private void onMoveEntityWithHeadingRedirect(EntityLivingBase self, float originalStrafing, float originalForward) {
        if (self instanceof EntityPlayerSP) {
            PrePlayerMovementInputEvent event = new PrePlayerMovementInputEvent(originalForward, originalStrafing);
            MinecraftForge.EVENT_BUS.post(event);
            self.func_70612_e(event.strafe, event.forward);
        } else {
            self.func_70612_e(originalStrafing, originalForward);
        }
    }
}
