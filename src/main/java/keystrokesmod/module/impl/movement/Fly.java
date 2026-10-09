package keystrokesmod.module.impl.movement;

import keystrokesmod.event.CollisionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.apache.commons.lang3.RandomUtils;

public class Fly extends Module {

    private SliderSetting mode;

    public static SliderSetting horizontalSpeed;

    private SliderSetting verticalSpeed;

    private ButtonSetting showBPS;

    private ButtonSetting stopMotion;

    private ButtonSetting keepY;

    private String[] modes = new String[] { "Vanilla", "Fast", "Fast 2", "Walk" };

    private boolean canFly;

    private int maxY;

    private AxisAlignedBB FULL_ABB = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public Fly() {
        super("Fly", Module.category.movement);
        this.registerSetting(this.mode = new SliderSetting("Fly", 0, this.modes));
        this.registerSetting(horizontalSpeed = new SliderSetting("Horizontal speed", 2.0, 1.0, 9.0, 0.1));
        this.registerSetting(this.verticalSpeed = new SliderSetting("Vertical speed", 2.0, 1.0, 9.0, 0.1));
        this.registerSetting(this.showBPS = new ButtonSetting("Show BPS", false));
        this.registerSetting(this.stopMotion = new ButtonSetting("Stop motion", false));
        this.registerSetting(this.keepY = new ButtonSetting("Keep-Y", false));
    }

    @Override
    public void guiUpdate() {
        horizontalSpeed.setVisible(this.mode.getInput() != 3.0, this);
        this.verticalSpeed.setVisible(this.mode.getInput() != 3.0, this);
        this.keepY.setVisible(this.mode.getInput() == 3.0, this);
    }

    @Override
    public String getInfo() {
        return this.modes[(int) this.mode.getInput()];
    }

    @Override
    public void onEnable() {
        this.canFly = mc.field_71439_g.field_71075_bZ.field_75100_b;
        this.maxY = mc.field_71439_g.func_180425_c().func_177956_o();
    }

    @SubscribeEvent
    public void onCollision(CollisionEvent e) {
        if (this.mode.getInput() == 3.0 && !Utils.isBindDown(mc.field_71474_y.field_74311_E)) {
            if (e.blockPos.func_177956_o() < (this.keepY.isToggled() ? this.maxY : mc.field_71439_g.field_70163_u)) {
                e.boundingBox = this.FULL_ABB.func_72317_d(e.blockPos.func_177958_n(), e.blockPos.func_177956_o(), e.blockPos.func_177952_p());
            }
        } else {
            this.maxY = mc.field_71439_g.func_180425_c().func_177956_o();
        }
    }

    @Override
    public void onUpdate() {
        if (this.mode.getInput() != 3.0) {
            switch((int) this.mode.getInput()) {
                case 0:
                    mc.field_71439_g.field_70181_x = 0.0;
                    mc.field_71439_g.field_71075_bZ.func_75092_a((float) (0.05F * horizontalSpeed.getInput()));
                    mc.field_71439_g.field_71075_bZ.field_75100_b = true;
                    break;
                case 1:
                    mc.field_71439_g.field_70122_E = true;
                    if (mc.field_71462_r == null) {
                        if (Utils.jumpDown()) {
                            mc.field_71439_g.field_70181_x = 0.3 * this.verticalSpeed.getInput();
                        } else if (Utils.jumpDown()) {
                            mc.field_71439_g.field_70181_x = -0.3 * this.verticalSpeed.getInput();
                        } else {
                            mc.field_71439_g.field_70181_x = 0.0;
                        }
                    } else {
                        mc.field_71439_g.field_70181_x = 0.0;
                    }
                    mc.field_71439_g.field_71075_bZ.func_75092_a(0.2F);
                    mc.field_71439_g.field_71075_bZ.field_75100_b = true;
                    this.setSpeed(0.85 * horizontalSpeed.getInput());
                    break;
                case 2:
                    double nextDouble = RandomUtils.nextDouble(1.0E-7, 1.2E-7);
                    if (mc.field_71439_g.field_70173_aa % 2 == 0) {
                        nextDouble = -nextDouble;
                    }
                    if (!mc.field_71439_g.field_70122_E) {
                        mc.field_71439_g.func_70107_b(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + nextDouble, mc.field_71439_g.field_70161_v);
                    }
                    mc.field_71439_g.field_70181_x = 0.0;
                    this.setSpeed(0.4 * horizontalSpeed.getInput());
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.field_71439_g.field_71075_bZ.field_75101_c) {
            mc.field_71439_g.field_71075_bZ.field_75100_b = this.canFly;
        } else {
            mc.field_71439_g.field_71075_bZ.field_75100_b = false;
        }
        this.canFly = false;
        switch((int) this.mode.getInput()) {
            case 0:
            case 1:
                mc.field_71439_g.field_71075_bZ.func_75092_a(0.05F);
            default:
                if (this.stopMotion.isToggled()) {
                    mc.field_71439_g.field_70179_y = 0.0;
                    mc.field_71439_g.field_70181_x = 0.0;
                    mc.field_71439_g.field_70159_w = 0.0;
                }
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent e) {
        if (this.showBPS.isToggled() && e.phase == Phase.END && Utils.nullCheck()) {
            if (mc.field_71462_r == null && !mc.field_71474_y.field_74330_P) {
                RenderUtils.renderBPS(true, false);
            }
        }
    }

    private void setSpeed(double speed) {
        if (speed == 0.0) {
            mc.field_71439_g.field_70179_y = 0.0;
            mc.field_71439_g.field_70159_w = 0.0;
        } else {
            double moveForward = mc.field_71439_g.field_71158_b.field_78900_b;
            double moveStrafe = mc.field_71439_g.field_71158_b.field_78902_a;
            float yaw = mc.field_71439_g.field_70177_z;
            if (moveForward == 0.0 && moveStrafe == 0.0) {
                mc.field_71439_g.field_70179_y = 0.0;
                mc.field_71439_g.field_70159_w = 0.0;
            } else {
                if (moveForward != 0.0) {
                    if (moveStrafe > 0.0) {
                        yaw += moveForward > 0.0 ? -45 : 45;
                    } else if (moveStrafe < 0.0) {
                        yaw += moveForward > 0.0 ? 45 : -45;
                    }
                    moveStrafe = 0.0;
                    if (moveForward > 0.0) {
                        moveForward = 1.0;
                    } else if (moveForward < 0.0) {
                        moveForward = -1.0;
                    }
                }
                double radians = Math.toRadians(yaw + 90.0F);
                double sin = Math.sin(radians);
                double cos = Math.cos(radians);
                mc.field_71439_g.field_70159_w = moveForward * speed * cos + moveStrafe * speed * sin;
                mc.field_71439_g.field_70179_y = moveForward * speed * sin - moveStrafe * speed * cos;
            }
        }
    }
}
