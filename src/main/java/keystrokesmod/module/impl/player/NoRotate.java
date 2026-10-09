package keystrokesmod.module.impl.player;

import keystrokesmod.event.PlayerTeleportEvent;
import keystrokesmod.module.Module;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoRotate extends Module {

    private float prevPitch = 0.0F;

    private float prevYaw = 0.0F;

    public NoRotate() {
        super("NoRotate", Module.category.player);
    }

    @SubscribeEvent
    public void onPrePlayerTeleport(PlayerTeleportEvent event) {
        if (this.isEnabled()) {
            if (mc.field_71439_g != null && mc.field_71439_g.field_70125_A != 0.0F) {
                this.prevPitch = mc.field_71439_g.field_70125_A % 360.0F;
                this.prevYaw = mc.field_71439_g.field_70177_z % 360.0F;
            }
        }
    }

    public void handlePlayerPosLook(S08PacketPlayerPosLook packet) {
        if (this.isEnabled()) {
            if (packet.func_148930_g() != 0.0F && mc.field_71439_g != null) {
                mc.field_71439_g.field_70126_B = this.prevYaw;
                mc.field_71439_g.field_70127_C = this.prevPitch;
                mc.field_71439_g.field_70125_A = this.prevPitch;
                mc.field_71439_g.field_70177_z = this.prevYaw;
            }
        }
    }
}
