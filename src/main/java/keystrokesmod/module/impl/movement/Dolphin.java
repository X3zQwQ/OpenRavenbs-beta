package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Dolphin extends Module {

    public SliderSetting horSpeed;

    public SliderSetting verSpeed;

    public ButtonSetting buoyant;

    public ButtonSetting disableUsing;

    public ButtonSetting disableVerticalWhileMoving;

    public ButtonSetting forwardOnly;

    public Dolphin() {
        super("Dolphin", Module.category.movement, 0);
        this.registerSetting(this.horSpeed = new SliderSetting("Horizontal speed", 1.0, 1.0, 8.0, 0.1));
        this.registerSetting(this.verSpeed = new SliderSetting("Vertical speed", 1.0, 1.0, 8.0, 0.1));
        this.registerSetting(this.buoyant = new ButtonSetting("Buoyant", false));
        this.registerSetting(this.disableUsing = new ButtonSetting("Disable while using", true));
        this.registerSetting(this.disableVerticalWhileMoving = new ButtonSetting("Disable vertical while moving", false));
        this.registerSetting(this.forwardOnly = new ButtonSetting("Forward only", true));
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent event) {
        if (mc.field_71439_g.func_70090_H() && !mc.field_71439_g.field_71075_bZ.field_75100_b) {
            if (!this.disableUsing.isToggled() || !mc.field_71439_g.func_71039_bw()) {
                if (!this.forwardOnly.isToggled() || Keyboard.isKeyDown(mc.field_71474_y.field_74351_w.func_151463_i())) {
                    if (this.buoyant.isToggled()) {
                        mc.field_71439_g.field_70181_x = 0.0;
                    }
                    if (Utils.isUserMoving()) {
                        double horizontalSpeed = 0.078 * this.horSpeed.getInput();
                        Utils.setSpeed(horizontalSpeed);
                        if (this.disableVerticalWhileMoving.isToggled()) {
                            return;
                        }
                    }
                    if (Keyboard.isKeyDown(mc.field_71474_y.field_74314_A.func_151463_i())) {
                        mc.field_71439_g.field_70181_x = 0.02 + 0.04 * this.verSpeed.getInput();
                    } else if (!mc.field_71439_g.field_70122_E && Keyboard.isKeyDown(mc.field_71474_y.field_74311_E.func_151463_i())) {
                        mc.field_71439_g.field_71158_b.field_78899_d = false;
                        mc.field_71439_g.field_70181_x = -0.1 - 0.03 * this.verSpeed.getInput();
                    } else if (this.buoyant.isToggled()) {
                        mc.field_71439_g.field_70181_x = 0.0;
                    }
                }
            }
        }
    }
}
