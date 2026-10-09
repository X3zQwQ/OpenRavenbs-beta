package keystrokesmod.module.impl.combat;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFishingRod;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RodAimbot extends Module {

    private SliderSetting fov;

    private SliderSetting predicatedTicks;

    private SliderSetting distance;

    private ButtonSetting aimInvis;

    private ButtonSetting ignoreTeammates;

    public boolean rotate;

    private boolean rightClick;

    private EntityPlayer entity;

    public RodAimbot() {
        super("RodAimbot", Module.category.combat, 0);
        this.registerSetting(this.fov = new SliderSetting("FOV", 180.0, 30.0, 360.0, 4.0));
        this.registerSetting(this.predicatedTicks = new SliderSetting("Predicted ticks", 5.0, 0.0, 20.0, 1.0));
        this.registerSetting(this.distance = new SliderSetting("Distance", 6.0, 3.0, 30.0, 0.5));
        this.registerSetting(this.aimInvis = new ButtonSetting("Aim invis", false));
        this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", false));
    }

    @Override
    public void onDisable() {
        this.rotate = false;
        this.rightClick = false;
        this.entity = null;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent mouseEvent) {
        if (mouseEvent.button == 1 && mouseEvent.buttonstate && Utils.nullCheck() && mc.field_71462_r == null) {
            if (mc.field_71439_g.func_71045_bC() != null && mc.field_71439_g.func_71045_bC().func_77973_b() instanceof ItemFishingRod && mc.field_71439_g.field_71104_cf == null) {
                this.entity = this.getEntity();
                if (this.entity != null) {
                    mouseEvent.setCanceled(true);
                    this.rightClick = true;
                    this.rotate = true;
                }
            }
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (Utils.nullCheck()) {
            if (this.rightClick || this.rotate) {
                if (mc.field_71439_g.func_71045_bC() == null || !(mc.field_71439_g.func_71045_bC().func_77973_b() instanceof ItemFishingRod)) {
                    return;
                }
                float[] rotations = RotationUtils.getRotationsPredicated(this.entity, (int) this.predicatedTicks.getInput());
                if (rotations == null) {
                    return;
                }
                event.setYaw(rotations[0]);
                event.setPitch(rotations[1]);
                if (!this.rightClick && this.rotate) {
                    this.rotate = false;
                }
                if (this.rightClick) {
                    ((IAccessorMinecraft) mc).callRightClickMouse();
                    this.rightClick = false;
                }
            }
        }
    }

    private EntityPlayer getEntity() {
        for (EntityPlayer entityPlayer : mc.field_71441_e.field_73010_i) {
            if (entityPlayer != mc.field_71439_g && entityPlayer.field_70725_aQ == 0 && (this.aimInvis.isToggled() || !entityPlayer.func_82150_aj()) && !(mc.field_71439_g.func_70068_e(entityPlayer) > this.distance.getInput() * this.distance.getInput()) && !Utils.isFriended(entityPlayer)) {
                float n = (float) this.fov.getInput();
                if ((n == 360.0F || Utils.inFov(n, entityPlayer)) && !AntiBot.isBot(entityPlayer) && (!this.ignoreTeammates.isToggled() || !Utils.isTeammate(entityPlayer))) {
                    return entityPlayer;
                }
            }
        }
        return null;
    }
}
