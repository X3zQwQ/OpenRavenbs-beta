package keystrokesmod.module.impl.movement;

import java.awt.Color;
import java.util.ArrayList;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Teleport extends Module {

    private ButtonSetting rightClick;

    private ButtonSetting highlightTarget;

    private ButtonSetting highlightPath;

    private ButtonSetting instant;

    private BlockPos targetPos;

    private ArrayList<Vec3> path = new ArrayList<>();

    public Teleport() {
        super("Teleport", Module.category.movement);
        this.registerSetting(this.rightClick = new ButtonSetting("Right click teleport", true));
        this.registerSetting(this.highlightTarget = new ButtonSetting("Highlight target", true));
        this.registerSetting(this.highlightPath = new ButtonSetting("Highlight path", false));
        this.registerSetting(this.instant = new ButtonSetting("Instant", false));
    }

    public void teleport(BlockPos targetBlock, boolean sendMessage) {
        targetBlock = targetBlock.func_177981_b(1);
        int packetsSent = 0;
        if (!this.instant.isToggled()) {
            for (Vec3 pathPos : this.path = this.getPath(targetBlock)) {
                mc.func_147114_u().func_147297_a(new C04PacketPlayerPosition(pathPos.field_72450_a, pathPos.field_72448_b, pathPos.field_72449_c, true));
                if (++packetsSent >= 175) {
                    if (sendMessage) {
                        Utils.sendMessage("&eToo many packets, ending loop.");
                    }
                    break;
                }
            }
        }
        mc.field_71439_g.func_70107_b(targetBlock.func_177958_n(), targetBlock.func_177956_o(), targetBlock.func_177952_p());
        if (sendMessage) {
            Utils.sendMessage("&eTeleported to &d(" + targetBlock.func_177958_n() + ", " + targetBlock.func_177956_o() + ", " + targetBlock.func_177952_p() + ")" + (!this.instant.isToggled() ? " &ewith &b" + packetsSent + " &epackets." : "&e."));
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (this.rightClick.isToggled() && this.highlightTarget.isToggled() && this.targetPos != null && Utils.nullCheck()) {
            RenderUtils.renderBlock(this.targetPos, Color.orange.getRGB(), true, true);
            if (this.highlightPath.isToggled() && !this.instant.isToggled()) {
                int positions = 0;
                for (Vec3 pos : this.path) {
                    if (positions >= 175) {
                        break;
                    }
                    RenderUtils.renderBlock(new BlockPos(pos.field_72450_a, pos.field_72448_b, pos.field_72449_c), Color.yellow.getRGB(), false, true);
                    positions++;
                }
            }
        }
    }

    private ArrayList getPath(BlockPos target) {
        ArrayList<Vec3> path = new ArrayList<>();
        double newX = target.func_177958_n() + 0.5;
        double newY = target.func_177956_o() + 1;
        double newZ = target.func_177952_p() + 0.5;
        double distance = mc.field_71439_g.func_70011_f(newX, newY, newZ);
        for (double d = 0.0; d < distance; d += 2.0) {
            path.add(new Vec3(mc.field_71439_g.field_70165_t + (newX - mc.field_71439_g.func_174811_aO().func_82601_c() - mc.field_71439_g.field_70165_t) * d / distance, mc.field_71439_g.field_70163_u + (newY - mc.field_71439_g.field_70163_u) * d / distance, mc.field_71439_g.field_70161_v + (newZ - mc.field_71439_g.func_174811_aO().func_82599_e() - mc.field_71439_g.field_70161_v) * d / distance));
        }
        return path;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent mouseEvent) {
        if (mouseEvent.button == 1 && mouseEvent.buttonstate && this.rightClick.isToggled() && Utils.nullCheck()) {
            MovingObjectPosition rayCast = RotationUtils.rayCast(150.0, mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A, true);
            if (rayCast != null && rayCast.field_72313_a == MovingObjectType.BLOCK) {
                BlockPos getBlockPos = rayCast.func_178782_a();
                this.targetPos = getBlockPos;
                this.teleport(getBlockPos, true);
            }
        }
    }

    @Override
    public void onEnable() {
        this.targetPos = null;
        this.path.clear();
        if (!this.rightClick.isToggled()) {
            MovingObjectPosition rayCast = RotationUtils.rayCast(150.0, mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A, true);
            if (rayCast != null && rayCast.field_72313_a == MovingObjectType.BLOCK) {
                this.teleport(rayCast.func_178782_a(), true);
                this.disable();
            }
        }
    }
}
