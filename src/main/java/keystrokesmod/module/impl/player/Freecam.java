package keystrokesmod.module.impl.player;

import java.awt.Color;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Freecam extends Module {

    public SliderSetting speed;

    private ButtonSetting disableOnDamage;

    private ButtonSetting showArm;

    private ButtonSetting allowDigging;

    private ButtonSetting allowInteracting;

    private ButtonSetting allowPlacing;

    public static EntityOtherPlayerMP freeEntity = null;

    private int[] lcc = new int[] { Integer.MAX_VALUE, 0 };

    private float[] sAng = new float[] { 0.0F, 0.0F };

    public Freecam() {
        super("Freecam", Module.category.player);
        this.registerSetting(this.speed = new SliderSetting("Speed", 2.5, 0.5, 10.0, 0.5));
        this.registerSetting(this.disableOnDamage = new ButtonSetting("Disable on damage", true));
        this.registerSetting(this.allowDigging = new ButtonSetting("Allow digging", false));
        this.registerSetting(this.allowInteracting = new ButtonSetting("Allow interacting", false));
        this.registerSetting(this.allowPlacing = new ButtonSetting("Allow placing", false));
        this.registerSetting(this.showArm = new ButtonSetting("Show arm", false));
    }

    @Override
    public void onEnable() {
        if (!mc.field_71439_g.field_70122_E) {
            this.disable();
        } else {
            freeEntity = new EntityOtherPlayerMP(mc.field_71441_e, mc.field_71439_g.func_146103_bH());
            freeEntity.func_82149_j(mc.field_71439_g);
            this.sAng[0] = freeEntity.field_70759_as = mc.field_71439_g.field_70759_as;
            this.sAng[1] = mc.field_71439_g.field_70125_A;
            freeEntity.func_70016_h(0.0, 0.0, 0.0);
            freeEntity.func_82142_c(true);
            mc.field_71441_e.func_73027_a(-8008, freeEntity);
            mc.func_175607_a(freeEntity);
        }
    }

    @Override
    public void onDisable() {
        if (freeEntity != null) {
            mc.func_175607_a(mc.field_71439_g);
            mc.field_71439_g.field_70177_z = mc.field_71439_g.field_70759_as = this.sAng[0];
            mc.field_71439_g.field_70125_A = this.sAng[1];
            mc.field_71441_e.func_72900_e(freeEntity);
            freeEntity = null;
        }
        this.lcc = new int[] { Integer.MAX_VALUE, 0 };
        int x = mc.field_71439_g.field_70176_ah;
        int z = mc.field_71439_g.field_70164_aj;
        for (int x2 = -1; x2 <= 1; x2++) {
            for (int z2 = -1; z2 <= 1; z2++) {
                int a = x + x2;
                int b = z + z2;
                mc.field_71441_e.func_147458_c(a * 16, 0, b * 16, a * 16 + 15, 256, b * 16 + 15);
            }
        }
    }

    @Override
    public void onUpdate() {
        if (this.disableOnDamage.isToggled() && mc.field_71439_g.field_70737_aN != 0) {
            this.disable();
        } else {
            double s = 0.215 * this.speed.getInput();
            if (Keyboard.isKeyDown(mc.field_71474_y.field_74351_w.func_151463_i())) {
                double rad = freeEntity.field_70759_as * (Math.PI / 180.0);
                double dx = -1.0 * Math.sin(rad) * s;
                double dz = Math.cos(rad) * s;
                freeEntity.field_70165_t += dx;
                freeEntity.field_70161_v += dz;
            }
            if (Keyboard.isKeyDown(mc.field_71474_y.field_74368_y.func_151463_i())) {
                double rad = freeEntity.field_70759_as * (Math.PI / 180.0);
                double dx = -1.0 * Math.sin(rad) * s;
                double dz = Math.cos(rad) * s;
                freeEntity.field_70165_t -= dx;
                freeEntity.field_70161_v -= dz;
            }
            if (Keyboard.isKeyDown(mc.field_71474_y.field_74370_x.func_151463_i())) {
                double rad = (freeEntity.field_70759_as - 90.0F) * (Math.PI / 180.0);
                double dx = -1.0 * Math.sin(rad) * s;
                double dz = Math.cos(rad) * s;
                freeEntity.field_70165_t += dx;
                freeEntity.field_70161_v += dz;
            }
            if (Keyboard.isKeyDown(mc.field_71474_y.field_74366_z.func_151463_i())) {
                double rad = (freeEntity.field_70759_as + 90.0F) * (Math.PI / 180.0);
                double dx = -1.0 * Math.sin(rad) * s;
                double dz = Math.cos(rad) * s;
                freeEntity.field_70165_t += dx;
                freeEntity.field_70161_v += dz;
            }
            if (Utils.jumpDown()) {
                freeEntity.field_70163_u += 0.93 * s;
            }
            if (Keyboard.isKeyDown(mc.field_71474_y.field_74311_E.func_151463_i())) {
                freeEntity.field_70163_u -= 0.93 * s;
            }
            if (this.lcc[0] != Integer.MAX_VALUE && (this.lcc[0] != freeEntity.field_70176_ah || this.lcc[1] != freeEntity.field_70164_aj)) {
                int x = freeEntity.field_70176_ah;
                int z = freeEntity.field_70164_aj;
                mc.field_71441_e.func_147458_c(x * 16, 0, z * 16, x * 16 + 15, 256, z * 16 + 15);
            }
            this.lcc[0] = freeEntity.field_70176_ah;
            this.lcc[1] = freeEntity.field_70164_aj;
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (Utils.nullCheck()) {
            if (!this.showArm.isToggled()) {
                mc.field_71439_g.field_71155_g = mc.field_71439_g.field_71164_i = 700.0F;
            }
            RenderUtils.renderEntity(mc.field_71439_g, 1, 0.0, 0.0, Color.green.getRGB(), false);
            RenderUtils.renderEntity(mc.field_71439_g, 2, 0.0, 0.0, Color.green.getRGB(), false);
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (Utils.nullCheck()) {
            MovingObjectType hitType = mc.field_71476_x == null ? MovingObjectType.MISS : mc.field_71476_x.field_72313_a;
            if (e.button == 0) {
                if (hitType == MovingObjectType.BLOCK && !this.allowDigging.isToggled() || hitType == MovingObjectType.ENTITY && !this.allowInteracting.isToggled()) {
                    e.setCanceled(true);
                }
            } else if (e.button == 1) {
                if (hitType == MovingObjectType.ENTITY) {
                    if (!this.allowInteracting.isToggled()) {
                        e.setCanceled(true);
                    }
                } else if (!this.allowPlacing.isToggled()) {
                    e.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (Utils.nullCheck()) {
            if (e.getPacket() instanceof C07PacketPlayerDigging && !this.allowDigging.isToggled() && this.isBlockDigging((C07PacketPlayerDigging) e.getPacket())) {
                e.setCanceled(true);
            } else if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && !this.allowPlacing.isToggled()) {
                e.setCanceled(true);
            } else if (e.getPacket() instanceof C02PacketUseEntity && !this.allowInteracting.isToggled()) {
                e.setCanceled(true);
            }
        }
    }

    private boolean isBlockDigging(C07PacketPlayerDigging packet) {
        Action action = packet.func_180762_c();
        return action == Action.START_DESTROY_BLOCK || action == Action.ABORT_DESTROY_BLOCK || action == Action.STOP_DESTROY_BLOCK;
    }
}
