package keystrokesmod.module.impl.combat;

import java.awt.AWTException;
import java.awt.Robot;
import keystrokesmod.helper.MouseHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class ClickAssist extends Module {

    private SliderSetting chanceLeft;

    private SliderSetting chanceRight;

    private ButtonSetting rightClick;

    private ButtonSetting blocksOnly;

    private ButtonSetting weaponOnly;

    private ButtonSetting onlyWhileTargeting;

    private ButtonSetting aboveCPS;

    private ButtonSetting leftClick;

    private ButtonSetting disableInCreative;

    private Robot bot;

    private boolean ignNL = false;

    private boolean ignNR = false;

    public ClickAssist() {
        super("ClickAssist", Module.category.combat, 0);
        this.liteModule = true;
        this.registerSetting(new DescriptionSetting("Boost your CPS."));
        this.registerSetting(this.disableInCreative = new ButtonSetting("Disable in creative", true));
        this.registerSetting(this.leftClick = new ButtonSetting("Left click", true));
        this.registerSetting(this.chanceLeft = new SliderSetting("Chance left", 80.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", true));
        this.registerSetting(this.onlyWhileTargeting = new ButtonSetting("Only while targeting", false));
        this.registerSetting(this.rightClick = new ButtonSetting("Right click", false));
        this.registerSetting(this.chanceRight = new SliderSetting("Chance right", 80.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.blocksOnly = new ButtonSetting("Blocks only", true));
        this.registerSetting(this.aboveCPS = new ButtonSetting("Above 5 cps", false));
        this.closetModule = true;
    }

    @Override
    public void onEnable() {
        try {
            this.bot = new Robot();
        } catch (AWTException exception) {
            this.disable();
        }
    }

    @Override
    public void onDisable() {
        this.ignNL = false;
        this.ignNR = false;
        this.bot = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouseUpdate(MouseEvent ev) {
        if (!this.disableInCreative.isToggled() || !mc.field_71439_g.field_71075_bZ.field_75098_d) {
            if (ev.button >= 0 && ev.buttonstate && Utils.nullCheck()) {
                if (mc.field_71462_r == null && !Utils.isConsuming(mc.field_71439_g)) {
                    if (ev.button == 0 && this.leftClick.isToggled() && this.chanceLeft.getInput() != 0.0) {
                        if (this.ignNL) {
                            this.ignNL = false;
                        } else {
                            if (this.chanceLeft.getInput() == 0.0) {
                                return;
                            }
                            if (this.weaponOnly.isToggled() && !Utils.holdingWeapon()) {
                                return;
                            }
                            if (this.onlyWhileTargeting.isToggled() && (mc.field_71476_x == null || mc.field_71476_x.field_72308_g == null)) {
                                return;
                            }
                            if (this.chanceLeft.getInput() != 100.0) {
                                double ch = Math.random();
                                if (ch >= this.chanceLeft.getInput() / 100.0) {
                                    this.fix(0);
                                    return;
                                }
                            }
                            this.bot.mouseRelease(16);
                            this.bot.mousePress(16);
                            this.ignNL = true;
                        }
                    } else if (ev.button == 1 && this.rightClick.isToggled()) {
                        if (this.ignNR) {
                            this.ignNR = false;
                        } else {
                            if (this.chanceRight.getInput() == 0.0) {
                                return;
                            }
                            if (this.blocksOnly.isToggled()) {
                                ItemStack item = mc.field_71439_g.func_70694_bm();
                                if (item == null || !(item.func_77973_b() instanceof ItemBlock)) {
                                    this.fix(1);
                                    return;
                                }
                            }
                            if (this.aboveCPS.isToggled() && MouseHelper.i() <= 5) {
                                this.fix(1);
                                return;
                            }
                            if (this.chanceRight.getInput() != 100.0) {
                                double ch = Math.random();
                                if (ch >= this.chanceRight.getInput() / 100.0) {
                                    this.fix(1);
                                    return;
                                }
                            }
                            this.bot.mouseRelease(4);
                            this.bot.mousePress(4);
                            this.ignNR = true;
                        }
                    }
                }
                this.fix(0);
                this.fix(1);
            }
        }
    }

    private void fix(int t) {
        if (t == 0) {
            if (this.ignNL && !Mouse.isButtonDown(0)) {
                this.bot.mouseRelease(16);
            }
        } else if (t == 1 && this.ignNR && !Mouse.isButtonDown(1)) {
            this.bot.mouseRelease(4);
        }
    }
}
