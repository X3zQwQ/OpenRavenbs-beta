package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PreAttackEvent;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.event.PreSlotScrollEvent;
import keystrokesmod.event.SlotUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.render.BlockOverlay;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class BedAura extends Module {

    private static final String[] MODES = new String[] { "Assist", "Auto" };

    private final SliderSetting mode;

    private final SliderSetting fov;

    private final SliderSetting range;

    private final SliderSetting rate;

    private final SliderSetting speed;

    private final SliderSetting breakDelay;

    private final SliderSetting breakSpeed;

    private final ButtonSetting silentAim;

    private final ButtonSetting breakBed;

    private final ButtonSetting onlyOnDamage;

    private final ButtonSetting breakNearBlock;

    private final ButtonSetting prioritizeKillAura;

    private final GroupSetting swapGroup;

    private final ButtonSetting swapToTools;

    private final ButtonSetting switchBackWhenDone;

    private final ButtonSetting overrideSwapBack;

    private final ButtonSetting renderOutline;

    private final ColorSetting outlineColor;

    private static final int MS_PER_TICK = 50;

    private static final int LEGIT_DAMAGE_WINDOW_TICKS = 20;

    private static final double LEGIT_RAY_INSET = 0.05;

    private static final double LEGIT_MIN_FACE_ALIGNMENT = 0.08;

    private static final double BED_FIND_EXTRA_BLOCKS = 1.0;

    private final List<BlockPos[]> bedPairsCache = new ArrayList<>();

    private int scanCooldown;

    private int activeMode = -1;

    private int legitDamageTicksRemaining;

    private int legitDamageCountdownTick = Integer.MIN_VALUE;

    private int previousHurtTime;

    private boolean legitDamageAssistActive;

    private BlockPos targetPos;

    private Vec3 targetHitVec;

    private EnumFacing targetSide;

    private BlockPos legitBedFoot;

    private boolean miningActive;

    private boolean controlsInput;

    private int hotbarProgrammaticDepth;

    private boolean hasSwapped;

    private int previousSlot = -1;

    public BedAura() {
        super("BedAura", Module.category.player);
        this.registerSetting(this.mode = new SliderSetting("Mode", 1, MODES));
        this.registerSetting(this.breakSpeed = new SliderSetting("Break speed", "x", 1.0, 1.0, 2.0, 0.02));
        this.registerSetting(this.breakDelay = new SliderSetting("Break delay", "ms", 250.0, 0.0, 250.0, 50.0));
        this.registerSetting(this.fov = new SliderSetting("FOV", "", 180.0, 30.0, 360.0, 1.0));
        this.registerSetting(this.range = new SliderSetting("Range", " block", 4.5, 2.0, 6.0, 0.1));
        this.registerSetting(this.rate = new SliderSetting("Rate", "ms", 250.0, 50.0, 2000.0, 50.0));
        this.registerSetting(this.speed = new SliderSetting("Aim speed", 10.0, 1.0, 30.0, 1.0));
        this.registerSetting(this.silentAim = new ButtonSetting("Silent aim", false));
        this.registerSetting(this.onlyOnDamage = new ButtonSetting("Activate on damage", false));
        this.registerSetting(this.breakBed = new ButtonSetting("Break bed", false));
        this.registerSetting(this.breakNearBlock = new ButtonSetting("Break near block", true));
        this.registerSetting(this.prioritizeKillAura = new ButtonSetting("Prioritize KillAura", false));
        this.registerSetting(this.swapGroup = new GroupSetting("Swap"));
        this.registerSetting(this.swapToTools = new ButtonSetting(this.swapGroup, "Swap to tools", true));
        this.registerSetting(this.switchBackWhenDone = new ButtonSetting(this.swapGroup, "Switch back when done", true, "Swap to previous slot"));
        this.registerSetting(this.overrideSwapBack = new ButtonSetting(this.swapGroup, "Override swap back", true));
        this.registerSetting(this.renderOutline = new ButtonSetting("Render block outline", true));
        this.registerSetting(this.outlineColor = new ColorSetting("Outline color", 255, 64, 64, 229));
    }

    @Override
    public String getInfo() {
        return MODES[(int) this.mode.getInput()];
    }

    @Override
    public void guiUpdate() {
        boolean autoMode = this.mode.getInput() == 1.0;
        this.speed.setVisible(!autoMode, this);
        this.silentAim.setVisible(!autoMode, this);
        this.breakBed.setVisible(!autoMode, this);
        this.onlyOnDamage.setVisible(!autoMode, this);
        this.breakNearBlock.setVisible(autoMode, this);
        this.swapGroup.setVisible(autoMode, this);
        this.swapToTools.setVisible(autoMode, this);
        this.switchBackWhenDone.setVisible(autoMode && this.swapToTools.isToggled(), this);
        this.overrideSwapBack.setVisible(autoMode && this.swapToTools.isToggled(), this);
        this.outlineColor.setVisible(this.renderOutline.isToggled(), this);
    }

    @Override
    public void onDisable() {
        this.resetMining();
        this.bedPairsCache.clear();
        this.scanCooldown = 0;
        this.activeMode = -1;
        this.resetLegitDamageCountdown();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPrePlayerInteract(PrePlayerInteractEvent e) {
        this.applyMiningKeyState();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouse(MouseEvent e) {
        if (this.shouldSuppressManualMouse()) {
            if (e.button == 0 || e.button == 1) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPreAttack(PreAttackEvent e) {
        if (this.shouldSuppressManualMouse()) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSlotScroll(PreSlotScrollEvent e) {
        if (this.shouldSuppressManualMouse()) {
            if (this.hasSwapped && this.overrideSwapBack.isToggled() && Utils.nullCheck()) {
                int slot = Integer.compare(e.slot, 0);
                this.previousSlot = Math.floorMod(mc.field_71439_g.field_71071_by.field_70461_c - slot, InventoryPlayer.func_70451_h());
            }
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSlotUpdate(SlotUpdateEvent e) {
        if (this.shouldSuppressManualMouse() && this.hotbarProgrammaticDepth <= 0) {
            if (this.hasSwapped && this.overrideSwapBack.isToggled()) {
                this.previousSlot = e.slot;
            }
            e.setCanceled(true);
        }
    }

    private boolean shouldSuppressManualMouse() {
        return this.isAutoMode() && this.miningActive && this.isEnabled() && Utils.nullCheck() && mc.field_71462_r == null && this.canMineBlocks() && !this.shouldYieldToKillAura();
    }

    public void applyMiningKeyState() {
        if (this.canMineBlocks() && !this.shouldYieldToKillAura()) {
            boolean shouldBreak = this.isAutoMode() || this.breakBed.isToggled() && this.isLookingAtTarget();
            if (this.miningActive && shouldBreak && this.isEnabled() && Utils.nullCheck() && mc.field_71462_r == null) {
                int atk = mc.field_71474_y.field_74312_F.func_151463_i();
                this.controlsInput = true;
                if (this.isAutoMode()) {
                    KeyBinding.func_74510_a(atk, false);
                    KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), false);
                }
                KeyBinding.func_74510_a(atk, true);
            } else {
                this.restoreInput();
            }
        } else {
            if (this.miningActive) {
                this.resetMining();
            }
        }
    }

    public BlockPos getAuraTargetPos() {
        return this.miningActive && this.canMineBlocks() ? this.targetPos : null;
    }

    public boolean isActivelyMining() {
        return this.miningActive && (this.isAutoMode() || this.breakBed.isToggled() && this.isLookingAtTarget()) && this.isEnabled() && Utils.nullCheck() && mc.field_71462_r == null && this.canMineBlocks() && !this.shouldYieldToKillAura();
    }

    public boolean isPrioritizingKillAura() {
        return this.prioritizeKillAura.isToggled();
    }

    public boolean shouldOverrideFastMine() {
        return this.isActivelyMining();
    }

    public float getBreakSpeedMultiplier() {
        float multiplier = (float) this.breakSpeed.getInput();
        return multiplier > 1.0F ? multiplier : 1.0F;
    }

    public int getBreakDelayTicks() {
        return Math.max(0, Math.min(5, (int) (this.breakDelay.getInput() / 50.0)));
    }

    public float getAuraBreakProgress() {
        if (this.canMineBlocks() && this.miningActive && mc.field_71442_b != null) {
            IAccessorPlayerControllerMP pc = (IAccessorPlayerControllerMP) mc.field_71442_b;
            BlockPos currentBlock = pc.getCurrentBlock();
            return this.targetPos != null && currentBlock != null && this.targetPos.equals(currentBlock) ? pc.getCurBlockDamageMP() : 0.0F;
        } else {
            return 0.0F;
        }
    }

    public boolean shouldOverrideMouseOver() {
        return this.isEnabled() && this.miningActive && Utils.nullCheck() && this.canMineBlocks() && this.targetPos != null && this.targetHitVec != null && this.targetSide != null && !this.shouldYieldToKillAura();
    }

    public void modifyMouseOverFromGetMouseOver(float partialTicks) {
        if (this.shouldOverrideMouseOver()) {
            if (this.isAutoMode()) {
                if (mc.func_175606_aa() != null) {
                    MovingObjectPosition mop = new MovingObjectPosition(this.targetHitVec, this.targetSide, this.targetPos);
                    mc.field_71476_x = mop;
                    mc.field_147125_j = null;
                    EntityRenderer renderer = mc.field_71460_t;
                    if (renderer instanceof IAccessorEntityRenderer) {
                        ((IAccessorEntityRenderer) renderer).setPointedEntity(null);
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientRotation(ClientRotationEvent e) {
        int selectedMode = (int) this.mode.getInput();
        if (this.activeMode != selectedMode) {
            this.resetMining();
            this.bedPairsCache.clear();
            this.scanCooldown = 0;
            this.activeMode = selectedMode;
            this.resetLegitDamageCountdown();
        }
        if (!this.isEnabled() || !Utils.nullCheck() || mc.field_71462_r != null || !this.canMineBlocks()) {
            this.resetMining();
        } else if (this.shouldYieldToKillAura()) {
            this.resetMining();
        } else if (e.scriptRotations) {
            this.resetMining();
        } else if (!this.isAutoMode() && !this.legitConditionsMet()) {
            this.resetMining();
        } else {
            double reach = this.range.getInput();
            double reachSq = reach * reach;
            if (--this.scanCooldown <= 0) {
                this.scanCooldown = Math.max(1, (int) Math.round(this.rate.getInput() / 50.0));
                this.rebuildBedPairsCache(reach + 1.0);
            }
            if (this.bedPairsCache.isEmpty()) {
                if (!this.isAutoMode() && this.legitDamageAssistActive) {
                    this.resetLegitDamageCountdown();
                }
                this.resetMining();
            } else {
                BedAura.Choice best = this.isAutoMode() ? this.chooseBestTarget(reachSq) : this.chooseBestLegitTarget(reachSq);
                if (best == null) {
                    if (!this.isAutoMode() && this.legitDamageAssistActive) {
                        this.resetLegitDamageCountdown();
                    }
                    this.resetMining();
                } else {
                    this.targetPos = best.pos;
                    this.targetHitVec = best.hitVec;
                    this.targetSide = best.side;
                    this.miningActive = true;
                    if (!this.isAutoMode() && this.onlyOnDamage.isToggled()) {
                        this.legitDamageAssistActive = true;
                    }
                    if (this.isAutoMode()) {
                        this.equipBestHotbarTool(BlockUtils.getBlock(this.targetPos));
                    }
                    if (this.isAutoMode() || this.silentAim.isToggled()) {
                        float baseYaw = e.yaw != null ? e.yaw : RotationUtils.serverRotations[0];
                        float basePitch = e.pitch != null ? e.pitch : RotationUtils.serverRotations[1];
                        float[] targetRotations = RotationUtils.getRotationsToPoint(this.targetHitVec.field_72450_a, this.targetHitVec.field_72448_b, this.targetHitVec.field_72449_c, baseYaw, basePitch);
                        float[] r = this.isAutoMode() ? targetRotations : RotationUtils.smoothRotation(baseYaw, basePitch, targetRotations[0], targetRotations[1], (int) this.speed.getInput());
                        e.setYaw(r[0]);
                        e.setPitch(r[1]);
                    }
                }
            }
        }
    }

    @Override
    public void onUpdate() {
        if (this.isEnabled() && !this.isAutoMode() && !this.silentAim.isToggled() && this.miningActive && Utils.nullCheck() && mc.field_71462_r == null && this.canMineBlocks() && !this.shouldYieldToKillAura() && this.legitConditionsMet()) {
            float baseYaw = mc.field_71439_g.field_70177_z;
            float basePitch = mc.field_71439_g.field_70125_A;
            float[] targetRotations = RotationUtils.getRotationsToPoint(this.targetHitVec.field_72450_a, this.targetHitVec.field_72448_b, this.targetHitVec.field_72449_c, baseYaw, basePitch);
            float[] r = RotationUtils.smoothRotation(baseYaw, basePitch, targetRotations[0], targetRotations[1], (int) this.speed.getInput());
            mc.field_71439_g.field_70177_z = r[0];
            mc.field_71439_g.field_70125_A = r[1];
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        if (this.isEnabled() && this.renderOutline.isToggled() && this.miningActive && this.targetPos != null && Utils.nullCheck() && this.canMineBlocks()) {
            IBlockState st = mc.field_71441_e.func_180495_p(this.targetPos);
            Block b = st.func_177230_c();
            if (b != null && b != Blocks.field_150350_a) {
                int c = this.outlineColor.getColor();
                BlockOverlay.renderBlockOutline(this.targetPos, c, c, 2.0F, true);
            }
        }
    }

    private void resetMining() {
        this.miningActive = false;
        if (this.swapToTools.isToggled() && this.switchBackWhenDone.isToggled() && this.previousSlot != -1 && Utils.nullCheck()) {
            this.setSlot(this.previousSlot);
        }
        this.restoreInput();
        this.hotbarProgrammaticDepth = 0;
        this.targetPos = null;
        this.targetHitVec = null;
        this.targetSide = null;
        this.legitBedFoot = null;
        this.hasSwapped = false;
        this.previousSlot = -1;
    }

    private void restoreInput() {
        if (this.controlsInput) {
            KeyBinding.func_74510_a(mc.field_71474_y.field_74312_F.func_151463_i(), Mouse.isButtonDown(0));
            KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), Mouse.isButtonDown(1));
            this.controlsInput = false;
        }
    }

    private void rebuildBedPairsCache(double searchRange) {
        this.bedPairsCache.clear();
        Set<BlockPos> seenFeet = new HashSet<>();
        int ri = (int) Math.ceil(searchRange);
        BlockPos origin = new BlockPos(mc.field_71439_g);
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -ri; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    BlockPos p = origin.func_177982_a(dx, dy, dz);
                    BlockPos[] pair = this.footHeadPair(p);
                    if (pair != null) {
                        BlockPos foot = pair[0];
                        if (!seenFeet.contains(foot) && this.bedInSearchRange(pair, searchRange)) {
                            Vec3 center = this.bedCenter(pair);
                            if (this.inFov(center, (float) this.fov.getInput())) {
                                seenFeet.add(foot);
                                this.bedPairsCache.add(pair);
                            }
                        }
                    }
                }
            }
        }
        if (ModuleManager.bedwars != null) {
            ModuleManager.bedwars.removeOwnBedPair(this.bedPairsCache);
        }
    }

    private BlockPos[] footHeadPair(BlockPos at) {
        IBlockState st = mc.field_71441_e.func_180495_p(at);
        if (!(st.func_177230_c() instanceof BlockBed)) {
            return null;
        } else {
            EnumPartType part = (EnumPartType) st.func_177229_b(BlockBed.field_176472_a);
            EnumFacing facing = (EnumFacing) st.func_177229_b(BlockBed.field_176387_N);
            BlockPos foot = part == EnumPartType.FOOT ? at : at.func_177972_a(facing.func_176734_d());
            IBlockState footSt = mc.field_71441_e.func_180495_p(foot);
            if (!(footSt.func_177230_c() instanceof BlockBed)) {
                return null;
            } else if (footSt.func_177229_b(BlockBed.field_176472_a) != EnumPartType.FOOT) {
                return null;
            } else {
                EnumFacing footFacing = (EnumFacing) footSt.func_177229_b(BlockBed.field_176387_N);
                BlockPos head = foot.func_177972_a(footFacing);
                IBlockState hs = mc.field_71441_e.func_180495_p(head);
                if (!(hs.func_177230_c() instanceof BlockBed)) {
                    return null;
                } else if (hs.func_177229_b(BlockBed.field_176472_a) != EnumPartType.HEAD) {
                    return null;
                } else {
                    return hs.func_177229_b(BlockBed.field_176387_N) != footFacing ? null : new BlockPos[] { foot, head };
                }
            }
        }
    }

    private Vec3 bedCenter(BlockPos[] pair) {
        AxisAlignedBB a = BlockUtils.unionBlockBounds(pair[0], pair[1]);
        return new Vec3((a.field_72340_a + a.field_72336_d) * 0.5, (a.field_72338_b + a.field_72337_e) * 0.5, (a.field_72339_c + a.field_72334_f) * 0.5);
    }

    private boolean bedInSearchRange(BlockPos[] pair, double searchRadius) {
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        double r2 = searchRadius * searchRadius + 1.0E-4;
        AxisAlignedBB u = BlockUtils.unionBlockBounds(pair[0], pair[1]);
        Vec3 vec3 = RotationUtils.closestPointOnAabb(u, eye);
        if (eye.func_72436_e(vec3) <= r2) {
            return true;
        } else {
            Vec3 mid = new Vec3((u.field_72340_a + u.field_72336_d) * 0.5, (u.field_72338_b + u.field_72337_e) * 0.5, (u.field_72339_c + u.field_72334_f) * 0.5);
            return eye.func_72436_e(mid) <= r2;
        }
    }

    private boolean inFov(Vec3 worldPoint, float fovDeg) {
        if (fovDeg >= 360.0F) {
            return true;
        } else {
            Vec3 eyes = mc.field_71439_g.func_174824_e(1.0F);
            Vec3 look = mc.field_71439_g.func_70676_i(1.0F);
            Vec3 to = worldPoint.func_178788_d(eyes);
            double len = to.func_72433_c();
            if (len < 1.0E-6) {
                return true;
            } else {
                to = new Vec3(to.field_72450_a / len, to.field_72448_b / len, to.field_72449_c / len);
                double dot = look.field_72450_a * to.field_72450_a + look.field_72448_b * to.field_72448_b + look.field_72449_c * to.field_72449_c;
                double ang = Math.acos(MathHelper.func_151237_a(dot, -1.0, 1.0)) * (180.0 / Math.PI);
                return ang <= fovDeg * 0.5;
            }
        }
    }

    private BedAura.Choice chooseBestLegitTarget(double reachSq) {
        List<BlockPos[]> exposed = new ArrayList<>();
        for (BlockPos[] pair : this.bedPairsCache) {
            if (this.isBedExposed(pair)) {
                exposed.add(pair);
            }
        }
        this.sortBedsByEyeDistance(exposed);
        if (this.legitBedFoot != null) {
            for (BlockPos[] blockPosArray : exposed) {
                if (this.legitBedFoot.equals(blockPosArray[0])) {
                    BedAura.Choice locked = this.getLockedLegitChoice(blockPosArray, reachSq);
                    if (locked != null) {
                        return locked;
                    }
                    BedAura.Choice replacement = this.chooseLegitPoint(blockPosArray, reachSq);
                    if (replacement != null) {
                        return replacement;
                    }
                    break;
                }
            }
            this.legitBedFoot = null;
        }
        for (BlockPos[] blockPosArray2 : exposed) {
            BedAura.Choice choice = this.chooseLegitPoint(blockPosArray2, reachSq);
            if (choice != null) {
                this.legitBedFoot = blockPosArray2[0];
                return choice;
            }
        }
        return null;
    }

    private BedAura.Choice chooseLegitPoint(BlockPos[] pair, double reachSq) {
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        List<BedAura.Choice> candidates = new ArrayList<>();
        AxisAlignedBB axisAlignedBB = BlockUtils.unionBlockBounds(pair[0], pair[1]);
        this.addLegitBedCandidates(pair, axisAlignedBB, reachSq, candidates);
        return candidates.isEmpty() ? null : Collections.min(candidates, Comparator.<BedAura.Choice>comparingDouble(choice -> this.legitAimScore(choice, eye)).thenComparingDouble(choice -> eye.func_72436_e(choice.hitVec)));
    }

    private BedAura.Choice getLockedLegitChoice(BlockPos[] pair, double reachSq) {
        if (this.targetPos != null && this.targetHitVec != null && this.targetSide != null && (this.targetPos.equals(pair[0]) || this.targetPos.equals(pair[1]))) {
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            if (eye.func_72436_e(this.targetHitVec) > reachSq + 0.001) {
                return null;
            } else {
                Vec3 inside = this.targetHitVec.func_72441_c(-this.targetSide.func_82601_c() * 0.05, -this.targetSide.func_96559_d() * 0.05, -this.targetSide.func_82599_e() * 0.05);
                MovingObjectPosition trace = mc.field_71441_e.func_147447_a(eye, inside, false, false, false);
                return trace != null && trace.field_72313_a == MovingObjectType.BLOCK && this.targetPos.equals(trace.func_178782_a()) && trace.field_72307_f != null && trace.field_178784_b != null && this.isCenteredBedHit(trace.func_178782_a(), trace.field_72307_f, trace.field_178784_b) && !(this.faceAlignment(eye, trace.field_72307_f, trace.field_178784_b) < 0.08) ? new BedAura.Choice(this.targetPos, this.targetHitVec, this.targetSide) : null;
            }
        } else {
            return null;
        }
    }

    private void addLegitBedCandidates(BlockPos[] pair, AxisAlignedBB box, double reachSq, List<BedAura.Choice> out) {
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        double[] samples = new double[] { 0.5, 0.25, 0.75, 0.4, 0.6, 0.3, 0.7, 0.2, 0.8 };
        for (EnumFacing face : EnumFacing.values()) {
            if (face != EnumFacing.DOWN) {
                for (double first : samples) {
                    for (double second : samples) {
                        double x = (box.field_72340_a + box.field_72336_d) * 0.5;
                        double y = (box.field_72338_b + box.field_72337_e) * 0.5;
                        double z = (box.field_72339_c + box.field_72334_f) * 0.5;
                        if (face == EnumFacing.UP) {
                            x = this.interpolate(box.field_72340_a, box.field_72336_d, first);
                            y = box.field_72337_e;
                            z = this.interpolate(box.field_72339_c, box.field_72334_f, second);
                        } else if (face != EnumFacing.NORTH && face != EnumFacing.SOUTH) {
                            x = face == EnumFacing.WEST ? box.field_72340_a : box.field_72336_d;
                            y = this.interpolate(box.field_72338_b, box.field_72337_e, first);
                            z = this.interpolate(box.field_72339_c, box.field_72334_f, second);
                        } else {
                            x = this.interpolate(box.field_72340_a, box.field_72336_d, first);
                            y = this.interpolate(box.field_72338_b, box.field_72337_e, second);
                            z = face == EnumFacing.NORTH ? box.field_72339_c : box.field_72334_f;
                        }
                        this.addLegitBedCandidate(pair, face, new Vec3(x, y, z), eye, reachSq, out);
                    }
                }
            }
        }
    }

    private void addLegitBedCandidate(BlockPos[] pair, EnumFacing face, Vec3 point, Vec3 eye, double reachSq, List<BedAura.Choice> out) {
        Vec3 inside = point.func_72441_c(-face.func_82601_c() * 0.05, -face.func_96559_d() * 0.05, -face.func_82599_e() * 0.05);
        MovingObjectPosition trace = mc.field_71441_e.func_147447_a(eye, inside, false, false, false);
        if (trace != null && trace.field_72313_a == MovingObjectType.BLOCK && trace.field_72307_f != null && trace.field_178784_b != null && (pair[0].equals(trace.func_178782_a()) || pair[1].equals(trace.func_178782_a()))) {
            if (!(eye.func_72436_e(trace.field_72307_f) > reachSq + 0.001)) {
                if (this.isCenteredBedHit(trace.func_178782_a(), trace.field_72307_f, trace.field_178784_b)) {
                    if (!(this.faceAlignment(eye, trace.field_72307_f, trace.field_178784_b) < 0.08)) {
                        out.add(new BedAura.Choice(trace.func_178782_a(), trace.field_72307_f, trace.field_178784_b));
                    }
                }
            }
        }
    }

    private boolean isCenteredBedHit(BlockPos pos, Vec3 hit, EnumFacing side) {
        AxisAlignedBB box = BlockUtils.getBlockSelectionBox(pos);
        if (box == null) {
            return false;
        } else {
            double marginX = Math.min(0.15, (box.field_72336_d - box.field_72340_a) * 0.25);
            double marginY = Math.min(0.12, (box.field_72337_e - box.field_72338_b) * 0.25);
            double marginZ = Math.min(0.15, (box.field_72334_f - box.field_72339_c) * 0.25);
            boolean centeredX = hit.field_72450_a >= box.field_72340_a + marginX && hit.field_72450_a <= box.field_72336_d - marginX;
            boolean centeredY = hit.field_72448_b >= box.field_72338_b + marginY && hit.field_72448_b <= box.field_72337_e - marginY;
            boolean centeredZ = hit.field_72449_c >= box.field_72339_c + marginZ && hit.field_72449_c <= box.field_72334_f - marginZ;
            if (side != EnumFacing.UP && side != EnumFacing.DOWN) {
                return side != EnumFacing.NORTH && side != EnumFacing.SOUTH ? centeredY && centeredZ : centeredX && centeredY;
            } else {
                return centeredX && centeredZ;
            }
        }
    }

    private double legitAimScore(BedAura.Choice choice, Vec3 eye) {
        AxisAlignedBB box = BlockUtils.getBlockSelectionBox(choice.pos);
        if (box == null) {
            return Double.POSITIVE_INFINITY;
        } else {
            double x = (choice.hitVec.field_72450_a - (box.field_72340_a + box.field_72336_d) * 0.5) / (box.field_72336_d - box.field_72340_a);
            double y = (choice.hitVec.field_72448_b - (box.field_72338_b + box.field_72337_e) * 0.5) / (box.field_72337_e - box.field_72338_b);
            double z = (choice.hitVec.field_72449_c - (box.field_72339_c + box.field_72334_f) * 0.5) / (box.field_72334_f - box.field_72339_c);
            double alignment = this.faceAlignment(eye, choice.hitVec, choice.side);
            return x * x + y * y + z * z + (1.0 - alignment) * 0.75;
        }
    }

    private double faceAlignment(Vec3 eye, Vec3 hit, EnumFacing side) {
        double x = eye.field_72450_a - hit.field_72450_a;
        double y = eye.field_72448_b - hit.field_72448_b;
        double z = eye.field_72449_c - hit.field_72449_c;
        double length = Math.sqrt(x * x + y * y + z * z);
        return length < 1.0E-6 ? 1.0 : Math.max(0.0, (x * side.func_82601_c() + y * side.func_96559_d() + z * side.func_82599_e()) / length);
    }

    private double interpolate(double min, double max, double amount) {
        return min + (max - min) * amount;
    }

    private BedAura.Choice chooseBestTarget(double reachSq) {
        IAccessorPlayerControllerMP pc = (IAccessorPlayerControllerMP) mc.field_71442_b;
        float curProg = pc.getCurBlockDamageMP();
        BlockPos breaking = pc.getCurrentBlock();
        List<BlockPos[]> exposed = new ArrayList<>();
        List<BlockPos[]> covered = new ArrayList<>();
        for (BlockPos[] pair : this.bedPairsCache) {
            if (this.isBedExposed(pair)) {
                exposed.add(pair);
            } else {
                covered.add(pair);
            }
        }
        this.sortBedsByEyeDistance(exposed);
        this.sortBedsByEyeDistance(covered);
        BedAura.Choice c = this.pickBestOnClosestBedWithCandidates(exposed, reachSq, curProg, breaking);
        return c != null ? c : this.pickBestOnClosestBedWithCandidates(covered, reachSq, curProg, breaking);
    }

    private void sortBedsByEyeDistance(List<BlockPos[]> pairs) {
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        pairs.sort(Comparator.comparingDouble(p -> eye.func_72436_e(this.bedCenter(p))));
    }

    private BedAura.Choice pickBestOnClosestBedWithCandidates(List<BlockPos[]> sortedPairs, double reachSq, float curProg, BlockPos breaking) {
        for (BlockPos[] pair : sortedPairs) {
            List<BedAura.Choice> candidates = this.buildCandidates(pair, reachSq);
            if (!candidates.isEmpty()) {
                BedAura.Choice best = null;
                double bestScore = Double.POSITIVE_INFINITY;
                for (BedAura.Choice ch : candidates) {
                    double score = this.scoreChoice(ch, curProg, breaking);
                    if (score < bestScore) {
                        bestScore = score;
                        best = ch;
                    }
                }
                return best;
            }
        }
        return null;
    }

    private double scoreChoice(BedAura.Choice ch, float curProg, BlockPos breaking) {
        Block block = BlockUtils.getBlock(ch.pos);
        float bestHotbar = BlockUtils.maxDigRateAcrossSlots(block, InventoryPlayer.func_70451_h());
        if (bestHotbar <= 0.0F) {
            return Double.POSITIVE_INFINITY;
        } else {
            double timeEst = 1.0 / bestHotbar;
            if (breaking != null && breaking.equals(ch.pos) && curProg > 0.02F) {
                timeEst -= curProg * 12.0;
            }
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            return timeEst + eye.func_72436_e(ch.hitVec) * 0.002;
        }
    }

    private List<BedAura.Choice> buildCandidates(BlockPos[] pair, double reachSq) {
        List<BedAura.Choice> out = new ArrayList<>();
        if (!this.breakNearBlock.isToggled()) {
            for (BlockPos bp : pair) {
                this.addBlockCandidate(bp, reachSq, out);
            }
            return out;
        } else {
            boolean exposed = this.isBedExposed(pair);
            if (exposed) {
                for (BlockPos bp : pair) {
                    this.addBlockCandidate(bp, reachSq, out);
                }
            } else {
                Set<BlockPos> seen = new HashSet<>();
                for (BlockPos bp : pair) {
                    for (EnumFacing f : EnumFacing.values()) {
                        if (f != EnumFacing.DOWN) {
                            BlockPos n = bp.func_177972_a(f);
                            if (!seen.contains(n)) {
                                IBlockState st = mc.field_71441_e.func_180495_p(n);
                                Block b = st.func_177230_c();
                                if (!this.canHitThrough(b) && !(b instanceof BlockBed)) {
                                    float hard = b.func_176195_g(mc.field_71441_e, n);
                                    if (!(hard < 0.0F)) {
                                        seen.add(n);
                                        this.addBlockCandidate(n, reachSq, out);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return out;
        }
    }

    private boolean isBedExposed(BlockPos[] pair) {
        for (BlockPos bp : pair) {
            for (EnumFacing f : EnumFacing.values()) {
                BlockPos n = bp.func_177972_a(f);
                if (this.canHitThrough(mc.field_71441_e.func_180495_p(n).func_177230_c())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canHitThrough(Block block) {
        return block == Blocks.field_150350_a || block == Blocks.field_150411_aY || block == Blocks.field_150355_j || block == Blocks.field_150358_i || block == Blocks.field_150353_l || block == Blocks.field_150356_k || block == Blocks.field_150329_H || block == Blocks.field_150328_O || block == Blocks.field_150327_N || block == Blocks.field_150398_cm || block == Blocks.field_150330_I || block == Blocks.field_150395_bd || block == Blocks.field_150378_br || block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockFenceGate || block instanceof BlockPane;
    }

    private void addBlockCandidate(BlockPos pos, double reachSq, List<BedAura.Choice> out) {
        IBlockState st = mc.field_71441_e.func_180495_p(pos);
        Block block = st.func_177230_c();
        if (block != Blocks.field_150350_a) {
            float hard = block.func_176195_g(mc.field_71441_e, pos);
            if (!(hard < 0.0F)) {
                AxisAlignedBB bb = BlockUtils.getBlockSelectionBox(pos);
                if (bb != null) {
                    Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
                    Vec3 hit = RotationUtils.closestPointOnAabb(bb, eye);
                    if (!(eye.func_72436_e(hit) > reachSq + 0.001)) {
                        MovingObjectPosition trace = block.func_180636_a(mc.field_71441_e, pos, eye, hit.func_72441_c((hit.field_72450_a - eye.field_72450_a) * 0.01, (hit.field_72448_b - eye.field_72448_b) * 0.01, (hit.field_72449_c - eye.field_72449_c) * 0.01));
                        EnumFacing side = BlockUtils.facingFromBlockCenterToPoint(pos, hit);
                        if (trace != null && trace.field_72307_f != null && trace.field_178784_b != null && pos.equals(trace.func_178782_a())) {
                            hit = trace.field_72307_f;
                            side = trace.field_178784_b;
                        }
                        if (!(block instanceof BlockBed) || side != EnumFacing.DOWN) {
                            out.add(new BedAura.Choice(pos, hit, side));
                        }
                    }
                }
            }
        }
    }

    private void equipBestHotbarTool(Block block) {
        if (this.swapToTools.isToggled()) {
            int slot = Utils.getTool(block);
            if (slot >= 0) {
                if (this.previousSlot == -1 && slot != mc.field_71439_g.field_71071_by.field_70461_c) {
                    this.previousSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                }
                if (slot != mc.field_71439_g.field_71071_by.field_70461_c) {
                    this.setSlot(slot);
                }
            }
        }
    }

    private void setSlot(int slot) {
        if (this.swapToTools.isToggled() && slot != -1 && slot != mc.field_71439_g.field_71071_by.field_70461_c) {
            this.hotbarProgrammaticDepth++;
            try {
                mc.field_71439_g.field_71071_by.field_70461_c = slot;
                this.hasSwapped = true;
                ((IAccessorPlayerControllerMP) mc.field_71442_b).callSyncCurrentPlayItem();
            } finally {
                this.hotbarProgrammaticDepth--;
            }
        }
    }

    private boolean canMineBlocks() {
        return mc.field_71439_g.field_71075_bZ.field_75099_e && !mc.field_71439_g.field_71075_bZ.field_75098_d && !mc.field_71439_g.func_175149_v();
    }

    private boolean isAutoMode() {
        return this.mode.getInput() == 1.0;
    }

    private boolean legitConditionsMet() {
        if (!this.onlyOnDamage.isToggled()) {
            this.resetLegitDamageCountdown();
            return true;
        } else {
            int currentTick = mc.field_71439_g.field_70173_aa;
            if (this.legitDamageCountdownTick != currentTick) {
                int hurtTime = mc.field_71439_g.field_70737_aN;
                if (hurtTime <= 0 || this.previousHurtTime != 0 && hurtTime <= this.previousHurtTime) {
                    if (!this.legitDamageAssistActive && this.legitDamageTicksRemaining > 0) {
                        this.legitDamageTicksRemaining--;
                    }
                } else {
                    this.legitDamageTicksRemaining = 20;
                }
                this.previousHurtTime = hurtTime;
                this.legitDamageCountdownTick = currentTick;
            }
            return this.legitDamageAssistActive || this.legitDamageTicksRemaining > 0;
        }
    }

    private void resetLegitDamageCountdown() {
        this.legitDamageTicksRemaining = 0;
        this.legitDamageCountdownTick = Integer.MIN_VALUE;
        this.previousHurtTime = Utils.nullCheck() ? mc.field_71439_g.field_70737_aN : 0;
        this.legitDamageAssistActive = false;
    }

    private boolean isLookingAtTarget() {
        MovingObjectPosition mouseOver = mc.field_71476_x;
        return this.targetPos != null && mouseOver != null && mouseOver.field_72313_a == MovingObjectType.BLOCK && this.targetPos.equals(mouseOver.func_178782_a());
    }

    private boolean shouldYieldToKillAura() {
        return !this.prioritizeKillAura.isToggled() ? false : ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && KillAura.target != null;
    }

    private static final class Choice {

        final BlockPos pos;

        final Vec3 hitVec;

        final EnumFacing side;

        Choice(BlockPos pos, Vec3 hitVec, EnumFacing side) {
            this.pos = pos;
            this.hitVec = hitVec;
            this.side = side;
        }
    }
}
