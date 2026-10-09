package keystrokesmod.module.impl.movement;

import javax.vecmath.Vector2d;
import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Jump45 extends Module {

    private final float INPUT_DAMPING = 0.98F;

    private final float SPRINT_JUMP_BOOST = 0.2F;

    private final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);

    private final float TRIG_TABLE_STEP = 0.005493164F;

    private final int GCD_SEARCH_RADIUS = 4;

    private final int JUMP_YAW_SEARCH_RADIUS = 4;

    private final ButtonSetting holdingBlocks;

    private final ButtonSetting holdingThrowable;

    private final ButtonSetting holdingRod;

    private final ButtonSetting movingForward;

    private final ButtonSetting inLiquid;

    private final ButtonSetting targeting;

    private float takeoffYaw;

    private boolean rotateTakeoffPacket;

    public Jump45() {
        super("45 Jump", Module.category.movement);
        this.registerSetting(this.holdingBlocks = new ButtonSetting("Holding blocks", true));
        this.registerSetting(this.holdingThrowable = new ButtonSetting("Holding throwable", true));
        this.registerSetting(this.holdingRod = new ButtonSetting("Holding rod", true));
        this.registerSetting(this.movingForward = new ButtonSetting("Moving forward", true));
        this.registerSetting(this.inLiquid = new ButtonSetting("In liquid", false));
        this.registerSetting(this.targeting = new ButtonSetting("Targeting", false));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onJump(JumpEvent e) {
        boolean inEnabledLiquid = this.inLiquid.isToggled() && this.isInLiquid();
        if (mc.field_71439_g != null && mc.field_71439_g.field_71158_b != null && e.entity == mc.field_71439_g && e.applySprint() && (mc.field_71439_g.field_70122_E || inEnabledLiquid) && !RotationHelper.get().isActive() && this.canActivate() && (!this.targeting.isToggled() || this.isTargeting())) {
            float forward = mc.field_71439_g.field_71158_b.field_78900_b;
            float strafe = mc.field_71439_g.field_71158_b.field_78902_a;
            if (!(forward < 0.99F) && !(Math.abs(strafe) < 0.99F)) {
                Jump45.RotationPlan plan = this.createRotationPlan(strafe);
                if (plan != null) {
                    this.takeoffYaw = plan.packetYaw;
                    this.rotateTakeoffPacket = true;
                    e.setYaw(plan.jumpYaw);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent event) {
        if (this.rotateTakeoffPacket) {
            event.setYaw(this.takeoffYaw);
        }
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent event) {
        this.reset();
    }

    @Override
    public void onDisable() {
        this.reset();
    }

    private Jump45.RotationPlan createRotationPlan(float strafeInput) {
        float acceleration = this.getGroundAcceleration();
        if (Float.isFinite(acceleration) && !(acceleration <= 0.0F)) {
            Vector2d actualInput = this.getMovementAddition(mc.field_71439_g.field_70177_z, strafeInput * 0.98F, 0.98F, acceleration);
            double actualInputLength = actualInput.length();
            float predictedForward = 0.98F * acceleration;
            double predictedLength = 0.2F + predictedForward;
            if (!(actualInputLength <= 0.0) && !(predictedLength <= 0.0)) {
                double denominator = 2.0 * predictedLength * actualInputLength;
                double cosine = (predictedLength * predictedLength + actualInputLength * actualInputLength - 0.040000003F) / denominator;
                cosine = Math.max(-1.0, Math.min(1.0, cosine));
                float inputYaw = this.yawFromVector(actualInput, mc.field_71439_g.field_70177_z);
                float correction = (float) Math.toDegrees(Math.acos(cosine));
                float idealPacketYaw = inputYaw + Math.copySign(correction, strafeInput);
                float lastReportedYaw = ((IAccessorEntityPlayerSP) mc.field_71439_g).getLastReportedYaw();
                float sensitivity = mc.field_71474_y.field_74341_c * 0.6F + 0.2F;
                double gcd = sensitivity * sensitivity * sensitivity * 1.2;
                if (gcd <= 0.0) {
                    return this.evaluatePacketYaw(RotationHelper.unwrapYaw(idealPacketYaw, lastReportedYaw), actualInput);
                } else {
                    float delta = MathHelper.func_76142_g(idealPacketYaw - lastReportedYaw);
                    long closestStep = Math.round(delta / gcd);
                    Jump45.RotationPlan best = null;
                    for (int i = -4; i <= 4; i++) {
                        float packetYaw = lastReportedYaw + (float) ((closestStep + i) * gcd);
                        Jump45.RotationPlan candidate = this.evaluatePacketYaw(packetYaw, actualInput);
                        if (best == null || candidate.error < best.error) {
                            best = candidate;
                        }
                    }
                    return best;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private Jump45.RotationPlan evaluatePacketYaw(float packetYaw, Vector2d actualInput) {
        Vector2d predicted = this.getMovementAddition(packetYaw, 0.0F, 0.98F, this.getGroundAcceleration());
        predicted.add(this.getForwardVector(packetYaw, 0.2F));
        Vector2d requiredJump = new Vector2d(predicted.x - actualInput.x, predicted.y - actualInput.y);
        float baseJumpYaw = this.yawFromVector(requiredJump, packetYaw);
        Jump45.RotationPlan best = null;
        for (int i = -4; i <= 4; i++) {
            float jumpYaw = baseJumpYaw + i * 0.005493164F;
            Vector2d actual = new Vector2d(actualInput.x, actualInput.y);
            actual.add(this.getForwardVector(jumpYaw, 0.2F));
            double error = this.distance(actual, predicted);
            if (best == null || error < best.error) {
                best = new Jump45.RotationPlan(packetYaw, jumpYaw, error);
            }
        }
        return best;
    }

    private boolean canActivate() {
        boolean hasActivationFilter = this.holdingBlocks.isToggled() || this.holdingRod.isToggled() || this.holdingThrowable.isToggled() || this.movingForward.isToggled() || this.inLiquid.isToggled();
        if (!hasActivationFilter) {
            return true;
        } else if (this.movingForward.isToggled() && !Utils.isBindDown(mc.field_71474_y.field_74351_w)) {
            return true;
        } else if (this.inLiquid.isToggled() && this.isInLiquid()) {
            return true;
        } else {
            ItemStack held = mc.field_71439_g.func_70694_bm();
            if (held == null) {
                return false;
            } else {
                Item item = held.func_77973_b();
                return this.holdingBlocks.isToggled() && item instanceof ItemBlock || this.holdingRod.isToggled() && item instanceof ItemFishingRod || this.holdingThrowable.isToggled() && (item instanceof ItemSnowball || item instanceof ItemEgg || item instanceof ItemEnderPearl || item == Items.field_151062_by || item instanceof ItemPotion && ItemPotion.func_77831_g(held.func_77960_j()));
            }
        }
    }

    private boolean isInLiquid() {
        return mc.field_71439_g != null && (mc.field_71439_g.func_70090_H() || mc.field_71439_g.func_180799_ab());
    }

    private boolean isTargeting() {
        return KillAura.target != null ? true : mc.field_71474_y.field_74312_F.func_151470_d() && mc.field_71476_x != null && mc.field_71476_x.field_72308_g != null;
    }

    private float getGroundAcceleration() {
        BlockPos underPlayer = new BlockPos(MathHelper.func_76128_c(mc.field_71439_g.field_70165_t), MathHelper.func_76128_c(mc.field_71439_g.func_174813_aQ().field_72338_b) - 1, MathHelper.func_76128_c(mc.field_71439_g.field_70161_v));
        Block block = mc.field_71441_e.func_180495_p(underPlayer).func_177230_c();
        float friction = block.field_149765_K * 0.91F;
        return mc.field_71439_g.func_70689_ay() * (0.16277136F / (friction * friction * friction));
    }

    private Vector2d getMovementAddition(float yaw, float strafe, float forward, float acceleration) {
        float length = strafe * strafe + forward * forward;
        if (length < 1.0E-4F) {
            return new Vector2d(0.0, 0.0);
        } else {
            length = MathHelper.func_76129_c(length);
            if (length < 1.0F) {
                length = 1.0F;
            }
            float scale = acceleration / length;
            strafe *= scale;
            forward *= scale;
            float sin = MathHelper.func_76126_a(yaw * (float) (Math.PI / 180.0));
            float cos = MathHelper.func_76134_b(yaw * (float) (Math.PI / 180.0));
            return new Vector2d(strafe * cos - forward * sin, forward * cos + strafe * sin);
        }
    }

    private Vector2d getForwardVector(float yaw, float magnitude) {
        float radians = yaw * (float) (Math.PI / 180.0);
        return new Vector2d(-MathHelper.func_76126_a(radians) * magnitude, MathHelper.func_76134_b(radians) * magnitude);
    }

    private float yawFromVector(Vector2d vector, float referenceYaw) {
        float yaw = (float) Math.toDegrees(Math.atan2(-vector.x, vector.y));
        return RotationHelper.unwrapYaw(yaw, referenceYaw);
    }

    private void reset() {
        this.rotateTakeoffPacket = false;
        this.takeoffYaw = 0.0F;
    }

    private double distance(Vector2d self, Vector2d other) {
        double deltaX = self.x - other.x;
        double deltaZ = self.y - other.y;
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    private static final class RotationPlan {

        private final float packetYaw;

        private final float jumpYaw;

        private final double error;

        private RotationPlan(float packetYaw, float jumpYaw, double error) {
            this.packetYaw = packetYaw;
            this.jumpYaw = jumpYaw;
            this.error = error;
        }
    }
}
