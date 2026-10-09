package keystrokesmod.module.impl.combat;

import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class JumpReset extends Module {

    private SliderSetting chance;

    private ButtonSetting requireMouseDown;

    private ButtonSetting requireMovingForward;

    private ButtonSetting requireAim;

    private boolean setJump;

    private boolean ignoreNext;

    private int lastHurtTime;

    private double lastFallDistance;

    public JumpReset() {
        super("Jump Reset", Module.category.combat);
        this.liteModule = true;
        this.registerSetting(this.chance = new SliderSetting("Chance", "%", 80.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.requireMouseDown = new ButtonSetting("Require mouse down", false));
        this.registerSetting(this.requireMovingForward = new ButtonSetting("Require moving forward", true));
        this.registerSetting(this.requireAim = new ButtonSetting("Require aim", true));
        this.closetModule = true;
    }

    @Override
    public String getInfo() {
        return (int) this.chance.getInput() == 100 ? "" : (int) this.chance.getInput() + "%";
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        int hurtTime = mc.field_71439_g.field_70737_aN;
        boolean onGround = mc.field_71439_g.field_70122_E;
        if (onGround && this.lastFallDistance > 3.0 && !mc.field_71439_g.field_71075_bZ.field_75101_c) {
            this.ignoreNext = true;
        }
        if (hurtTime > this.lastHurtTime) {
            boolean mouseDown = mc.field_71474_y.field_74312_F.func_151470_d() || !this.requireMouseDown.isToggled();
            boolean aimingAt = !this.requireAim.isToggled() || this.checkAim();
            boolean forward = mc.field_71474_y.field_74351_w.func_151470_d() || !this.requireMovingForward.isToggled();
            boolean randomization = (int) this.chance.getInput() == 100 || Utils.randomizeDouble(0.0, 100.0) < this.chance.getInput();
            boolean fov = Utils.inFov(Utils.getDirection(), 330.0F, RotationUtils.deltaAngle(mc.field_71439_g.field_70159_w, mc.field_71439_g.field_70179_y));
            if (!this.ignoreNext && !mc.field_71439_g.func_70027_ad() && onGround && aimingAt && forward && mouseDown && randomization && !this.hasBadEffect() && fov) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(), this.setJump = true);
            }
            this.ignoreNext = false;
        }
        this.lastHurtTime = hurtTime;
        this.lastFallDistance = mc.field_71439_g.field_70143_R;
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (this.setJump && !Utils.jumpDown()) {
            KeyBinding.func_74510_a(mc.field_71474_y.field_74314_A.func_151463_i(), this.setJump = false);
        }
    }

    private boolean hasBadEffect() {
        PotionEffect jump = mc.field_71439_g.func_70660_b(Potion.field_76430_j);
        PotionEffect poison = mc.field_71439_g.func_70660_b(Potion.field_76436_u);
        PotionEffect wither = mc.field_71439_g.func_70660_b(Potion.field_82731_v);
        return jump != null || poison != null || wither != null;
    }

    private boolean checkAim() {
        MovingObjectPosition result = mc.field_71476_x;
        return result != null && result.field_72313_a == MovingObjectType.ENTITY && result.field_72308_g instanceof EntityPlayer;
    }
}
