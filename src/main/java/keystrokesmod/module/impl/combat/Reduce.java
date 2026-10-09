package keystrokesmod.module.impl.combat;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.entity.Entity;

public class Reduce extends Module {

    private static SliderSetting chance;

    private static SliderSetting reduction;

    public Reduce() {
        super("Reduce", Module.category.combat);
        this.registerSetting(new DescriptionSetting("Overrides KeepSprint."));
        this.registerSetting(reduction = new SliderSetting("Attack reduction %", 60.0, 60.0, 100.0, 0.5));
        this.registerSetting(chance = new SliderSetting("Chance", "%", 100.0, 0.0, 100.0, 1.0));
        this.closetModule = true;
    }

    public static void reduce(Entity entity) {
        if (chance.getInput() != 0.0) {
            if (chance.getInput() != 100.0 && Math.random() >= chance.getInput() / 100.0) {
                mc.field_71439_g.field_70159_w *= 0.6;
                mc.field_71439_g.field_70179_y *= 0.6;
            } else {
                double n = (100.0 - (float) reduction.getInput()) / 100.0;
                mc.field_71439_g.field_70159_w *= n;
                mc.field_71439_g.field_70179_y *= n;
            }
        }
    }
}
