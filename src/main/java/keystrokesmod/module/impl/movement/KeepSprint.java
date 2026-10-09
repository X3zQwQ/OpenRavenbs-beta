package keystrokesmod.module.impl.movement;

import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;

public class KeepSprint extends Module {

    public static SliderSetting slow;

    public static ButtonSetting stopSprint;

    public static ButtonSetting disableWhileJump;

    public static ButtonSetting reduceReachHits;

    public KeepSprint() {
        super("Keep Sprint", Module.category.movement, 0);
        this.registerSetting(new DescriptionSetting(new String("Default is 40% motion reduction.")));
        this.registerSetting(slow = new SliderSetting("Slow %", 40.0, 0.0, 40.0, 1.0));
        this.registerSetting(stopSprint = new ButtonSetting("Stop Sprint", true));
        this.registerSetting(disableWhileJump = new ButtonSetting("Disable while jumping", false));
        this.registerSetting(reduceReachHits = new ButtonSetting("Only reduce reach hits", false));
    }

    public static void keepSprint(Entity en) {
        boolean vanilla = false;
        if (disableWhileJump.isToggled() && !mc.field_71439_g.field_70122_E) {
            vanilla = true;
        } else if (reduceReachHits.isToggled() && !mc.field_71439_g.field_71075_bZ.field_75098_d) {
            double distance = -1.0;
            Vec3 getPositionEyes = mc.field_71439_g.func_174824_e(1.0F);
            if (ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && KillAura.target != null) {
                distance = getPositionEyes.func_72438_d(KillAura.target.func_174824_e(1.0F));
            } else if (ModuleManager.reach != null && ModuleManager.reach.isEnabled()) {
                distance = getPositionEyes.func_72438_d(mc.field_71476_x.field_72307_f);
            }
            if (distance != -1.0 && distance <= 3.0) {
                vanilla = true;
            }
        }
        if (vanilla) {
            mc.field_71439_g.field_70159_w *= 0.6;
            mc.field_71439_g.field_70179_y *= 0.6;
        } else {
            float mult = (100.0F - (float) slow.getInput()) / 100.0F;
            mc.field_71439_g.field_70159_w *= mult;
            mc.field_71439_g.field_70179_y *= mult;
        }
        if (stopSprint.isToggled()) {
            mc.field_71439_g.field_70159_w *= 0.5;
        }
    }
}
