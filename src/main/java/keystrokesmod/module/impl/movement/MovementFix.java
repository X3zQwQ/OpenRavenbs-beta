package keystrokesmod.module.impl.movement;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.DescriptionSetting;

public class MovementFix extends Module {

    public MovementFix() {
        super("Movement Fix", Module.category.movement);
        this.registerSetting(new DescriptionSetting("Aligns input with rotations"));
    }
}
