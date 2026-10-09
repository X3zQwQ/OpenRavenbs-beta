package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class SafeWalk extends Module {

    private SliderSetting sneakDelay;

    private SliderSetting motion;

    public ButtonSetting sneak;

    public static ButtonSetting blocksOnly;

    public static ButtonSetting pitchCheck;

    public static ButtonSetting disableOnForward;

    private int unsneakDelayTicks = 0;

    public boolean isSneaking;

    public SafeWalk() {
        super("SafeWalk", Module.category.player, 0);
        this.registerSetting(this.sneakDelay = new SliderSetting("Sneak delay", " tick", 0.0, 0.0, 20.0, 1.0));
        this.registerSetting(this.motion = new SliderSetting("Motion", "x", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(blocksOnly = new ButtonSetting("Blocks only", true));
        this.registerSetting(disableOnForward = new ButtonSetting("Disable on forward", false));
        this.registerSetting(pitchCheck = new ButtonSetting("Pitch check", false));
        this.registerSetting(this.sneak = new ButtonSetting("Sneak", false));
    }

    @Override
    public void onDisable() {
        if (this.sneak.isToggled() && Utils.isEdgeOfBlock()) {
            this.setSneakState(false);
        }
        this.isSneaking = false;
        this.unsneakDelayTicks = 0;
    }

    @Override
    public void onUpdate() {
        if (this.motion.getInput() != 1.0 && mc.field_71439_g.field_70122_E && Utils.isMoving() && this.settingsMet()) {
            mc.field_71439_g.field_70159_w = mc.field_71439_g.field_70159_w * this.motion.getInput();
            mc.field_71439_g.field_70179_y = mc.field_71439_g.field_70179_y * this.motion.getInput();
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (this.sneak.isToggled() && Utils.nullCheck()) {
            boolean edge = mc.field_71439_g.field_70122_E && Utils.isEdgeOfBlock();
            if (edge) {
                if (!this.settingsMet()) {
                    this.setSneakState(false);
                    return;
                }
                if (!this.isSneaking) {
                    this.setSneakState(true);
                    this.unsneakDelayTicks = (int) this.sneakDelay.getInput();
                }
            } else if (this.isSneaking) {
                if (!this.settingsMet()) {
                    this.setSneakState(false);
                    return;
                }
                if (this.unsneakDelayTicks > 0) {
                    this.unsneakDelayTicks--;
                    return;
                }
                this.setSneakState(false);
            }
            if (this.isSneaking && (mc.field_71439_g.field_71075_bZ.field_75100_b || !this.settingsMet())) {
                this.setSneakState(false);
            }
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent e) {
        if (this.sneak.isToggled() && e.gui == null) {
            this.isSneaking = mc.field_71439_g.func_70093_af();
        }
    }

    private void setSneakState(boolean sneakState) {
        if (!sneakState) {
            this.unsneakDelayTicks = 0;
        }
        if (this.isSneaking != sneakState) {
            if (sneakState || !Utils.isBindDown(mc.field_71474_y.field_74311_E)) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74311_E.func_151463_i(), sneakState);
                if (sneakState) {
                    KeyBinding.func_74507_a(mc.field_71474_y.field_74311_E.func_151463_i());
                }
                this.isSneaking = sneakState;
            }
        }
    }

    public static boolean canSafeWalk() {
        if (ModuleManager.safeWalk == null || !ModuleManager.safeWalk.isEnabled()) {
            return false;
        } else if (disableOnForward.isToggled() && Keyboard.isKeyDown(mc.field_71474_y.field_74351_w.func_151463_i())) {
            return false;
        } else if (pitchCheck.isToggled() && mc.field_71439_g.field_70125_A < 70.0F) {
            return false;
        } else {
            if (blocksOnly.isToggled()) {
                ItemStack held = mc.field_71439_g.func_70694_bm();
                if (held == null || !(held.func_77973_b() instanceof ItemBlock)) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean settingsMet() {
        if (blocksOnly.isToggled()) {
            ItemStack held = mc.field_71439_g.func_70694_bm();
            if (held == null || !(held.func_77973_b() instanceof ItemBlock)) {
                return false;
            }
        }
        return disableOnForward.isToggled() && Keyboard.isKeyDown(mc.field_71474_y.field_74351_w.func_151463_i()) ? false : !pitchCheck.isToggled() || !(mc.field_71439_g.field_70125_A < 70.0F);
    }
}
