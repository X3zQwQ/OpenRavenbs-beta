package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;

public class ExtraBobbing extends Module {

    public SliderSetting level;

    private boolean viewBobbingEnabled;

    public ExtraBobbing() {
        super("Extra Bobbing", Module.category.render);
        this.registerSetting(this.level = new SliderSetting("Level", 1.0, 0.0, 8.0, 0.1));
    }

    @Override
    public void onEnable() {
        this.viewBobbingEnabled = mc.field_71474_y.field_74336_f;
        if (!this.viewBobbingEnabled) {
            mc.field_71474_y.field_74336_f = true;
        }
    }

    @Override
    public void onDisable() {
        mc.field_71474_y.field_74336_f = this.viewBobbingEnabled;
    }

    @Override
    public void onUpdate() {
        if (!mc.field_71474_y.field_74336_f) {
            mc.field_71474_y.field_74336_f = true;
        }
        if (mc.field_71439_g.field_71158_b.field_78900_b != 0.0F || mc.field_71439_g.field_71158_b.field_78902_a != 0.0F) {
            mc.field_71439_g.field_71109_bG = (float) (mc.field_71439_g.field_71109_bG + this.level.getInput() / 2.0);
        }
    }
}
