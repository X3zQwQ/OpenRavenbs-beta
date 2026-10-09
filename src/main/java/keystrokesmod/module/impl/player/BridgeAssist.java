package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.PreSlotScrollEvent;
import keystrokesmod.event.RightClickMouseEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.SimulatedPlayer;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BridgeAssist extends Module {

    private static final EnumFacing[] SIDES = new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST };

    private final SliderSetting edgeOffset;

    private final SliderSetting unsneakDelay;

    private final SliderSetting sneakOnJump;

    private final ButtonSetting avoidDoubleSneaking;

    private final ButtonSetting sneakKeyPressed;

    private final ButtonSetting holdingBlocks;

    private final ButtonSetting lookingDown;

    private final ButtonSetting notMovingForward;

    private final ButtonSetting prePlace;

    private final ButtonSetting disableHotbarScrolling;

    private boolean sneakingFromModule;

    private boolean placed;

    private boolean forceRelease;

    private boolean cancelRightClickThisTick;

    private int rightClickGuard;

    private int sneakJumpDelayTicks = -1;

    private int sneakJumpStartTick = -1;

    private int unsneakDelayTicks = -1;

    private int unsneakStartTick = -1;

    public BridgeAssist() {
        super("Bridge Assist", Module.category.player);
        this.registerSetting(this.edgeOffset = new SliderSetting("Edge offset", " block", 0.0, 0.0, 0.3, 0.01));
        this.registerSetting(this.unsneakDelay = new SliderSetting("Unsneak delay", "ms", 50.0, 50.0, 300.0, 5.0));
        this.registerSetting(this.sneakOnJump = new SliderSetting("Sneak on jump", "ms", 0.0, 0.0, 500.0, 5.0));
        this.registerSetting(this.avoidDoubleSneaking = new ButtonSetting("Avoid double sneaking", false));
        this.registerSetting(this.prePlace = new ButtonSetting("Pre place", false));
        this.registerSetting(this.disableHotbarScrolling = new ButtonSetting("Disable hotbar scrolling", false));
        this.registerSetting(new DescriptionSetting("Conditions"));
        this.registerSetting(this.sneakKeyPressed = new ButtonSetting("Sneak key pressed", false));
        this.registerSetting(this.holdingBlocks = new ButtonSetting("Holding blocks", false));
        this.registerSetting(this.lookingDown = new ButtonSetting("Looking down", false));
        this.registerSetting(this.notMovingForward = new ButtonSetting("Not moving forward", false));
        this.closetModule = true;
        this.liteModule = true;
    }

    @Override
    public String getInfo() {
        double offset = this.edgeOffset.getInput();
        return offset == Math.rint(offset) ? Integer.toString((int) offset) : Double.toString(Utils.round(offset, 2));
    }

    @Override
    public void onDisable() {
        this.sneakingFromModule = false;
        this.cancelRightClickThisTick = false;
        this.rightClickGuard = 0;
        this.resetUnsneak();
    }

    @SubscribeEvent
    public void onPrePlayerInput(PrePlayerInputEvent e) {
        this.cancelRightClickThisTick = false;
        if (!this.avoidDoubleSneaking.isToggled()) {
            this.rightClickGuard = 0;
        } else if (mc.field_71415_G && this.rightClickGuard > 0) {
            this.rightClickGuard--;
        }
        if (Utils.nullCheck() && mc.field_71462_r == null && !mc.field_71439_g.field_71075_bZ.field_75100_b) {
            boolean manualSneak = this.isManualSneak();
            boolean requireSneak = this.sneakKeyPressed.isToggled();
            if (manualSneak && !requireSneak) {
                this.resetUnsneak();
            } else if (!requireSneak || manualSneak && (e.getForward() != 0.0F || e.getStrafe() != 0.0F)) {
                if (this.notMovingForward.isToggled() && e.getForward() > 0.0F) {
                    this.clearSneak(e);
                } else if (this.lookingDown.isToggled() && mc.field_71439_g.field_70125_A < 70.0F) {
                    this.clearSneak(e);
                } else {
                    if (this.holdingBlocks.isToggled()) {
                        ItemStack held = mc.field_71439_g.func_70694_bm();
                        if (held == null || !(held.func_77973_b() instanceof ItemBlock)) {
                            this.clearSneak(e);
                            return;
                        }
                    }
                    if (!e.isJump() || !mc.field_71439_g.field_70122_E || e.getForward() == 0.0F && e.getStrafe() == 0.0F || !(this.sneakOnJump.getInput() > 0.0) || requireSneak && !this.forceRelease) {
                        SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(mc.field_71439_g.field_71158_b);
                        sim.movementInput.field_78899_d = false;
                        sim.tick();
                        double offset = this.computeEdgeOffset(sim.getEntityBoundingBox());
                        if (!Double.isNaN(offset)) {
                            if (offset > this.edgeOffset.getInput()) {
                                this.pressSneak(e, true);
                            } else if (this.sneakingFromModule) {
                                this.tryReleaseSneak(e, true);
                            }
                        } else {
                            if (e.isJump() && (this.sneakOnJump.getInput() <= 0.0 || e.getForward() == 0.0F && e.getStrafe() == 0.0F)) {
                                if (this.sneakingFromModule) {
                                    this.tryReleaseSneak(e, true);
                                }
                            } else if (mc.field_71439_g.field_70122_E) {
                                this.pressSneak(e, true);
                            } else if (this.sneakingFromModule) {
                                this.tryReleaseSneak(e, true);
                            }
                        }
                    } else {
                        this.sneakJumpStartTick = mc.field_71439_g.field_70173_aa;
                        double raw = this.sneakOnJump.getInput() / 50.0;
                        int base = (int) raw;
                        this.sneakJumpDelayTicks = base + (Math.random() < raw - base ? 1 : 0);
                        this.pressSneak(e, true);
                    }
                }
            } else {
                if (!manualSneak) {
                    this.resetUnsneak();
                }
                this.repressSneak(e);
            }
        }
    }

    @SubscribeEvent
    public void onRightClickMouse(RightClickMouseEvent e) {
        if (this.avoidDoubleSneaking.isToggled() && mc.field_71415_G && this.isAssistActive()) {
            ItemStack held = mc.field_71439_g.func_70694_bm();
            if (held != null && held.func_77973_b() instanceof ItemBlock && this.rightClickGuard != 20) {
                if (this.cancelRightClickThisTick && this.shouldCancelRightClick()) {
                    e.setCanceled(true);
                } else {
                    this.rightClickGuard = 20;
                }
            }
        }
    }

    private boolean shouldCancelRightClick() {
        MovingObjectPosition hit = mc.field_71476_x;
        if (hit == null || hit.field_72313_a != MovingObjectType.BLOCK || hit.field_178784_b == null || hit.field_178784_b == EnumFacing.UP || hit.field_178784_b == EnumFacing.DOWN || hit.field_72307_f == null || hit.func_178782_a() == null || !mc.field_71439_g.field_70122_E) {
            return false;
        } else if (mc.field_71474_y.field_74368_y.func_151470_d() && !mc.field_71474_y.field_74370_x.func_151470_d() && !mc.field_71474_y.field_74366_z.func_151470_d()) {
            float yawSector = Math.abs((float) (mc.field_71439_g.field_70177_z % 90.0));
            if (!(yawSector < 30.0F) && !(yawSector > 60.0F)) {
                Vec3 playerPosition = new Vec3(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v);
                Vec3 hitVec = hit.field_72307_f;
                double dx = hitVec.field_72450_a - playerPosition.field_72450_a;
                double dy = hitVec.field_72448_b - playerPosition.field_72448_b;
                double dz = hitVec.field_72449_c - playerPosition.field_72449_c;
                if (!(Math.abs(dx) > 1.0) && !(dy < -1.0) && !(dy > 0.0) && !(Math.abs(dz) > 1.0)) {
                    BlockPos blockPos = hit.func_178782_a();
                    double margin = playerPosition.field_72449_c > blockPos.func_177952_p() && playerPosition.field_72449_c < blockPos.func_177952_p() + 1.0 ? 0.25 : 0.15;
                    double fractionX = Math.abs(hitVec.field_72450_a % 1.0);
                    double fractionZ = Math.abs(hitVec.field_72449_c % 1.0);
                    return fractionX >= margin && fractionX <= 1.0 - margin || fractionZ >= margin && fractionZ <= 1.0 - margin;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement c08 = (C08PacketPlayerBlockPlacement) e.getPacket();
            if (c08.func_149568_f() != 255 && this.sneakingFromModule && this.sneakKeyPressed.isToggled()) {
                this.placed = true;
            }
        }
    }

    @SubscribeEvent
    public void onSlotScroll(PreSlotScrollEvent e) {
        if (this.disableHotbarScrolling.isToggled() && this.isAssistActive()) {
            e.setCanceled(true);
        }
    }

    private boolean isAssistActive() {
        if (this.isEnabled() && Utils.nullCheck() && mc.field_71462_r == null && !mc.field_71439_g.field_71075_bZ.field_75100_b) {
            float forward = mc.field_71439_g.field_71158_b.field_78900_b;
            float strafe = mc.field_71439_g.field_71158_b.field_78902_a;
            boolean manualSneak = this.isManualSneak();
            if (manualSneak && !this.sneakKeyPressed.isToggled()) {
                return false;
            } else if (!this.sneakKeyPressed.isToggled() || manualSneak && (forward != 0.0F || strafe != 0.0F)) {
                if (this.notMovingForward.isToggled() && forward > 0.0F) {
                    return false;
                } else if (this.lookingDown.isToggled() && mc.field_71439_g.field_70125_A < 70.0F) {
                    return false;
                } else {
                    if (this.holdingBlocks.isToggled()) {
                        ItemStack held = mc.field_71439_g.func_70694_bm();
                        if (held == null || !(held.func_77973_b() instanceof ItemBlock)) {
                            return false;
                        }
                    }
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent e) {
        if (this.prePlace.isToggled()) {
            if (Utils.nullCheck() && mc.field_71462_r == null && !mc.field_71439_g.field_71075_bZ.field_75100_b) {
                if (ModuleManager.bedAura == null || !ModuleManager.bedAura.shouldOverrideMouseOver()) {
                    ItemStack held = mc.field_71439_g.func_70694_bm();
                    if (held != null && held.func_77973_b() instanceof ItemBlock) {
                        if (!this.lookingDown.isToggled() || !(mc.field_71439_g.field_70125_A < 70.0F)) {
                            if (!this.notMovingForward.isToggled() || !(mc.field_71439_g.field_71158_b.field_78900_b > 0.0F)) {
                                float basePitch = e.pitch != null ? e.pitch : RotationUtils.serverRotations[1];
                                double reach = mc.field_71442_b.func_78757_d();
                                BridgeAssist.TargetResult target = this.findTarget(basePitch, reach);
                                if (target != null) {
                                    float baseYaw = e.yaw != null ? e.yaw : RotationUtils.serverRotations[0];
                                    float[] sm = RotationUtils.smoothRotation(baseYaw, basePitch, target.yaw, target.pitch, 15, 20.0F);
                                    e.setYaw(sm[0]);
                                    e.setPitch(sm[1]);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void pressSneak(PrePlayerInputEvent e, boolean resetDelay) {
        e.setSneak(true);
        this.sneakingFromModule = true;
        this.cancelRightClickThisTick = this.avoidDoubleSneaking.isToggled();
        if (resetDelay) {
            this.unsneakStartTick = -1;
        }
        this.repressSneak(e);
    }

    private void tryReleaseSneak(PrePlayerInputEvent e, boolean resetDelay) {
        int existed = mc.field_71439_g.field_70173_aa;
        if (this.unsneakStartTick == -1 && this.sneakJumpStartTick == -1) {
            this.unsneakStartTick = existed;
            double raw = (this.unsneakDelay.getInput() - 50.0) / 50.0;
            int base = (int) raw;
            this.unsneakDelayTicks = base + (Math.random() < raw - base ? 1 : 0);
        }
        if (this.sneakJumpStartTick != -1 && existed - this.sneakJumpStartTick < this.sneakJumpDelayTicks) {
            this.pressSneak(e, false);
        } else if (this.unsneakStartTick != -1 && existed - this.unsneakStartTick < this.unsneakDelayTicks) {
            this.pressSneak(e, false);
        } else {
            this.releaseSneak(e, resetDelay);
        }
    }

    private void releaseSneak(PrePlayerInputEvent e, boolean resetDelay) {
        if (!this.sneakKeyPressed.isToggled()) {
            e.setSneak(false);
        } else if (!this.sneakingFromModule || !this.isManualSneak() || !this.placed && mc.field_71439_g.field_70122_E) {
            if (this.forceRelease) {
                e.setSneak(false);
            }
        } else {
            KeyBinding.func_74510_a(mc.field_71474_y.field_74311_E.func_151463_i(), false);
            e.setSneak(false);
            this.forceRelease = true;
        }
        this.sneakingFromModule = false;
        this.placed = false;
        if (resetDelay) {
            this.resetUnsneak();
        }
    }

    private void repressSneak(PrePlayerInputEvent e) {
        if (this.forceRelease && this.isManualSneak()) {
            KeyBinding.func_74510_a(mc.field_71474_y.field_74311_E.func_151463_i(), true);
            e.setSneak(true);
        }
        this.forceRelease = false;
    }

    private void clearSneak(PrePlayerInputEvent e) {
        this.sneakingFromModule = false;
        this.resetUnsneak();
        if (this.sneakKeyPressed.isToggled()) {
            this.repressSneak(e);
        }
    }

    private void resetUnsneak() {
        this.unsneakStartTick = -1;
        this.sneakJumpStartTick = -1;
        this.sneakJumpDelayTicks = -1;
        this.unsneakDelayTicks = -1;
    }

    private boolean isManualSneak() {
        return Utils.isBindDown(mc.field_71474_y.field_74311_E);
    }

    private double computeEdgeOffset(AxisAlignedBB axisAlignedBB) {
        AxisAlignedBB groundCheck = new AxisAlignedBB(axisAlignedBB.field_72340_a, axisAlignedBB.field_72338_b - 0.51, axisAlignedBB.field_72339_c, axisAlignedBB.field_72336_d, axisAlignedBB.field_72338_b, axisAlignedBB.field_72334_f);
        List<AxisAlignedBB> groundBoxes = mc.field_71441_e.func_72945_a(mc.field_71439_g, groundCheck);
        if (groundBoxes.isEmpty()) {
            return Double.NaN;
        } else {
            double feetX = (axisAlignedBB.field_72340_a + axisAlignedBB.field_72336_d) / 2.0;
            double feetZ = (axisAlignedBB.field_72339_c + axisAlignedBB.field_72334_f) / 2.0;
            double minDist = Double.MAX_VALUE;
            for (AxisAlignedBB box : groundBoxes) {
                double closestX = Math.max(box.field_72340_a, Math.min(feetX, box.field_72336_d));
                double closestZ = Math.max(box.field_72339_c, Math.min(feetZ, box.field_72334_f));
                double dx = Math.abs(feetX - closestX);
                double dz = Math.abs(feetZ - closestZ);
                double dist = Math.max(dx, dz);
                minDist = Math.min(minDist, dist);
            }
            return minDist;
        }
    }

    private BridgeAssist.TargetResult findTarget(float currentPitch, double reach) {
        float yaw = mc.field_71439_g.field_70177_z;
        AxisAlignedBB axisAlignedBB2 = mc.field_71439_g.func_174813_aQ();
        int standY = MathHelper.func_76128_c(axisAlignedBB2.field_72338_b) - 1;
        int minX = MathHelper.func_76128_c(axisAlignedBB2.field_72340_a);
        int maxX = MathHelper.func_76128_c(axisAlignedBB2.field_72336_d);
        int minZ = MathHelper.func_76128_c(axisAlignedBB2.field_72339_c);
        int maxZ = MathHelper.func_76128_c(axisAlignedBB2.field_72334_f);
        ArrayList<BridgeAssist.FaceTarget> targets = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos standBlock = new BlockPos(x, standY, z);
                if (!BlockUtils.replaceable(standBlock)) {
                    for (EnumFacing face : SIDES) {
                        BlockPos placed = standBlock.func_177972_a(face);
                        if (BlockUtils.replaceable(placed)) {
                            targets.add(new BridgeAssist.FaceTarget(standBlock, face));
                        }
                    }
                }
            }
        }
        if (targets.isEmpty()) {
            return null;
        } else {
            float bestDelta = Float.MAX_VALUE;
            float bestPitch = Float.NaN;
            BlockPos bestSupport = null;
            EnumFacing bestFace = null;
            float randScale = 0.2F;
            float pitch = 60.0F;
            while (pitch <= 90.0F) {
                float step = 1.0F + (float) (Math.random() * 2.0 - 1.0) * (0.3F + randScale * 0.4F);
                if (step < 0.4F) {
                    step = 0.4F;
                }
                if (step > 1.8F) {
                    step = 1.8F;
                }
                pitch += step;
                float samplePitch = Math.min(pitch, 90.0F);
                MovingObjectPosition mop = RotationUtils.rayCastBlock(reach, yaw, samplePitch);
                if (mop != null) {
                    EnumFacing hitFace = mop.field_178784_b;
                    if (hitFace != EnumFacing.UP && hitFace != EnumFacing.DOWN) {
                        BlockPos hitBlock = mop.func_178782_a();
                        Iterator iterator2 = targets.iterator();
                        while (true) {
                            if (iterator2.hasNext()) {
                                BridgeAssist.FaceTarget t = (BridgeAssist.FaceTarget) iterator2.next();
                                if (!hitBlock.equals(t.block) || hitFace != t.face) {
                                    continue;
                                }
                                float delta = Math.abs(samplePitch - currentPitch);
                                if (delta < bestDelta) {
                                    bestDelta = delta;
                                    bestPitch = samplePitch;
                                    bestSupport = t.block;
                                    bestFace = t.face;
                                }
                            }
                            if (pitch >= 90.0F) {
                                return bestSupport != null && bestFace != null && !Float.isNaN(bestPitch) ? new BridgeAssist.TargetResult(yaw, bestPitch, bestSupport, bestFace) : null;
                            }
                            break;
                        }
                    }
                }
            }
            return bestSupport != null && bestFace != null && !Float.isNaN(bestPitch) ? new BridgeAssist.TargetResult(yaw, bestPitch, bestSupport, bestFace) : null;
        }
    }

    private static class FaceTarget {

        final BlockPos block;

        final EnumFacing face;

        FaceTarget(BlockPos block, EnumFacing face) {
            this.block = block;
            this.face = face;
        }
    }

    private static class TargetResult {

        final float yaw;

        final float pitch;

        final BlockPos support;

        final EnumFacing face;

        TargetResult(float yaw, float pitch, BlockPos support, EnumFacing face) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.support = support;
            this.face = face;
        }
    }
}
