package keystrokesmod.module.impl.movement;

import java.util.Iterator;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.SafeWalk;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockSnow;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Speed extends Module {

    public SliderSetting speed;

    public static SliderSetting multiplier;

    private ButtonSetting onlyForward;

    private ButtonSetting onlyStrafe;

    private String[] speedOptions = new String[] { "Vanilla", "Float" };

    private boolean canFloat;

    private boolean requireJump;

    private double[] floatSpeedLevels = new double[] { 0.2, 0.22, 0.28, 0.29, 0.3 };

    public Speed() {
        super("Speed", Module.category.movement, 0);
        this.registerSetting(this.speed = new SliderSetting("Speed", 0, this.speedOptions));
        this.registerSetting(multiplier = new SliderSetting("Multiplier", "x", 1.2, 1.0, 1.5, 0.01));
        this.registerSetting(this.onlyForward = new ButtonSetting("Only forward", false));
        this.registerSetting(this.onlyStrafe = new ButtonSetting("Only strafe", false));
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        double horizontalSpeed = Utils.getHorizontalSpeed();
        if (horizontalSpeed != 0.0) {
            if (mc.field_71439_g.field_70122_E && !mc.field_71439_g.field_71075_bZ.field_75100_b) {
                if (mc.field_71439_g.field_70737_aN != mc.field_71439_g.field_70738_aO || mc.field_71439_g.field_70738_aO <= 0) {
                    if (!Utils.jumpDown()) {
                        if (this.settingsMet()) {
                            if (this.speed.getInput() == 0.0) {
                                double val = multiplier.getInput() - (multiplier.getInput() - 1.0) * 0.5;
                                Utils.setSpeed(horizontalSpeed * val, true);
                            } else if (this.speed.getInput() == 1.0) {
                                if (ModuleUtils.groundTicks <= 8 || this.floatConditions()) {
                                    this.canFloat = true;
                                }
                                if (!this.floatConditions()) {
                                    this.canFloat = false;
                                }
                                if (!mc.field_71439_g.field_70122_E) {
                                    this.requireJump = false;
                                }
                                if (this.canFloat && this.floatConditions() && !this.requireJump) {
                                    e.setPosY(e.getPosY() + ModuleUtils.offsetValue);
                                    if (Utils.isMoving()) {
                                        Utils.setSpeed(this.getFloatSpeed(this.getSpeedLevel()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean settingsMet() {
        return this.onlyForward.isToggled() && !Utils.isBindDown(mc.field_71474_y.field_74351_w) ? false : !this.onlyStrafe.isToggled() || mc.field_71439_g.field_70702_br != 0.0F;
    }

    private boolean floatConditions() {
        int edgeY = (int) Math.round(mc.field_71439_g.field_70163_u % 1.0 * 100.0);
        if (ModuleUtils.stillTicks > 20) {
            this.requireJump = true;
            return false;
        } else if (mc.field_71439_g.field_70163_u % 1.0 != 0.0 && edgeY >= 10 && !this.allowedBlocks()) {
            this.requireJump = true;
            return false;
        } else if (SafeWalk.canSafeWalk()) {
            this.requireJump = true;
            return false;
        } else if (ModuleManager.bHop.isEnabled()) {
            this.requireJump = true;
            return false;
        } else if (!mc.field_71439_g.field_70122_E) {
            return false;
        } else if (Utils.jumpDown()) {
            return false;
        } else {
            return LongJump.function ? false : !Utils.isBindDown(mc.field_71474_y.field_74311_E);
        }
    }

    private boolean allowedBlocks() {
        Block block = BlockUtils.getBlock(new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v));
        return block instanceof BlockSnow ? true : block instanceof BlockCarpet;
    }

    private double getFloatSpeed(int speedLevel) {
        double min = 0.0;
        if (mc.field_71439_g.field_70702_br != 0.0F && mc.field_71439_g.field_70701_bs != 0.0F) {
            min = 0.003;
        }
        return speedLevel >= 0 ? this.floatSpeedLevels[speedLevel] - min : this.floatSpeedLevels[0] - min;
    }

    private int getSpeedLevel() {
        Iterator iterator2 = mc.field_71439_g.func_70651_bq().iterator();
        if (iterator2.hasNext()) {
            PotionEffect potionEffect = (PotionEffect) iterator2.next();
            return potionEffect.func_76453_d().equals("potion.moveSpeed") ? potionEffect.func_76458_c() + 1 : 0;
        } else {
            return 0;
        }
    }
}
