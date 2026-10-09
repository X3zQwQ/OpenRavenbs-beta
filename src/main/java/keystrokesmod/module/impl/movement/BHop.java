package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BHop extends Module {

    public SliderSetting mode;

    public static SliderSetting speedSetting;

    private ButtonSetting liquidDisable;

    private ButtonSetting sneakDisable;

    private ButtonSetting jumpMoving;

    public ButtonSetting rotateYawOption;

    public ButtonSetting damageBoost;

    public ButtonSetting damageBoostRequireKey;

    public GroupSetting damageBoostGroup;

    public KeySetting damageBoostKey;

    public String[] modes = new String[] { "Strafe", "Ground", "9 tick", "8 tick", "7 tick" };

    public boolean hopping;

    public boolean lowhop;

    public boolean didMove;

    public boolean setRotation;

    public BHop() {
        super("BHop", Module.category.movement);
        this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.modes));
        this.registerSetting(speedSetting = new SliderSetting("Speed", 2.0, 0.8, 1.2, 0.01));
        this.registerSetting(this.liquidDisable = new ButtonSetting("Disable in liquid", true));
        this.registerSetting(this.sneakDisable = new ButtonSetting("Disable while sneaking", true));
        this.registerSetting(this.jumpMoving = new ButtonSetting("Only jump when moving", true));
        this.registerSetting(this.rotateYawOption = new ButtonSetting("Rotate yaw", false));
        this.registerSetting(this.damageBoostGroup = new GroupSetting("Damage boost"));
        this.registerSetting(this.damageBoost = new ButtonSetting(this.damageBoostGroup, "Enable", false));
        this.registerSetting(this.damageBoostRequireKey = new ButtonSetting(this.damageBoostGroup, "Require key", false));
        this.registerSetting(this.damageBoostKey = new KeySetting(this.damageBoostGroup, "Enable key", 51));
    }

    @Override
    public void guiUpdate() {
        this.damageBoostKey.setVisible(this.damageBoostRequireKey.isToggled(), this);
    }

    @Override
    public String getInfo() {
        return this.modes[(int) this.mode.getInput()];
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (mc.field_71439_g.field_70122_E && !mc.field_71439_g.field_71075_bZ.field_75100_b) {
            if (this.hopping) {
                mc.field_71439_g.field_71158_b.field_78901_c = false;
            }
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if ((!mc.field_71439_g.func_70090_H() && !mc.field_71439_g.func_180799_ab() || !this.liquidDisable.isToggled()) && (!mc.field_71439_g.func_70093_af() || !this.sneakDisable.isToggled())) {
            if (!LongJump.function) {
                if (this.mode.getInput() >= 1.0) {
                    if (mc.field_71439_g.field_70122_E && (!this.jumpMoving.isToggled() || Utils.isMoving())) {
                        if (mc.field_71439_g.field_70701_bs <= -0.5 && mc.field_71439_g.field_70702_br == 0.0F && KillAura.target == null && !Utils.noSlowingBackWithBow() && !mc.field_71439_g.field_70123_F) {
                            this.setRotation = true;
                        }
                        mc.field_71439_g.func_70664_aZ();
                        double speed = speedSetting.getInput() - 0.52;
                        double speedModifier = speed;
                        int speedAmplifier = Utils.getSpeedAmplifier();
                        switch(speedAmplifier) {
                            case 1:
                                speedModifier = speed + 0.02;
                                break;
                            case 2:
                                speedModifier = speed + 0.04;
                                break;
                            case 3:
                                speedModifier = speed + 0.1;
                        }
                        if (Utils.isMoving() && !Utils.noSlowingBackWithBow()) {
                            Utils.setSpeed(speedModifier - Utils.randomizeDouble(3.0E-4, 1.0E-4));
                            this.didMove = true;
                        }
                        this.hopping = true;
                    }
                    if (mc.field_71439_g.field_70701_bs <= 0.5 && this.hopping) {
                        ModuleUtils.handleSlow();
                    }
                    if (!mc.field_71439_g.field_70122_E) {
                        this.hopping = false;
                    }
                }
                switch((int) this.mode.getInput()) {
                    case 0:
                        if (Utils.isMoving()) {
                            if (mc.field_71439_g.field_70122_E) {
                                mc.field_71439_g.func_70664_aZ();
                            }
                            mc.field_71439_g.func_70031_b(true);
                            Utils.setSpeed(Utils.getHorizontalSpeed() + 0.005 * speedSetting.getInput());
                            this.hopping = true;
                        }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        this.hopping = false;
    }
}
