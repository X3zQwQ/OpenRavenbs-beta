package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class InstantStop extends Module {

    private final ButtonSetting onlyOnGround;

    private final ButtonSetting disableWhenFlying;

    private float lastForward;

    private float lastStrafe;

    public InstantStop() {
        super("Instant Stop", Module.category.movement, 0);
        this.registerSetting(this.onlyOnGround = new ButtonSetting("Only on ground", true));
        this.registerSetting(this.disableWhenFlying = new ButtonSetting("Disable when flying", true));
    }

    @Override
    public void onDisable() {
        this.lastForward = 0.0F;
        this.lastStrafe = 0.0F;
    }

    @SubscribeEvent
    public void onPrePlayerInput(PrePlayerInputEvent e) {
        if (this.isEnabled() && Utils.nullCheck()) {
            if (mc.field_71462_r == null) {
                if (this.onlyOnGround.isToggled() && !mc.field_71439_g.field_70122_E) {
                    this.lastForward = e.getForward();
                    this.lastStrafe = e.getStrafe();
                } else if (this.disableWhenFlying.isToggled() && mc.field_71439_g.field_71075_bZ.field_75100_b) {
                    this.lastForward = e.getForward();
                    this.lastStrafe = e.getStrafe();
                } else {
                    float rawF = e.getForward();
                    float rawS = e.getStrafe();
                    if (rawF == 0.0F && rawS == 0.0F && (this.lastForward != 0.0F || this.lastStrafe != 0.0F)) {
                        e.setForward(Math.signum(-this.lastForward));
                        e.setStrafe(Math.signum(-this.lastStrafe));
                    }
                    this.lastForward = rawF;
                    this.lastStrafe = rawS;
                }
            }
        }
    }
}
