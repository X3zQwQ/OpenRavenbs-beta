package keystrokesmod.mixin.impl.entity;

import keystrokesmod.event.ClientLookEvent;
import keystrokesmod.event.PlayerMoveEvent;
import keystrokesmod.event.StepHeightEvent;
import keystrokesmod.event.StrafeEvent;
import keystrokesmod.module.impl.player.SafeWalk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public double field_70159_w;

    @Shadow
    public double field_70179_y;

    @Shadow
    public float field_70177_z;

    @ModifyVariable(method = "moveEntity", at = @At(value = "STORE", ordinal = 0), name = "flag")
    private boolean injectSafeWalk(boolean flag) {
        Entity entity = (Entity) (Object) this;
        Minecraft mc = Minecraft.func_71410_x();
        return entity != null && entity == mc.field_71439_g && entity.field_70122_E && SafeWalk.canSafeWalk() ? true : flag;
    }

    @Overwrite
    public void func_70060_a(float strafe, float forward, float friction) {
        StrafeEvent strafeEvent = new StrafeEvent(strafe, forward, friction, this.field_70177_z);
        if ((Object) this == Minecraft.func_71410_x().field_71439_g) {
            MinecraftForge.EVENT_BUS.post(strafeEvent);
        }
        strafe = strafeEvent.getStrafe();
        forward = strafeEvent.getForward();
        friction = strafeEvent.getFriction();
        float yaw = strafeEvent.getYaw();
        float f = strafe * strafe + forward * forward;
        if (f >= 1.0E-4F) {
            f = MathHelper.func_76129_c(f);
            if (f < 1.0F) {
                f = 1.0F;
            }
            f = friction / f;
            strafe *= f;
            forward *= f;
            float f1 = MathHelper.func_76126_a(yaw * (float) Math.PI / 180.0F);
            float f2 = MathHelper.func_76134_b(yaw * (float) Math.PI / 180.0F);
            this.field_70159_w += strafe * f2 - forward * f1;
            this.field_70179_y += forward * f2 + strafe * f1;
        }
    }

    @Redirect(method = "moveEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;stepHeight:F", opcode = 180, ordinal = 0))
    private float redirectStepHeight(Entity instance) {
        StepHeightEvent stepHeightEvent = new StepHeightEvent(instance, instance.field_70138_W);
        MinecraftForge.EVENT_BUS.post(stepHeightEvent);
        return stepHeightEvent.stepHeight;
    }

    @Overwrite
    protected final Vec3 func_174806_f(float pitch, float yaw) {
        ClientLookEvent event = new ClientLookEvent(yaw, pitch);
        MinecraftForge.EVENT_BUS.post(event);
        pitch = event.pitch;
        yaw = event.yaw;
        float f = MathHelper.func_76134_b(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f1 = MathHelper.func_76126_a(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float f2 = -MathHelper.func_76134_b(-pitch * (float) (Math.PI / 180.0));
        float f3 = MathHelper.func_76126_a(-pitch * (float) (Math.PI / 180.0));
        return new Vec3(f1 * f2, f3, f * f2);
    }

    @Inject(method = "moveEntity", at = @At("HEAD"))
    private void injectPlayerMoveEvent(double x, double y, double z, CallbackInfo ci) {
        if ((Object) this instanceof EntityPlayerSP) {
            MinecraftForge.EVENT_BUS.post(new PlayerMoveEvent(x, y, z));
        }
    }
}
