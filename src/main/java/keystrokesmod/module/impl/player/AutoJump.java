package keystrokesmod.module.impl.player;

import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.SimulatedPlayer;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoJump extends Module {

    public AutoJump() {
        super("Auto Jump", Module.category.player);
    }

    @SubscribeEvent
    public void onPrePlayerInput(PrePlayerInputEvent e) {
        if (Utils.nullCheck() && mc.field_71462_r == null && !mc.field_71439_g.field_71075_bZ.field_75100_b) {
            if (mc.field_71439_g.field_70122_E) {
                SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(mc.field_71439_g.field_71158_b);
                sim.tick();
                if (!sim.onGround && !e.isJump()) {
                    e.setJump(true);
                }
            }
        }
    }
}
