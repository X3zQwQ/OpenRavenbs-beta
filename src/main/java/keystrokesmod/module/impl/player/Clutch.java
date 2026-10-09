package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class Clutch extends Module {

    private static final Map<String, Integer> BLOCK_SCORE = new HashMap<>();

    private static final double HALF_WIDTH = 0.3;

    private static final double[][] CORNERS = new double[][] { { -0.3, -0.3 }, { 0.3, -0.3 }, { -0.3, 0.3 }, { 0.3, 0.3 } };

    private final SliderSetting reach;

    private final SliderSetting speed;

    private final SliderSetting snapbackSpeed;

    private final SliderSetting maxDistance;

    private final SliderSetting rotationTolerance;

    private final ButtonSetting simulateFuturePosition;

    private final ButtonSetting autoClutch;

    private final ButtonSetting requireVoid;

    private final SliderSetting minimumFallDistance;

    private final KeySetting selectKeybind;

    private final ItemListSetting itemBlacklist;

    private BlockPos placeAtBlock;

    private EnumFacing hitSide;

    private Vec3 hitVec;

    private boolean placeQueued;

    private boolean placing;

    private boolean slotWasSwapped;

    private boolean autoClickerWasOn;

    private int prevSlot = -1;

    private int plannedSlot = -1;

    private float aimYaw;

    private float aimPitch;

    private BlockPos targetHitPos;

    private EnumFacing targetSide;

    private boolean hasAim;

    private boolean resetting;

    private BlockPos lastPlaced;

    private int clutchBlocksPlaced;

    private boolean autoClutchActive;

    private boolean autoClutchChecking;

    private int autoClutchCheckCounter;

    private boolean autoClutchLandedGuard;

    private int autoClutchLandedTick;

    private int prevHurtTime = -1;

    public Clutch() {
        super("Clutch", Module.category.player);
        this.registerSetting(this.reach = new SliderSetting("Reach", " block", 4.5, 0.5, 4.5, 0.1));
        this.registerSetting(this.speed = new SliderSetting("Speed", 8.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.snapbackSpeed = new SliderSetting("Snapback Speed", 12.0, 0.0, 100.0, 1.0));
        this.registerSetting(this.maxDistance = new SliderSetting("Max distance", " block", 10.0, 0.0, 20.0, 1.0));
        this.registerSetting(this.rotationTolerance = new SliderSetting("Rotation Tolerance", "°", 25.0, 20.0, 100.0, 1.0));
        this.registerSetting(this.minimumFallDistance = new SliderSetting("Minimum fall distance", " block", 10.0, 3.0, 20.0, 1.0));
        this.registerSetting(this.simulateFuturePosition = new ButtonSetting("Simulate future position", true));
        this.registerSetting(this.autoClutch = new ButtonSetting("Auto Clutch", false));
        this.registerSetting(this.requireVoid = new ButtonSetting("Require void", false));
        this.registerSetting(this.selectKeybind = new KeySetting("Select Keybind", 0));
        this.registerSetting(this.itemBlacklist = new ItemListSetting("Item blacklist"));
        this.closetModule = true;
    }

    @Override
    public void onEnable() {
        this.hasAim = false;
        this.resetting = false;
        this.clutchBlocksPlaced = 0;
        this.autoClutchActive = false;
        this.autoClutchChecking = false;
        this.autoClutchCheckCounter = 0;
        this.autoClutchLandedGuard = false;
        this.autoClutchLandedTick = 0;
        this.prevHurtTime = -1;
    }

    @Override
    public void onDisable() {
        this.clearAim(false);
        this.disablePlacing(true);
        this.placeQueued = false;
        this.autoClutchActive = false;
        this.autoClutchChecking = false;
        this.autoClutchLandedGuard = false;
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent e) {
        if (Utils.nullCheck()) {
            if (ModuleManager.bedAura == null || !ModuleManager.bedAura.shouldOverrideMouseOver()) {
                this.runPrePlayerInteract();
                if (mc.field_71462_r != null) {
                    this.disablePlacing(false);
                }
                float baseYaw = e.yaw != null ? e.yaw : RotationUtils.serverRotations[0];
                float basePitch = e.pitch != null ? e.pitch : RotationUtils.serverRotations[1];
                if (this.resetting) {
                    this.aimYaw = mc.field_71439_g.field_70177_z;
                    this.aimPitch = mc.field_71439_g.field_70125_A;
                    float[] smoothed = this.getRotationsSmoothed(baseYaw, basePitch, this.aimYaw, this.aimPitch, true);
                    if (Math.abs(MathHelper.func_76142_g(smoothed[0] - this.aimYaw)) < 0.5F && Math.abs(smoothed[1] - this.aimPitch) < 0.5F) {
                        this.resetting = false;
                        this.restoreInputsAndAutoClicker();
                    } else {
                        RotationHelper.get().forceMovementFix = true;
                        e.setYaw(smoothed[0]);
                        e.setPitch(smoothed[1]);
                    }
                } else if (this.hasAim) {
                    float[] smoothed = this.getRotationsSmoothed(baseYaw, basePitch, this.aimYaw, this.aimPitch, false);
                    if (this.placing && this.targetHitPos != null) {
                        MovingObjectPosition mop = RotationUtils.rayCastBlock(this.reach.getInput(), smoothed[0], smoothed[1]);
                        if (mop != null && this.targetHitPos.equals(mop.func_178782_a()) && this.targetSide == mop.field_178784_b) {
                            int maxBlocks = (int) this.maxDistance.getInput();
                            if (maxBlocks == 0 || this.clutchBlocksPlaced < maxBlocks) {
                                double tolerance = this.rotationTolerance.getInput();
                                if (Math.abs(MathHelper.func_76142_g(smoothed[0] - RotationUtils.serverRotations[0])) <= tolerance && Math.abs(smoothed[1] - RotationUtils.serverRotations[1]) <= tolerance) {
                                    this.placeAtBlock = mop.func_178782_a();
                                    this.hitSide = mop.field_178784_b;
                                    this.hitVec = mop.field_72307_f;
                                    this.placeQueued = true;
                                }
                            }
                        }
                    }
                    RotationHelper.get().forceMovementFix = true;
                    e.setYaw(smoothed[0]);
                    e.setPitch(smoothed[1]);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (Utils.nullCheck() && this.placeQueued) {
            this.placeQueued = false;
            if (this.canClutchHere()) {
                if (this.placeAtBlock != null && this.hitSide != null && this.hitVec != null && mc.field_71442_b.func_178890_a(mc.field_71439_g, mc.field_71441_e, mc.field_71439_g.func_70694_bm(), this.placeAtBlock, this.hitSide, this.hitVec)) {
                    if (this.hitSide != EnumFacing.UP) {
                        this.clutchBlocksPlaced++;
                    }
                    this.lastPlaced = this.placeAtBlock;
                    mc.field_71439_g.func_71038_i();
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent e) {
        if ((this.placing || this.resetting || this.hasAim) && e.button > -1) {
            e.setCanceled(true);
        }
    }

    public boolean isActiveWindow() {
        return this.isEnabled() && (this.placing || this.resetting || this.hasAim || this.placeQueued);
    }

    private void runPrePlayerInteract() {
        if (mc.field_71439_g.field_70122_E) {
            this.clutchBlocksPlaced = 0;
        }
        int ticksExisted = mc.field_71439_g.field_70173_aa;
        this.updateAutoClutch(ticksExisted);
        boolean active = this.selectKeybind.isPressed() || this.autoClutchActive;
        if (mc.field_71462_r == null && active && this.canClutchHere()) {
            BlockPos below = new BlockPos(MathHelper.func_76128_c(mc.field_71439_g.field_70165_t), MathHelper.func_76128_c(mc.field_71439_g.field_70163_u) - 1, MathHelper.func_76128_c(mc.field_71439_g.field_70161_v));
            if (!this.canPlaceThrough(below)) {
                this.disablePlacing(false);
            } else {
                int weakSlot = this.pickBlockSlot();
                if (weakSlot == -1) {
                    this.disablePlacing(false);
                } else {
                    this.plannedSlot = weakSlot;
                    Clutch.AimResult target = this.clutchAim();
                    if (target != null) {
                        this.targetHitPos = target.ray.func_178782_a();
                        this.targetSide = target.ray.field_178784_b;
                        this.aimYaw = target.yaw;
                        this.aimPitch = target.pitch;
                        this.hasAim = true;
                        this.resetting = false;
                    }
                    if (this.hasAim && !this.placing) {
                        this.enablePlacing();
                    }
                    if (this.placing || this.resetting || this.hasAim) {
                        KeyBinding.func_74510_a(mc.field_71474_y.field_74312_F.func_151463_i(), false);
                        KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), false);
                        this.equipPlannedSlot();
                    }
                }
            }
        } else {
            this.clearAim(true);
            this.disablePlacing(false);
        }
    }

    private void updateAutoClutch(int ticksExisted) {
        if (this.autoClutch.isToggled()) {
            int curHurtTime = mc.field_71439_g.field_70737_aN;
            if (curHurtTime > this.prevHurtTime) {
                this.autoClutchChecking = true;
                this.autoClutchCheckCounter = 0;
                this.autoClutchLandedGuard = false;
            }
            this.prevHurtTime = curHurtTime;
            if (this.autoClutchChecking && !this.autoClutchActive && !this.autoClutchLandedGuard) {
                if ((this.autoClutchCheckCounter == 0 || this.autoClutchCheckCounter % 3 == 0) && this.willFallFar(this.minimumFallDistance.getInput())) {
                    this.autoClutchActive = true;
                }
                this.autoClutchCheckCounter++;
            }
            if (this.autoClutchLandedGuard) {
                boolean expired = ticksExisted - this.autoClutchLandedTick >= 10;
                boolean jumped = mc.field_71474_y.field_74314_A.func_151470_d();
                boolean airborneUp = !mc.field_71439_g.field_70122_E && mc.field_71439_g.field_70181_x > 0.0;
                if (expired || jumped || airborneUp) {
                    this.autoClutchActive = false;
                    this.autoClutchChecking = false;
                    this.autoClutchLandedGuard = false;
                }
            }
            if (this.autoClutchActive && mc.field_71439_g.field_70122_E && mc.field_71439_g.field_70737_aN < mc.field_71439_g.field_70738_aO - 2 && !this.autoClutchLandedGuard) {
                this.autoClutchLandedGuard = true;
                this.autoClutchLandedTick = ticksExisted;
                if (!this.willFallSoon()) {
                    this.autoClutchActive = false;
                    this.autoClutchChecking = false;
                    this.autoClutchLandedGuard = false;
                }
            }
            if (!this.autoClutchActive && !this.autoClutchLandedGuard && mc.field_71439_g.field_70122_E && mc.field_71439_g.field_70737_aN == 0) {
                this.autoClutchChecking = false;
                this.autoClutchCheckCounter = 0;
            }
        } else {
            this.autoClutchActive = false;
            this.autoClutchChecking = false;
            this.autoClutchLandedGuard = false;
            this.prevHurtTime = mc.field_71439_g.field_70737_aN;
        }
    }

    void adoptLadderSlot(int slot) {
        if (!this.slotWasSwapped) {
            this.prevSlot = slot;
            this.slotWasSwapped = true;
        }
    }

    private void enablePlacing() {
        if (!this.placing) {
            Module ladderClutch = ModuleManager.getModule(AutoLadder.class);
            if (ladderClutch instanceof AutoLadder) {
                int savedSlot = ((AutoLadder) ladderClutch).yieldToClutch();
                if (savedSlot >= 0 && !this.slotWasSwapped) {
                    this.prevSlot = savedSlot;
                    this.slotWasSwapped = true;
                }
            }
            this.placing = true;
            if (!this.slotWasSwapped) {
                this.prevSlot = mc.field_71439_g.field_71071_by.field_70461_c;
            }
            this.autoClickerWasOn = this.autoClickerWasOn || ModuleManager.autoClicker != null && ModuleManager.autoClicker.isEnabled();
            if (this.autoClickerWasOn && ModuleManager.autoClicker != null) {
                ModuleManager.autoClicker.disable();
            }
        }
    }

    private void disablePlacing(boolean forceRestore) {
        if (this.placing || forceRestore) {
            this.placing = false;
            this.plannedSlot = -1;
            if ((forceRestore || !this.hasAim) && this.slotWasSwapped) {
                if (this.prevSlot != -1) {
                    mc.field_71439_g.field_71071_by.field_70461_c = this.prevSlot;
                }
                this.slotWasSwapped = false;
            }
            if (forceRestore) {
                this.prevSlot = -1;
                this.restoreInputsAndAutoClicker();
            }
        }
    }

    private void clearAim(boolean allowSnapback) {
        if (this.slotWasSwapped) {
            if (this.prevSlot != -1) {
                mc.field_71439_g.field_71071_by.field_70461_c = this.prevSlot;
            }
            this.slotWasSwapped = false;
        }
        this.targetHitPos = null;
        this.targetSide = null;
        this.lastPlaced = null;
        this.clutchBlocksPlaced = 0;
        if (allowSnapback && this.hasAim) {
            this.resetting = true;
        }
        this.hasAim = false;
        this.prevSlot = -1;
    }

    private void restoreInputsAndAutoClicker() {
        if (mc.field_71462_r == null) {
            KeyBinding.func_74510_a(mc.field_71474_y.field_74312_F.func_151463_i(), Mouse.isButtonDown(0));
            KeyBinding.func_74510_a(mc.field_71474_y.field_74313_G.func_151463_i(), Mouse.isButtonDown(1));
        }
        if (this.autoClickerWasOn && ModuleManager.autoClicker != null) {
            ModuleManager.autoClicker.enable();
            this.autoClickerWasOn = false;
        }
    }

    private boolean willFallFar(double minFall) {
        double startY = mc.field_71439_g.field_70163_u;
        Clutch.PredictionState prediction = Clutch.PredictionState.fromPlayer();
        for (int t = 0; t < 60; t++) {
            prediction.tick(false);
            if (prediction.onGround) {
                return false;
            }
            double fall = startY - prediction.posY;
            if (fall > minFall) {
                return true;
            }
        }
        return false;
    }

    private boolean willFallSoon() {
        Clutch.PredictionState prediction = Clutch.PredictionState.fromPlayer();
        for (int t = 0; t < 10; t++) {
            prediction.tick(true);
            if (!prediction.onGround && prediction.motionY < 0.0) {
                return true;
            }
        }
        return false;
    }

    private Clutch.AimResult clutchAim() {
        Vec3 playerPos = new Vec3(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v);
        Vec3 eye = mc.field_71439_g.func_174824_e(1.0F);
        Vec3 futurePos = playerPos;
        if (this.simulateFuturePosition.isToggled()) {
            Clutch.PredictionState prediction = Clutch.PredictionState.fromPlayer();
            for (int t = 0; t < 20; t++) {
                prediction.tick(false);
                if (prediction.posY < playerPos.field_72448_b - 2.0 || prediction.onGround) {
                    break;
                }
            }
            futurePos = prediction.getPos();
        }
        int feetX = MathHelper.func_76128_c(playerPos.field_72450_a);
        int feetZ = MathHelper.func_76128_c(playerPos.field_72449_c);
        int feetY = MathHelper.func_76128_c(playerPos.field_72448_b);
        int minX = feetX - 5;
        int maxX = feetX + 4;
        int minZ = feetZ - 5;
        int maxZ = feetZ + 4;
        int maxY = feetY - 1;
        int minY = feetY - 4;
        ArrayList<Clutch.BlockCandidate> candidates = new ArrayList<>();
        for (int y = maxY; y >= minY; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!this.canPlaceThrough(pos)) {
                        double currentDist = BlockUtils.dist2PointAABB(playerPos, pos);
                        double futureDist = BlockUtils.dist2PointAABB(futurePos, pos);
                        double score = this.simulateFuturePosition.isToggled() ? currentDist * 0.3 + futureDist * 0.7 : currentDist;
                        if (pos.equals(this.lastPlaced)) {
                            score *= 0.95;
                        }
                        candidates.add(new Clutch.BlockCandidate(score, pos));
                    }
                }
            }
        }
        candidates.sort((a, b) -> Double.compare(a.score, b.score));
        ItemStack held = this.plannedSlot >= 0 && this.plannedSlot <= 8 ? mc.field_71439_g.field_71071_by.field_70462_a[this.plannedSlot] : null;
        for (Clutch.BlockCandidate candidate : candidates) {
            boolean underPlayer = this.isBlockUnderPlayer(candidate.pos, playerPos);
            Clutch.AimResult result = this.getBestRotationsToBlock(held, candidate.pos, eye, this.reach.getInput(), underPlayer);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private boolean isBlockUnderPlayer(BlockPos blockPos, Vec3 pos) {
        if (blockPos.func_177956_o() >= MathHelper.func_76128_c(pos.field_72448_b)) {
            return false;
        } else {
            for (double[] corner : CORNERS) {
                int cx = MathHelper.func_76128_c(pos.field_72450_a + corner[0]);
                int cz = MathHelper.func_76128_c(pos.field_72449_c + corner[1]);
                if (blockPos.func_177958_n() == cx && blockPos.func_177952_p() == cz) {
                    return true;
                }
            }
            return false;
        }
    }

    private Clutch.AimResult getBestRotationsToBlock(ItemStack held, BlockPos targetCell, Vec3 eye, double reachVal, boolean underPlayer) {
        double inset = 0.05;
        double step = 0.2;
        double jitter = step * 0.1;
        boolean faceSouth = Math.abs(eye.field_72449_c - (targetCell.func_177952_p() + 1)) < Math.abs(eye.field_72449_c - targetCell.func_177952_p());
        boolean faceEast = Math.abs(eye.field_72450_a - (targetCell.func_177958_n() + 1)) < Math.abs(eye.field_72450_a - targetCell.func_177958_n());
        float baseYaw = normYaw(RotationUtils.serverRotations[0]);
        float basePitch = RotationUtils.serverRotations[1];
        int n = (int) Math.round(1.0 / step);
        ArrayList<Clutch.RotationCandidate> candidates = new ArrayList<>();
        candidates.add(new Clutch.RotationCandidate(0.0, baseYaw, basePitch));
        for (int row = 0; row <= n; row++) {
            double v = clamp01(row * step + randomRange(-jitter, jitter));
            for (int col = 0; col <= n; col++) {
                double u = clamp01(col * step + randomRange(-jitter, jitter));
                if (underPlayer) {
                    float[] rV = getRotationsWrapped(eye, targetCell.func_177958_n() + u, targetCell.func_177956_o() + 1 - inset, targetCell.func_177952_p() + v);
                    double costV = Math.abs(wrapYawDelta(baseYaw, rV[0])) + Math.abs(rV[1] - basePitch);
                    candidates.add(new Clutch.RotationCandidate(costV, rV[0], rV[1]));
                }
                float[] rZ = getRotationsWrapped(eye, targetCell.func_177958_n() + u, targetCell.func_177956_o() + v, faceSouth ? targetCell.func_177952_p() + 1 - inset : targetCell.func_177952_p() + inset);
                double costZ = Math.abs(wrapYawDelta(baseYaw, rZ[0])) + Math.abs(rZ[1] - basePitch);
                candidates.add(new Clutch.RotationCandidate(costZ, rZ[0], rZ[1]));
                float[] rX = getRotationsWrapped(eye, faceEast ? targetCell.func_177958_n() + 1 - inset : targetCell.func_177958_n() + inset, targetCell.func_177956_o() + v, targetCell.func_177952_p() + u);
                double costX = Math.abs(wrapYawDelta(baseYaw, rX[0])) + Math.abs(rX[1] - basePitch);
                candidates.add(new Clutch.RotationCandidate(costX, rX[0], rX[1]));
            }
        }
        candidates.sort((a, b) -> Double.compare(a.cost, b.cost));
        for (Clutch.RotationCandidate candidate : candidates) {
            float yaw = unwrapYaw(candidate.yaw, RotationUtils.serverRotations[0]);
            MovingObjectPosition ray = RotationUtils.rayCastBlock(reachVal, yaw, candidate.pitch);
            if (ray != null) {
                EnumFacing face = ray.field_178784_b;
                if (face != EnumFacing.DOWN && (face != EnumFacing.UP || underPlayer) && targetCell.equals(ray.func_178782_a()) && BlockUtils.canPlaceBlockOnSide(held, ray.func_178782_a(), face)) {
                    return new Clutch.AimResult(ray, yaw, candidate.pitch);
                }
            }
        }
        return null;
    }

    private int pickBlockSlot() {
        boolean playingBedwars = Utils.getBedwarsStatus() == 2;
        if (!playingBedwars) {
            int current = mc.field_71439_g.field_71071_by.field_70461_c;
            if (this.isBlockSlot(current)) {
                return current;
            } else {
                for (int slot = 8; slot >= 0; slot--) {
                    if (this.isBlockSlot(slot)) {
                        return slot;
                    }
                }
                return -1;
            }
        } else {
            int best = -1;
            int bestScore = Integer.MIN_VALUE;
            for (int intValue = 8; intValue >= 0; intValue--) {
                ItemStack stack = mc.field_71439_g.field_71071_by.field_70462_a[intValue];
                if (stack != null && stack.field_77994_a != 0 && stack.func_77973_b() instanceof ItemBlock && !this.itemBlacklist.matches(stack)) {
                    Block block = ((ItemBlock) stack.func_77973_b()).func_179223_d();
                    ResourceLocation id = (ResourceLocation) Block.field_149771_c.func_177774_c(block);
                    if (id != null) {
                        Integer score = BLOCK_SCORE.get(id.func_110623_a());
                        if (score != null && score > bestScore) {
                            bestScore = score;
                            best = intValue;
                        }
                    }
                }
            }
            return best;
        }
    }

    private boolean isBlockSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            ItemStack stack = mc.field_71439_g.field_71071_by.field_70462_a[slot];
            return stack != null && stack.field_77994_a > 0 && stack.func_77973_b() instanceof ItemBlock && !this.itemBlacklist.matches(stack);
        } else {
            return false;
        }
    }

    private void equipPlannedSlot() {
        int current = mc.field_71439_g.field_71071_by.field_70461_c;
        if (this.plannedSlot != -1 && this.plannedSlot != current) {
            mc.field_71439_g.field_71071_by.field_70461_c = this.plannedSlot;
            this.slotWasSwapped = true;
        }
    }

    private float[] getRotationsSmoothed(float currentYaw, float currentPitch, float targetYaw, float targetPitch, boolean snapback) {
        float curYaw = currentYaw;
        float curPitch = currentPitch;
        float deltaYaw = MathHelper.func_76142_g(targetYaw - currentYaw);
        float deltaPitch = targetPitch - currentPitch;
        if (Math.abs(deltaYaw) < 0.1F) {
            curYaw = targetYaw;
        }
        if (Math.abs(deltaPitch) < 0.1F) {
            curPitch = targetPitch;
        }
        if (curYaw == targetYaw && curPitch == targetPitch) {
            return new float[] { curYaw, RotationUtils.clampPitch(curPitch) };
        } else {
            float maxStep = (float) (snapback ? this.snapbackSpeed.getInput() : this.speed.getInput());
            float factor = 1.0F - (float) randomRange(0.0, 0.2);
            maxStep *= factor;
            float totalDelta = Math.abs(deltaYaw) + Math.abs(deltaPitch);
            if (totalDelta <= maxStep) {
                curYaw = targetYaw;
                curPitch = targetPitch;
            } else if (maxStep > 0.0F) {
                float scale = maxStep / totalDelta;
                curYaw += deltaYaw * scale;
                curPitch += deltaPitch * scale;
            }
            return new float[] { curYaw, RotationUtils.clampPitch(curPitch) };
        }
    }

    private boolean canPlaceThrough(BlockPos pos) {
        Block block = BlockUtils.getBlock(pos);
        Material material = block.func_149688_o();
        return material == Material.field_151579_a || material == Material.field_151586_h || material == Material.field_151587_i || block == Blocks.field_150480_ab;
    }

    private boolean canClutchHere() {
        return !this.requireVoid.isToggled() || Utils.overVoid(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    private static double randomRange(double min, double max) {
        return min + Math.random() * (max - min);
    }

    private static float normYaw(float yaw) {
        yaw = (yaw % 360.0F + 360.0F) % 360.0F;
        return yaw > 180.0F ? yaw - 360.0F : yaw;
    }

    private static float wrapYawDelta(float base, float target) {
        return MathHelper.func_76142_g(target - base);
    }

    private static float unwrapYaw(float yaw, float prevYaw) {
        return prevYaw + MathHelper.func_76142_g(yaw - prevYaw);
    }

    private static float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.field_72450_a;
        double dy = ty - eye.field_72448_b;
        double dz = tz - eye.field_72449_c;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDistance));
        return new float[] { normYaw(yaw), RotationUtils.clampPitch(pitch) };
    }

    static {
        BLOCK_SCORE.put("obsidian", 0);
        BLOCK_SCORE.put("end_stone", 1);
        BLOCK_SCORE.put("planks", 2);
        BLOCK_SCORE.put("log", 2);
        BLOCK_SCORE.put("log2", 2);
        BLOCK_SCORE.put("glass", 3);
        BLOCK_SCORE.put("stained_glass", 3);
        BLOCK_SCORE.put("hardened_clay", 4);
        BLOCK_SCORE.put("stained_hardened_clay", 4);
        BLOCK_SCORE.put("stone", 5);
        BLOCK_SCORE.put("wool", 5);
    }

    private static class AimResult {

        final MovingObjectPosition ray;

        final float yaw;

        final float pitch;

        AimResult(MovingObjectPosition ray, float yaw, float pitch) {
            this.ray = ray;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static class BlockCandidate {

        final double score;

        final BlockPos pos;

        BlockCandidate(double score, BlockPos pos) {
            this.score = score;
            this.pos = pos;
        }
    }

    private static class PredictionState {

        private AxisAlignedBB box;

        private double motionX;

        private double motionY;

        private double motionZ;

        private double posY;

        private boolean onGround;

        static Clutch.PredictionState fromPlayer() {
            Clutch.PredictionState state = new Clutch.PredictionState();
            state.box = Clutch.mc.field_71439_g.func_174813_aQ();
            state.motionX = Clutch.mc.field_71439_g.field_70159_w;
            state.motionY = Clutch.mc.field_71439_g.field_70181_x;
            state.motionZ = Clutch.mc.field_71439_g.field_70179_y;
            state.posY = Clutch.mc.field_71439_g.field_70163_u;
            state.onGround = Clutch.mc.field_71439_g.field_70122_E;
            return state;
        }

        Vec3 getPos() {
            return new Vec3((this.box.field_72340_a + this.box.field_72336_d) / 2.0, this.box.field_72338_b, (this.box.field_72339_c + this.box.field_72334_f) / 2.0);
        }

        void tick(boolean stopHorizontal) {
            if (stopHorizontal) {
                this.motionX = 0.0;
                this.motionZ = 0.0;
            }
            this.motionY -= 0.08;
            this.move(this.motionX, this.motionY, this.motionZ);
            this.motionY *= 0.98F;
            this.motionX *= 0.91;
            this.motionZ *= 0.91;
        }

        private void move(double x, double y, double z) {
            double originalX = x;
            double originalY = y;
            double originalZ = z;
            List<AxisAlignedBB> collisions = Clutch.mc.field_71441_e.func_72945_a(Clutch.mc.field_71439_g, this.box.func_72321_a(x, y, z));
            for (AxisAlignedBB collision : collisions) {
                y = collision.func_72323_b(this.box, y);
            }
            this.box = this.box.func_72317_d(0.0, y, 0.0);
            for (AxisAlignedBB collision : collisions) {
                x = collision.func_72316_a(this.box, x);
            }
            this.box = this.box.func_72317_d(x, 0.0, 0.0);
            for (AxisAlignedBB collision : collisions) {
                z = collision.func_72322_c(this.box, z);
            }
            this.box = this.box.func_72317_d(0.0, 0.0, z);
            this.onGround = originalY != y && originalY < 0.0;
            this.posY = this.box.field_72338_b;
            if (originalX != x) {
                this.motionX = 0.0;
            }
            if (originalY != y) {
                this.motionY = 0.0;
            }
            if (originalZ != z) {
                this.motionZ = 0.0;
            }
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
}
