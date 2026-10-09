package keystrokesmod.module.impl.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Map;
import keystrokesmod.Raven;
import keystrokesmod.event.AttackEvent;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreAttackEvent;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.event.RightClickMouseEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.lag.api.EnumLagDirection;
import keystrokesmod.lag.api.LagRequest;
import keystrokesmod.lag.timeout.ModuleBackedTimeout;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.CombatTargeting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos.MutableBlockPos;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class Displace extends Module {

    private static final int DISPLACE_WINDOW_TICKS = 10;

    private static final int DISPLACEMENT_LOCK_IDLE_TICKS = 20;

    private static final int VOID_SCAN_DIRECTIONS = 48;

    private static final double VOID_SCAN_STEP = 0.5;

    private static final double VOID_COLLISION_STEP = 0.25;

    private static final double VOID_COLLISION_INSET = 0.03;

    private static final double VOID_REFINEMENT_STEP = 1.5;

    private static final double VOID_SCORE_EPSILON = 1.0E-4;

    private static final long VOID_DEBUG_DURATION_MS = 30000L;

    private static final double VOID_DEBUG_PROBE_Y_OFFSET = 0.08;

    private static final long ARROW_FADE_MS = 250L;

    private static final double ARROW_BASE_GAP = 0.3;

    private static final long ARROW_NUDGE_MS = 320L;

    private static final double ARROW_NUDGE_DISTANCE = 0.3;

    private static final double ARROW_NUDGE_DECAY = 6.0;

    private static final double ARROW_NUDGE_FADE_START = 0.58;

    private static final double ARROW_NUDGE_FADE_DECAY = 5.0;

    private static final int OVERRIDE_MAX_FLICK_TICKS = 5;

    private static final int OVERRIDE_ATTACK_TICKS = 2;

    private static final int OVERRIDE_RESTORE_TICKS = 2;

    private static final float OVERRIDE_FLICK_TOLERANCE = 6.0F;

    private static final double OVERRIDE_TARGET_RANGE_SQ = 25.0;

    private final SliderSetting mode;

    private final SliderSetting yawOffset;

    private final SliderSetting scanRadius;

    private final SliderSetting delay;

    private final SliderSetting direction;

    private final ButtonSetting findVoid;

    private final ButtonSetting ignoreOneBlockWall;

    private final ButtonSetting blink;

    private final ButtonSetting overrideAttack;

    private final ButtonSetting renderArrow;

    private final ButtonSetting onlyKnockbackItems;

    private final ButtonSetting ignoreTeammates;

    private final ButtonSetting weaponOnly;

    private boolean displaceThisTick = false;

    private boolean active = false;

    private boolean hasKB = false;

    private boolean compensateNextTick = false;

    private boolean displaceLeft = false;

    private boolean wasDisplacingLastTick = false;

    private boolean releaseBlinkNextGameTick = false;

    private EntityPlayer arrowPlayer;

    private float arrowYaw;

    private float arrowFadeStartAlpha;

    private float arrowFadeEndAlpha;

    private long arrowFadeStartMs;

    private boolean arrowVisible;

    private long arrowNudgeStartMs = -1L;

    private int arrowNudgePlayerId = -1;

    private int tickCounter;

    private final Map<Integer, Integer> targetWindowStartTicks = new HashMap<>();

    private final Map<Integer, Displace.DisplacementLock> targetDisplacementLocks = new HashMap<>();

    private LagRequest outboundBlink;

    private Displace.VoidDebugScan latestVoidDebugScan;

    private Displace.VoidDebugScan frozenVoidDebugScan;

    private long frozenVoidDebugExpiresAtMs;

    private Displace.OverrideAttackState overrideAttackState = Displace.OverrideAttackState.IDLE;

    private EntityPlayer overrideTarget;

    private float overrideTargetYaw;

    private float overrideTargetPitch;

    private float overrideFlickYaw;

    private float overrideFlickOffset;

    private boolean overrideAbsoluteFlickYaw;

    private boolean overrideAwayPacketSent;

    private boolean overrideAttackInProgress;

    private boolean overrideAttackPacketSent;

    private boolean suppressUseForOverrideAttackTick;

    private int overrideStateTicks;

    private long lastOverrideFlickMs = -1L;

    private static final String[] MODES = new String[] { "Offset", "Void" };

    private static final String[] DIRECTIONS = new String[] { "Left", "Right" };

    public Displace() {
        super("Displace", Module.category.combat);
        this.registerSetting(this.mode = new SliderSetting("Mode", 0, MODES));
        this.registerSetting(this.yawOffset = new SliderSetting("Yaw offset", 90.0, 0.0, 180.0, 1.0));
        this.registerSetting(this.scanRadius = new SliderSetting("Scan radius", " block", 6.0, 1.0, 12.0, 0.5));
        this.registerSetting(this.delay = new SliderSetting("Delay", "ms", 0.0, 0.0, 500.0, 50.0));
        this.registerSetting(this.direction = new SliderSetting("Direction", 0, DIRECTIONS));
        this.registerSetting(this.findVoid = new ButtonSetting("Find void", false));
        this.registerSetting(this.blink = new ButtonSetting("Blink", false));
        this.registerSetting(this.ignoreOneBlockWall = new ButtonSetting("Ignore one block wall", false));
        this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.registerSetting(this.onlyKnockbackItems = new ButtonSetting("Only knockback items", false, "Has knockback"));
        this.registerSetting(this.overrideAttack = new ButtonSetting("Override attack", false));
        this.registerSetting(this.renderArrow = new ButtonSetting("Render arrow", true));
        this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
        this.liteModule = true;
        this.closetModule = true;
    }

    @Override
    public void guiUpdate() {
        boolean offsetMode = !this.isVoidMode();
        this.direction.setVisible(offsetMode, this);
        this.findVoid.setVisible(offsetMode, this);
        this.scanRadius.setVisible(!offsetMode, this);
        this.ignoreOneBlockWall.setVisible(!offsetMode, this);
    }

    @Override
    public String getInfo() {
        return MODES[(int) this.mode.getInput()];
    }

    @Override
    public void onEnable() {
        this.displaceThisTick = false;
        this.active = false;
        this.hasKB = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.releaseBlinkNextGameTick = false;
        this.clearArrow();
        this.clearVoidDebugState();
        this.tickCounter = 0;
        this.targetWindowStartTicks.clear();
        this.targetDisplacementLocks.clear();
        this.resetOverrideAttackState();
        this.lastOverrideFlickMs = -1L;
        this.releaseBlink();
    }

    @Override
    public void onDisable() {
        this.active = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.releaseBlinkNextGameTick = false;
        this.clearArrow();
        this.clearVoidDebugState();
        this.targetWindowStartTicks.clear();
        this.targetDisplacementLocks.clear();
        this.resetOverrideAttackState();
    }

    @Override
    public void guiButtonToggled(ButtonSetting button) {
        if (button == this.overrideAttack) {
            this.resetOverrideAttackState();
            this.releaseBlinkNextGameTick = false;
        }
    }

    private static int msToTicks(double ms) {
        return ms <= 0.0 ? 0 : (int) Math.ceil(ms / 50.0);
    }

    private boolean anyMovementKey() {
        return mc.field_71474_y.field_74351_w.func_151470_d() || mc.field_71474_y.field_74368_y.func_151470_d() || mc.field_71474_y.field_74370_x.func_151470_d() || mc.field_71474_y.field_74366_z.func_151470_d();
    }

    private boolean isVoidMode() {
        return this.mode.getInput() == 1.0;
    }

    private boolean tryFindVoidDirection(EntityPlayer target) {
        double dx = target.field_70165_t - mc.field_71439_g.field_70165_t;
        double dz = target.field_70161_v - mc.field_71439_g.field_70161_v;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001) {
            return false;
        } else {
            dx /= dist;
            dz /= dist;
            double rightX = -dz;
            double rightZ = dx;
            double eyeY = target.field_70163_u + target.func_70047_e();
            int leftVoidCount = 0;
            int rightVoidCount = 0;
            for (int i = 1; i <= 12; i++) {
                double off = i * 0.5;
                double rx = target.field_70165_t + rightX * off;
                double rz = target.field_70161_v + rightZ * off;
                if (mc.field_71441_e.func_72933_a(new Vec3(rx, eyeY, rz), new Vec3(rx, eyeY - 10.0, rz)) == null) {
                    rightVoidCount++;
                }
                double lx = target.field_70165_t - rightX * off;
                double lz = target.field_70161_v - rightZ * off;
                if (mc.field_71441_e.func_72933_a(new Vec3(lx, eyeY, lz), new Vec3(lx, eyeY - 10.0, lz)) == null) {
                    leftVoidCount++;
                }
            }
            if (leftVoidCount == 0 && rightVoidCount == 0) {
                return false;
            } else {
                if (leftVoidCount != rightVoidCount) {
                    this.displaceLeft = leftVoidCount > rightVoidCount;
                }
                return true;
            }
        }
    }

    private Float findBestVoidYaw(EntityPlayer target, float playerYaw) {
        if (target != null && mc.field_71439_g != null && mc.field_71441_e != null) {
            float offset = (float) this.yawOffset.getInput();
            float preferredYawPositive = MathHelper.func_76142_g(playerYaw + offset);
            float preferredYawNegative = MathHelper.func_76142_g(playerYaw - offset);
            double scanAngleStep = 7.5;
            double scanDistance = this.scanRadius.getInput();
            Map<Long, Boolean> voidColumns = new HashMap<>();
            Map<Long, Displace.VoidNeighborhood> voidNeighborhoods = new HashMap<>();
            Displace.VoidDebugScan debugScan = Raven.DEBUG ? new Displace.VoidDebugScan(mc.field_71441_e, target.func_145782_y(), target.field_70165_t, target.field_70163_u, target.field_70161_v, scanDistance, preferredYawPositive, preferredYawNegative) : null;
            if (debugScan == null) {
                this.latestVoidDebugScan = null;
            }
            Displace.VoidYawCandidate best = null;
            for (int intValue = 0; intValue < 48; intValue++) {
                float candidateYaw = MathHelper.func_76142_g(preferredYawPositive + (float) (intValue * scanAngleStep));
                best = this.selectBetterVoidYaw(best, candidateYaw, preferredYawPositive, preferredYawNegative, target, scanDistance, voidColumns, voidNeighborhoods, debugScan);
            }
            if (best == null) {
                this.latestVoidDebugScan = debugScan;
                return null;
            } else {
                float coarseYaw = best.yaw;
                for (double refinementOffset = -scanAngleStep + 1.5; refinementOffset < scanAngleStep; refinementOffset += 1.5) {
                    if (!(Math.abs(refinementOffset) < 1.0E-4)) {
                        float candidateYaw = MathHelper.func_76142_g(coarseYaw + (float) refinementOffset);
                        best = this.selectBetterVoidYaw(best, candidateYaw, preferredYawPositive, preferredYawNegative, target, scanDistance, voidColumns, voidNeighborhoods, debugScan);
                    }
                }
                if (debugScan != null) {
                    debugScan.bestCandidate = best.debugCandidate;
                    this.latestVoidDebugScan = debugScan;
                }
                return best.yaw;
            }
        } else {
            return null;
        }
    }

    private Displace.VoidYawCandidate selectBetterVoidYaw(Displace.VoidYawCandidate currentBest, float candidateYaw, float preferredYawPositive, float preferredYawNegative, EntityPlayer target, double scanDistance, Map<Long, Boolean> voidColumns, Map<Long, Displace.VoidNeighborhood> voidNeighborhoods, Displace.VoidDebugScan debugScan) {
        double radians = Math.toRadians(candidateYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double positiveDistance = Math.abs(MathHelper.func_76142_g(candidateYaw - preferredYawPositive));
        double negativeDistance = Math.abs(MathHelper.func_76142_g(candidateYaw - preferredYawNegative));
        double priorityDistance = Math.min(positiveDistance, negativeDistance);
        Displace.VoidDebugCandidate debugCandidate = debugScan == null ? null : new Displace.VoidDebugCandidate(candidateYaw, forwardX, forwardZ, priorityDistance);
        Displace.VoidPathScore pathScore = this.scoreVoidPath(target, forwardX, forwardZ, scanDistance, voidColumns, voidNeighborhoods, debugCandidate);
        if (debugCandidate != null) {
            debugCandidate.pathScore = pathScore;
            debugScan.candidates.add(debugCandidate);
        }
        if (pathScore == null) {
            return currentBest;
        } else {
            Displace.VoidYawCandidate candidate = new Displace.VoidYawCandidate(candidateYaw, pathScore, priorityDistance, debugCandidate);
            return currentBest != null && candidate.compareTo(currentBest) <= 0 ? currentBest : candidate;
        }
    }

    private Displace.VoidPathScore scoreVoidPath(EntityPlayer target, double forwardX, double forwardZ, double scanDistance, Map<Long, Boolean> voidColumns, Map<Long, Displace.VoidNeighborhood> voidNeighborhoods, Displace.VoidDebugCandidate debugCandidate) {
        double sideX = -forwardZ;
        double sideZ = forwardX;
        double sideOffset = Math.max(0.2, target.field_70130_N * 0.45);
        double checkedForward = 0.0;
        double pathQuality = 0.0;
        int consecutiveCenterVoid = 0;
        int longestCenterVoidRun = 0;
        int bestNeighborhoodQuality = -1;
        int bestDestinationWidth = 0;
        double bestDestinationDistance = Double.MAX_VALUE;
        double bestDestinationX = 0.0;
        double bestDestinationZ = 0.0;
        Displace.VoidNeighborhood bestNeighborhood = null;
        AxisAlignedBB axisAlignedBB3 = target.func_174813_aQ().func_72331_e(0.03, 0.001, 0.03);
        for (double forward = 0.5; forward <= scanDistance + 1.0E-4; forward += 0.5) {
            double blockedDistance = this.getVoidPathBlockedDistance(axisAlignedBB3, forwardX, forwardZ, checkedForward, forward);
            if (blockedDistance >= 0.0) {
                if (debugCandidate != null) {
                    debugCandidate.blockedDistance = blockedDistance;
                }
                break;
            }
            checkedForward = forward;
            if (debugCandidate != null) {
                debugCandidate.reachedDistance = forward;
            }
            double centerX = target.field_70165_t + forwardX * forward;
            double centerZ = target.field_70161_v + forwardZ * forward;
            boolean centerVoid = this.isVoidColumn(centerX, target.field_70163_u, centerZ, voidColumns);
            boolean leftVoid = this.isVoidColumn(centerX - sideX * sideOffset, target.field_70163_u, centerZ - sideZ * sideOffset, voidColumns);
            boolean rightVoid = this.isVoidColumn(centerX + sideX * sideOffset, target.field_70163_u, centerZ + sideZ * sideOffset, voidColumns);
            double distanceWeight = scanDistance + 0.5 - forward;
            if (debugCandidate != null) {
                debugCandidate.probes.add(new Displace.VoidDebugProbe(centerX - sideX * sideOffset, centerZ - sideZ * sideOffset, leftVoid, centerX, centerZ, centerVoid, centerX + sideX * sideOffset, centerZ + sideZ * sideOffset, rightVoid));
            }
            if (leftVoid) {
                pathQuality += distanceWeight;
            }
            if (rightVoid) {
                pathQuality += distanceWeight;
            }
            if (centerVoid) {
                longestCenterVoidRun = Math.max(longestCenterVoidRun, ++consecutiveCenterVoid);
                pathQuality += distanceWeight * 1.6 + consecutiveCenterVoid * 2.0;
                Displace.VoidNeighborhood neighborhood = this.getVoidNeighborhood(centerX, target.field_70163_u, centerZ, voidColumns, voidNeighborhoods);
                int neighborhoodQuality = neighborhood.quality;
                int destinationWidth = 1 + (leftVoid ? 1 : 0) + (rightVoid ? 1 : 0);
                if (neighborhoodQuality > bestNeighborhoodQuality || neighborhoodQuality == bestNeighborhoodQuality && destinationWidth > bestDestinationWidth || neighborhoodQuality == bestNeighborhoodQuality && destinationWidth == bestDestinationWidth && forward < bestDestinationDistance) {
                    bestNeighborhoodQuality = neighborhoodQuality;
                    bestDestinationWidth = destinationWidth;
                    bestDestinationDistance = forward;
                    bestDestinationX = centerX;
                    bestDestinationZ = centerZ;
                    bestNeighborhood = neighborhood;
                }
            } else {
                consecutiveCenterVoid = 0;
            }
        }
        return bestNeighborhoodQuality < 0 ? null : new Displace.VoidPathScore(bestNeighborhood.immediateVoidNeighbors, bestNeighborhood.extendedVoidNeighbors, bestDestinationWidth, longestCenterVoidRun, pathQuality, bestDestinationDistance, bestDestinationX, bestDestinationZ, bestNeighborhood.blockX, bestNeighborhood.blockZ, bestNeighborhood.voidMask);
    }

    private Displace.VoidNeighborhood getVoidNeighborhood(double x, double y, double z, Map<Long, Boolean> voidColumns, Map<Long, Displace.VoidNeighborhood> voidNeighborhoods) {
        int blockX = MathHelper.func_76128_c(x);
        int blockZ = MathHelper.func_76128_c(z);
        long columnKey = this.getColumnKey(blockX, blockZ);
        Displace.VoidNeighborhood cached = voidNeighborhoods.get(columnKey);
        if (cached != null) {
            return cached;
        } else {
            int immediateVoidNeighbors = 0;
            int extendedVoidNeighbors = 0;
            long voidMask = 0L;
            for (int offsetX = -2; offsetX <= 2; offsetX++) {
                for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
                    int radius = Math.max(Math.abs(offsetX), Math.abs(offsetZ));
                    if (radius != 0 && this.isVoidColumn(blockX + offsetX + 0.5, y, blockZ + offsetZ + 0.5, voidColumns)) {
                        int intValue2 = (offsetX + 2) * 5 + offsetZ + 2;
                        voidMask |= 1L << intValue2;
                        if (radius == 1) {
                            immediateVoidNeighbors++;
                        } else {
                            extendedVoidNeighbors++;
                        }
                    }
                }
            }
            int quality = immediateVoidNeighbors * 100 + extendedVoidNeighbors;
            Displace.VoidNeighborhood neighborhood = new Displace.VoidNeighborhood(blockX, blockZ, immediateVoidNeighbors, extendedVoidNeighbors, quality, voidMask);
            voidNeighborhoods.put(columnKey, neighborhood);
            return neighborhood;
        }
    }

    private double getVoidPathBlockedDistance(AxisAlignedBB axisAlignedBB, double forwardX, double forwardZ, double fromForward, double toForward) {
        double forward = fromForward + 0.25;
        while (forward <= toForward + 1.0E-4) {
            AxisAlignedBB axisAlignedBB4 = axisAlignedBB.func_72317_d(forwardX * forward, 0.0, forwardZ * forward);
            BlockPos minCorner = new BlockPos(axisAlignedBB4.field_72340_a, axisAlignedBB4.field_72338_b, axisAlignedBB4.field_72339_c);
            BlockPos maxCorner = new BlockPos(axisAlignedBB4.field_72336_d, axisAlignedBB4.field_72337_e, axisAlignedBB4.field_72334_f);
            if (mc.field_71441_e.func_175667_e(minCorner) && mc.field_71441_e.func_175667_e(maxCorner)) {
                List<AxisAlignedBB> blockCollisions = mc.field_71441_e.func_147461_a(axisAlignedBB4);
                if (blockCollisions.isEmpty() || this.ignoreOneBlockWall.isToggled() && this.isOneBlockWall(axisAlignedBB4, blockCollisions)) {
                    forward += 0.25;
                    continue;
                }
                return forward;
            }
            return forward;
        }
        return -1.0;
    }

    private boolean isOneBlockWall(AxisAlignedBB axisAlignedBB2, List<AxisAlignedBB> blockCollisions) {
        double maximumWallY = axisAlignedBB2.field_72338_b + 1.0 + 1.0E-4;
        for (AxisAlignedBB collision : blockCollisions) {
            if (collision.field_72337_e > maximumWallY) {
                return false;
            }
        }
        return true;
    }

    private boolean isVoidColumn(double x, double y, double z, Map<Long, Boolean> voidColumns) {
        int blockX = MathHelper.func_76128_c(x);
        int blockZ = MathHelper.func_76128_c(z);
        long columnKey = this.getColumnKey(blockX, blockZ);
        Boolean cached = voidColumns.get(columnKey);
        if (cached != null) {
            return cached;
        } else {
            int startY = Math.min(255, MathHelper.func_76128_c(y - 0.01));
            MutableBlockPos blockPos = new MutableBlockPos(blockX, Math.max(0, startY), blockZ);
            if (!mc.field_71441_e.func_175667_e(blockPos)) {
                voidColumns.put(columnKey, false);
                return false;
            } else {
                for (int blockY = startY; blockY >= 0; blockY--) {
                    blockPos.func_181079_c(blockX, blockY, blockZ);
                    if (!mc.field_71441_e.func_175623_d(blockPos)) {
                        voidColumns.put(columnKey, false);
                        return false;
                    }
                }
                voidColumns.put(columnKey, true);
                return true;
            }
        }
    }

    private long getColumnKey(int blockX, int blockZ) {
        return (long) blockX << 32 ^ blockZ & 4294967295L;
    }

    private void updateDisplaceSide(float playerYaw, float displaceYaw) {
        this.displaceLeft = MathHelper.func_76142_g(displaceYaw - playerYaw) < 0.0F;
    }

    private void pruneTargetDelayStates() {
        if (mc.field_71441_e == null) {
            this.targetWindowStartTicks.clear();
            this.targetDisplacementLocks.clear();
        } else {
            Iterator<Entry<Integer, Integer>> iterator = this.targetWindowStartTicks.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<Integer, Integer> entry = iterator.next();
                Entity entity = mc.field_71441_e.func_73045_a(entry.getKey());
                if (!(entity instanceof EntityPlayer) || entity.field_70128_L || ((EntityPlayer) entity).field_70725_aQ != 0) {
                    iterator.remove();
                }
            }
            Iterator<Entry<Integer, Displace.DisplacementLock>> lockIterator = this.targetDisplacementLocks.entrySet().iterator();
            while (lockIterator.hasNext()) {
                Entry<Integer, Displace.DisplacementLock> entry = lockIterator.next();
                Entity entity = mc.field_71441_e.func_73045_a(entry.getKey());
                if (entity instanceof EntityPlayer && !entity.field_70128_L && ((EntityPlayer) entity).field_70725_aQ == 0) {
                    EntityPlayer player = (EntityPlayer) entity;
                    Displace.DisplacementLock lock = entry.getValue();
                    if (!player.field_70122_E) {
                        lock.airborne = true;
                    } else if (lock.airborne || this.tickCounter - lock.lastUseTick >= 20) {
                        lockIterator.remove();
                    }
                } else {
                    lockIterator.remove();
                }
            }
        }
    }

    private float lockDisplacementYaw(EntityPlayer target, float yaw) {
        int targetId = target.func_145782_y();
        Displace.DisplacementLock lock = this.targetDisplacementLocks.get(targetId);
        if (lock != null && (lock.airborne && target.field_70122_E || this.tickCounter - lock.lastUseTick >= 20)) {
            this.targetDisplacementLocks.remove(targetId);
            lock = null;
        }
        if (lock == null) {
            lock = new Displace.DisplacementLock(yaw, this.tickCounter, !target.field_70122_E);
            this.targetDisplacementLocks.put(targetId, lock);
        } else {
            lock.lastUseTick = this.tickCounter;
            if (!target.field_70122_E) {
                lock.airborne = true;
            }
        }
        return lock.yaw;
    }

    private float getLockedDisplacementYaw(EntityPlayer target, float fallbackYaw) {
        Displace.DisplacementLock lock = this.targetDisplacementLocks.get(target.func_145782_y());
        return lock == null ? fallbackYaw : lock.yaw;
    }

    private boolean shouldDisplaceInCurrentWindow(EntityPlayer target, int currentTick) {
        if (target == null) {
            return true;
        } else {
            int targetId = target.func_145782_y();
            Integer windowStartTick = this.targetWindowStartTicks.get(targetId);
            if (windowStartTick != null && currentTick - windowStartTick < 10) {
                int delayTicks = msToTicks(this.delay.getInput());
                if (delayTicks <= 0) {
                    return true;
                } else {
                    int elapsed = currentTick - windowStartTick;
                    return elapsed >= delayTicks;
                }
            } else {
                this.targetWindowStartTicks.put(targetId, currentTick);
                return true;
            }
        }
    }

    private void releaseBlink() {
        if (this.outboundBlink != null) {
            this.outboundBlink.getTimeout().forceTimeOut();
            this.outboundBlink = null;
        }
    }

    private boolean isOverrideAttackEnabled() {
        return this.overrideAttack.isToggled();
    }

    private boolean isOverrideTargetValid(EntityPlayer target) {
        return target != null && target.field_70170_p == mc.field_71441_e && CombatTargeting.asValidPlayer(target, 25.0, this.ignoreTeammates.isToggled()) != null;
    }

    private boolean passesOverrideItemConditions() {
        return this.onlyKnockbackItems.isToggled() && EnchantmentHelper.func_77501_a(mc.field_71439_g) <= 0 ? false : !this.weaponOnly.isToggled() || Utils.holdingWeapon();
    }

    private boolean canStartOverrideAttack(EntityPlayer target) {
        if (this.overrideAttackState == Displace.OverrideAttackState.IDLE && Utils.nullCheck() && mc.field_71462_r == null && this.isOverrideTargetValid(target) && this.passesOverrideItemConditions()) {
            long configuredDelay = (long) this.delay.getInput();
            return this.lastOverrideFlickMs < 0L || configuredDelay <= 0L || System.currentTimeMillis() - this.lastOverrideFlickMs >= configuredDelay;
        } else {
            return false;
        }
    }

    private float[] getOverrideTargetRotations(EntityPlayer target, float baseYaw, float basePitch) {
        return RotationUtils.getRotations(target, 100.0, 100.0, baseYaw, basePitch);
    }

    private void startOverrideBlink() {
        this.releaseBlink();
        this.releaseBlinkNextGameTick = false;
        if (this.blink.isToggled()) {
            this.outboundBlink = new LagRequest(EnumLagDirection.ONLY_OUTBOUND, new ModuleBackedTimeout(this));
            Raven.lagHandler.requestLag(this.outboundBlink);
        }
    }

    private void releaseUseForOverrideAttack() {
        if (Utils.nullCheck() && mc.field_71442_b != null) {
            if (mc.field_71439_g.func_71039_bw()) {
                mc.field_71442_b.func_78766_c(mc.field_71439_g);
            } else if (ModuleUtils.isBlocked) {
                PacketUtils.sendReleasePacket();
            }
        }
    }

    private boolean startOverrideAttack(EntityPlayer target) {
        if (!this.canStartOverrideAttack(target)) {
            return false;
        } else {
            float baseYaw = RotationUtils.serverRotations[0];
            float basePitch = RotationUtils.serverRotations[1];
            float[] targetRotations = this.getOverrideTargetRotations(target, baseYaw, basePitch);
            if (targetRotations == null) {
                return false;
            } else {
                this.overrideTargetYaw = targetRotations[0];
                this.overrideTargetPitch = targetRotations[1];
                this.overrideAbsoluteFlickYaw = this.isVoidMode();
                if (this.overrideAbsoluteFlickYaw) {
                    Float bestVoidYaw = this.findBestVoidYaw(target, this.overrideTargetYaw);
                    if (bestVoidYaw == null) {
                        return false;
                    }
                    this.overrideFlickYaw = bestVoidYaw;
                    this.overrideFlickOffset = 0.0F;
                    this.updateDisplaceSide(this.overrideTargetYaw, this.overrideFlickYaw);
                } else {
                    if (!this.findVoid.isToggled() || !this.tryFindVoidDirection(target)) {
                        this.displaceLeft = this.direction.getInput() == 0.0;
                    }
                    this.overrideFlickOffset = this.displaceLeft ? -((float) this.yawOffset.getInput()) : (float) this.yawOffset.getInput();
                    this.overrideFlickYaw = this.overrideTargetYaw + this.overrideFlickOffset;
                }
                this.overrideFlickYaw = this.lockDisplacementYaw(target, this.overrideFlickYaw);
                this.updateDisplaceSide(this.overrideTargetYaw, this.overrideFlickYaw);
                this.overrideTarget = target;
                this.overrideAttackState = Displace.OverrideAttackState.FLICKING_AWAY;
                this.overrideStateTicks = 0;
                this.overrideAwayPacketSent = false;
                this.overrideAttackInProgress = false;
                this.overrideAttackPacketSent = false;
                this.suppressUseForOverrideAttackTick = false;
                this.lastOverrideFlickMs = System.currentTimeMillis();
                this.active = true;
                this.displaceThisTick = true;
                this.hasKB = EnchantmentHelper.func_77501_a(mc.field_71439_g) > 0;
                this.compensateNextTick = false;
                this.wasDisplacingLastTick = false;
                this.showArrow(target, this.overrideFlickYaw);
                this.releaseUseForOverrideAttack();
                this.startOverrideBlink();
                return true;
            }
        }
    }

    private void resetOverrideAttackState() {
        this.releaseBlink();
        this.overrideAttackState = Displace.OverrideAttackState.IDLE;
        this.overrideTarget = null;
        this.overrideTargetYaw = 0.0F;
        this.overrideTargetPitch = 0.0F;
        this.overrideFlickYaw = 0.0F;
        this.overrideFlickOffset = 0.0F;
        this.overrideAbsoluteFlickYaw = false;
        this.overrideAwayPacketSent = false;
        this.overrideAttackInProgress = false;
        this.overrideAttackPacketSent = false;
        this.suppressUseForOverrideAttackTick = false;
        this.overrideStateTicks = 0;
        this.active = false;
        this.displaceThisTick = false;
        this.compensateNextTick = false;
        this.wasDisplacingLastTick = false;
        this.hideArrow();
    }

    private void updateOverrideRotation(ClientRotationEvent event) {
        if (!this.isOverrideTargetValid(this.overrideTarget)) {
            this.resetOverrideAttackState();
        } else {
            float baseYaw = event.yaw != null ? event.yaw : RotationUtils.serverRotations[0];
            float basePitch = event.pitch != null ? event.pitch : RotationUtils.serverRotations[1];
            float[] targetRotations = this.getOverrideTargetRotations(this.overrideTarget, baseYaw, basePitch);
            if (targetRotations == null) {
                this.resetOverrideAttackState();
            } else {
                this.overrideTargetYaw = targetRotations[0];
                this.overrideTargetPitch = targetRotations[1];
                if (!this.overrideAbsoluteFlickYaw) {
                    this.overrideFlickYaw = this.getLockedDisplacementYaw(this.overrideTarget, this.overrideTargetYaw + this.overrideFlickOffset);
                }
                this.updateDisplaceSide(this.overrideTargetYaw, this.overrideFlickYaw);
                if (this.overrideAttackState == Displace.OverrideAttackState.FLICKING_AWAY) {
                    event.yaw = this.overrideFlickYaw;
                    this.displaceThisTick = true;
                } else {
                    event.yaw = this.overrideTargetYaw;
                    this.displaceThisTick = false;
                }
                event.pitch = this.overrideTargetPitch;
                this.active = true;
                this.showArrow(this.overrideTarget, this.overrideFlickYaw);
                RotationHelper.get().forceMovementFix = true;
            }
        }
    }

    private void advanceOverrideAttackState() {
        if (this.overrideAttackState != Displace.OverrideAttackState.IDLE) {
            if (Utils.nullCheck() && mc.field_71462_r == null && this.isOverrideTargetValid(this.overrideTarget)) {
                this.overrideStateTicks++;
                switch(this.overrideAttackState) {
                    case FLICKING_AWAY:
                        if (this.overrideAwayPacketSent || this.overrideStateTicks >= 5) {
                            this.overrideAttackState = Displace.OverrideAttackState.ATTACKING;
                            this.overrideStateTicks = 0;
                            this.displaceThisTick = false;
                        }
                        break;
                    case ATTACKING:
                        if (this.overrideStateTicks >= 2) {
                            this.resetOverrideAttackState();
                        }
                        break;
                    case RESTORING:
                        if (this.overrideStateTicks >= 2) {
                            this.resetOverrideAttackState();
                        }
                }
            } else {
                this.resetOverrideAttackState();
            }
        }
    }

    private void clearVoidDebugState() {
        this.latestVoidDebugScan = null;
        this.frozenVoidDebugScan = null;
        this.frozenVoidDebugExpiresAtMs = 0L;
    }

    private void showArrow(EntityPlayer player, float yaw) {
        long now = System.currentTimeMillis();
        boolean playerChanged = this.arrowPlayer != player;
        float currentAlpha = playerChanged ? 0.0F : this.getArrowAlpha(now);
        if (playerChanged) {
            this.clearArrowNudge();
        }
        this.arrowPlayer = player;
        this.arrowYaw = yaw;
        if (!this.arrowVisible || playerChanged) {
            this.arrowFadeStartAlpha = currentAlpha;
            this.arrowFadeEndAlpha = 1.0F;
            this.arrowFadeStartMs = now;
        }
        this.arrowVisible = true;
    }

    private void hideArrow() {
        if (this.arrowPlayer != null && this.arrowVisible) {
            long now = System.currentTimeMillis();
            this.arrowFadeStartAlpha = this.getArrowAlpha(now);
            this.arrowFadeEndAlpha = 0.0F;
            this.arrowFadeStartMs = now;
            this.arrowVisible = false;
        }
    }

    private float getArrowAlpha(long now) {
        float progress = Math.min(1.0F, Math.max(0.0F, (float) (now - this.arrowFadeStartMs) / 250.0F));
        return this.arrowFadeStartAlpha + (this.arrowFadeEndAlpha - this.arrowFadeStartAlpha) * progress;
    }

    private void triggerArrowNudge(EntityPlayer player) {
        if (player != null && this.arrowPlayer == player) {
            long now = System.currentTimeMillis();
            if (this.arrowNudgePlayerId != player.func_145782_y() || now - this.arrowNudgeStartMs >= 320L) {
                this.arrowNudgePlayerId = player.func_145782_y();
                this.arrowNudgeStartMs = now;
            }
        }
    }

    private double getArrowNudgeProgress(EntityPlayer player, long now) {
        if (player != null && this.arrowNudgePlayerId == player.func_145782_y() && this.arrowNudgeStartMs >= 0L) {
            double progress = (now - this.arrowNudgeStartMs) / 320.0;
            return Math.min(1.0, Math.max(0.0, progress));
        } else {
            return -1.0;
        }
    }

    private double getArrowNudgeOffset(double progress) {
        if (progress < 0.0) {
            return 0.0;
        } else {
            double endValue = Math.exp(-6.0);
            double movement = (1.0 - Math.exp(-6.0 * progress)) / (1.0 - endValue);
            return movement * 0.3;
        }
    }

    private float getArrowNudgeAlpha(double progress) {
        if (!(progress < 0.0) && !(progress <= 0.58)) {
            double fadeProgress = (progress - 0.58) / 0.42000000000000004;
            double endValue = Math.exp(-5.0);
            double alpha = (Math.exp(-5.0 * fadeProgress) - endValue) / (1.0 - endValue);
            return (float) alpha;
        } else {
            return 1.0F;
        }
    }

    private void clearArrowNudge() {
        this.arrowNudgeStartMs = -1L;
        this.arrowNudgePlayerId = -1;
    }

    private void clearArrow() {
        this.arrowPlayer = null;
        this.arrowFadeStartAlpha = 0.0F;
        this.arrowFadeEndAlpha = 0.0F;
        this.arrowFadeStartMs = 0L;
        this.arrowVisible = false;
        this.clearArrowNudge();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck()) {
            this.clearArrow();
            this.clearVoidDebugState();
        } else {
            this.renderFrozenVoidDebug();
            if (!this.renderArrow.isToggled()) {
                this.clearArrow();
            } else if (this.arrowPlayer != null && !this.arrowPlayer.field_70128_L && this.arrowPlayer.field_70725_aQ == 0) {
                long now = System.currentTimeMillis();
                double nudgeProgress = this.getArrowNudgeProgress(this.arrowPlayer, now);
                float alpha = this.getArrowAlpha(now) * this.getArrowNudgeAlpha(nudgeProgress);
                if (alpha <= 0.0F) {
                    if (!this.arrowVisible) {
                        this.clearArrow();
                    }
                } else {
                    this.drawArrow(this.arrowPlayer, this.arrowYaw, e.partialTicks, alpha, this.getArrowNudgeOffset(nudgeProgress));
                }
            } else {
                this.clearArrow();
            }
        }
    }

    private void renderFrozenVoidDebug() {
        Displace.VoidDebugScan scan = this.frozenVoidDebugScan;
        if (Raven.DEBUG && scan != null) {
            long now = System.currentTimeMillis();
            if (now < this.frozenVoidDebugExpiresAtMs && scan.world == mc.field_71441_e) {
                List<Displace.VoidDebugCandidate> rankedCandidates = new ArrayList<>(scan.candidates);
                Collections.sort(rankedCandidates, new Comparator<Displace.VoidDebugCandidate>() {

                    public int compare(Displace.VoidDebugCandidate first, Displace.VoidDebugCandidate second) {
                        return -Displace.this.compareVoidDebugCandidates(first, second);
                    }
                });
                if (scan.bestCandidate != null) {
                    rankedCandidates.remove(scan.bestCandidate);
                    rankedCandidates.add(0, scan.bestCandidate);
                }
                double renderY = scan.originY + 0.08;
                double viewerX = mc.func_175598_ae().field_78730_l;
                double viewerY = mc.func_175598_ae().field_78731_m;
                double viewerZ = mc.func_175598_ae().field_78728_n;
                GL11.glPushMatrix();
                GL11.glPushAttrib(1048575);
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(3553);
                GL11.glDisable(2896);
                GL11.glDisable(2929);
                GL11.glDisable(2884);
                GL11.glDepthMask(false);
                GL11.glEnable(2848);
                for (Displace.VoidDebugCandidate candidate : scan.candidates) {
                    int rank = rankedCandidates.indexOf(candidate);
                    int color = this.getVoidDebugRankColor(rank, rankedCandidates.size());
                    double rayLength = candidate.reachedDistance;
                    if (rayLength <= 0.0 && candidate.blockedDistance >= 0.0) {
                        rayLength = candidate.blockedDistance;
                    }
                    GL11.glLineWidth(candidate == scan.bestCandidate ? 3.5F : 1.25F);
                    this.setVoidDebugColor(color, candidate == scan.bestCandidate ? 1.0F : 0.72F);
                    GL11.glBegin(1);
                    this.voidDebugVertex(scan.originX, renderY, scan.originZ, viewerX, viewerY, viewerZ);
                    this.voidDebugVertex(scan.originX + candidate.forwardX * rayLength, renderY, scan.originZ + candidate.forwardZ * rayLength, viewerX, viewerY, viewerZ);
                    GL11.glEnd();
                }
                GL11.glLineWidth(2.0F);
                GL11.glColor4f(0.85F, 0.2F, 1.0F, 0.9F);
                GL11.glBegin(1);
                this.drawVoidDebugPreferredRay(scan, scan.preferredYawPositive, renderY, viewerX, viewerY, viewerZ);
                if (Math.abs(MathHelper.func_76142_g(scan.preferredYawPositive - scan.preferredYawNegative)) > 1.0E-4) {
                    this.drawVoidDebugPreferredRay(scan, scan.preferredYawNegative, renderY, viewerX, viewerY, viewerZ);
                }
                GL11.glEnd();
                GL11.glPointSize(3.0F);
                GL11.glBegin(0);
                for (Displace.VoidDebugCandidate candidate : scan.candidates) {
                    for (Displace.VoidDebugProbe probe : candidate.probes) {
                        this.setVoidProbeColor(probe.leftVoid);
                        this.voidDebugVertex(probe.leftX, renderY, probe.leftZ, viewerX, viewerY, viewerZ);
                        this.setVoidProbeColor(probe.centerVoid);
                        this.voidDebugVertex(probe.centerX, renderY, probe.centerZ, viewerX, viewerY, viewerZ);
                        this.setVoidProbeColor(probe.rightVoid);
                        this.voidDebugVertex(probe.rightX, renderY, probe.rightZ, viewerX, viewerY, viewerZ);
                    }
                }
                GL11.glEnd();
                GL11.glLineWidth(1.75F);
                GL11.glColor4f(1.0F, 0.05F, 0.05F, 0.95F);
                GL11.glBegin(1);
                for (Displace.VoidDebugCandidate candidate : scan.candidates) {
                    if (!(candidate.blockedDistance < 0.0)) {
                        double blockedX = scan.originX + candidate.forwardX * candidate.blockedDistance;
                        double blockedZ = scan.originZ + candidate.forwardZ * candidate.blockedDistance;
                        this.drawVoidDebugCross(blockedX, renderY + 0.03, blockedZ, 0.13, viewerX, viewerY, viewerZ);
                    }
                }
                GL11.glEnd();
                this.renderBestVoidDebugDetails(scan, renderY, viewerX, viewerY, viewerZ);
                GL11.glPopAttrib();
                GL11.glPopMatrix();
                for (Displace.VoidDebugCandidate voidDebugCandidate : scan.candidates) {
                    int rank = rankedCandidates.indexOf(voidDebugCandidate);
                    int color = this.getVoidDebugRankColor(rank, rankedCandidates.size());
                    double labelDistance = scan.scanDistance + 0.45;
                    double labelX = scan.originX + voidDebugCandidate.forwardX * labelDistance;
                    double labelY = renderY + 0.32 + rank % 5 * 0.11;
                    double labelZ = scan.originZ + voidDebugCandidate.forwardZ * labelDistance;
                    this.renderVoidDebugLabel(this.formatVoidDebugScore(voidDebugCandidate, rank, voidDebugCandidate == scan.bestCandidate), labelX, labelY, labelZ, color, 0.0105F);
                }
                String legend = String.format(Locale.ROOT, "Score: N > E > W > Run > Path > Near > Angle | preferred %.1f° / %.1f° | scan %.1f", scan.preferredYawPositive, scan.preferredYawNegative, scan.scanDistance);
                this.renderVoidDebugLabel(legend, scan.originX, scan.originY + 2.35, scan.originZ, 16777215, 0.0125F);
            } else {
                this.frozenVoidDebugScan = null;
                this.frozenVoidDebugExpiresAtMs = 0L;
            }
        }
    }

    private void drawVoidDebugPreferredRay(Displace.VoidDebugScan scan, float preferredYaw, double renderY, double viewerX, double viewerY, double viewerZ) {
        double radians = Math.toRadians(preferredYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        this.voidDebugVertex(scan.originX, renderY + 0.025, scan.originZ, viewerX, viewerY, viewerZ);
        this.voidDebugVertex(scan.originX + forwardX * scan.scanDistance, renderY + 0.025, scan.originZ + forwardZ * scan.scanDistance, viewerX, viewerY, viewerZ);
    }

    private int compareVoidDebugCandidates(Displace.VoidDebugCandidate first, Displace.VoidDebugCandidate second) {
        if (first.pathScore != null && second.pathScore != null) {
            int pathComparison = first.pathScore.compareTo(second.pathScore);
            if (pathComparison != 0) {
                return pathComparison;
            }
        } else {
            if (first.pathScore != null) {
                return 1;
            }
            if (second.pathScore != null) {
                return -1;
            }
        }
        if (Math.abs(first.priorityDistance - second.priorityDistance) > 1.0E-4) {
            return first.priorityDistance < second.priorityDistance ? 1 : -1;
        } else {
            return 0;
        }
    }

    private int getVoidDebugRankColor(int rank, int candidateCount) {
        if (rank <= 0) {
            return 2162501;
        } else {
            float progress = candidateCount <= 1 ? 1.0F : Math.min(1.0F, (float) rank / (candidateCount - 1));
            int red = Math.min(255, Math.round(510.0F * progress));
            int green = Math.min(255, Math.round(510.0F * (1.0F - progress)));
            int blue = Math.round(24.0F * (1.0F - progress));
            return red << 16 | green << 8 | blue;
        }
    }

    private void setVoidDebugColor(int color, float alpha) {
        GL11.glColor4f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
    }

    private void setVoidProbeColor(boolean voidColumn) {
        if (voidColumn) {
            GL11.glColor4f(0.15F, 0.9F, 1.0F, 0.82F);
        } else {
            GL11.glColor4f(1.0F, 0.42F, 0.05F, 0.72F);
        }
    }

    private void renderBestVoidDebugDetails(Displace.VoidDebugScan scan, double renderY, double viewerX, double viewerY, double viewerZ) {
        Displace.VoidDebugCandidate best = scan.bestCandidate;
        if (best != null && best.pathScore != null) {
            GL11.glPointSize(6.0F);
            GL11.glBegin(0);
            for (Displace.VoidDebugProbe probe : best.probes) {
                this.setVoidProbeColor(probe.leftVoid);
                this.voidDebugVertex(probe.leftX, renderY + 0.035, probe.leftZ, viewerX, viewerY, viewerZ);
                this.setVoidProbeColor(probe.centerVoid);
                this.voidDebugVertex(probe.centerX, renderY + 0.035, probe.centerZ, viewerX, viewerY, viewerZ);
                this.setVoidProbeColor(probe.rightVoid);
                this.voidDebugVertex(probe.rightX, renderY + 0.035, probe.rightZ, viewerX, viewerY, viewerZ);
            }
            GL11.glEnd();
            Displace.VoidPathScore score = best.pathScore;
            for (int offsetX = -2; offsetX <= 2; offsetX++) {
                for (int offsetZ = -2; offsetZ <= 2; offsetZ++) {
                    int intValue3 = (offsetX + 2) * 5 + offsetZ + 2;
                    boolean destination = offsetX == 0 && offsetZ == 0;
                    boolean voidColumn = destination || (score.destinationVoidMask & 1L << intValue3) != 0L;
                    int radius = Math.max(Math.abs(offsetX), Math.abs(offsetZ));
                    if (destination) {
                        GL11.glColor4f(0.1F, 1.0F, 0.25F, 1.0F);
                    } else if (voidColumn && radius == 1) {
                        GL11.glColor4f(0.15F, 1.0F, 0.55F, 0.9F);
                    } else if (voidColumn) {
                        GL11.glColor4f(0.1F, 0.75F, 1.0F, 0.78F);
                    } else {
                        GL11.glColor4f(1.0F, 0.12F, 0.08F, 0.68F);
                    }
                    GL11.glLineWidth(destination ? 3.0F : 1.25F);
                    GL11.glBegin(2);
                    this.drawVoidDebugSquare(score.destinationBlockX + offsetX + 0.5, renderY + 0.055, score.destinationBlockZ + offsetZ + 0.5, 0.43, viewerX, viewerY, viewerZ);
                    GL11.glEnd();
                }
            }
            GL11.glLineWidth(3.0F);
            GL11.glColor4f(0.1F, 1.0F, 0.25F, 1.0F);
            GL11.glBegin(1);
            this.voidDebugVertex(score.destinationX, renderY, score.destinationZ, viewerX, viewerY, viewerZ);
            this.voidDebugVertex(score.destinationX, renderY + 1.25, score.destinationZ, viewerX, viewerY, viewerZ);
            GL11.glEnd();
        }
    }

    private String formatVoidDebugScore(Displace.VoidDebugCandidate candidate, int rank, boolean best) {
        String prefix = best ? "BEST " : "";
        if (candidate.pathScore == null) {
            String result = candidate.blockedDistance >= 0.0 ? String.format(Locale.ROOT, "blocked@%.2f", candidate.blockedDistance) : String.format(Locale.ROOT, "no-void reached=%.1f", candidate.reachedDistance);
            return String.format(Locale.ROOT, "%s#%02d %.1f° INVALID %s A%.1f", prefix, rank + 1, candidate.yaw, result, candidate.priorityDistance);
        } else {
            Displace.VoidPathScore score = candidate.pathScore;
            return String.format(Locale.ROOT, "%s#%02d %.1f° [N%d E%d W%d R%d P%.1f D%.1f A%.1f]", prefix, rank + 1, candidate.yaw, score.immediateVoidNeighbors, score.extendedVoidNeighbors, score.destinationWidth, score.longestCenterVoidRun, score.pathQuality, score.destinationDistance, candidate.priorityDistance);
        }
    }

    private void renderVoidDebugLabel(String text, double x, double y, double z, int color, float scale) {
        RenderManager renderManager = mc.func_175598_ae();
        GlStateManager.func_179094_E();
        GlStateManager.func_179137_b(x - renderManager.field_78730_l, y - renderManager.field_78731_m, z - renderManager.field_78728_n);
        GlStateManager.func_179114_b(-renderManager.field_78735_i, 0.0F, 1.0F, 0.0F);
        GlStateManager.func_179114_b((mc.field_71474_y.field_74320_O == 2 ? -1.0F : 1.0F) * renderManager.field_78732_j, 1.0F, 0.0F, 0.0F);
        GlStateManager.func_179152_a(-scale, -scale, scale);
        GlStateManager.func_179140_f();
        GlStateManager.func_179097_i();
        GlStateManager.func_179132_a(false);
        GlStateManager.func_179147_l();
        GlStateManager.func_179120_a(770, 771, 1, 0);
        int textX = -mc.field_71466_p.func_78256_a(text) / 2;
        mc.field_71466_p.func_175065_a(text, textX, 0.0F, 0xFF000000 | color, true);
        GlStateManager.func_179084_k();
        GlStateManager.func_179132_a(true);
        GlStateManager.func_179126_j();
        GlStateManager.func_179145_e();
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179121_F();
    }

    private void drawVoidDebugCross(double x, double y, double z, double size, double viewerX, double viewerY, double viewerZ) {
        this.voidDebugVertex(x - size, y, z - size, viewerX, viewerY, viewerZ);
        this.voidDebugVertex(x + size, y, z + size, viewerX, viewerY, viewerZ);
        this.voidDebugVertex(x - size, y, z + size, viewerX, viewerY, viewerZ);
        this.voidDebugVertex(x + size, y, z - size, viewerX, viewerY, viewerZ);
    }

    private void drawVoidDebugSquare(double x, double y, double z, double halfSize, double viewerX, double viewerY, double viewerZ) {
        this.voidDebugVertex(x - halfSize, y, z - halfSize, viewerX, viewerY, viewerZ);
        this.voidDebugVertex(x + halfSize, y, z - halfSize, viewerX, viewerY, viewerZ);
        this.voidDebugVertex(x + halfSize, y, z + halfSize, viewerX, viewerY, viewerZ);
        this.voidDebugVertex(x - halfSize, y, z + halfSize, viewerX, viewerY, viewerZ);
    }

    private void voidDebugVertex(double x, double y, double z, double viewerX, double viewerY, double viewerZ) {
        GL11.glVertex3d(x - viewerX, y - viewerY, z - viewerZ);
    }

    private void drawArrow(EntityPlayer player, float yaw, float partialTicks, float alpha, double nudgeOffset) {
        double x = player.field_70142_S + (player.field_70165_t - player.field_70142_S) * partialTicks;
        double y = player.field_70137_T + (player.field_70163_u - player.field_70137_T) * partialTicks + player.field_70131_O * 0.5;
        double z = player.field_70136_U + (player.field_70161_v - player.field_70136_U) * partialTicks;
        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double startDistance = player.field_70130_N * 0.5 + 0.3 + nudgeOffset;
        double startX = x + forwardX * startDistance;
        double startZ = z + forwardZ * startDistance;
        double bodyX = startX + forwardX * 0.74;
        double bodyZ = startZ + forwardZ * 0.74;
        double headBaseX = startX + forwardX * 0.56;
        double headBaseZ = startZ + forwardZ * 0.56;
        double tipX = bodyX + forwardX * 0.52;
        double tipZ = bodyZ + forwardZ * 0.52;
        double viewerX = mc.func_175598_ae().field_78730_l;
        double viewerY = mc.func_175598_ae().field_78731_m;
        double viewerZ = mc.func_175598_ae().field_78728_n;
        GL11.glPushMatrix();
        GL11.glPushAttrib(1048575);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        GL11.glDisable(2896);
        GL11.glDisable(2929);
        GL11.glDisable(2884);
        GL11.glDepthMask(false);
        GL11.glEnable(2848);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.82F * alpha);
        GL11.glBegin(4);
        this.vertex(startX, y, startZ, 0.0, viewerX, viewerY, viewerZ);
        this.vertex(bodyX, y, bodyZ, -0.08, viewerX, viewerY, viewerZ);
        this.vertex(bodyX, y, bodyZ, 0.08, viewerX, viewerY, viewerZ);
        this.vertex(bodyX, y, bodyZ, -0.08, viewerX, viewerY, viewerZ);
        this.vertex(headBaseX, y, headBaseZ, -0.3, viewerX, viewerY, viewerZ);
        this.vertex(tipX, y, tipZ, 0.0, viewerX, viewerY, viewerZ);
        this.vertex(bodyX, y, bodyZ, -0.08, viewerX, viewerY, viewerZ);
        this.vertex(tipX, y, tipZ, 0.0, viewerX, viewerY, viewerZ);
        this.vertex(bodyX, y, bodyZ, 0.08, viewerX, viewerY, viewerZ);
        this.vertex(bodyX, y, bodyZ, 0.08, viewerX, viewerY, viewerZ);
        this.vertex(tipX, y, tipZ, 0.0, viewerX, viewerY, viewerZ);
        this.vertex(headBaseX, y, headBaseZ, 0.3, viewerX, viewerY, viewerZ);
        GL11.glEnd();
        GL11.glLineWidth(2.0F);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.95F * alpha);
        GL11.glBegin(2);
        this.vertex(startX, y, startZ, 0.0, viewerX, viewerY, viewerZ);
        this.vertex(bodyX, y, bodyZ, -0.08, viewerX, viewerY, viewerZ);
        this.vertex(headBaseX, y, headBaseZ, -0.3, viewerX, viewerY, viewerZ);
        this.vertex(tipX, y, tipZ, 0.0, viewerX, viewerY, viewerZ);
        this.vertex(headBaseX, y, headBaseZ, 0.3, viewerX, viewerY, viewerZ);
        this.vertex(bodyX, y, bodyZ, 0.08, viewerX, viewerY, viewerZ);
        GL11.glEnd();
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    private void vertex(double x, double y, double z, double yOffset, double viewerX, double viewerY, double viewerZ) {
        GL11.glVertex3d(x - viewerX, y + yOffset - viewerY, z - viewerZ);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGameTick(GameTickEvent e) {
        this.suppressUseForOverrideAttackTick = false;
        if (this.isOverrideAttackEnabled()) {
            if (this.releaseBlinkNextGameTick) {
                this.releaseBlink();
                this.releaseBlinkNextGameTick = false;
            }
            this.advanceOverrideAttackState();
        } else {
            if (this.overrideAttackState != Displace.OverrideAttackState.IDLE) {
                this.resetOverrideAttackState();
            }
            if (this.releaseBlinkNextGameTick) {
                this.releaseBlink();
                this.releaseBlinkNextGameTick = false;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickMouse(RightClickMouseEvent event) {
        if (this.isOverrideAttackEnabled()) {
            if (this.overrideAttackState == Displace.OverrideAttackState.FLICKING_AWAY || this.overrideAttackState == Displace.OverrideAttackState.ATTACKING || this.suppressUseForOverrideAttackTick) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreAttack(PreAttackEvent event) {
        if (this.isOverrideAttackEnabled()) {
            if (this.overrideAttackState != Displace.OverrideAttackState.IDLE) {
                event.setCanceled(true);
            } else {
                MovingObjectPosition mouseOver = event.objectMouseOver;
                EntityPlayer target = mouseOver != null && mouseOver.field_72313_a == MovingObjectType.ENTITY ? CombatTargeting.asValidPlayer(mouseOver.field_72308_g, 25.0, this.ignoreTeammates.isToggled()) : null;
                if (target != null && this.startOverrideAttack(target)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(AttackEvent event) {
        if (this.isOverrideAttackEnabled() && event.attacker == mc.field_71439_g && !this.overrideAttackInProgress) {
            if (this.overrideAttackState != Displace.OverrideAttackState.IDLE) {
                event.setCanceled(true);
            } else {
                EntityPlayer target = CombatTargeting.asValidPlayer(event.target, 25.0, this.ignoreTeammates.isToggled());
                if (target != null && this.startOverrideAttack(target)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPrePlayerInteract(PrePlayerInteractEvent event) {
        if (this.isOverrideAttackEnabled() && this.overrideAttackState == Displace.OverrideAttackState.ATTACKING && !this.overrideAttackInProgress) {
            if (!this.isOverrideTargetValid(this.overrideTarget)) {
                this.resetOverrideAttackState();
            } else {
                this.overrideAttackPacketSent = false;
                this.releaseUseForOverrideAttack();
                this.suppressUseForOverrideAttackTick = true;
                this.overrideAttackInProgress = true;
                try {
                    Utils.attackEntity(this.overrideTarget, true, false);
                } finally {
                    this.overrideAttackInProgress = false;
                }
                if (!this.overrideAttackPacketSent) {
                    this.resetOverrideAttackState();
                } else {
                    this.overrideAttackState = Displace.OverrideAttackState.RESTORING;
                    this.overrideStateTicks = 0;
                    this.displaceThisTick = false;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPostInput(PostPlayerInputEvent e) {
        if (this.isOverrideAttackEnabled()) {
            this.compensateNextTick = false;
        } else if (!this.active) {
            this.compensateNextTick = false;
        } else if (this.compensateNextTick && !this.displaceThisTick) {
            this.compensateNextTick = false;
            if (this.displaceLeft) {
                mc.field_71439_g.field_71158_b.field_78902_a = -1.0F;
            } else {
                mc.field_71439_g.field_71158_b.field_78902_a = 1.0F;
            }
        } else if (this.displaceThisTick && !this.hasKB) {
            if (this.anyMovementKey()) {
                mc.field_71439_g.field_71158_b.field_78900_b = 1.0F;
                this.compensateNextTick = true;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSendPacket(SendPacketEvent e) {
        if (!this.isOverrideAttackEnabled() && this.overrideAttackState != Displace.OverrideAttackState.IDLE) {
            this.resetOverrideAttackState();
        }
        if (this.isOverrideAttackEnabled() && this.overrideAttackState != Displace.OverrideAttackState.IDLE) {
            if (!e.isCanceled()) {
                if (this.overrideAttackState == Displace.OverrideAttackState.FLICKING_AWAY && e.getPacket() instanceof C03PacketPlayer) {
                    C03PacketPlayer movementPacket = (C03PacketPlayer) e.getPacket();
                    if (movementPacket.func_149463_k()) {
                        float yawError = Math.abs(MathHelper.func_76142_g(movementPacket.func_149462_g() - this.overrideFlickYaw));
                        if (yawError <= 6.0F) {
                            this.overrideAwayPacketSent = true;
                        }
                    }
                }
                if (this.overrideAttackInProgress && e.getPacket() instanceof C02PacketUseEntity && ((C02PacketUseEntity) e.getPacket()).func_149565_c() == Action.ATTACK) {
                    this.overrideAttackPacketSent = true;
                    this.releaseBlink();
                }
            }
        } else if (this.blink.isToggled() && this.active && this.displaceThisTick && !this.releaseBlinkNextGameTick) {
            if (e.getPacket() instanceof C03PacketPlayer) {
                if (this.outboundBlink == null) {
                    this.outboundBlink = new LagRequest(EnumLagDirection.ONLY_OUTBOUND, new ModuleBackedTimeout(this));
                    Raven.lagHandler.requestLag(this.outboundBlink);
                    this.releaseBlinkNextGameTick = true;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onArrowNudgeAttackPacket(SendPacketEvent e) {
        if (this.active && this.renderArrow.isToggled() && Utils.nullCheck() && !e.isCanceled() && e.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) e.getPacket();
            if (packet.func_149565_c() == Action.ATTACK) {
                Entity attackedEntity = packet.func_149564_a(mc.field_71441_e);
                if (attackedEntity == this.arrowPlayer) {
                    this.triggerArrowNudge(this.arrowPlayer);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onVoidDebugAttackPacket(SendPacketEvent e) {
        if (Raven.DEBUG && this.isVoidMode() && this.active && Utils.nullCheck() && !e.isCanceled() && e.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) e.getPacket();
            if (packet.func_149565_c() == Action.ATTACK) {
                Entity attackedEntity = packet.func_149564_a(mc.field_71441_e);
                Displace.VoidDebugScan scan = this.latestVoidDebugScan;
                if (attackedEntity instanceof EntityPlayer && scan != null && scan.bestCandidate != null && scan.world == mc.field_71441_e && attackedEntity.func_145782_y() == scan.targetEntityId) {
                    this.frozenVoidDebugScan = scan;
                    this.frozenVoidDebugExpiresAtMs = System.currentTimeMillis() + 30000L;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientRotation(ClientRotationEvent e) {
        if (!Utils.nullCheck()) {
            this.resetOverrideAttackState();
            this.active = false;
            this.compensateNextTick = false;
            this.wasDisplacingLastTick = false;
            this.targetDisplacementLocks.clear();
            this.clearArrow();
            this.clearVoidDebugState();
        } else {
            this.tickCounter++;
            int currentTick = this.tickCounter;
            this.pruneTargetDelayStates();
            if (this.isOverrideAttackEnabled()) {
                if (this.overrideAttackState == Displace.OverrideAttackState.IDLE) {
                    this.active = false;
                    this.displaceThisTick = false;
                    this.compensateNextTick = false;
                    this.wasDisplacingLastTick = false;
                    this.hideArrow();
                } else {
                    this.updateOverrideRotation(e);
                }
            } else {
                if (this.overrideAttackState != Displace.OverrideAttackState.IDLE) {
                    this.resetOverrideAttackState();
                }
                if (!Raven.DEBUG || !this.isVoidMode()) {
                    this.latestVoidDebugScan = null;
                }
                boolean passesItemCondition = (!this.onlyKnockbackItems.isToggled() || EnchantmentHelper.func_77501_a(mc.field_71439_g) > 0) && (!this.weaponOnly.isToggled() || Utils.holdingWeapon());
                if (!passesItemCondition) {
                    this.active = false;
                    this.displaceThisTick = false;
                    this.compensateNextTick = false;
                    this.wasDisplacingLastTick = false;
                    this.hideArrow();
                } else {
                    EntityPlayer target = null;
                    boolean killAuraIsAttacking = ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && KillAura.attackingEntity != null;
                    if (killAuraIsAttacking) {
                        target = CombatTargeting.asValidPlayer(KillAura.attackingEntity, 9.0, this.ignoreTeammates.isToggled());
                    } else if (Mouse.isButtonDown(0)) {
                        target = CombatTargeting.getMouseOverTarget(9.0, this.ignoreTeammates.isToggled());
                    }
                    boolean hasKBEnchant = EnchantmentHelper.func_77501_a(mc.field_71439_g) > 0;
                    this.active = target != null;
                    if (!this.active) {
                        this.displaceThisTick = false;
                        this.compensateNextTick = false;
                        this.wasDisplacingLastTick = false;
                        this.hideArrow();
                    } else {
                        float playerYaw = e.yaw != null ? e.yaw : RotationUtils.serverRotations[0];
                        float displaceYaw;
                        if (this.isVoidMode()) {
                            Float bestVoidYaw = this.findBestVoidYaw(target, playerYaw);
                            if (bestVoidYaw == null) {
                                this.active = false;
                                this.displaceThisTick = false;
                                this.compensateNextTick = false;
                                this.wasDisplacingLastTick = false;
                                this.hideArrow();
                                return;
                            }
                            displaceYaw = bestVoidYaw;
                            this.updateDisplaceSide(playerYaw, displaceYaw);
                        } else {
                            if (!this.findVoid.isToggled() || !this.tryFindVoidDirection(target)) {
                                this.displaceLeft = this.direction.getInput() == 0.0;
                            }
                            float offset = (float) this.yawOffset.getInput();
                            displaceYaw = this.displaceLeft ? playerYaw - offset : playerYaw + offset;
                        }
                        this.hasKB = hasKBEnchant;
                        this.displaceThisTick = !this.displaceThisTick;
                        if (this.displaceThisTick && !this.shouldDisplaceInCurrentWindow(target, currentTick)) {
                            this.displaceThisTick = false;
                            this.compensateNextTick = false;
                            this.wasDisplacingLastTick = false;
                            this.hideArrow();
                        } else {
                            if (this.displaceThisTick) {
                                displaceYaw = this.lockDisplacementYaw(target, displaceYaw);
                                this.updateDisplaceSide(playerYaw, displaceYaw);
                            } else {
                                displaceYaw = this.getLockedDisplacementYaw(target, displaceYaw);
                            }
                            this.showArrow(target, displaceYaw);
                            if (!this.displaceThisTick && this.wasDisplacingLastTick) {
                                int key = mc.field_71474_y.field_74312_F.func_151463_i();
                                if (key != 0) {
                                    KeyBinding.func_74507_a(key);
                                }
                            }
                            this.wasDisplacingLastTick = this.displaceThisTick;
                            if (this.displaceThisTick) {
                                e.yaw = displaceYaw;
                                RotationHelper.get().forceMovementFix = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private static final class DisplacementLock {

        private final float yaw;

        private int lastUseTick;

        private boolean airborne;

        private DisplacementLock(float yaw, int lastUseTick, boolean airborne) {
            this.yaw = yaw;
            this.lastUseTick = lastUseTick;
            this.airborne = airborne;
        }
    }

    private static enum OverrideAttackState {

        IDLE, FLICKING_AWAY, ATTACKING, RESTORING
    }

    private static final class VoidDebugCandidate {

        private final float yaw;

        private final double forwardX;

        private final double forwardZ;

        private final double priorityDistance;

        private final List<Displace.VoidDebugProbe> probes = new ArrayList<>();

        private Displace.VoidPathScore pathScore;

        private double reachedDistance;

        private double blockedDistance = -1.0;

        private VoidDebugCandidate(float yaw, double forwardX, double forwardZ, double priorityDistance) {
            this.yaw = yaw;
            this.forwardX = forwardX;
            this.forwardZ = forwardZ;
            this.priorityDistance = priorityDistance;
        }
    }

    private static final class VoidDebugProbe {

        private final double leftX;

        private final double leftZ;

        private final boolean leftVoid;

        private final double centerX;

        private final double centerZ;

        private final boolean centerVoid;

        private final double rightX;

        private final double rightZ;

        private final boolean rightVoid;

        private VoidDebugProbe(double leftX, double leftZ, boolean leftVoid, double centerX, double centerZ, boolean centerVoid, double rightX, double rightZ, boolean rightVoid) {
            this.leftX = leftX;
            this.leftZ = leftZ;
            this.leftVoid = leftVoid;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.centerVoid = centerVoid;
            this.rightX = rightX;
            this.rightZ = rightZ;
            this.rightVoid = rightVoid;
        }
    }

    private static final class VoidDebugScan {

        private final Object world;

        private final int targetEntityId;

        private final double originX;

        private final double originY;

        private final double originZ;

        private final double scanDistance;

        private final float preferredYawPositive;

        private final float preferredYawNegative;

        private final List<Displace.VoidDebugCandidate> candidates = new ArrayList<>();

        private Displace.VoidDebugCandidate bestCandidate;

        private VoidDebugScan(Object world, int targetEntityId, double originX, double originY, double originZ, double scanDistance, float preferredYawPositive, float preferredYawNegative) {
            this.world = world;
            this.targetEntityId = targetEntityId;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.scanDistance = scanDistance;
            this.preferredYawPositive = preferredYawPositive;
            this.preferredYawNegative = preferredYawNegative;
        }
    }

    private static final class VoidNeighborhood {

        private final int blockX;

        private final int blockZ;

        private final int immediateVoidNeighbors;

        private final int extendedVoidNeighbors;

        private final int quality;

        private final long voidMask;

        private VoidNeighborhood(int blockX, int blockZ, int immediateVoidNeighbors, int extendedVoidNeighbors, int quality, long voidMask) {
            this.blockX = blockX;
            this.blockZ = blockZ;
            this.immediateVoidNeighbors = immediateVoidNeighbors;
            this.extendedVoidNeighbors = extendedVoidNeighbors;
            this.quality = quality;
            this.voidMask = voidMask;
        }
    }

    private static final class VoidPathScore {

        private final int immediateVoidNeighbors;

        private final int extendedVoidNeighbors;

        private final int destinationWidth;

        private final int longestCenterVoidRun;

        private final double pathQuality;

        private final double destinationDistance;

        private final double destinationX;

        private final double destinationZ;

        private final int destinationBlockX;

        private final int destinationBlockZ;

        private final long destinationVoidMask;

        private VoidPathScore(int immediateVoidNeighbors, int extendedVoidNeighbors, int destinationWidth, int longestCenterVoidRun, double pathQuality, double destinationDistance, double destinationX, double destinationZ, int destinationBlockX, int destinationBlockZ, long destinationVoidMask) {
            this.immediateVoidNeighbors = immediateVoidNeighbors;
            this.extendedVoidNeighbors = extendedVoidNeighbors;
            this.destinationWidth = destinationWidth;
            this.longestCenterVoidRun = longestCenterVoidRun;
            this.pathQuality = pathQuality;
            this.destinationDistance = destinationDistance;
            this.destinationX = destinationX;
            this.destinationZ = destinationZ;
            this.destinationBlockX = destinationBlockX;
            this.destinationBlockZ = destinationBlockZ;
            this.destinationVoidMask = destinationVoidMask;
        }

        private int compareTo(Displace.VoidPathScore other) {
            if (this.immediateVoidNeighbors != other.immediateVoidNeighbors) {
                return Integer.compare(this.immediateVoidNeighbors, other.immediateVoidNeighbors);
            } else if (this.extendedVoidNeighbors != other.extendedVoidNeighbors) {
                return Integer.compare(this.extendedVoidNeighbors, other.extendedVoidNeighbors);
            } else if (this.destinationWidth != other.destinationWidth) {
                return Integer.compare(this.destinationWidth, other.destinationWidth);
            } else if (this.longestCenterVoidRun != other.longestCenterVoidRun) {
                return Integer.compare(this.longestCenterVoidRun, other.longestCenterVoidRun);
            } else if (Math.abs(this.pathQuality - other.pathQuality) > 1.0E-4) {
                return this.pathQuality > other.pathQuality ? 1 : -1;
            } else if (Math.abs(this.destinationDistance - other.destinationDistance) > 1.0E-4) {
                return this.destinationDistance < other.destinationDistance ? 1 : -1;
            } else {
                return 0;
            }
        }
    }

    private static final class VoidYawCandidate {

        private final float yaw;

        private final Displace.VoidPathScore pathScore;

        private final double priorityDistance;

        private final Displace.VoidDebugCandidate debugCandidate;

        private VoidYawCandidate(float yaw, Displace.VoidPathScore pathScore, double priorityDistance, Displace.VoidDebugCandidate debugCandidate) {
            this.yaw = yaw;
            this.pathScore = pathScore;
            this.priorityDistance = priorityDistance;
            this.debugCandidate = debugCandidate;
        }

        private int compareTo(Displace.VoidYawCandidate other) {
            int pathComparison = this.pathScore.compareTo(other.pathScore);
            if (pathComparison != 0) {
                return pathComparison;
            } else if (Math.abs(this.priorityDistance - other.priorityDistance) > 1.0E-4) {
                return this.priorityDistance < other.priorityDistance ? 1 : -1;
            } else {
                return 0;
            }
        }
    }
}
