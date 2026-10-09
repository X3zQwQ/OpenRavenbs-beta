package keystrokesmod.module.impl.movement;

import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;

public class Boost extends Module {

    private SliderSetting multiplier;

    private SliderSetting time;

    private int ticks = 0;

    private boolean timerDisabled = false;

    public Boost() {
        super("Boost", Module.category.movement);
        this.registerSetting(new DescriptionSetting("20 ticks are in 1 second"));
        this.registerSetting(this.multiplier = new SliderSetting("Multiplier", "x", 2.0, 1.0, 3.0, 0.05));
        this.registerSetting(this.time = new SliderSetting("Time", " tick", 15.0, 1.0, 80.0, 1.0));
    }

    @Override
    public void onEnable() {
        if (ModuleManager.timer.isEnabled()) {
            this.timerDisabled = true;
            ModuleManager.timer.disable();
        }
    }

    @Override
    public void onDisable() {
        this.ticks = 0;
        if (((IAccessorMinecraft) mc).getTimer().field_74278_d != 1.0F) {
            Utils.resetTimer();
        }
        if (this.timerDisabled) {
            ModuleManager.timer.enable();
        }
        this.timerDisabled = false;
    }

    @Override
    public void onUpdate() {
        if (this.ticks == 0) {
            this.ticks = mc.field_71439_g.field_70173_aa;
        }
        ((IAccessorMinecraft) mc).getTimer().field_74278_d = (float) this.multiplier.getInput();
        if (this.ticks == mc.field_71439_g.field_70173_aa - this.time.getInput()) {
            Utils.resetTimer();
            this.disable();
        }
    }
}
