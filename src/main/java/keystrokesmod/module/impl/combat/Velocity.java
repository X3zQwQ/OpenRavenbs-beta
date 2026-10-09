package keystrokesmod.module.impl.combat;

import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Velocity extends Module {

    public static SliderSetting horizontal;

    public static SliderSetting vertical;

    private SliderSetting chance;

    private ButtonSetting onlyWhileTargeting;

    private ButtonSetting disableS;

    public boolean disable;

    public Velocity() {
        super("Velocity", Module.category.combat, 0);
        this.registerSetting(horizontal = new SliderSetting("Horizontal", "%", 90.0, 0.0, 100.0, 1.0));
        this.registerSetting(vertical = new SliderSetting("Vertical", "%", 100.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.chance = new SliderSetting("Chance", "%", 100.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.onlyWhileTargeting = new ButtonSetting("Only while targeting", false));
        this.registerSetting(this.disableS = new ButtonSetting("Disable while holding S", false));
        this.closetModule = true;
    }

    @Override
    public String getInfo() {
        return (int) horizontal.getInput() + "% " + (int) vertical.getInput() + "%";
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent ev) {
        if (Utils.nullCheck() && !LongJump.stopVelocity && !this.disable) {
            if (ev.entity != mc.field_71439_g) {
                return;
            }
            if (ModuleManager.antiKnockback.isEnabled()) {
                return;
            }
            if (mc.field_71439_g.field_70738_aO <= 0 || mc.field_71439_g.field_70737_aN != mc.field_71439_g.field_70738_aO) {
                return;
            }
            if (this.onlyWhileTargeting.isToggled() && (mc.field_71476_x == null || mc.field_71476_x.field_72308_g == null)) {
                return;
            }
            if (this.disableS.isToggled() && Keyboard.isKeyDown(mc.field_71474_y.field_74368_y.func_151463_i())) {
                return;
            }
            if (this.chance.getInput() == 0.0) {
                return;
            }
            if (this.chance.getInput() != 100.0) {
                double ch = Math.random();
                if (ch >= this.chance.getInput() / 100.0) {
                    return;
                }
            }
            if (horizontal.getInput() != 100.0) {
                mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70159_w * (horizontal.getInput() / 100.0);
                mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70179_y * (horizontal.getInput() / 100.0);
            }
            if (vertical.getInput() != 100.0) {
                mc.field_71439_g.field_70181_x = mc.field_71439_g.field_70181_x * (vertical.getInput() / 100.0);
            }
        }
    }
}
