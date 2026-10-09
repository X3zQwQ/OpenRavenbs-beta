package keystrokesmod.mixin.impl.client;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.module.impl.player.Freecam;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(MovementInputFromOptions.class)
public class MixinMovementInputFromOptions extends MovementInput {

    @Shadow
    @Final
    private GameSettings field_78903_e;

    @Overwrite
    public void func_78898_a() {
        this.field_78902_a = 0.0F;
        this.field_78900_b = 0.0F;
        if (this.field_78903_e.field_74351_w.func_151470_d()) {
            this.field_78900_b++;
        }
        if (this.field_78903_e.field_74368_y.func_151470_d()) {
            this.field_78900_b--;
        }
        if (this.field_78903_e.field_74370_x.func_151470_d()) {
            this.field_78902_a++;
        }
        if (this.field_78903_e.field_74366_z.func_151470_d()) {
            this.field_78902_a--;
        }
        this.field_78901_c = this.field_78903_e.field_74314_A.func_151470_d();
        this.field_78899_d = this.field_78903_e.field_74311_E.func_151470_d();
        PrePlayerInputEvent moveInputEvent = new PrePlayerInputEvent(this.field_78900_b, this.field_78902_a, this.field_78901_c, this.field_78899_d, 0.3);
        MinecraftForge.EVENT_BUS.post(moveInputEvent);
        double sneakMultiplier = moveInputEvent.getSneakSlowDownMultiplier();
        this.field_78900_b = moveInputEvent.getForward();
        this.field_78902_a = moveInputEvent.getStrafe();
        this.field_78901_c = moveInputEvent.isJump();
        this.field_78899_d = moveInputEvent.isSneak();
        if (this.field_78899_d) {
            this.field_78902_a = (float) (this.field_78902_a * sneakMultiplier);
            this.field_78900_b = (float) (this.field_78900_b * sneakMultiplier);
        }
    }

    @Inject(method = "updatePlayerMoveState", at = @At("RETURN"))
    private void onUpdatePlayerMoveState(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PostPlayerInputEvent());
        if (Freecam.freeEntity != null) {
            this.field_78900_b = 0.0F;
            this.field_78902_a = 0.0F;
            this.field_78901_c = false;
            this.field_78899_d = false;
        }
    }
}
