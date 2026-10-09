package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;

public class NoHurtCam extends Module {

    public SliderSetting multiplier;

    public NoHurtCam() {
        super("NoHurtCam", Module.category.render);
        this.registerSetting(new DescriptionSetting("Default is 14x multiplier."));
        this.registerSetting(this.multiplier = new SliderSetting("Multiplier", 14.0, -40.0, 40.0, 1.0));
    }
}
