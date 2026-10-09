package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Shadow
    @Final
    private Minecraft field_72777_q;

    @Unique
    private boolean shouldRender() {
        return ModuleManager.playerESP != null && ModuleManager.playerESP.isEnabled() && ModuleManager.playerESP.outline.isToggled();
    }

    @Unique
    private boolean raven$isFreelookActive() {
        return ModuleManager.freelook != null && ModuleManager.freelook.isEnabled() && Freelook.perspectiveToggled;
    }

    @Unique
    private boolean raven$isDetachedAimCameraActive() {
        return ModuleManager.aimAssist != null && ModuleManager.aimAssist.isDetachedCameraActive();
    }

    @Redirect(method = "setupTerrain", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F"))
    private float raven$setupTerrainRotationPitch(Entity entity) {
        if (entity == this.field_72777_q.func_175606_aa() && this.raven$isFreelookActive()) {
            return Freelook.cameraPitch;
        } else {
            return entity == this.field_72777_q.func_175606_aa() && this.raven$isDetachedAimCameraActive() ? ModuleManager.aimAssist.getCameraPitch() : entity.field_70125_A;
        }
    }

    @Redirect(method = "setupTerrain", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F"))
    private float raven$setupTerrainRotationYaw(Entity entity) {
        if (entity == this.field_72777_q.func_175606_aa() && this.raven$isFreelookActive()) {
            return Freelook.cameraYaw;
        } else {
            return entity == this.field_72777_q.func_175606_aa() && this.raven$isDetachedAimCameraActive() ? ModuleManager.aimAssist.getCameraYaw() : entity.field_70177_z;
        }
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;getViewVector(Lnet/minecraft/entity/Entity;D)Lorg/lwjgl/util/vector/Vector3f;"))
    private Vector3f raven$setupTerrainViewVector(RenderGlobal renderGlobal, Entity entityIn, double partialTicks) {
        float pitch;
        float yaw;
        if (entityIn == this.field_72777_q.func_175606_aa() && this.raven$isFreelookActive()) {
            pitch = Freelook.cameraPitch;
            yaw = Freelook.cameraYaw;
        } else if (entityIn == this.field_72777_q.func_175606_aa() && this.raven$isDetachedAimCameraActive()) {
            pitch = ModuleManager.aimAssist.getCameraPitch();
            yaw = ModuleManager.aimAssist.getCameraYaw();
        } else {
            pitch = (float) (entityIn.field_70127_C + (entityIn.field_70125_A - entityIn.field_70127_C) * partialTicks);
            yaw = (float) (entityIn.field_70126_B + (entityIn.field_70177_z - entityIn.field_70126_B) * partialTicks);
        }
        if (this.field_72777_q.field_71474_y.field_74320_O == 2) {
            pitch += 180.0F;
        }
        float cosYaw = MathHelper.func_76134_b(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float sinYaw = MathHelper.func_76126_a(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
        float cosPitch = -MathHelper.func_76134_b(-pitch * (float) (Math.PI / 180.0));
        float sinPitch = MathHelper.func_76126_a(-pitch * (float) (Math.PI / 180.0));
        return new Vector3f(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }
}
