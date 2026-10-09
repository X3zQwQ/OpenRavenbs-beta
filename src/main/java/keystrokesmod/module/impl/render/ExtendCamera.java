package keystrokesmod.module.impl.render;

import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;

public class ExtendCamera extends Module {

    public SliderSetting distance;

    private float lastDistance;

    public ExtendCamera() {
        super("ExtendCamera", Module.category.render);
        this.registerSetting(new DescriptionSetting("Extends camera in third person."));
        this.registerSetting(new DescriptionSetting("Default is 4 blocks."));
        this.registerSetting(this.distance = new SliderSetting("Distance", " block", 4.0, 1.0, 40.0, 0.5));
    }

    @Override
    public void onEnable() {
        this.setThirdPersonDistance((float) this.distance.getInput());
    }

    @Override
    public void onUpdate() {
        try {
            float input = (float) this.distance.getInput();
            if (this.lastDistance != input) {
                this.setThirdPersonDistance(this.lastDistance = input);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            Utils.sendMessage("&cThere was an issue setting third person distance.");
        }
    }

    @Override
    public void onDisable() {
        this.setThirdPersonDistance(4.0F);
    }

    private void setThirdPersonDistance(float distance) {
        ((IAccessorEntityRenderer) mc.field_71460_t).setThirdPersonDistance(distance);
    }
}
