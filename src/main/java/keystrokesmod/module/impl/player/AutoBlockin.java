package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ItemListSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockWall;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class AutoBlockin extends Module {

    private static final EnumFacing[] HORIZONTALS = new EnumFacing[] { EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.NORTH };

    private static final AutoBlockin.SupportOffset[] SUPPORTS = new AutoBlockin.SupportOffset[] { new AutoBlockin.SupportOffset(0, 1, 0, EnumFacing.DOWN), new AutoBlockin.SupportOffset(0, -1, 0, EnumFacing.UP), new AutoBlockin.SupportOffset(0, 0, -1, EnumFacing.SOUTH), new AutoBlockin.SupportOffset(0, 0, 1, EnumFacing.NORTH), new AutoBlockin.SupportOffset(1, 0, 0, EnumFacing.WEST), new AutoBlockin.SupportOffset(-1, 0, 0, EnumFacing.EAST) };

    private static final double REACH = 4.5;

    private static final double GRID_INSET = 0.05;

    private static final double GRID_STEP = 0.2;

    private static final int GRID_N = (int) Math.round(5.0);

    private final SliderSetting speed;

    private final SliderSetting randomization;

    private final SliderSetting rotationTol;

    private final SliderSetting showProgress;

    private final ButtonSetting disableInCreative;

    private final ButtonSetting skipNearBed;

    private final KeySetting activationKey;

    private final ButtonSetting ignoreBlocksToggle;

    private final ItemListSetting ignoredBlocks;

    private boolean placing;

    private boolean slotWasSwapped;

    private int prevSlot = -1;

    private int plannedSlot = -1;

    private boolean placeQueued;

    private BlockPos targetHitPos;

    private EnumFacing targetSide;

    private float aimYaw;

    private float aimPitch;

    private BlockPos hitAt;

    private EnumFacing hitSide;

    private Vec3 placeAt;

    private float fillCount;

    private float lastFillCount = -1.0F;

    private float circleProgress;

    private float animStartProgress;

    private float animTargetProgress;

    private long animStartTime;

    private Timer progressFadeTimer;

    private Timer progressFadeInTimer;

    private float previousProgressAlpha;

    private boolean lastTargetAdjacent;

    private float fillTargetCount;

    private float lastFillTargetCount = -1.0F;

    public AutoBlockin() {
        super("AutoBlockin", Module.category.player);
        this.liteModule = true;
        this.registerSetting(this.speed = new SliderSetting("Speed", 10.0, 1.0, 30.0, 1.0));
        this.registerSetting(this.randomization = new SliderSetting("Randomization", "%", 10.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.rotationTol = new SliderSetting("Rotation tolerance", "°", 25.0, 20.0, 100.0, 1.0));
        this.registerSetting(this.showProgress = new SliderSetting("Show progress", true, 0, new String[] { "Circle", "Percentage" }));
        this.registerSetting(this.disableInCreative = new ButtonSetting("Disable in creative", true));
        this.registerSetting(this.skipNearBed = new ButtonSetting("Skip near bed", true));
        this.registerSetting(this.activationKey = new KeySetting("Activation key", 0));
        this.registerSetting(this.ignoreBlocksToggle = new ButtonSetting("Ignore blocks", false));
        this.registerSetting(this.ignoredBlocks = new ItemListSetting("Items"));
        this.closetModule = true;
    }

    @Override
    public void onDisable() {
        this.disablePlacing();
        this.placeQueued = false;
        this.fillCount = 0.0F;
        this.fillTargetCount = 0.0F;
        this.lastFillCount = -1.0F;
        this.lastFillTargetCount = -1.0F;
        this.circleProgress = 0.0F;
        this.resetProgressFade();
    }

    @Override
    public void guiUpdate() {
        this.ignoredBlocks.setVisible(this.ignoreBlocksToggle.isToggled(), this);
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent e) {
        if (Utils.nullCheck()) {
            if (!this.disableInCreative.isToggled() || !mc.field_71439_g.field_71075_bZ.field_75098_d) {
                if (ModuleManager.bedAura == null || !ModuleManager.bedAura.shouldOverrideMouseOver()) {
                    this.runTargetSelection();
                    if (mc.field_71462_r != null) {
                        this.disablePlacing();
                    }
                    if (this.placing && this.targetHitPos != null) {
                        float baseYaw = e.yaw != null ? e.yaw : RotationUtils.serverRotations[0];
                        float basePitch = e.pitch != null ? e.pitch : RotationUtils.serverRotations[1];
                        float[] sm = RotationUtils.smoothRotation(baseYaw, basePitch, this.aimYaw, this.aimPitch, (int) this.speed.getInput(), (float) this.randomization.getInput());
                        double r = 4.5;
                        MovingObjectPosition mop = RotationUtils.rayCastBlock(r, RotationUtils.serverRotations[0], RotationUtils.serverRotations[1]);
                        if (mop != null) {
                            BlockPos hitBlock = mop.func_178782_a();
                            EnumFacing side = mop.field_178784_b;
                            if (hitBlock.equals(this.targetHitPos) && side == this.targetSide) {
                                double tol = this.rotationTol.getInput();
                                if (Math.abs(MathHelper.func_76142_g(sm[0] - RotationUtils.serverRotations[0])) <= tol && Math.abs(sm[1] - RotationUtils.serverRotations[1]) <= tol) {
                                    this.hitAt = hitBlock;
                                    this.hitSide = side;
                                    this.placeAt = mop.field_72307_f;
                                    this.placeQueued = true;
                                }
                            }
                        }
                        RotationHelper.get().forceMovementFix = true;
                        e.setYaw(sm[0]);
                        e.setPitch(sm[1]);
                    }
                }
            }
        }
    }

    private void runTargetSelection() {
        this.clearAim();
        if (this.activationKey.isPressed() && mc.field_71462_r == null) {
            int strongSlot = this.pickBlockSlot(true);
            int weakSlot = this.pickBlockSlot(false);
            if (strongSlot == -1 && weakSlot == -1) {
                this.disablePlacing();
            } else {
                this.plannedSlot = strongSlot != -1 ? strongSlot : weakSlot;
                if (!this.getTarget()) {
                    this.disablePlacing();
                } else {
                    if (this.lastTargetAdjacent) {
                        this.plannedSlot = strongSlot != -1 ? strongSlot : weakSlot;
                    } else {
                        this.plannedSlot = weakSlot != -1 ? weakSlot : strongSlot;
                    }
                    if (!this.placing) {
                        this.enablePlacing();
                    }
                    if (mc.field_71474_y.field_74312_F.func_151470_d() || mc.field_71474_y.field_74313_G.func_151470_d()) {
                        this.clearAim();
                    }
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74312_F.func_151463_i(), false);
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), false);
                    this.equipPlannedSlot();
                }
            }
        } else {
            this.disablePlacing();
            this.circleProgress = 0.0F;
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (Utils.nullCheck()) {
            if (this.placeQueued) {
                this.placeQueued = false;
                if (this.hitAt != null && this.hitSide != null && this.placeAt != null) {
                    BlockPos placementPos = this.hitAt.func_177972_a(this.hitSide);
                    if (!this.isNearBed(placementPos) && mc.field_71442_b.func_178890_a(mc.field_71439_g, mc.field_71441_e, mc.field_71439_g.func_70694_bm(), this.hitAt, this.hitSide, this.placeAt)) {
                        mc.field_71439_g.func_71038_i();
                    }
                }
            }
            this.fillCount = 0.0F;
            this.fillTargetCount = 0.0F;
            if (this.activationKey.isPressed() && mc.field_71462_r == null) {
                BlockPos feet = new BlockPos(MathHelper.func_76128_c(mc.field_71439_g.field_70165_t), MathHelper.func_76128_c(mc.field_71439_g.field_70163_u), MathHelper.func_76128_c(mc.field_71439_g.field_70161_v));
                this.countProgressPosition(feet.func_177984_a().func_177984_a());
                for (EnumFacing dir : HORIZONTALS) {
                    BlockPos lowerSide = feet.func_177972_a(dir);
                    this.countProgressPosition(lowerSide);
                    this.countProgressPosition(lowerSide.func_177984_a());
                }
                if (this.fillCount != this.lastFillCount || this.fillTargetCount != this.lastFillTargetCount) {
                    this.animStartProgress = this.circleProgress;
                    if (this.fillTargetCount <= 0.0F) {
                        this.animTargetProgress = 0.0F;
                    } else {
                        this.animTargetProgress = Math.max(0.0F, Math.min(1.0F, this.fillCount / this.fillTargetCount));
                    }
                    this.animStartTime = System.currentTimeMillis();
                    this.lastFillCount = this.fillCount;
                    this.lastFillTargetCount = this.fillTargetCount;
                }
            } else {
                this.lastFillCount = -1.0F;
                this.lastFillTargetCount = -1.0F;
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent e) {
        if (e.phase == Phase.END && Utils.nullCheck()) {
            int progressMode = (int) this.showProgress.getInput();
            if (mc.field_71462_r != null || progressMode == -1) {
                this.resetProgressFade();
            } else if (this.disableInCreative.isToggled() && mc.field_71439_g.field_71075_bZ.field_75098_d) {
                this.resetProgressFade();
            } else {
                long elapsed = System.currentTimeMillis() - this.animStartTime;
                if (elapsed < 50L) {
                    float t = (float) elapsed / 50.0F;
                    this.circleProgress = lerp(this.animStartProgress, this.animTargetProgress, quadInOutEasing(t));
                } else {
                    this.circleProgress = this.animTargetProgress;
                }
                boolean hasValidProgress = this.fillTargetCount > 0.0F && this.fillCount > 0.0F;
                int alpha = this.updateProgressAlpha(hasValidProgress);
                if (alpha > 10) {
                    float ratio = Math.max(0.0F, Math.min(1.0F, this.circleProgress));
                    if (progressMode == 1) {
                        this.renderPercentage(ratio, alpha);
                    } else {
                        this.renderCircleProgress(ratio, alpha);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent e) {
        if (this.placing && e.button > -1) {
            e.setCanceled(true);
        }
    }

    private void enablePlacing() {
        if (!this.placing) {
            this.placing = true;
            this.slotWasSwapped = false;
            this.prevSlot = mc.field_71439_g.field_71071_by.field_70461_c;
        }
    }

    private int updateProgressAlpha(boolean shouldShow) {
        if (shouldShow) {
            if (this.progressFadeTimer != null) {
                this.progressFadeTimer = null;
                this.progressFadeInTimer = new Timer(150.0F);
                this.progressFadeInTimer.start();
            } else if (this.progressFadeInTimer == null && this.previousProgressAlpha < 255.0F) {
                this.progressFadeInTimer = new Timer(150.0F);
                this.progressFadeInTimer.start();
            }
            float alpha;
            if (this.progressFadeInTimer != null) {
                alpha = this.progressFadeInTimer.getValueFloat(10.0F, 255.0F, 1);
                if (alpha >= 255.0F) {
                    alpha = 255.0F;
                    this.progressFadeInTimer = null;
                }
            } else {
                alpha = 255.0F;
            }
            this.previousProgressAlpha = alpha;
            return (int) alpha;
        } else if (this.previousProgressAlpha <= 10.0F) {
            this.resetProgressFade();
            return 0;
        } else {
            if (this.progressFadeTimer == null) {
                this.progressFadeTimer = new Timer(150.0F);
                this.progressFadeTimer.start();
                this.progressFadeInTimer = null;
            }
            float alpha = 255.0F - this.progressFadeTimer.getValueInt(0, 255, 1);
            if (alpha <= 10.0F) {
                this.resetProgressFade();
                return 0;
            } else {
                this.previousProgressAlpha = alpha;
                return (int) alpha;
            }
        }
    }

    private void renderCircleProgress(float ratio, int alpha) {
        ScaledResolution resolution = new ScaledResolution(mc);
        float centerX = resolution.func_78326_a() / 2.0F + 0.5F;
        float centerY = resolution.func_78328_b() / 2.0F + 0.5F;
        float radius = 10.0F;
        float thickness = 3.0F;
        float alphaFloat = alpha / 255.0F;
        RenderUtils.draw2DCircle(centerX, centerY, radius, 100, thickness, 0.0F, 0.0F, 0.0F, alphaFloat * 0.5F);
        if (ratio >= 0.999F) {
            RenderUtils.draw2DCircle(centerX, centerY, radius, 100, thickness, 0.0F, 1.0F, 0.0F, alphaFloat);
        } else {
            int red = (int) ((1.0F - ratio) * 255.0F + 0.5F);
            int green = (int) (ratio * 255.0F + 0.5F);
            int color = (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8;
            float startAngle = 90.0F;
            float endAngle = startAngle + ratio * 360.0F + 0.5F;
            RenderUtils.draw2DCircleArc(centerX, centerY, radius, startAngle, endAngle, thickness, color);
        }
    }

    private void renderPercentage(float ratio, int alpha) {
        ScaledResolution resolution = new ScaledResolution(mc);
        String text = Math.round(ratio * 100.0F) + "%";
        float x = resolution.func_78326_a() / 2.0F - mc.field_71466_p.func_78256_a(text) / 2.0F;
        float y = resolution.func_78328_b() / 2.0F + 12.0F;
        int color = Utils.mergeAlpha(ratio >= 0.999F ? '\uff00' : 16777215, alpha);
        GL11.glPushMatrix();
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        mc.field_71466_p.func_175063_a(text, x, y, color);
        GL11.glDisable(3042);
        GL11.glPopMatrix();
    }

    private void resetProgressFade() {
        this.progressFadeTimer = null;
        this.progressFadeInTimer = null;
        this.previousProgressAlpha = 0.0F;
    }

    private void disablePlacing() {
        if (this.placing) {
            if (this.slotWasSwapped && this.prevSlot != -1 && this.prevSlot != mc.field_71439_g.field_71071_by.field_70461_c) {
                mc.field_71439_g.field_71071_by.field_70461_c = this.prevSlot;
            }
            this.placing = false;
            this.slotWasSwapped = false;
            this.prevSlot = -1;
            this.plannedSlot = -1;
            if (mc.field_71462_r == null) {
                KeyBinding.func_74510_a(mc.field_71474_y.field_74312_F.func_151463_i(), Mouse.isButtonDown(0));
                KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), Mouse.isButtonDown(1));
            }
        }
    }

    private void clearAim() {
        this.targetHitPos = null;
        this.targetSide = null;
    }

    private void equipPlannedSlot() {
        int cur = mc.field_71439_g.field_71071_by.field_70461_c;
        if (this.plannedSlot != -1 && this.plannedSlot != cur) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.plannedSlot;
            this.slotWasSwapped = true;
        }
    }

    private int pickBlockSlot(boolean preferStrong) {
        int best = -1;
        float bestScore = preferStrong ? -1.0F : Float.MAX_VALUE;
        for (int slot = 8; slot >= 0; slot--) {
            ItemStack s = mc.field_71439_g.field_71071_by.field_70462_a[slot];
            if (s != null && s.field_77994_a != 0 && s.func_77973_b() instanceof ItemBlock && (!this.ignoreBlocksToggle.isToggled() || !this.ignoredBlocks.matches(s))) {
                Block block = ((ItemBlock) s.func_77973_b()).func_179223_d();
                if (!(block instanceof BlockLadder)) {
                    float score = BlockUtils.getFistBreakTicks(block);
                    if (preferStrong ? score > bestScore : score < bestScore) {
                        bestScore = score;
                        best = slot;
                    }
                }
            }
        }
        return best;
    }

    private boolean getTarget() {
        AutoBlockin.AimResult result = this.roofAim();
        if (result == null) {
            result = this.sidesAim();
        }
        if (result == null) {
            return false;
        } else {
            BlockPos placed = result.supportBlock.func_177972_a(result.face);
            this.lastTargetAdjacent = this.isDirectAdjacentPlacement(placed);
            this.targetHitPos = result.supportBlock;
            this.targetSide = result.face;
            this.aimYaw = result.yaw;
            this.aimPitch = result.pitch;
            return true;
        }
    }

    private AutoBlockin.AimResult roofAim() {
        Vec3 pos = new Vec3(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v);
        BlockPos aboveHead = new BlockPos(MathHelper.func_76128_c(pos.field_72450_a), MathHelper.func_76128_c(pos.field_72448_b) + 2, MathHelper.func_76128_c(pos.field_72449_c));
        if (!BlockUtils.replaceable(aboveHead)) {
            return null;
        } else if (this.plannedSlot >= 0 && this.plannedSlot <= 8) {
            ItemStack held = mc.field_71439_g.field_71071_by.field_70462_a[this.plannedSlot];
            double r = 4.5;
            Vec3 eye = new Vec3(pos.field_72450_a, pos.field_72448_b + mc.field_71439_g.func_70047_e(), pos.field_72449_c);
            double r2 = r * r;
            double rp12 = (r + 1.0) * (r + 1.0);
            int minY = MathHelper.func_76128_c(eye.field_72448_b) + 1;
            int maxY = MathHelper.func_76128_c(eye.field_72448_b + r);
            int minX = MathHelper.func_76128_c(eye.field_72450_a - r);
            int maxX = MathHelper.func_76128_c(eye.field_72450_a + r);
            int minZ = MathHelper.func_76128_c(eye.field_72449_c - r);
            int maxZ = MathHelper.func_76128_c(eye.field_72449_c + r);
            ArrayList<AutoBlockin.BlockCandidate> cands = new ArrayList<>();
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        double dx = x + 0.5 - eye.field_72450_a;
                        double dy = y + 0.5 - eye.field_72448_b;
                        double dz = z + 0.5 - eye.field_72449_c;
                        if (!(dx * dx + dy * dy + dz * dz > rp12)) {
                            BlockPos bp = new BlockPos(x, y, z);
                            if (!BlockUtils.replaceable(bp)) {
                                Block block = BlockUtils.getBlock(bp);
                                if (!BlockUtils.isInteractable(block) && !(block instanceof BlockFence) && !(block instanceof BlockWall)) {
                                    double d2 = BlockUtils.dist2PointAABB(eye, bp);
                                    if (!(d2 > r2)) {
                                        cands.add(new AutoBlockin.BlockCandidate(d2, bp));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            cands.sort((a, b) -> Double.compare(a.dist, b.dist));
            for (AutoBlockin.BlockCandidate cand : cands) {
                AutoBlockin.AimResult res = this.getBestRotationsToBlock(held, cand.pos, eye, r, minY);
                if (res != null) {
                    return res;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private AutoBlockin.AimResult getBestRotationsToBlock(ItemStack held, BlockPos targetCell, Vec3 eye, double reachVal, int minY) {
        float baseYaw = RotationUtils.serverRotations[0];
        float basePitch = RotationUtils.serverRotations[1];
        boolean faceUp = Math.abs(eye.field_72448_b - (targetCell.func_177956_o() + 1)) < Math.abs(eye.field_72448_b - targetCell.func_177956_o());
        boolean faceSouth = Math.abs(eye.field_72449_c - (targetCell.func_177952_p() + 1)) < Math.abs(eye.field_72449_c - targetCell.func_177952_p());
        boolean faceEast = Math.abs(eye.field_72450_a - (targetCell.func_177958_n() + 1)) < Math.abs(eye.field_72450_a - targetCell.func_177958_n());
        double bx = targetCell.func_177958_n();
        double by = targetCell.func_177956_o();
        double bz = targetCell.func_177952_p();
        double jit = 0.020000000000000004;
        ArrayList<AutoBlockin.RotationCandidate> cands = new ArrayList<>((GRID_N + 1) * (GRID_N + 1) * 3 + 1);
        cands.add(new AutoBlockin.RotationCandidate(0.0, baseYaw, basePitch));
        for (int row = 0; row <= GRID_N; row++) {
            double v = clamp01(row * 0.2 + jitter(jit));
            for (int col = 0; col <= GRID_N; col++) {
                double u = clamp01(col * 0.2 + jitter(jit));
                float[] rY = RotationUtils.getRotationsFromEye(eye, bx + u, faceUp ? by + 1.0 - 0.05 : by + 0.05, bz + v);
                cands.add(new AutoBlockin.RotationCandidate(Math.abs(MathHelper.func_76142_g(rY[0] - baseYaw)) + Math.abs(rY[1] - basePitch), rY[0], rY[1]));
                float[] rZ = RotationUtils.getRotationsFromEye(eye, bx + u, by + v, faceSouth ? bz + 1.0 - 0.05 : bz + 0.05);
                cands.add(new AutoBlockin.RotationCandidate(Math.abs(MathHelper.func_76142_g(rZ[0] - baseYaw)) + Math.abs(rZ[1] - basePitch), rZ[0], rZ[1]));
                float[] rX = RotationUtils.getRotationsFromEye(eye, faceEast ? bx + 1.0 - 0.05 : bx + 0.05, by + v, bz + u);
                cands.add(new AutoBlockin.RotationCandidate(Math.abs(MathHelper.func_76142_g(rX[0] - baseYaw)) + Math.abs(rX[1] - basePitch), rX[0], rX[1]));
            }
        }
        cands.sort((a, b) -> Double.compare(a.cost, b.cost));
        int byY = targetCell.func_177956_o();
        for (AutoBlockin.RotationCandidate c : cands) {
            MovingObjectPosition mop = RotationUtils.rayCastBlock(reachVal, c.yaw, c.pitch);
            if (mop != null) {
                BlockPos hitBlock = mop.func_178782_a();
                EnumFacing face = mop.field_178784_b;
                if (hitBlock.equals(targetCell) && hitBlock.func_177956_o() >= minY && (face != EnumFacing.DOWN || byY != minY) && BlockUtils.canPlaceBlockOnSide(held, hitBlock, face)) {
                    BlockPos placementPos = hitBlock.func_177972_a(face);
                    if (!this.isNearBed(placementPos)) {
                        return new AutoBlockin.AimResult(hitBlock, face, c.yaw, c.pitch);
                    }
                }
            }
        }
        return null;
    }

    private void countProgressPosition(BlockPos pos) {
        if (!this.isNearBed(pos)) {
            this.fillTargetCount++;
            if (!BlockUtils.replaceable(pos)) {
                this.fillCount++;
            }
        }
    }

    private AutoBlockin.AimResult sidesAim() {
        BlockPos feet = new BlockPos(MathHelper.func_76128_c(mc.field_71439_g.field_70165_t), MathHelper.func_76128_c(mc.field_71439_g.field_70163_u), MathHelper.func_76128_c(mc.field_71439_g.field_70161_v));
        BlockPos head = feet.func_177984_a();
        double r = 4.5;
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        ArrayList<BlockPos> baseline = new ArrayList<>(8);
        for (EnumFacing dir : HORIZONTALS) {
            baseline.add(feet.func_177972_a(dir));
            baseline.add(head.func_177972_a(dir));
        }
        ArrayList<BlockPos> primaryGoals = new ArrayList<>(baseline.size());
        for (BlockPos pos : baseline) {
            if (BlockUtils.replaceable(pos) && this.hasReplaceableNeighbor(pos, feet, head)) {
                primaryGoals.add(pos);
            }
        }
        if (primaryGoals.isEmpty()) {
            return null;
        } else {
            Vec3 enemyPos = Utils.getClosestPlayerPos(100.0);
            if (enemyPos != null) {
                baseline.sort((a, b) -> {
                    double da = sq(a.func_177958_n() + 0.5 - enemyPos.field_72450_a) + sq(a.func_177956_o() + 0.5 - enemyPos.field_72448_b) + sq(a.func_177952_p() + 0.5 - enemyPos.field_72449_c);
                    double db = sq(b.func_177958_n() + 0.5 - enemyPos.field_72450_a) + sq(b.func_177956_o() + 0.5 - enemyPos.field_72448_b) + sq(b.func_177952_p() + 0.5 - enemyPos.field_72449_c);
                    return Double.compare(da, db);
                });
                int picked = 0;
                for (int i = 0; i < baseline.size() && picked < 3; i++) {
                    BlockPos blockPos = baseline.get(i);
                    if (BlockUtils.replaceable(blockPos) && this.hasReplaceableNeighbor(blockPos, feet, head)) {
                        AutoBlockin.AimResult rEnemy = this.findBestForGoals(Collections.singletonList(blockPos), r, eye);
                        if (rEnemy != null) {
                            return rEnemy;
                        }
                        picked++;
                    }
                }
            }
            AutoBlockin.AimResult result = this.findBestForGoals(primaryGoals, r, eye);
            if (result != null) {
                return result;
            } else {
                ArrayList<BlockPos> frontier = new ArrayList<>(primaryGoals);
                HashSet<Long> seen = new HashSet<>(frontier.size() * 8);
                for (BlockPos g : frontier) {
                    seen.add(g.func_177986_g());
                }
                for (int iter = 0; iter < 5 && !frontier.isEmpty(); iter++) {
                    ArrayList<BlockPos> layer = new ArrayList<>(frontier.size() * 3);
                    for (BlockPos g : frontier) {
                        for (EnumFacing f : EnumFacing.values()) {
                            BlockPos s = g.func_177972_a(f);
                            if (BlockUtils.replaceable(s) && seen.add(s.func_177986_g())) {
                                layer.add(s);
                            }
                        }
                    }
                    if (!layer.isEmpty()) {
                        AutoBlockin.AimResult rLayer = this.findBestForGoals(layer, r, eye);
                        if (rLayer != null) {
                            return rLayer;
                        }
                    }
                    frontier = layer;
                }
                return null;
            }
        }
    }

    private boolean hasReplaceableNeighbor(BlockPos pos, BlockPos... exclude) {
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos neighbor = pos.func_177972_a(facing);
            if (BlockUtils.replaceable(neighbor)) {
                boolean excluded = false;
                for (BlockPos excludedPos : exclude) {
                    if (neighbor.equals(excludedPos)) {
                        excluded = true;
                        break;
                    }
                }
                if (!excluded) {
                    return true;
                }
            }
        }
        return false;
    }

    private AutoBlockin.AimResult findBestForGoals(List<BlockPos> goals, double reachVal, Vec3 eye) {
        if (goals != null && !goals.isEmpty()) {
            if (this.plannedSlot >= 0 && this.plannedSlot <= 8) {
                ItemStack held = mc.field_71439_g.field_71071_by.field_70462_a[this.plannedSlot];
                float curYaw = RotationUtils.serverRotations[0];
                float curPitch = RotationUtils.serverRotations[1];
                MovingObjectPosition now = RotationUtils.rayCastBlock(reachVal, curYaw, curPitch);
                if (now != null) {
                    BlockPos support = now.func_178782_a();
                    EnumFacing faceHit = now.field_178784_b;
                    if (!BlockUtils.replaceable(support) && BlockUtils.canPlaceBlockOnSide(held, support, faceHit)) {
                        for (BlockPos goal : goals) {
                            AutoBlockin.AimResult ok = this.tryPlacement(reachVal, RotationUtils.serverRotations[0], RotationUtils.serverRotations[1], support, faceHit, goal);
                            if (ok != null) {
                                return ok;
                            }
                        }
                    }
                }
                double jit = 0.020000000000000004;
                double insetTop = 0.949;
                double insetBot = 0.051000000000000004;
                ArrayList<AutoBlockin.PlacementCandidate> cands = new ArrayList<>(Math.max(16, goals.size() * 6 * (GRID_N + 1) * (GRID_N + 1)));
                for (BlockPos g : goals) {
                    for (AutoBlockin.SupportOffset s : SUPPORTS) {
                        BlockPos support = new BlockPos(g.func_177958_n() + s.dx, g.func_177956_o() + s.dy, g.func_177952_p() + s.dz);
                        if (!BlockUtils.replaceable(support) && BlockUtils.canPlaceBlockOnSide(held, support, s.face)) {
                            double sx = support.func_177958_n();
                            double sy = support.func_177956_o();
                            double sz = support.func_177952_p();
                            for (int row = 0; row <= GRID_N; row++) {
                                boolean ltr = (row & 1) == 0;
                                double v = clamp01(row * 0.2 + jitter(jit));
                                for (int col = 0; col <= GRID_N; col++) {
                                    double cu = clamp01(col * 0.2 + jitter(jit));
                                    double u = ltr ? cu : 1.0 - cu;
                                    double px;
                                    double py;
                                    double pz;
                                    if (s.dy != 0) {
                                        px = sx + u;
                                        pz = sz + v;
                                        py = sy + (s.dy < 0 ? insetTop : insetBot);
                                    } else if (s.dz != 0) {
                                        px = sx + u;
                                        py = sy + v;
                                        pz = sz + (s.dz < 0 ? insetTop : insetBot);
                                    } else {
                                        pz = sz + u;
                                        py = sy + v;
                                        px = sx + (s.dx < 0 ? insetTop : insetBot);
                                    }
                                    float[] rot = RotationUtils.getRotationsFromEye(eye, px, py, pz);
                                    float dYaw = Math.abs(MathHelper.func_76142_g(rot[0] - curYaw));
                                    float dPit = Math.abs(rot[1] - curPitch);
                                    if (!(dYaw < 0.1F) || !(dPit < 0.1F)) {
                                        cands.add(new AutoBlockin.PlacementCandidate(dYaw + dPit, rot[0], rot[1], support, s.face, g));
                                    }
                                }
                            }
                        }
                    }
                }
                if (cands.isEmpty()) {
                    return null;
                } else {
                    cands.sort((a, b) -> Double.compare(a.cost, b.cost));
                    for (AutoBlockin.PlacementCandidate c : cands) {
                        AutoBlockin.AimResult ok = this.tryPlacement(reachVal, c.yaw, c.pitch, c.support, c.face, c.goal);
                        if (ok != null) {
                            return ok;
                        }
                    }
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private AutoBlockin.AimResult tryPlacement(double reachVal, float yaw, float pit, BlockPos expectedSupport, EnumFacing expectedFace, BlockPos goal) {
        MovingObjectPosition mop = RotationUtils.rayCastBlock(reachVal, yaw, pit);
        if (mop == null) {
            return null;
        } else {
            BlockPos hitBlock = mop.func_178782_a();
            EnumFacing faceHit = mop.field_178784_b;
            if (!hitBlock.equals(expectedSupport)) {
                return null;
            } else if (faceHit != expectedFace) {
                return null;
            } else {
                BlockPos placed = hitBlock.func_177972_a(faceHit);
                if (!placed.equals(goal)) {
                    return null;
                } else {
                    return this.isNearBed(placed) ? null : new AutoBlockin.AimResult(hitBlock, faceHit, yaw, pit);
                }
            }
        }
    }

    private boolean isDirectAdjacentPlacement(BlockPos p) {
        BlockPos feet = new BlockPos(MathHelper.func_76128_c(mc.field_71439_g.field_70165_t), MathHelper.func_76128_c(mc.field_71439_g.field_70163_u), MathHelper.func_76128_c(mc.field_71439_g.field_70161_v));
        int dx = p.func_177958_n() - feet.func_177958_n();
        int dy = p.func_177956_o() - feet.func_177956_o();
        int dz = p.func_177952_p() - feet.func_177952_p();
        return dx == 0 && dz == 0 && dy == 2 ? true : (dy == 0 || dy == 1) && (Math.abs(dx) == 1 && dz == 0 || Math.abs(dz) == 1 && dx == 0);
    }

    private boolean isNearBed(BlockPos placementPos) {
        if (!this.skipNearBed.isToggled()) {
            return false;
        } else if (BlockUtils.getBlock(placementPos) instanceof BlockBed) {
            return true;
        } else {
            for (EnumFacing facing : EnumFacing.values()) {
                if (BlockUtils.getBlock(placementPos.func_177972_a(facing)) instanceof BlockBed) {
                    return true;
                }
            }
            return false;
        }
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private static float quadInOutEasing(float t) {
        return t < 0.5F ? 2.0F * t * t : -1.0F + (4.0F - 2.0F * t) * t;
    }

    private static double sq(double v) {
        return v * v;
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    private static double jitter(double range) {
        return range > 0.0 ? (Math.random() * 2.0 - 1.0) * range : 0.0;
    }

    private static class AimResult {

        final BlockPos supportBlock;

        final EnumFacing face;

        final float yaw;

        final float pitch;

        AimResult(BlockPos supportBlock, EnumFacing face, float yaw, float pitch) {
            this.supportBlock = supportBlock;
            this.face = face;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static class BlockCandidate {

        final double dist;

        final BlockPos pos;

        BlockCandidate(double dist, BlockPos pos) {
            this.dist = dist;
            this.pos = pos;
        }
    }

    private static class PlacementCandidate {

        final double cost;

        final float yaw;

        final float pitch;

        final BlockPos support;

        final BlockPos goal;

        final EnumFacing face;

        PlacementCandidate(double cost, float yaw, float pitch, BlockPos support, EnumFacing face, BlockPos goal) {
            this.cost = cost;
            this.yaw = yaw;
            this.pitch = pitch;
            this.support = support;
            this.face = face;
            this.goal = goal;
        }
    }

    private static class RotationCandidate {

        final double cost;

        final float yaw;

        final float pitch;

        RotationCandidate(double cost, float yaw, float pitch) {
            this.cost = cost;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static class SupportOffset {

        final int dx;

        final int dy;

        final int dz;

        final EnumFacing face;

        SupportOffset(int dx, int dy, int dz, EnumFacing face) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
            this.face = face;
        }
    }
}
