package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.event.PreAttackEvent;
import keystrokesmod.event.PreInputEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.RightClickMouseEvent;
import keystrokesmod.helper.AutoLadderHelper;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.Timer;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.model.Simulation;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing.Plane;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoLadder extends Module {

    private static final int LOOKAHEAD_TICKS = 20;

    private static final int GROUND_LOOKAHEAD_TICKS = 40;

    private static final int GUIDE_TIMEOUT_TICKS = 26;

    private static final int GUIDE_INVALID_LIMIT = 4;

    private static final int STAGE_INVALID_TICK_LIMIT = 2;

    private final AutoLadderHelper session = new AutoLadderHelper();

    private int retryAfterTick;

    private boolean fallQualified;

    private final List<Vec3> fallPath = new ArrayList<>();

    private boolean predictedLanding;

    private World trackedWorld;

    private AutoLadder.RotationHit stableHit;

    private AutoLadder.ExecutionStage stableStage;

    private static final double MAX_PLACE_BELOW_PLAYER_Y = 3.0;

    private static final double MAX_PLACE_XZ_DELTA = 4.5;

    private static final double ENTRY_TICK_WEIGHT = 120.0;

    private static final double ENTRY_ALIGNMENT_WEIGHT = 90.0;

    private static final double TARGET_DISTANCE_WEIGHT = 10.0;

    private static final double[] HIT_OFFSETS = new double[] { 0.5, 0.35, 0.65, 0.2, 0.8, 0.1, 0.9 };

    private final SliderSetting minimumDropDistance;

    private final SliderSetting rotationSpeed;

    private final ButtonSetting onlyWhileHoldingLadder;

    private final ButtonSetting renderPlacements;

    private AutoLadder.ExecutionStage stage = AutoLadder.ExecutionStage.IDLE;

    private AutoLadder.PlacementPlan activePlan;

    private int currentPlacementTick;

    private boolean placedThisTick;

    private int lastPlacementAttemptTick = Integer.MIN_VALUE;

    private int guideStartTick = Integer.MIN_VALUE;

    private int guideInvalidTicks;

    private int stageInvalidTicks;

    private float stickyYaw;

    private float stickyPitch;

    private boolean hasStickyRotation;

    private int stageStartTick;

    private int rotationTick = Integer.MIN_VALUE;

    private AutoLadder.ExecutionStage rotationStage;

    private AutoLadder.RotationHit placementHit;

    private EntityPlayerSP trackedPlayer;

    private boolean movementClaimed;

    public AutoLadder() {
        super("AutoLadder", Module.category.player);
        this.registerSetting(this.minimumDropDistance = new SliderSetting("Minimum height", " blocks", 4.0, 0.0, 20.0, 0.5, "Minimum drop distance"));
        this.registerSetting(this.rotationSpeed = new SliderSetting("Rotation speed", "°", 90.0, 10.0, 180.0, 5.0));
        this.registerSetting(this.onlyWhileHoldingLadder = new ButtonSetting("Only while holding ladder", false));
        this.registerSetting(this.renderPlacements = new ButtonSetting("Render placements", false));
        this.closetModule = true;
        this.session.observe();
    }

    @Override
    public void onEnable() {
        this.fallQualified = false;
        this.session.start();
        this.trackedPlayer = mc.field_71439_g;
        this.trackedWorld = mc.field_71441_e;
        this.retryAfterTick = 0;
        this.placedThisTick = false;
        this.resetExecutionState(true, "enabled");
        this.hasStickyRotation = false;
    }

    @Override
    public void onDisable() {
        this.releaseMovement();
        this.fallQualified = false;
        this.resetExecutionState(true, "disabled");
        this.session.finish();
        this.hasStickyRotation = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGameTick(GameTickEvent e) {
        this.movementClaimed = false;
        this.placedThisTick = false;
        this.currentPlacementTick++;
        this.rotationTick = Integer.MIN_VALUE;
        this.placementHit = null;
        this.session.updatePlayer();
        if (this.trackedPlayer != mc.field_71439_g || this.trackedWorld != mc.field_71441_e) {
            this.fallQualified = false;
            this.resetExecutionState(false, "player-changed");
            this.trackedWorld = mc.field_71441_e;
            this.hasStickyRotation = false;
            this.trackedPlayer = mc.field_71439_g;
        }
        if (!this.isModuleReady()) {
            this.fallQualified = false;
            this.resetExecutionState(true, "module-not-ready");
            this.hasStickyRotation = false;
        } else {
            if (!this.isCurrentlyFalling()) {
                this.fallQualified = false;
            }
            if (this.shouldYieldToClutch()) {
                this.yieldToClutch();
            } else if (!this.session.changedManually() && !this.hasIncompatibleModule()) {
                if (!this.isCurrentlyFalling()) {
                    this.fallQualified = false;
                    this.resetExecutionState(true, "fall-ended");
                } else {
                    if (!this.fallQualified && (this.minimumDropDistance.getInput() == 0.0 || this.heightAboveGround() >= this.minimumDropDistance.getInput())) {
                        this.fallQualified = true;
                    }
                    if (this.activePlan == null) {
                        if (this.currentPlacementTick >= this.retryAfterTick && !this.session.restoring() && this.session.sent().rotationKnown && this.session.sent().positionKnown && this.shouldStartPlanningNow()) {
                            this.startExecutionPlan();
                        }
                    } else if (this.isInsideTargetLadder(this.activePlan.ladderPos)) {
                        this.resetExecutionState(true, "ladder-occupied");
                    } else if (!this.isPlanWithinPlacementBounds(this.activePlan)) {
                        this.resetExecutionState(true, "plan-out-of-bounds");
                    } else {
                        if (this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT && this.isSolidLadderSupportBlock(this.activePlan.supportPlacePos)) {
                            this.stage = AutoLadder.ExecutionStage.PLACE_LADDER;
                            this.stageStartTick = this.currentPlacementTick;
                        }
                        if (this.stage == AutoLadder.ExecutionStage.PLACE_LADDER && this.isLadderBlock(this.activePlan.ladderPos)) {
                            this.enterGuideState(this.currentPlacementTick);
                        }
                        if (this.stage == AutoLadder.ExecutionStage.GUIDE_ENTRY) {
                            this.tickGuideState();
                        } else if (!this.shouldAbortForMissingItems() && this.currentPlacementTick - this.stageStartTick <= (this.activePlan.groundCatch ? 40 : 10)) {
                            this.prepareHeldItemForStage();
                        } else {
                            this.resetExecutionState(true, "placement-expired");
                        }
                    }
                }
            } else {
                if (this.session.changedManually()) {
                    this.fallQualified = false;
                }
                this.resetExecutionState(true, "control-conflict");
                this.session.clearManualChange();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientRotation(ClientRotationEvent e) {
        if (!this.isModuleReady()) {
            this.resetExecutionState(true, "module-not-ready");
            this.hasStickyRotation = false;
        } else if (this.shouldYieldToClutch()) {
            this.yieldToClutch();
        } else if (e.yaw != null || e.pitch != null || this.hasIncompatibleModule()) {
            this.resetExecutionState(true, "rotation-conflict");
            this.hasStickyRotation = false;
        } else if (this.session.holdingAim()) {
            this.claimRotations(e, this.session.heldYaw(), this.session.heldPitch());
        } else if (this.activePlan == null || this.stage == AutoLadder.ExecutionStage.GUIDE_ENTRY) {
            this.restoreRotations(e);
            if (this.activePlan != null && e.yaw == null) {
                this.claimRotations(e, mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A);
            }
        } else if (!this.isPlanWithinPlacementBounds(this.activePlan)) {
            this.resetExecutionState(true, "plan-out-of-bounds");
            this.restoreRotations(e);
        } else {
            AutoLadder.RotationHit hit = this.resolveStableHit();
            if (hit == null) {
                if (++this.stageInvalidTicks > 2) {
                    this.resetExecutionState(true, "no-placement-ray");
                }
                this.restoreRotations(e);
            } else {
                this.stageInvalidTicks = 0;
                this.applyRotations(e, hit.yaw, hit.pitch, (float) this.rotationSpeed.getInput());
                this.rotationTick = this.currentPlacementTick;
                this.rotationStage = this.stage;
                this.placementHit = hit;
            }
        }
    }

    private void applyRotations(ClientRotationEvent e, float yaw, float pitch, float limit) {
        float baseYaw = this.session.sent().yaw;
        float basePitch = this.session.sent().pitch;
        float yawStep = MathHelper.func_76131_a(MathHelper.func_76142_g(yaw - baseYaw), -limit, limit);
        float pitchStep = MathHelper.func_76131_a(pitch - basePitch, -limit, limit);
        float[] fixed = RotationUtils.fixRotation(baseYaw + yawStep, basePitch + pitchStep, baseYaw, basePitch);
        this.stickyYaw = fixed[0];
        this.stickyPitch = fixed[1];
        this.hasStickyRotation = true;
        this.session.rememberAim();
        this.claimRotations(e, this.stickyYaw, this.stickyPitch);
    }

    private void claimRotations(ClientRotationEvent event, float yaw, float pitch) {
        this.movementClaimed = this.activePlan != null;
        event.claimExactRotations(yaw, pitch, this.movementClaimed);
    }

    private void releaseMovement() {
        if (this.movementClaimed) {
            RotationHelper.get().releaseServerRelativeMovement();
            this.movementClaimed = false;
        }
    }

    private void restoreRotations(ClientRotationEvent e) {
        if (this.hasStickyRotation) {
            if (e.yaw == null && e.pitch == null) {
                float yaw = mc.field_71439_g.field_70177_z;
                float pitch = mc.field_71439_g.field_70125_A;
                if (Math.abs(MathHelper.func_76142_g(yaw - this.session.sent().yaw)) < 1.0F && Math.abs(pitch - this.session.sent().pitch) < 1.0F) {
                    this.hasStickyRotation = false;
                } else {
                    this.applyRotations(e, yaw, pitch, Math.min(45.0F, (float) this.rotationSpeed.getInput()));
                }
            } else {
                this.hasStickyRotation = false;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreInput(PreInputEvent e) {
        if (this.shouldYieldToClutch()) {
            this.yieldToClutch();
        } else if (this.isModuleReady() && this.isCurrentlyFalling()) {
            if (Timer.shouldProcessPlayerActions() && !this.placedThisTick && this.activePlan != null && !this.session.holdingAim() && !this.session.changedManually() && this.session.canSwitch() && this.lastPlacementAttemptTick != this.currentPlacementTick && this.rotationTick == this.currentPlacementTick && this.rotationStage == this.stage && this.placementHit != null && (this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT || this.stage == AutoLadder.ExecutionStage.PLACE_LADDER)) {
                Float yaw = RotationHelper.get().getServerYaw();
                Float pitch = RotationHelper.get().getServerPitch();
                if (yaw != null && pitch != null && RotationHelper.get().ownsServerRelativeMovement()) {
                    MovingObjectPosition hit = RotationUtils.rayCastBlock(this.getPlacementReach(), yaw, pitch);
                    BlockPos placePos = this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT ? this.activePlan.supportPlacePos : this.activePlan.ladderPos;
                    BlockPos anchor = this.placementHit.mop.func_178782_a();
                    EnumFacing face = this.placementHit.mop.field_178784_b;
                    AutoLadderHelper.SentState sent = this.session.sent();
                    Vec3 eye = new Vec3(sent.x, sent.y + mc.field_71439_g.func_70047_e(), sent.z);
                    Vec3 look = RotationUtils.getVectorForRotation(sent.pitch, sent.yaw);
                    MovingObjectPosition publishedHit = mc.field_71441_e.func_147447_a(eye, eye.func_72441_c(look.field_72450_a * this.getPlacementReach(), look.field_72448_b * this.getPlacementReach(), look.field_72449_c * this.getPlacementReach()), false, false, true);
                    if (sent.rotationKnown && sent.positionKnown && this.isValidPlacementRay(publishedHit, placePos, anchor, face) && this.isValidPlacementRay(hit, placePos, anchor, face)) {
                        if (!this.isPlanStillValidForCurrentStage()) {
                            this.resetExecutionState(true, "stage-invalid");
                        } else {
                            ItemStack heldStack = this.resolveHeldStackForCurrentStage();
                            if (heldStack != null && this.session.sent().slot == mc.field_71439_g.field_71071_by.field_70461_c) {
                                this.lastPlacementAttemptTick = this.currentPlacementTick;
                                IBlockState previousState = mc.field_71441_e.func_180495_p(placePos);
                                int previousCount = heldStack.field_77994_a;
                                C08PacketPlayerBlockPlacement previousPlacement = this.session.lastPlacement();
                                boolean accepted = mc.field_71442_b.func_178890_a(mc.field_71439_g, mc.field_71441_e, heldStack, hit.func_178782_a(), hit.field_178784_b, hit.field_72307_f);
                                C08PacketPlayerBlockPlacement dispatched = this.session.lastPlacement();
                                if (dispatched != previousPlacement && dispatched != null && anchor.equals(dispatched.func_179724_a()) && dispatched.func_149568_f() == face.func_176745_a()) {
                                    this.session.pinAim(yaw, pitch);
                                    if (accepted) {
                                        this.placedThisTick = true;
                                        mc.field_71439_g.func_71038_i();
                                        mc.func_175597_ag().func_78444_b();
                                        if (this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT && this.isSolidLadderSupportBlock(this.activePlan.supportPlacePos)) {
                                            this.stage = AutoLadder.ExecutionStage.PLACE_LADDER;
                                            this.stageStartTick = this.currentPlacementTick;
                                        } else if (this.stage == AutoLadder.ExecutionStage.PLACE_LADDER && this.isLadderBlock(this.activePlan.ladderPos)) {
                                            this.enterGuideState(this.currentPlacementTick);
                                        }
                                    }
                                } else {
                                    mc.field_71441_e.func_180501_a(placePos, previousState, 3);
                                    heldStack.field_77994_a = previousCount;
                                    this.resetExecutionState(true, "placement-not-dispatched");
                                }
                            }
                        }
                    }
                }
            }
        } else {
            this.resetExecutionState(true, "placement-interrupted");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPrePlayerInput(PrePlayerInputEvent e) {
        if (!this.isModuleReady() || !this.isCurrentlyFalling() || this.activePlan == null || this.stage == AutoLadder.ExecutionStage.IDLE || !RotationHelper.get().ownsServerRelativeMovement() || this.session.changedManually() || this.hasIncompatibleModule()) {
            this.releaseMovement();
        } else if (this.shouldYieldToClutch()) {
            this.yieldToClutch();
        } else {
            this.prepareHeldItemForStage();
            Vec3 movementTarget = this.getLadderSteerTarget(this.activePlan.ladderPos);
            this.applyMovementOverride(e, movementTarget);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClick(RightClickMouseEvent e) {
        if (this.placedThisTick) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(PreAttackEvent e) {
        if (this.placedThisTick) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (this.renderPlacements.isToggled() && this.isModuleReady() && this.activePlan != null && this.trackedWorld == mc.field_71441_e && this.trackedPlayer == mc.field_71439_g && (this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT || this.stage == AutoLadder.ExecutionStage.PLACE_LADDER)) {
            AutoLadder.PlacementPlan plan = this.activePlan;
            if (this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT && plan.supportPlacePos != null && !this.isSolidLadderSupportBlock(plan.supportPlacePos)) {
                RenderUtils.renderBlock(plan.supportPlacePos, -43691, false, true);
            }
            if (!this.isLadderBlock(plan.ladderPos)) {
                double x = plan.ladderPos.func_177958_n();
                double z = plan.ladderPos.func_177952_p();
                double width = 1.0;
                double depth = 1.0;
                switch(plan.ladderFace) {
                    case NORTH:
                        z += 0.875;
                        depth = 0.125;
                        break;
                    case SOUTH:
                        depth = 0.125;
                        break;
                    case WEST:
                        x += 0.875;
                        width = 0.125;
                        break;
                    case EAST:
                        width = 0.125;
                        break;
                    default:
                        return;
                }
                RenderUtils.renderBox(x, plan.ladderPos.func_177956_o(), z, width, 1.0, depth, -21931, false, true);
            }
        }
    }

    private void startExecutionPlan() {
        AutoLadder.PlacementPlan plan = this.buildDeterministicPlan();
        if (plan == null) {
            this.resetExecutionState(true, "no-valid-plan");
        } else {
            this.activatePlan(plan);
        }
    }

    private void activatePlan(AutoLadder.PlacementPlan plan) {
        this.activePlan = plan;
        this.stableHit = null;
        this.stage = plan.requiresSupportPlacement ? AutoLadder.ExecutionStage.PLACE_SUPPORT : AutoLadder.ExecutionStage.PLACE_LADDER;
        this.stageStartTick = this.currentPlacementTick;
        this.guideStartTick = this.currentPlacementTick;
        this.guideInvalidTicks = 0;
        this.stageInvalidTicks = 0;
        if (this.isLadderBlock(plan.ladderPos)) {
            this.enterGuideState(this.currentPlacementTick);
        } else {
            this.prepareHeldItemForStage();
        }
    }

    private void tickGuideState() {
        if (this.activePlan != null && this.stage == AutoLadder.ExecutionStage.GUIDE_ENTRY) {
            if (mc.field_71439_g.func_70617_f_() || this.isInsideTargetLadder(this.activePlan.ladderPos)) {
                this.resetExecutionState(true, "guide-success");
            } else if (mc.field_71439_g.field_70122_E) {
                this.resetExecutionState(true, "guide-grounded");
            } else if (!this.isPlanWithinPlacementBounds(this.activePlan)) {
                this.resetExecutionState(true, "guide-plan-out-of-bounds");
            } else if (!this.isLadderBlock(this.activePlan.ladderPos)) {
                this.resetExecutionState(true, "ladder-missing");
            } else {
                AutoLadder.EntryEstimate estimate = this.estimateLadderEntry(this.activePlan.ladderPos, this.activePlan.ladderFace, null, 0, 0);
                if (!estimate.valid()) {
                    this.guideInvalidTicks++;
                    if (this.guideInvalidTicks > 4) {
                        this.resetExecutionState(true, "guide-unreachable");
                        return;
                    }
                } else {
                    this.guideInvalidTicks = 0;
                }
                int playerTick = this.getPlacementPlayerTick();
                if (playerTick != Integer.MIN_VALUE && this.guideStartTick != Integer.MIN_VALUE && playerTick - this.guideStartTick > 26) {
                    this.resetExecutionState(true, "guide-timeout");
                }
            }
        }
    }

    private void enterGuideState(int playerTick) {
        this.stage = AutoLadder.ExecutionStage.GUIDE_ENTRY;
        this.guideStartTick = playerTick;
        this.guideInvalidTicks = 0;
        this.stageInvalidTicks = 0;
        this.restoreOriginalSlotIfNeeded();
    }

    private AutoLadder.RotationHit resolveStableHit() {
        float yaw = this.session.sent().yaw;
        float pitch = this.session.sent().pitch;
        if (this.activePlan.groundCatch) {
            BlockPos anchor = this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT ? this.activePlan.supportPlacePos.func_177977_b() : this.activePlan.ladderSupportPos;
            EnumFacing face = this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT ? EnumFacing.UP : this.activePlan.ladderFace;
            Vec3 point = this.buildFaceHitVec(anchor, face, 0.5, 0.5);
            float[] aim = this.getFixedRotationsForVec(point, yaw, pitch);
            return new AutoLadder.RotationHit(aim[0], aim[1], new MovingObjectPosition(point, face, anchor), 0.0);
        } else {
            if (this.stableHit != null && this.stableStage == this.stage) {
                BlockPos anchor = this.stableHit.mop.func_178782_a();
                EnumFacing face = this.stableHit.mop.field_178784_b;
                BlockPos destination = this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT ? this.activePlan.supportPlacePos : this.activePlan.ladderPos;
                float[] aim = this.getFixedRotationsForVec(this.stableHit.mop.field_72307_f, yaw, pitch);
                MovingObjectPosition hit = RotationUtils.rayCastBlock(this.getPlacementReach(), aim[0], aim[1]);
                if (this.isValidPlacementRay(hit, destination, anchor, face)) {
                    return new AutoLadder.RotationHit(aim[0], aim[1], this.stableHit.mop, 0.0);
                }
            }
            this.stableStage = this.stage;
            return this.stableHit = this.resolveRotationForCurrentStage(yaw, pitch);
        }
    }

    private AutoLadder.RotationHit resolveRotationForCurrentStage(float baseYaw, float basePitch) {
        return this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT ? this.resolveSupportPlacementHit(this.activePlan, baseYaw, basePitch) : this.resolveLadderPlacementHit(this.activePlan, baseYaw, basePitch);
    }

    private AutoLadder.RotationHit resolveSupportPlacementHit(AutoLadder.PlacementPlan plan, float baseYaw, float basePitch) {
        return plan != null && plan.requiresSupportPlacement && plan.supportPlacePos != null && BlockUtils.replaceable(plan.supportPlacePos) ? this.findSupportBlockPlacementHit(plan.supportPlacePos, baseYaw, basePitch) : null;
    }

    private AutoLadder.RotationHit resolveLadderPlacementHit(AutoLadder.PlacementPlan plan, float baseYaw, float basePitch) {
        if (plan == null || plan.ladderPos == null || plan.ladderSupportPos == null || plan.ladderFace == null) {
            return null;
        } else if (!this.isReplaceableOrLadder(plan.ladderPos)) {
            return null;
        } else {
            return !this.canUseAsLadderSupport(plan.ladderPos, plan.ladderFace, plan.ladderSupportPos) ? null : this.findRotationHitForPlacement(plan.ladderPos, plan.ladderSupportPos, plan.ladderFace, baseYaw, basePitch);
        }
    }

    private AutoLadder.PlacementPlan buildDeterministicPlan() {
        if (this.findLadderSlot() == -1) {
            return null;
        } else {
            this.predictFallPath();
            AutoLadder.PlacementPlan direct = this.findPlacementPlan(false);
            return direct != null ? direct : (this.findSupportSlot() == -1 ? null : this.findGroundPlan());
        }
    }

    private void predictFallPath() {
        this.fallPath.clear();
        this.predictedLanding = false;
        Simulation simulation = Simulation.create();
        simulation.setJump(false);
        for (int tick = 0; tick <= 40; tick++) {
            keystrokesmod.script.model.Vec3 pos = simulation.getPosition();
            this.fallPath.add(new Vec3(pos.x, pos.y, pos.z));
            if (simulation.isInLiquid()) {
                return;
            }
            if (simulation.onGround()) {
                this.predictedLanding = true;
                return;
            }
            simulation.tick();
        }
    }

    private boolean fallPassesCell(BlockPos cell) {
        for (Vec3 pos : this.fallPath) {
            if (pos.field_72448_b < cell.func_177956_o()) {
                break;
            }
            if (pos.field_72448_b < cell.func_177956_o() + 1 && pos.field_72450_a >= cell.func_177958_n() && pos.field_72450_a < cell.func_177958_n() + 1 && pos.field_72449_c >= cell.func_177952_p() && pos.field_72449_c < cell.func_177952_p() + 1) {
                return true;
            }
        }
        return false;
    }

    private AutoLadder.PlacementPlan findGroundPlan() {
        if (this.predictedLanding && !this.fallPath.isEmpty()) {
            Vec3 landing = this.fallPath.get(this.fallPath.size() - 1);
            BlockPos center = new BlockPos(landing.field_72450_a, Math.ceil(landing.field_72448_b - 1.0E-7), landing.field_72449_c);
            AutoLadder.PlacementPlan best = null;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos ladder = center.func_177982_a(x, 0, z);
                    if (BlockUtils.replaceable(ladder)) {
                        for (EnumFacing face : Plane.HORIZONTAL) {
                            BlockPos support = ladder.func_177972_a(face.func_176734_d());
                            if (BlockUtils.replaceable(support) && this.isSolidLadderSupportBlock(support.func_177977_b())) {
                                AutoLadder.PlacementPlan candidate = new AutoLadder.PlacementPlan(ladder, support, face, true, 0.0, true);
                                AutoLadder.EntryEstimate estimate = this.estimateGroundEntry(candidate);
                                if (estimate.valid()) {
                                    double dx = ladder.func_177958_n() + 0.5 - landing.field_72450_a;
                                    double dz = ladder.func_177952_p() + 0.5 - landing.field_72449_c;
                                    candidate = new AutoLadder.PlacementPlan(ladder, support, face, true, this.computePlanCost(estimate.tickAhead, estimate.entryAlignmentErrorSq, dx * dx + dz * dz), true);
                                    best = this.pickBetterPlan(best, candidate);
                                }
                            }
                        }
                    }
                }
            }
            return best;
        } else {
            return null;
        }
    }

    private AutoLadder.EntryEstimate estimateGroundEntry(AutoLadder.PlacementPlan plan) {
        Simulation simulation = Simulation.create();
        boolean supportPlaced = this.isSolidLadderSupportBlock(plan.supportPlacePos);
        boolean ladderPlaced = this.isLadderBlock(plan.ladderPos);
        if (supportPlaced || BlockUtils.replaceable(plan.supportPlacePos) && this.isSolidLadderSupportBlock(plan.supportPlacePos.func_177977_b())) {
            if (!this.isReplaceableOrLadder(plan.ladderPos)) {
                return AutoLadder.EntryEstimate.invalid();
            } else {
                AutoLadderHelper.SentState sent = this.session.sent();
                float yaw = sent.yaw;
                float pitch = sent.pitch;
                Vec3 publishedEye = new Vec3(sent.x, sent.y + mc.field_71439_g.func_70047_e(), sent.z);
                Vec3 target = this.getLadderSteerTarget(plan.ladderPos);
                for (int tick = 0; tick <= 40; tick++) {
                    keystrokesmod.script.model.Vec3 pos = simulation.getPosition();
                    if (simulation.onGround() || simulation.isInLiquid() || pos.y < plan.ladderPos.func_177956_o()) {
                        return AutoLadder.EntryEstimate.invalid();
                    }
                    if (ladderPlaced && simulation.isOnLadder() && this.feetOccupyLadderCell(pos.x, pos.y, pos.z, plan.ladderPos)) {
                        double dx = pos.x - target.field_72450_a;
                        double dz = pos.z - target.field_72449_c;
                        return new AutoLadder.EntryEstimate(tick, dx * dx + dz * dz);
                    }
                    Vec3 eye = new Vec3(pos.x, pos.y + mc.field_71439_g.func_70047_e(), pos.z);
                    float nextYaw;
                    float nextPitch;
                    if (!ladderPlaced) {
                        BlockPos anchor = supportPlaced ? plan.supportPlacePos : plan.supportPlacePos.func_177977_b();
                        EnumFacing face = supportPlaced ? plan.ladderFace : EnumFacing.UP;
                        Vec3 point = this.buildFaceHitVec(anchor, face, 0.5, 0.5);
                        double dx = point.field_72450_a - eye.field_72450_a;
                        double dy = point.field_72448_b - eye.field_72448_b;
                        double dz = point.field_72449_c - eye.field_72449_c;
                        float[] aim = RotationUtils.fixRotation(yaw + MathHelper.func_76142_g((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F - yaw), (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))), yaw, pitch);
                        float speed = (float) this.rotationSpeed.getInput();
                        float[] step = RotationUtils.fixRotation(yaw + MathHelper.func_76131_a(MathHelper.func_76142_g(aim[0] - yaw), -speed, speed), pitch + MathHelper.func_76131_a(aim[1] - pitch, -speed, speed), yaw, pitch);
                        nextYaw = step[0];
                        nextPitch = step[1];
                        BlockPos hypothetical = supportPlaced ? plan.supportPlacePos : null;
                        if ((tick > 0 || this.session.canSwitch()) && this.groundRayReaches(publishedEye, yaw, pitch, anchor, face, hypothetical) && this.groundRayReaches(eye, nextYaw, nextPitch, anchor, face, hypothetical)) {
                            if (!supportPlaced) {
                                double half = mc.field_71439_g.field_70130_N * 0.5;
                                AxisAlignedBB box = new AxisAlignedBB(pos.x - half, pos.y, pos.z - half, pos.x + half, pos.y + mc.field_71439_g.field_70131_O, pos.z + half);
                                if (box.func_72326_a(new AxisAlignedBB(plan.supportPlacePos, plan.supportPlacePos.func_177982_a(1, 1, 1)))) {
                                    return AutoLadder.EntryEstimate.invalid();
                                }
                                simulation.setBlockState(plan.supportPlacePos, Blocks.field_150348_b.func_176223_P());
                                supportPlaced = true;
                            } else {
                                simulation.setBlockState(plan.ladderPos, Blocks.field_150468_ap.func_176223_P().func_177226_a(BlockLadder.field_176382_a, plan.ladderFace));
                                ladderPlaced = true;
                            }
                        }
                    } else {
                        float speed = Math.min(45.0F, (float) this.rotationSpeed.getInput());
                        float[] step = RotationUtils.fixRotation(yaw + MathHelper.func_76131_a(MathHelper.func_76142_g(mc.field_71439_g.field_70177_z - yaw), -speed, speed), pitch + MathHelper.func_76131_a(mc.field_71439_g.field_70125_A - pitch, -speed, speed), yaw, pitch);
                        nextYaw = step[0];
                        nextPitch = step[1];
                    }
                    if (ladderPlaced && simulation.isOnLadder() && this.feetOccupyLadderCell(pos.x, pos.y, pos.z, plan.ladderPos)) {
                        double entryX = pos.x - target.field_72450_a;
                        double entryZ = pos.z - target.field_72449_c;
                        return new AutoLadder.EntryEstimate(tick, entryX * entryX + entryZ * entryZ);
                    }
                    float[] input = this.chooseSteering(target, pos.x, pos.z, simulation.getMotion().x, simulation.getMotion().z, nextYaw, simulation.getAirAcceleration());
                    simulation.setForward(input[0]);
                    simulation.setStrafe(input[1]);
                    simulation.setYaw(nextYaw);
                    simulation.setJump(false);
                    simulation.setSneak(false);
                    simulation.setSprinting(mc.field_71474_y.field_151444_V.func_151470_d());
                    simulation.tick();
                    pos = simulation.getPosition();
                    publishedEye = new Vec3(pos.x, pos.y + mc.field_71439_g.func_70047_e(), pos.z);
                    yaw = nextYaw;
                    pitch = nextPitch;
                }
                return AutoLadder.EntryEstimate.invalid();
            }
        } else {
            return AutoLadder.EntryEstimate.invalid();
        }
    }

    private boolean groundRayReaches(Vec3 eye, float yaw, float pitch, BlockPos anchor, EnumFacing face, BlockPos hypotheticalSupport) {
        Vec3 look = RotationUtils.getVectorForRotation(pitch, yaw);
        double reach = this.getPlacementReach();
        Vec3 end = eye.func_72441_c(look.field_72450_a * reach, look.field_72448_b * reach, look.field_72449_c * reach);
        MovingObjectPosition hit = mc.field_71441_e.func_147447_a(eye, end, false, false, true);
        if (hypotheticalSupport != null) {
            MovingObjectPosition supportHit = new AxisAlignedBB(hypotheticalSupport, hypotheticalSupport.func_177982_a(1, 1, 1)).func_72327_a(eye, end);
            if (supportHit != null && (hit == null || hit.field_72313_a != MovingObjectType.BLOCK || eye.func_72436_e(supportHit.field_72307_f) < eye.func_72436_e(hit.field_72307_f))) {
                hit = new MovingObjectPosition(supportHit.field_72307_f, supportHit.field_178784_b, hypotheticalSupport);
            }
        }
        return hit != null && hit.field_72313_a == MovingObjectType.BLOCK && anchor.equals(hit.func_178782_a()) && face == hit.field_178784_b;
    }

    private AutoLadder.PlacementPlan findPlacementPlan(boolean requiresSupportPlacement) {
        int feetY = MathHelper.func_76128_c(this.getPlayerFeetY());
        int minX = MathHelper.func_76128_c(mc.field_71439_g.field_70165_t - 4.5);
        int maxX = MathHelper.func_76128_c(mc.field_71439_g.field_70165_t + 4.5);
        int minZ = MathHelper.func_76128_c(mc.field_71439_g.field_70161_v - 4.5);
        int maxZ = MathHelper.func_76128_c(mc.field_71439_g.field_70161_v + 4.5);
        AutoLadder.PlacementPlan best = null;
        for (int y = feetY; y >= feetY - 3; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos ladderPos = new BlockPos(x, y, z);
                    if (this.isWithinPlacementBounds(ladderPos) && this.canReachCellBeforePassing(ladderPos) && this.isReplaceableOrLadder(ladderPos) && this.fallPassesCell(ladderPos)) {
                        AutoLadder.PlacementPlan plan = requiresSupportPlacement ? this.buildSupportThenLadderPlanForCell(ladderPos) : this.buildDirectLadderPlanForCell(ladderPos);
                        best = this.pickBetterPlan(best, plan);
                    }
                }
            }
        }
        return best;
    }

    private boolean canReachCellBeforePassing(BlockPos pos) {
        double y = this.getPlayerFeetY();
        double motionY = mc.field_71439_g.field_70181_x;
        double speed = Math.sqrt(mc.field_71439_g.field_70159_w * mc.field_71439_g.field_70159_w + mc.field_71439_g.field_70179_y * mc.field_71439_g.field_70179_y);
        double distance = mc.field_71439_g.field_70130_N;
        for (int tick = 0; tick < 20 && y >= pos.func_177956_o(); tick++) {
            distance += speed + 0.026;
            speed = (speed + 0.026) * 0.91;
            y += motionY;
            motionY = (motionY - 0.08) * 0.98;
        }
        double dx = Math.max(0.0, Math.abs(pos.func_177958_n() + 0.5 - mc.field_71439_g.field_70165_t) - 0.5);
        double dz = Math.max(0.0, Math.abs(pos.func_177952_p() + 0.5 - mc.field_71439_g.field_70161_v) - 0.5);
        return dx * dx + dz * dz <= distance * distance;
    }

    private AutoLadder.PlacementPlan buildDirectLadderPlanForCell(BlockPos ladderPos) {
        AutoLadder.PlacementPlan best = null;
        for (EnumFacing ladderFace : Plane.HORIZONTAL) {
            BlockPos ladderSupportPos = ladderPos.func_177972_a(ladderFace.func_176734_d());
            if (this.isWithinPlacementBounds(ladderSupportPos) && this.canUseAsLadderSupport(ladderPos, ladderFace, ladderSupportPos)) {
                AutoLadder.RotationHit hit = this.isLadderBlock(ladderPos) ? null : this.findRotationHitForPlacement(ladderPos, ladderSupportPos, ladderFace, this.getPlacementBaseYaw(), this.getPlacementBasePitch());
                if (hit != null || this.isLadderBlock(ladderPos)) {
                    int readyTick = hit == null ? 0 : this.rotationTicks(hit.yaw, hit.pitch, this.getPlacementBaseYaw(), this.getPlacementBasePitch());
                    AutoLadder.EntryEstimate estimate = this.estimateLadderEntry(ladderPos, ladderFace, null, readyTick, 0);
                    if (estimate.valid()) {
                        double planCost = this.computePlanCost(estimate.tickAhead, estimate.entryAlignmentErrorSq, this.getHorizontalDistanceSq(ladderPos));
                        AutoLadder.PlacementPlan candidate = new AutoLadder.PlacementPlan(ladderPos, ladderSupportPos, ladderFace, false, planCost);
                        best = this.pickBetterPlan(best, candidate);
                    }
                }
            }
        }
        return best;
    }

    private AutoLadder.PlacementPlan buildSupportThenLadderPlanForCell(BlockPos ladderPos) {
        AutoLadder.PlacementPlan best = null;
        for (EnumFacing ladderFace : Plane.HORIZONTAL) {
            BlockPos supportPlacePos = ladderPos.func_177972_a(ladderFace.func_176734_d());
            if (this.isWithinPlacementBounds(supportPlacePos) && BlockUtils.replaceable(supportPlacePos) && !this.doesPlayerIntersectPlacementNowOrNextTick(supportPlacePos) && this.hasSupportPlacementAnchor(supportPlacePos)) {
                AutoLadder.RotationHit supportHit = this.findSupportBlockPlacementHit(supportPlacePos, this.getPlacementBaseYaw(), this.getPlacementBasePitch());
                if (supportHit != null) {
                    float[] ladderAim = this.getFixedRotationsForVec(this.buildFaceHitVec(supportPlacePos, ladderFace, 0.5, 0.5), supportHit.yaw, supportHit.pitch);
                    int supportReadyTick = this.rotationTicks(supportHit.yaw, supportHit.pitch, this.getPlacementBaseYaw(), this.getPlacementBasePitch());
                    int readyTick = supportReadyTick + 1 + this.rotationTicks(ladderAim[0], ladderAim[1], supportHit.yaw, supportHit.pitch);
                    AutoLadder.EntryEstimate estimate = this.estimateLadderEntry(ladderPos, ladderFace, supportPlacePos, readyTick, supportReadyTick);
                    if (estimate.valid()) {
                        double planCost = this.computePlanCost(estimate.tickAhead, estimate.entryAlignmentErrorSq, this.getHorizontalDistanceSq(ladderPos));
                        AutoLadder.PlacementPlan candidate = new AutoLadder.PlacementPlan(ladderPos, supportPlacePos, ladderFace, true, planCost);
                        best = this.pickBetterPlan(best, candidate);
                    }
                }
            }
        }
        return best;
    }

    private AutoLadder.PlacementPlan pickBetterPlan(AutoLadder.PlacementPlan currentBest, AutoLadder.PlacementPlan candidate) {
        if (candidate == null) {
            return currentBest;
        } else if (currentBest == null) {
            return candidate;
        } else if (Math.abs(candidate.planCost - currentBest.planCost) > 1.0E-6) {
            return candidate.planCost < currentBest.planCost ? candidate : currentBest;
        } else {
            return candidate.ladderPos.func_177986_g() < currentBest.ladderPos.func_177986_g() ? candidate : currentBest;
        }
    }

    private double computePlanCost(int entryTick, double entryAlignmentErrorSq, double horizontalDistanceSq) {
        return entryTick * 120.0 + entryAlignmentErrorSq * 90.0 + horizontalDistanceSq * 10.0;
    }

    private AutoLadder.RotationHit findSupportBlockPlacementHit(BlockPos placePos, float baseYaw, float basePitch) {
        AutoLadder.RotationHit best = null;
        for (EnumFacing placeFace : EnumFacing.values()) {
            if (placeFace != EnumFacing.DOWN) {
                BlockPos anchorPos = placePos.func_177972_a(placeFace.func_176734_d());
                if (this.isAnchorBlockValid(anchorPos)) {
                    AutoLadder.RotationHit hit = this.findRotationHitForPlacement(placePos, anchorPos, placeFace, baseYaw, basePitch);
                    if (hit != null && (best == null || hit.score < best.score)) {
                        best = hit;
                    }
                }
            }
        }
        return best;
    }

    private AutoLadder.RotationHit findRotationHitForPlacement(BlockPos placePos, BlockPos supportPos, EnumFacing supportFace, float baseYaw, float basePitch) {
        if (placePos != null && supportPos != null && supportFace != null && mc.field_71439_g != null) {
            if (!this.isWithinReachBounds(placePos, supportPos)) {
                return null;
            } else {
                Vec3i faceDirection = supportFace.func_176730_m();
                Vec3 supportFaceCenter = new Vec3(supportPos.func_177958_n() + 0.5 + faceDirection.func_177958_n() * 0.5, supportPos.func_177956_o() + 0.5 + faceDirection.func_177956_o() * 0.5, supportPos.func_177952_p() + 0.5 + faceDirection.func_177952_p() * 0.5);
                AutoLadder.RotationHit best = null;
                double reach = this.getPlacementReach();
                Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
                for (double u : HIT_OFFSETS) {
                    for (double v : HIT_OFFSETS) {
                        Vec3 hitVec = this.buildFaceHitVec(supportPos, supportFace, u, v);
                        if (hitVec != null) {
                            float[] fixedRotations = this.getFixedRotationsForHitVec(hitVec, baseYaw, basePitch);
                            MovingObjectPosition traced = RotationUtils.rayCastBlock(reach, fixedRotations[0], fixedRotations[1]);
                            if (this.isValidPlacementRay(traced, placePos, supportPos, supportFace)) {
                                double yawDelta = Math.abs(MathHelper.func_76142_g(fixedRotations[0] - baseYaw));
                                double pitchDelta = Math.abs(fixedRotations[1] - basePitch);
                                double centerPenalty = supportFaceCenter.func_72436_e(traced.field_72307_f);
                                double score = yawDelta + pitchDelta + centerPenalty * 4.0;
                                if (best == null || score < best.score) {
                                    best = new AutoLadder.RotationHit(fixedRotations[0], fixedRotations[1], traced, score);
                                }
                            }
                        }
                    }
                }
                return best;
            }
        } else {
            return null;
        }
    }

    private boolean isValidPlacementRay(MovingObjectPosition mop, BlockPos expectedPlacePos, BlockPos expectedSupportPos, EnumFacing expectedFace) {
        if (mop != null && mop.field_72313_a == MovingObjectType.BLOCK && mop.func_178782_a() != null && mop.field_178784_b != null && mop.field_72307_f != null && expectedPlacePos != null && expectedSupportPos != null && expectedFace != null) {
            if (BlockUtils.isBlockPosEqual(mop.func_178782_a(), expectedSupportPos) && mop.field_178784_b == expectedFace) {
                BlockPos placePos = BlockUtils.offsetPos(mop);
                return placePos != null && BlockUtils.isBlockPosEqual(placePos, expectedPlacePos);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean canUseAsLadderSupport(BlockPos ladderPos, EnumFacing ladderFace, BlockPos supportPos) {
        if (ladderPos == null || ladderFace == null || supportPos == null || mc.field_71441_e == null) {
            return false;
        } else if (!this.isSolidLadderSupportBlock(supportPos)) {
            return false;
        } else {
            return !this.isReplaceableOrLadder(ladderPos) ? false : Blocks.field_150468_ap.func_176198_a(mc.field_71441_e, ladderPos, ladderFace);
        }
    }

    private boolean isAnchorBlockValid(BlockPos pos) {
        if (pos != null && !BlockUtils.replaceable(pos) && !BlockUtils.isInteractable(BlockUtils.getBlock(pos))) {
            Block block = BlockUtils.getBlock(pos);
            return block != null && block != Blocks.field_150350_a;
        } else {
            return false;
        }
    }

    private boolean hasSupportPlacementAnchor(BlockPos placePos) {
        if (placePos == null) {
            return false;
        } else {
            for (EnumFacing placeFace : EnumFacing.values()) {
                if (placeFace != EnumFacing.DOWN) {
                    BlockPos anchorPos = placePos.func_177972_a(placeFace.func_176734_d());
                    if (this.isAnchorBlockValid(anchorPos)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private boolean isSolidLadderSupportBlock(BlockPos pos) {
        if (pos != null && !BlockUtils.replaceable(pos) && !BlockUtils.isInteractable(BlockUtils.getBlock(pos))) {
            Block block = BlockUtils.getBlock(pos);
            return block != null && block != Blocks.field_150350_a && block.func_149686_d();
        } else {
            return false;
        }
    }

    private boolean isWithinReachBounds(BlockPos placePos, BlockPos supportPos) {
        if (mc.field_71439_g == null) {
            return false;
        } else {
            Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
            Vec3 placeCenter = new Vec3(placePos.func_177958_n() + 0.5, placePos.func_177956_o() + 0.5, placePos.func_177952_p() + 0.5);
            Vec3 supportCenter = new Vec3(supportPos.func_177958_n() + 0.5, supportPos.func_177956_o() + 0.5, supportPos.func_177952_p() + 0.5);
            double reach = this.getPlacementReach() + 1.0;
            return eye.func_72438_d(placeCenter) <= reach || eye.func_72438_d(supportCenter) <= reach;
        }
    }

    private float[] getFixedRotationsForHitVec(Vec3 hitVec, float baseYaw, float basePitch) {
        return this.getFixedRotationsForVec(hitVec, baseYaw, basePitch);
    }

    private float[] getFixedRotationsForVec(Vec3 targetVec, float baseYaw, float basePitch) {
        if (targetVec != null && mc.field_71439_g != null) {
            double dx = targetVec.field_72450_a - mc.field_71439_g.field_70165_t;
            double dy = targetVec.field_72448_b - (mc.field_71439_g.field_70163_u + mc.field_71439_g.func_70047_e());
            double dz = targetVec.field_72449_c - mc.field_71439_g.field_70161_v;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            float yaw = horizontalDistance < 1.0E-6 ? baseYaw : (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDistance)));
            yaw = baseYaw + MathHelper.func_76142_g(yaw - baseYaw);
            pitch = MathHelper.func_76131_a(basePitch + MathHelper.func_76142_g(pitch - basePitch), -90.0F, 90.0F);
            return RotationUtils.fixRotation(yaw, pitch, baseYaw, basePitch);
        } else {
            return null;
        }
    }

    private Vec3 buildFaceHitVec(BlockPos blockPos, EnumFacing face, double u, double v) {
        if (blockPos != null && face != null) {
            double cu = MathHelper.func_151237_a(u, 0.01, 0.99);
            double cv = MathHelper.func_151237_a(v, 0.01, 0.99);
            double x;
            double y;
            double z;
            switch(face) {
                case NORTH:
                    x = blockPos.func_177958_n() + cu;
                    y = blockPos.func_177956_o() + cv;
                    z = blockPos.func_177952_p() + 0.01;
                    break;
                case SOUTH:
                    x = blockPos.func_177958_n() + cu;
                    y = blockPos.func_177956_o() + cv;
                    z = blockPos.func_177952_p() + 0.99;
                    break;
                case WEST:
                    x = blockPos.func_177958_n() + 0.01;
                    y = blockPos.func_177956_o() + cv;
                    z = blockPos.func_177952_p() + cu;
                    break;
                case EAST:
                    x = blockPos.func_177958_n() + 0.99;
                    y = blockPos.func_177956_o() + cv;
                    z = blockPos.func_177952_p() + cu;
                    break;
                case DOWN:
                    x = blockPos.func_177958_n() + cu;
                    y = blockPos.func_177956_o() + 0.01;
                    z = blockPos.func_177952_p() + cv;
                    break;
                case UP:
                    x = blockPos.func_177958_n() + cu;
                    y = blockPos.func_177956_o() + 0.99;
                    z = blockPos.func_177952_p() + cv;
                    break;
                default:
                    return null;
            }
            return new Vec3(x, y, z);
        } else {
            return null;
        }
    }

    private int rotationTicks(float yaw, float pitch, float baseYaw, float basePitch) {
        double angle = Math.max(Math.abs(MathHelper.func_76142_g(yaw - baseYaw)), Math.abs(pitch - basePitch));
        return Math.max(1, (int) Math.ceil(angle / this.rotationSpeed.getInput()));
    }

    private AutoLadder.EntryEstimate estimateLadderEntry(BlockPos ladderPos, EnumFacing ladderFace, BlockPos supportPlacementPos, int ladderReadyTick, int supportReadyTick) {
        Simulation simulation = Simulation.create();
        Vec3 target = this.getLadderSteerTarget(ladderPos);
        for (int tickAhead = 0; tickAhead <= 20; tickAhead++) {
            keystrokesmod.script.model.Vec3 pos = simulation.getPosition();
            if (simulation.onGround() || simulation.isInLiquid() || pos.y < ladderPos.func_177956_o()) {
                return AutoLadder.EntryEstimate.invalid();
            }
            if (tickAhead >= ladderReadyTick && simulation.isOnLadder() && this.feetOccupyLadderCell(pos.x, pos.y, pos.z, ladderPos)) {
                double dx = target.field_72450_a - pos.x;
                double dz = target.field_72449_c - pos.z;
                return new AutoLadder.EntryEstimate(tickAhead, dx * dx + dz * dz);
            }
            if (supportPlacementPos != null && tickAhead == supportReadyTick) {
                double halfWidth = mc.field_71439_g.field_70130_N * 0.5;
                AxisAlignedBB axisAlignedBB = new AxisAlignedBB(pos.x - halfWidth, pos.y, pos.z - halfWidth, pos.x + halfWidth, pos.y + mc.field_71439_g.field_70131_O, pos.z + halfWidth);
                if (axisAlignedBB.func_72326_a(new AxisAlignedBB(supportPlacementPos, supportPlacementPos.func_177982_a(1, 1, 1)))) {
                    return AutoLadder.EntryEstimate.invalid();
                }
                simulation.setBlockState(supportPlacementPos, Blocks.field_150348_b.func_176223_P());
            }
            if (tickAhead == ladderReadyTick && !this.isLadderBlock(ladderPos)) {
                simulation.setBlockState(ladderPos, Blocks.field_150468_ap.func_176223_P().func_177226_a(BlockLadder.field_176382_a, ladderFace));
            }
            if (tickAhead >= ladderReadyTick && simulation.isOnLadder() && this.feetOccupyLadderCell(pos.x, pos.y, pos.z, ladderPos)) {
                double dx = target.field_72450_a - pos.x;
                double dz = target.field_72449_c - pos.z;
                return new AutoLadder.EntryEstimate(tickAhead, dx * dx + dz * dz);
            }
            float[] input = this.chooseSteering(target, pos.x, pos.z, simulation.getMotion().x, simulation.getMotion().z, this.getSteeringYaw(), simulation.getAirAcceleration());
            simulation.setForward(input[0]);
            simulation.setStrafe(input[1]);
            simulation.setYaw(this.getSteeringYaw());
            simulation.setJump(false);
            simulation.setSneak(false);
            simulation.setSprinting(mc.field_71474_y.field_151444_V.func_151470_d());
            simulation.tick();
        }
        return AutoLadder.EntryEstimate.invalid();
    }

    private boolean feetOccupyLadderCell(double x, double y, double z, BlockPos pos) {
        return MathHelper.func_76128_c(x) == pos.func_177958_n() && MathHelper.func_76128_c(y) == pos.func_177956_o() && MathHelper.func_76128_c(z) == pos.func_177952_p();
    }

    private boolean isPlanStillValidForCurrentStage() {
        if (this.activePlan == null || !this.isPlanWithinPlacementBounds(this.activePlan)) {
            return false;
        } else if (this.activePlan.groundCatch) {
            return !this.shouldAbortForMissingItems() && this.estimateGroundEntry(this.activePlan).valid();
        } else if (this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT) {
            if (this.activePlan.requiresSupportPlacement && this.activePlan.supportPlacePos != null && BlockUtils.replaceable(this.activePlan.supportPlacePos) && this.findSupportSlot() != -1 && !this.doesPlayerIntersectPlacementNowOrNextTick(this.activePlan.supportPlacePos) && this.hasSupportPlacementAnchor(this.activePlan.supportPlacePos)) {
                float[] aim = this.getFixedRotationsForVec(this.buildFaceHitVec(this.activePlan.supportPlacePos, this.activePlan.ladderFace, 0.5, 0.5), this.placementHit.yaw, this.placementHit.pitch);
                int readyTick = this.rotationTicks(aim[0], aim[1], this.placementHit.yaw, this.placementHit.pitch);
                AutoLadder.EntryEstimate estimate = this.estimateLadderEntry(this.activePlan.ladderPos, this.activePlan.ladderFace, this.activePlan.supportPlacePos, readyTick, 0);
                return estimate.valid();
            } else {
                return false;
            }
        } else if (this.stage == AutoLadder.ExecutionStage.PLACE_LADDER) {
            if (this.findLadderSlot() != -1 && this.isReplaceableOrLadder(this.activePlan.ladderPos) && this.canUseAsLadderSupport(this.activePlan.ladderPos, this.activePlan.ladderFace, this.activePlan.ladderSupportPos)) {
                AutoLadder.EntryEstimate estimate = this.estimateLadderEntry(this.activePlan.ladderPos, this.activePlan.ladderFace, null, 0, 0);
                return estimate.valid();
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean shouldAbortForMissingItems() {
        if (this.activePlan == null) {
            return false;
        } else {
            return this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT ? this.findSupportSlot() == -1 || this.findLadderSlot() == -1 : this.stage == AutoLadder.ExecutionStage.PLACE_LADDER && this.findLadderSlot() == -1;
        }
    }

    private void prepareHeldItemForStage() {
        if (mc.field_71439_g != null && this.activePlan != null) {
            if (this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT) {
                int supportSlot = this.findSupportSlot();
                if (supportSlot != -1) {
                    this.switchToSlot(supportSlot);
                }
            } else if (this.stage == AutoLadder.ExecutionStage.PLACE_LADDER) {
                int ladderSlot = this.findLadderSlot();
                if (ladderSlot != -1) {
                    this.switchToSlot(ladderSlot);
                }
            }
        }
    }

    private ItemStack resolveHeldStackForCurrentStage() {
        if (mc.field_71439_g == null) {
            return null;
        } else {
            ItemStack held = mc.field_71439_g.func_70694_bm();
            if (this.stage == AutoLadder.ExecutionStage.PLACE_SUPPORT) {
                if (this.isSupportBlockStack(held)) {
                    return held;
                } else {
                    int slot = this.findSupportSlot();
                    if (slot == -1) {
                        return null;
                    } else {
                        this.switchToSlot(slot);
                        held = mc.field_71439_g.func_70694_bm();
                        return this.isSupportBlockStack(held) ? held : null;
                    }
                }
            } else if (this.isLadderStack(held)) {
                return held;
            } else {
                int ladderSlot = this.findLadderSlot();
                if (ladderSlot == -1) {
                    return null;
                } else {
                    this.switchToSlot(ladderSlot);
                    held = mc.field_71439_g.func_70694_bm();
                    return this.isLadderStack(held) ? held : null;
                }
            }
        }
    }

    private void switchToSlot(int slot) {
        this.session.select(slot);
    }

    private void restoreOriginalSlotIfNeeded() {
        this.session.restore();
    }

    private int findLadderSlot() {
        if (mc.field_71439_g == null) {
            return -1;
        } else {
            for (int slot = 0; slot < 9; slot++) {
                if (this.isLadderStack(mc.field_71439_g.field_71071_by.func_70301_a(slot))) {
                    return slot;
                }
            }
            return -1;
        }
    }

    private int findSupportSlot() {
        if (mc.field_71439_g == null) {
            return -1;
        } else {
            for (int slot = 0; slot < 9; slot++) {
                if (this.isSupportBlockStack(mc.field_71439_g.field_71071_by.func_70301_a(slot))) {
                    return slot;
                }
            }
            return -1;
        }
    }

    private boolean isLadderStack(ItemStack stack) {
        if (stack != null && stack.field_77994_a > 0 && stack.func_77973_b() instanceof ItemBlock) {
            Block block = ((ItemBlock) stack.func_77973_b()).func_179223_d();
            return block == Blocks.field_150468_ap;
        } else {
            return false;
        }
    }

    private boolean isSupportBlockStack(ItemStack stack) {
        if (stack != null && stack.field_77994_a > 0 && stack.func_77973_b() instanceof ItemBlock) {
            ItemBlock itemBlock = (ItemBlock) stack.func_77973_b();
            Block block = itemBlock.func_179223_d();
            return block != Blocks.field_150468_ap && block != null && block.func_149686_d() && Utils.canBePlaced(itemBlock);
        } else {
            return false;
        }
    }

    private float[] steeringInput(Vec3 target, double x, double z, double motionX, double motionZ, float yaw) {
        return this.chooseSteering(target, x, z, motionX, motionZ, yaw, mc.field_71439_g.field_70747_aH);
    }

    private float[] chooseSteering(Vec3 target, double x, double z, double motionX, double motionZ, float yaw, float acceleration) {
        double dx = target.field_72450_a - x;
        double dz = target.field_72449_c - z;
        double best = Double.POSITIVE_INFINITY;
        int bestForward = 0;
        int bestStrafe = 0;
        float sin = MathHelper.func_76126_a(yaw * (float) Math.PI / 180.0F);
        float cos = MathHelper.func_76134_b(yaw * (float) Math.PI / 180.0F);
        for (int forward = -1; forward <= 1; forward++) {
            for (int strafe = -1; strafe <= 1; strafe++) {
                float forwardInput = forward * 0.98F;
                float strafeInput = strafe * 0.98F;
                float scale = acceleration / Math.max(1.0F, MathHelper.func_76129_c(forwardInput * forwardInput + strafeInput * strafeInput));
                double velocityX = motionX + (strafeInput * cos - forwardInput * sin) * scale;
                double velocityZ = motionZ + (forwardInput * cos + strafeInput * sin) * scale;
                double errorX = dx - velocityX * 5.0;
                double errorZ = dz - velocityZ * 5.0;
                double cost = errorX * errorX + errorZ * errorZ + (forward * forward + strafe * strafe) * 1.0E-6;
                if (cost < best) {
                    best = cost;
                    bestForward = forward;
                    bestStrafe = strafe;
                }
            }
        }
        return new float[] { bestForward, bestStrafe };
    }

    private void applyMovementOverride(PrePlayerInputEvent e, Vec3 target) {
        float[] input = this.steeringInput(target, mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70161_v, mc.field_71439_g.field_70159_w, mc.field_71439_g.field_70179_y, this.getSteeringYaw());
        e.setForward(input[0]);
        e.setStrafe(input[1]);
        e.setJump(false);
        e.setSneak(false);
    }

    private Vec3 getLadderSteerTarget(BlockPos ladderPos) {
        return new Vec3(ladderPos.func_177958_n() + 0.5, ladderPos.func_177956_o() + 0.5, ladderPos.func_177952_p() + 0.5);
    }

    private double getHorizontalDistanceSq(BlockPos pos) {
        if (pos != null && mc.field_71439_g != null) {
            double dx = pos.func_177958_n() + 0.5 - mc.field_71439_g.field_70165_t;
            double dz = pos.func_177952_p() + 0.5 - mc.field_71439_g.field_70161_v;
            return dx * dx + dz * dz;
        } else {
            return Double.MAX_VALUE;
        }
    }

    private float getSteeringYaw() {
        Float yaw = RotationHelper.get().getServerYaw();
        return yaw == null ? this.session.sent().yaw : yaw;
    }

    private float getPlacementBaseYaw() {
        return this.session.sent().yaw;
    }

    private float getPlacementBasePitch() {
        return this.session.sent().pitch;
    }

    private boolean shouldYieldToClutch() {
        Module clutchModule = ModuleManager.getModule(Clutch.class);
        return clutchModule instanceof Clutch && ((Clutch) clutchModule).isActiveWindow();
    }

    int yieldToClutch() {
        int original = this.session.transferSlot();
        Module clutch = ModuleManager.getModule(Clutch.class);
        if (original >= 0 && clutch instanceof Clutch) {
            ((Clutch) clutch).adoptLadderSlot(original);
        }
        this.resetExecutionState(false, "yield-clutch");
        this.hasStickyRotation = false;
        return original;
    }

    private boolean hasIncompatibleModule() {
        return this.getBlockingModule() != null;
    }

    private String getBlockingModule() {
        return Raven.lagHandler != null && Raven.lagHandler.isBuffering() ? "Buffered packets" : null;
    }

    private boolean shouldStartPlanningNow() {
        return this.isCurrentlyFalling() && (!this.onlyWhileHoldingLadder.isToggled() || this.isLadderStack(mc.field_71439_g.func_70694_bm())) && this.findLadderSlot() != -1 && (this.fallQualified || this.minimumDropDistance.getInput() == 0.0 || this.heightAboveGround() >= this.minimumDropDistance.getInput());
    }

    private double heightAboveGround() {
        AxisAlignedBB feet = mc.field_71439_g.func_174813_aQ();
        AxisAlignedBB column = new AxisAlignedBB(feet.field_72340_a + 1.0E-7, -1.0, feet.field_72339_c + 1.0E-7, feet.field_72336_d - 1.0E-7, feet.field_72338_b + 1.0E-7, feet.field_72334_f - 1.0E-7);
        double surface = Double.NEGATIVE_INFINITY;
        List<AxisAlignedBB> boxes = new ArrayList<>();
        for (int x = MathHelper.func_76128_c(column.field_72340_a); x <= MathHelper.func_76128_c(column.field_72336_d); x++) {
            for (int z = MathHelper.func_76128_c(column.field_72339_c); z <= MathHelper.func_76128_c(column.field_72334_f); z++) {
                for (int y = MathHelper.func_76128_c(feet.field_72338_b); y >= 0 && y + 1.5 > surface; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    IBlockState state = mc.field_71441_e.func_180495_p(pos);
                    boxes.clear();
                    state.func_177230_c().func_180638_a(mc.field_71441_e, pos, state, column, boxes, mc.field_71439_g);
                    for (AxisAlignedBB box : boxes) {
                        if (box.field_72337_e <= feet.field_72338_b + 1.0E-7) {
                            surface = Math.max(surface, box.field_72337_e);
                        }
                    }
                }
            }
        }
        return feet.field_72338_b - surface;
    }

    private boolean isCurrentlyFalling() {
        return mc.field_71439_g != null && !mc.field_71439_g.field_70122_E && !mc.field_71439_g.func_70617_f_() && mc.field_71439_g.field_70181_x < 0.0 && !mc.field_71439_g.func_70090_H() && !mc.field_71439_g.func_180799_ab() && !mc.field_71439_g.field_71075_bZ.field_75100_b && !mc.field_71439_g.func_175149_v() && !mc.field_71439_g.field_70145_X && mc.field_71439_g.func_70089_S() && !mc.field_71439_g.func_70115_ae() && !mc.field_71439_g.func_71039_bw();
    }

    private boolean isPlanWithinPlacementBounds(AutoLadder.PlacementPlan plan) {
        if (plan == null || plan.ladderPos == null) {
            return false;
        } else if (plan.groundCatch && this.stage != AutoLadder.ExecutionStage.GUIDE_ENTRY) {
            return this.getPlayerFeetY() >= plan.ladderPos.func_177956_o() && this.getHorizontalDistanceSq(plan.ladderPos) <= 20.25;
        } else if (this.stage == AutoLadder.ExecutionStage.GUIDE_ENTRY) {
            double belowDelta = this.getPlayerFeetY() - plan.ladderPos.func_177956_o();
            return mc.field_71439_g != null && Math.abs(plan.ladderPos.func_177958_n() + 0.5 - mc.field_71439_g.field_70165_t) <= 4.5 && Math.abs(plan.ladderPos.func_177952_p() + 0.5 - mc.field_71439_g.field_70161_v) <= 4.5 && belowDelta >= -1.0 && belowDelta <= 3.0;
        } else if (!this.isWithinPlacementBounds(plan.ladderPos)) {
            return false;
        } else {
            return plan.ladderSupportPos != null && !this.isWithinPlacementBounds(plan.ladderSupportPos) ? false : plan.supportPlacePos == null || this.isWithinPlacementBounds(plan.supportPlacePos);
        }
    }

    private boolean isWithinPlacementBounds(BlockPos pos) {
        if (pos != null && mc.field_71439_g != null) {
            double dx = Math.abs(pos.func_177958_n() + 0.5 - mc.field_71439_g.field_70165_t);
            double dz = Math.abs(pos.func_177952_p() + 0.5 - mc.field_71439_g.field_70161_v);
            if (dx <= 4.5 && dz <= 4.5) {
                double belowDelta = this.getPlayerFeetY() - pos.func_177956_o();
                return belowDelta > -1.0 && belowDelta <= 3.0;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private double getPlayerFeetY() {
        if (mc.field_71439_g == null) {
            return 0.0;
        } else {
            return mc.field_71439_g.func_174813_aQ() != null ? mc.field_71439_g.func_174813_aQ().field_72338_b : mc.field_71439_g.field_70163_u;
        }
    }

    private boolean isReplaceableOrLadder(BlockPos pos) {
        if (pos == null) {
            return false;
        } else {
            Block block = BlockUtils.getBlock(pos);
            return block == Blocks.field_150468_ap || BlockUtils.replaceable(pos);
        }
    }

    private boolean isInsideTargetLadder(BlockPos ladderPos) {
        return ladderPos != null && mc.field_71439_g != null && mc.field_71439_g.func_70617_f_() && this.isLadderBlock(ladderPos) && this.feetOccupyLadderCell(mc.field_71439_g.field_70165_t, this.getPlayerFeetY(), mc.field_71439_g.field_70161_v, ladderPos);
    }

    private boolean doesPlayerIntersectPlacementNowOrNextTick(BlockPos pos) {
        if (mc.field_71439_g != null && pos != null) {
            AxisAlignedBB axisAlignedBB2 = new AxisAlignedBB(pos, pos.func_177982_a(1, 1, 1));
            AxisAlignedBB axisAlignedBB3 = mc.field_71439_g.func_174813_aQ();
            return axisAlignedBB3.func_72326_a(axisAlignedBB2) || axisAlignedBB3.func_72317_d(mc.field_71439_g.field_70159_w, mc.field_71439_g.field_70181_x, mc.field_71439_g.field_70179_y).func_72326_a(axisAlignedBB2);
        } else {
            return false;
        }
    }

    private boolean isLadderBlock(BlockPos pos) {
        return pos != null && BlockUtils.getBlock(pos) == Blocks.field_150468_ap;
    }

    private boolean isModuleReady() {
        if (this.isEnabled() && Utils.nullCheck() && mc.field_71442_b != null && !mc.func_147113_T()) {
            EntityPlayerSP player = mc.field_71439_g;
            return player != null && player.field_71158_b != null && mc.field_71462_r == null;
        } else {
            return false;
        }
    }

    private int getPlacementPlayerTick() {
        return Utils.nullCheck() && mc.field_71439_g != null ? this.currentPlacementTick : Integer.MIN_VALUE;
    }

    private double getPlacementReach() {
        return mc.field_71442_b == null ? 4.5 : mc.field_71442_b.func_78757_d();
    }

    private void resetExecutionState(boolean restoreOriginalSlot, String reason) {
        this.releaseMovement();
        if (this.activePlan != null || "no-valid-plan".equals(reason) || "control-conflict".equals(reason)) {
            this.retryAfterTick = this.currentPlacementTick + 3;
        }
        this.stableHit = null;
        this.stage = AutoLadder.ExecutionStage.IDLE;
        this.activePlan = null;
        this.placementHit = null;
        this.rotationTick = Integer.MIN_VALUE;
        this.lastPlacementAttemptTick = Integer.MIN_VALUE;
        this.guideStartTick = Integer.MIN_VALUE;
        this.guideInvalidTicks = 0;
        this.stageInvalidTicks = 0;
        if (restoreOriginalSlot) {
            this.restoreOriginalSlotIfNeeded();
        }
    }

    private static final class EntryEstimate {

        private final int tickAhead;

        private final double entryAlignmentErrorSq;

        private EntryEstimate(int tickAhead, double entryAlignmentErrorSq) {
            this.tickAhead = tickAhead;
            this.entryAlignmentErrorSq = entryAlignmentErrorSq;
        }

        private static AutoLadder.EntryEstimate invalid() {
            return new AutoLadder.EntryEstimate(-1, Double.MAX_VALUE);
        }

        private boolean valid() {
            return this.tickAhead >= 0;
        }
    }

    private static enum ExecutionStage {

        IDLE, PLACE_SUPPORT, PLACE_LADDER, GUIDE_ENTRY
    }

    private static final class PlacementPlan {

        private final BlockPos ladderPos;

        private final BlockPos ladderSupportPos;

        private final EnumFacing ladderFace;

        private final boolean requiresSupportPlacement;

        private final BlockPos supportPlacePos;

        private final double planCost;

        private final boolean groundCatch;

        private PlacementPlan(BlockPos ladderPos, BlockPos supportPos, EnumFacing ladderFace, boolean requiresSupportPlacement, double planCost) {
            this(ladderPos, supportPos, ladderFace, requiresSupportPlacement, planCost, false);
        }

        private PlacementPlan(BlockPos ladderPos, BlockPos supportPos, EnumFacing ladderFace, boolean requiresSupportPlacement, double planCost, boolean groundCatch) {
            this.ladderPos = ladderPos;
            this.ladderSupportPos = supportPos;
            this.ladderFace = ladderFace;
            this.requiresSupportPlacement = requiresSupportPlacement;
            this.supportPlacePos = requiresSupportPlacement ? supportPos : null;
            this.planCost = planCost;
            this.groundCatch = groundCatch;
        }
    }

    private static final class RotationHit {

        private final float yaw;

        private final float pitch;

        private final MovingObjectPosition mop;

        private final double score;

        private RotationHit(float yaw, float pitch, MovingObjectPosition mop, double score) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.mop = mop;
            this.score = score;
        }
    }
}
