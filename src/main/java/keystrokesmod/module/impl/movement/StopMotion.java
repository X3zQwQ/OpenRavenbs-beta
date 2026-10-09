package keystrokesmod.module.impl.movement;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;

public class StopMotion extends Module {

    private ButtonSetting stopX;

    private ButtonSetting stopY;

    private ButtonSetting stopZ;

    public StopMotion() {
        super("Stop Motion", Module.category.movement, 0);
        this.registerSetting(this.stopX = new ButtonSetting("Stop X", true));
        this.registerSetting(this.stopY = new ButtonSetting("Stop Y", true));
        this.registerSetting(this.stopZ = new ButtonSetting("Stop Z", true));
    }

    @Override
    public void onEnable() {
        if (this.stopX.isToggled()) {
            mc.field_71439_g.field_70159_w = 0.0;
        }
        if (this.stopY.isToggled()) {
            mc.field_71439_g.field_70181_x = 0.0;
        }
        if (this.stopZ.isToggled()) {
            mc.field_71439_g.field_70179_y = 0.0;
        }
        this.disable();
    }
}
