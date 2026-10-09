package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoFall extends Module {

    public SliderSetting mode;

    private SliderSetting minFallDistance;

    private ButtonSetting disableAdventure;

    private ButtonSetting ignoreVoid;

    private String[] modes = new String[] { "Spoof", "NoGround", "Packet A", "Packet B" };

    private double initialY;

    private double dynamic;

    private boolean isFalling;

    public NoFall() {
        super("NoFall", Module.category.player);
        this.registerSetting(this.mode = new SliderSetting("Mode", 2, this.modes));
        this.registerSetting(this.minFallDistance = new SliderSetting("Minimum fall distance", 3.0, 0.0, 10.0, 0.1));
        this.registerSetting(this.disableAdventure = new ButtonSetting("Disable adventure", false));
        this.registerSetting(this.ignoreVoid = new ButtonSetting("Ignore void", true));
    }

    @Override
    public void onDisable() {
        Utils.resetTimer();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (this.reset()) {
            Utils.resetTimer();
            this.initialY = mc.field_71439_g.field_70163_u;
            this.isFalling = false;
        } else {
            if (mc.field_71439_g.field_70143_R >= this.minFallDistance.getInput()) {
                this.isFalling = true;
            }
            double predictedY = mc.field_71439_g.field_70163_u + mc.field_71439_g.field_70181_x;
            double distanceFallen = this.initialY - predictedY;
            if (mc.field_71439_g.field_70181_x >= -1.0) {
                this.dynamic = 3.0;
            }
            if (mc.field_71439_g.field_70181_x < -1.0) {
                this.dynamic = 4.0;
            }
            if (mc.field_71439_g.field_70181_x < -2.0) {
                this.dynamic = 5.0;
            }
            if (this.isFalling && this.mode.getInput() == 2.0 && distanceFallen >= this.dynamic) {
                ((IAccessorMinecraft) mc).getTimer().field_74278_d = 0.7399789F + (float) Utils.randomizeDouble(-0.012, 0.012);
                mc.func_147114_u().func_147297_a(new C03PacketPlayer(true));
                this.initialY = mc.field_71439_g.field_70163_u;
            }
            if (this.isFalling && this.mode.getInput() == 3.0) {
                if (mc.field_71439_g.field_70173_aa % 2 == 0) {
                    ((IAccessorMinecraft) mc).getTimer().field_74278_d = (float) Utils.randomizeDouble(0.5, 0.50201);
                } else {
                    ((IAccessorMinecraft) mc).getTimer().field_74278_d = 1.0F;
                }
                if (distanceFallen >= 3.0) {
                    mc.func_147114_u().func_147297_a(new C03PacketPlayer(true));
                    this.initialY = mc.field_71439_g.field_70163_u;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        switch((int) this.mode.getInput()) {
            case 0:
                e.setOnGround(true);
                break;
            case 1:
                e.setOnGround(false);
        }
    }

    @Override
    public String getInfo() {
        return this.modes[(int) this.mode.getInput()];
    }

    private boolean isVoid() {
        return Utils.overVoid(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v);
    }

    private boolean reset() {
        if (this.disableAdventure.isToggled() && mc.field_71442_b.func_178889_l().func_82752_c()) {
            return true;
        } else if (this.ignoreVoid.isToggled() && this.isVoid()) {
            return true;
        } else if (Utils.isBedwarsPracticeOrReplay()) {
            return true;
        } else if (Utils.spectatorCheck()) {
            return true;
        } else if (mc.field_71439_g.field_70122_E) {
            return true;
        } else if (BlockUtils.getBlock(new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u - 1.0, mc.field_71439_g.field_70161_v)) != Blocks.field_150350_a) {
            return true;
        } else if (mc.field_71439_g.field_70181_x > -0.0784) {
            return true;
        } else if (mc.field_71439_g.field_71075_bZ.field_75098_d) {
            return true;
        } else {
            return this.isVoid() && mc.field_71439_g.field_70163_u <= 41.0 ? true : mc.field_71439_g.field_71075_bZ.field_75100_b;
        }
    }
}
