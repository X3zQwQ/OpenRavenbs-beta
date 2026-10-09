package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;

public class ItemPhysics extends Module {

    public static ItemPhysics instance;

    public final SliderSetting rotationSpeed;

    public ItemPhysics() {
        super("Item Physics", Module.category.render, 0);
        this.registerSetting(this.rotationSpeed = new SliderSetting("Rotation speed", 1.0, 0.0, 5.0, 0.1));
    }

    @Override
    public void onEnable() {
        instance = this;
    }

    @Override
    public void onDisable() {
        instance = null;
    }
}
