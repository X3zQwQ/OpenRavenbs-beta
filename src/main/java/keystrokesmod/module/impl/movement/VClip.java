package keystrokesmod.module.impl.movement;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;

public class VClip extends Module {

    private SliderSetting distance;

    private ButtonSetting sendMessage;

    public VClip() {
        super("VClip", Module.category.movement, 0);
        this.registerSetting(this.distance = new SliderSetting("Distance", 3.0, -20.0, 20.0, 0.5));
        this.registerSetting(this.sendMessage = new ButtonSetting("Send message", true));
    }

    @Override
    public void onEnable() {
        double distance = this.distance.getInput();
        if (this.distance.getInput() != 0.0) {
            mc.field_71439_g.func_70107_b(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + distance, mc.field_71439_g.field_70161_v);
            if (this.sendMessage.isToggled()) {
                Utils.sendMessage("&7Teleported you " + (distance > 0.0 ? "upwards" : "downwards") + " by &b" + distance + " &7blocks.");
            }
        }
        this.disable();
    }
}
