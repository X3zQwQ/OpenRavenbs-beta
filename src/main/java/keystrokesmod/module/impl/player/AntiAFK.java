package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiAFK extends Module {

    private SliderSetting afk;

    private ButtonSetting jump;

    private ButtonSetting jumpWhenCollided;

    private ButtonSetting randomClicks;

    private ButtonSetting swapItem;

    private SliderSetting spin;

    private ButtonSetting randomizeDelta;

    private ButtonSetting randomizePitch;

    private SliderSetting minDelay;

    private SliderSetting maxDelay;

    private String[] afkModes = new String[] { "None", "Wander", "Lateral shuffle", "Forward", "Backward" };

    private String[] spinModes = new String[] { "None", "Random", "Right", "Left" };

    private int ticks;

    private boolean random;

    public boolean stop = false;

    public AntiAFK() {
        super("AntiAFK", Module.category.player);
        this.registerSetting(this.afk = new SliderSetting("AFK", 0, this.afkModes));
        this.registerSetting(this.jump = new ButtonSetting("Jump", false));
        this.registerSetting(this.jumpWhenCollided = new ButtonSetting("Jump only when collided", false));
        this.registerSetting(this.randomClicks = new ButtonSetting("Random clicks", false));
        this.registerSetting(this.swapItem = new ButtonSetting("Swap item", false));
        this.registerSetting(this.spin = new SliderSetting("Spin", 0, this.spinModes));
        this.registerSetting(this.randomizeDelta = new ButtonSetting("Randomize delta", true));
        this.registerSetting(this.randomizePitch = new ButtonSetting("Randomize pitch", true));
        this.registerSetting(this.minDelay = new SliderSetting("Minimum delay ticks", 10.0, 4.0, 160.0, 2.0));
        this.registerSetting(this.maxDelay = new SliderSetting("Maximum delay ticks", 80.0, 4.0, 160.0, 2.0));
    }

    @Override
    public void onEnable() {
        Utils.sendMessage("Bye");
        this.ticks = this.h();
        this.random = Utils.getRandom().nextBoolean();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!this.stop) {
            if (mc.field_71462_r == null || mc.field_71462_r instanceof GuiChat) {
                this.ticks--;
                switch((int) this.afk.getInput()) {
                    case 1:
                        if (this.random) {
                            KeyBinding.func_74510_a(mc.field_71474_y.field_74351_w.func_151463_i(), Utils.getRandom().nextBoolean());
                            KeyBinding.func_74510_a(mc.field_71474_y.field_74366_z.func_151463_i(), Utils.getRandom().nextBoolean());
                        } else {
                            KeyBinding.func_74510_a(mc.field_71474_y.field_74368_y.func_151463_i(), Utils.getRandom().nextBoolean());
                            KeyBinding.func_74510_a(mc.field_71474_y.field_74370_x.func_151463_i(), Utils.getRandom().nextBoolean());
                        }
                        break;
                    case 2:
                        KeyBinding.func_74510_a(mc.field_71474_y.field_74366_z.func_151463_i(), this.random);
                        KeyBinding.func_74510_a(mc.field_71474_y.field_74370_x.func_151463_i(), !this.random);
                        break;
                    case 3:
                        KeyBinding.func_74510_a(mc.field_71474_y.field_74351_w.func_151463_i(), true);
                        break;
                    case 4:
                        KeyBinding.func_74510_a(mc.field_71474_y.field_74368_y.func_151463_i(), true);
                }
                switch((int) this.spin.getInput()) {
                    case 1:
                        mc.field_71439_g.field_70177_z = (float) (mc.field_71439_g.field_70177_z + this.c(this.random));
                        this.d();
                        break;
                    case 2:
                        mc.field_71439_g.field_70177_z = (float) (mc.field_71439_g.field_70177_z + this.c(true));
                        this.d();
                        break;
                    case 3:
                        mc.field_71439_g.field_70177_z = (float) (mc.field_71439_g.field_70177_z + this.c(false));
                        this.d();
                }
                if (this.jump.isToggled() && mc.field_71439_g.field_70122_E && (!this.jumpWhenCollided.isToggled() || mc.field_71439_g.field_70123_F)) {
                    mc.field_71439_g.func_70664_aZ();
                }
                if (this.ticks == 0) {
                    if (this.swapItem.isToggled()) {
                        mc.field_71439_g.field_71071_by.field_70461_c = Utils.randomizeInt(0, 8);
                    }
                    if (this.randomClicks.isToggled()) {
                        ((IAccessorMinecraft) mc).callClickMouse();
                    }
                    this.ticks = this.h();
                    this.random = !this.random;
                }
            }
        }
    }

    private double a() {
        int n = Utils.getRandom().nextBoolean() ? 1 : -1;
        if (!this.randomizeDelta.isToggled()) {
            return 2 * n;
        } else {
            double n2 = Utils.randomizeInt(100, 500) / 100.0;
            if (n2 % 1.0 == 0.0) {
                n2 += Utils.randomizeInt(1, 10) / 10.0 * n;
            }
            return n2 * n;
        }
    }

    @Override
    public void onDisable() {
        this.b(0);
        this.stop = false;
    }

    private void b(int n) {
        switch(n) {
            case 1:
                KeyBinding.func_74510_a(mc.field_71474_y.field_74351_w.func_151463_i(), false);
                KeyBinding.func_74510_a(mc.field_71474_y.field_74368_y.func_151463_i(), false);
                KeyBinding.func_74510_a(mc.field_71474_y.field_74366_z.func_151463_i(), false);
                KeyBinding.func_74510_a(mc.field_71474_y.field_74370_x.func_151463_i(), false);
                break;
            case 2:
                KeyBinding.func_74510_a(mc.field_71474_y.field_74366_z.func_151463_i(), false);
                KeyBinding.func_74510_a(mc.field_71474_y.field_74370_x.func_151463_i(), false);
                break;
            case 3:
                KeyBinding.func_74510_a(mc.field_71474_y.field_74351_w.func_151463_i(), false);
                break;
            case 4:
                KeyBinding.func_74510_a(mc.field_71474_y.field_74368_y.func_151463_i(), false);
        }
    }

    private int h() {
        return this.minDelay.getInput() == this.maxDelay.getInput() ? (int) this.minDelay.getInput() : Utils.randomizeInt((int) this.minDelay.getInput(), (int) this.maxDelay.getInput());
    }

    private void d() {
        if (this.randomizePitch.isToggled()) {
            mc.field_71439_g.field_70125_A = RotationUtils.clampPitch((float) (mc.field_71439_g.field_70125_A + this.a()));
        }
    }

    private double c(boolean b) {
        int n = b ? 1 : -1;
        if (!this.randomizeDelta.isToggled()) {
            return 3 * n;
        } else {
            double n2 = Utils.randomizeInt(100, 1000) / 100.0;
            if (n2 % 1.0 == 0.0) {
                n2 += Utils.randomizeInt(1, 10) / 10.0 * n;
            }
            return n2 * n;
        }
    }
}
