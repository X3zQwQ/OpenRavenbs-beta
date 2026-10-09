package keystrokesmod.mixin.impl.render;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.AimAssist;
import keystrokesmod.module.impl.render.Freelook;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin(ActiveRenderInfo.class)
public class MixinActiveRenderInfo {

    @Redirect(method = "updateRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;rotationYaw:F"))
    private static float redirectRotationYaw(EntityPlayer entity) {
        if (ModuleManager.freelook != null && ModuleManager.freelook.isEnabled() && Freelook.perspectiveToggled) {
            return Freelook.cameraYaw;
        } else {
            AimAssist aimAssist = ModuleManager.aimAssist;
            return aimAssist != null && aimAssist.isDetachedCameraActive() ? aimAssist.getCameraYaw() : entity.field_70177_z;
        }
    }

    @Redirect(method = "updateRenderInfo", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;rotationPitch:F"))
    private static float redirectRotationPitch(EntityPlayer entity) {
        if (ModuleManager.freelook != null && ModuleManager.freelook.isEnabled() && Freelook.perspectiveToggled) {
            return Freelook.cameraPitch;
        } else {
            AimAssist aimAssist = ModuleManager.aimAssist;
            return aimAssist != null && aimAssist.isDetachedCameraActive() ? aimAssist.getCameraPitch() : entity.field_70125_A;
        }
    }
}
