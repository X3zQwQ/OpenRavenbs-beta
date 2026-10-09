package keystrokesmod.helper;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.StrafeEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RotationHelper {

    private static RotationHelper INSTANCE = new RotationHelper();

    private Float serverYaw = null;

    private Float serverPitch = null;

    private boolean setRotations = false;

    private boolean serverRelativeMovement;

    public boolean forceMovementFix = false;

    private float savedYaw;

    private float savedPitch;

    private float savedPrevYaw;

    private float savedPrevPitch;

    public boolean swappedForMouseOver;

    private boolean rotationsUpdatedThisTick = false;

    private boolean needsArmYawUpdate = false;

    private Minecraft mc = Minecraft.func_71410_x();

    public static float unwrapYaw(float yaw, float prevYaw) {
        return prevYaw + (((yaw - prevYaw + 180.0F) % 360.0F + 360.0F) % 360.0F - 180.0F);
    }

    public float[] getRotationsToTarget(Entity target, ClientRotationEvent e, float smoothingFactor) {
        if (target != null && this.mc.field_71439_g != null) {
            float baseYaw = e.yaw != null ? e.yaw : RotationUtils.serverRotations[0];
            float basePitch = e.pitch != null ? e.pitch : RotationUtils.serverRotations[1];
            float[] rot = RotationUtils.getRotations(target, baseYaw, basePitch);
            if (rot == null) {
                return null;
            } else {
                float factor = Math.max(1.0F, smoothingFactor);
                float yaw = baseYaw + MathHelper.func_76142_g(rot[0] - baseYaw) / factor;
                float pitch = basePitch + (rot[1] - basePitch) / factor;
                return new float[] { yaw, pitch };
            }
        } else {
            return null;
        }
    }

    public float[] getRotationsToTarget(Entity target, float smoothingFactor) {
        if (target != null && this.mc.field_71439_g != null) {
            float baseYaw = this.mc.field_71439_g.field_70177_z;
            float basePitch = this.mc.field_71439_g.field_70125_A;
            float[] rot = RotationUtils.getRotations(target, baseYaw, basePitch);
            if (rot == null) {
                return null;
            } else {
                float factor = Math.max(1.0F, smoothingFactor);
                float yaw = baseYaw + MathHelper.func_76142_g(rot[0] - baseYaw) / factor;
                float pitch = basePitch + (rot[1] - basePitch) / factor;
                return new float[] { yaw, pitch };
            }
        } else {
            return null;
        }
    }

    public float[] getRotationsToTarget(Entity target, ClientRotationEvent e, int speed) {
        return this.getRotationsToTarget(target, e, speed, 0.0, 0.0, 0.0F);
    }

    public float[] getRotationsToTarget(Entity target, ClientRotationEvent e, int speed, double horizontalMultipoint, double verticalMultipoint, float randomizationPercent) {
        return this.getRotationsToTarget(target, e, speed, horizontalMultipoint, verticalMultipoint, randomizationPercent, false, 10.0);
    }

    public float[] getRotationsToTarget(Entity target, ClientRotationEvent e, int speed, double horizontalMultipoint, double verticalMultipoint, float randomizationPercent, boolean useBackupPoints, double range) {
        return this.getRotationsToTarget(target, e, speed, horizontalMultipoint, verticalMultipoint, randomizationPercent, useBackupPoints, range, false, true);
    }

    public float[] getRotationsToTarget(Entity target, ClientRotationEvent e, int speed, double horizontalMultipoint, double verticalMultipoint, float randomizationPercent, boolean useBackupPoints, double range, boolean allowThroughBlocks, boolean allowThroughEntities) {
        return this.getRotationsToTarget(target, e, speed, horizontalMultipoint, verticalMultipoint, randomizationPercent, useBackupPoints, range, allowThroughBlocks, allowThroughEntities, false);
    }

    public float[] getRotationsToTarget(Entity target, ClientRotationEvent e, int speed, double horizontalMultipoint, double verticalMultipoint, float randomizationPercent, boolean useBackupPoints, double range, boolean allowThroughBlocks, boolean allowThroughEntities, boolean ignoreOpenFenceGates) {
        if (target != null && this.mc.field_71439_g != null) {
            float baseYaw = e.yaw != null ? e.yaw : RotationUtils.serverRotations[0];
            float basePitch = e.pitch != null ? e.pitch : RotationUtils.serverRotations[1];
            float[] rot = useBackupPoints ? RotationUtils.getRotationsWithBackup(target, horizontalMultipoint, verticalMultipoint, baseYaw, basePitch, range, allowThroughBlocks, allowThroughEntities, ignoreOpenFenceGates) : RotationUtils.getRotations(target, horizontalMultipoint, verticalMultipoint, baseYaw, basePitch);
            return rot == null ? null : RotationUtils.smoothRotation(baseYaw, basePitch, rot[0], rot[1], speed, randomizationPercent);
        } else {
            return null;
        }
    }

    public float[] getRotationsToTarget(Entity target, int speed) {
        return this.getRotationsToTarget(target, speed, 0.0, 0.0, 0.0F);
    }

    public float[] getRotationsToTarget(Entity target, int speed, double horizontalMultipoint, double verticalMultipoint, float randomizationPercent) {
        return this.getRotationsToTarget(target, speed, horizontalMultipoint, verticalMultipoint, randomizationPercent, false, 10.0);
    }

    public float[] getRotationsToTarget(Entity target, int speed, double horizontalMultipoint, double verticalMultipoint, float randomizationPercent, boolean useBackupPoints, double range) {
        return this.getRotationsToTarget(target, speed, horizontalMultipoint, verticalMultipoint, randomizationPercent, useBackupPoints, range, false, true);
    }

    public float[] getRotationsToTarget(Entity target, int speed, double horizontalMultipoint, double verticalMultipoint, float randomizationPercent, boolean useBackupPoints, double range, boolean allowThroughBlocks, boolean allowThroughEntities) {
        return this.getRotationsToTarget(target, speed, horizontalMultipoint, verticalMultipoint, randomizationPercent, useBackupPoints, range, allowThroughBlocks, allowThroughEntities, false);
    }

    public float[] getRotationsToTarget(Entity target, int speed, double horizontalMultipoint, double verticalMultipoint, float randomizationPercent, boolean useBackupPoints, double range, boolean allowThroughBlocks, boolean allowThroughEntities, boolean ignoreOpenFenceGates) {
        if (target != null && this.mc.field_71439_g != null) {
            float baseYaw = this.mc.field_71439_g.field_70177_z;
            float basePitch = this.mc.field_71439_g.field_70125_A;
            float[] rot = useBackupPoints ? RotationUtils.getRotationsWithBackup(target, horizontalMultipoint, verticalMultipoint, baseYaw, basePitch, range, allowThroughBlocks, allowThroughEntities, ignoreOpenFenceGates) : RotationUtils.getRotations(target, horizontalMultipoint, verticalMultipoint, baseYaw, basePitch);
            return rot == null ? null : RotationUtils.smoothRotation(baseYaw, basePitch, rot[0], rot[1], speed, randomizationPercent);
        } else {
            return null;
        }
    }

    public void updateServerRotations() {
        if (this.mc.field_71439_g != null) {
            if (!this.rotationsUpdatedThisTick) {
                this.rotationsUpdatedThisTick = true;
                ClientRotationEvent event = new ClientRotationEvent(this.serverYaw, this.serverPitch);
                MinecraftForge.EVENT_BUS.post(event);
                this.serverYaw = event.yaw;
                this.serverPitch = event.pitch;
                this.serverRelativeMovement = event.hasServerRelativeMovement();
                if (this.serverYaw != null || this.serverPitch != null) {
                    if (!event.hasExactRotations()) {
                        if (this.serverYaw != null && Math.abs(this.serverYaw - this.mc.field_71439_g.field_70177_z) >= 1.0F) {
                            int randomFactor = (int) Settings.randomYawFactor.getInput();
                            if (randomFactor != 0) {
                                int n13 = randomFactor * 100 + Utils.randomizeInt(-30, 30);
                                this.serverYaw = this.serverYaw + Utils.randomizeInt(-n13, n13) / 100.0F;
                            }
                        }
                        float[] fixed = RotationUtils.fixRotation(this.serverYaw == null ? this.mc.field_71439_g.field_70177_z : this.serverYaw, this.serverPitch == null ? this.mc.field_71439_g.field_70125_A : this.serverPitch, RotationUtils.serverRotations[0], RotationUtils.serverRotations[1]);
                        this.serverYaw = fixed[0];
                        this.serverPitch = fixed[1];
                    }
                    if (this.serverYaw != this.mc.field_71439_g.field_70177_z && (event.yaw == null || !event.yaw.isNaN())) {
                        this.setRotations = true;
                    }
                    if (this.serverPitch != this.mc.field_71439_g.field_70125_A && (event.pitch == null || !event.pitch.isNaN())) {
                        this.setRotations = true;
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreUpdate(PreUpdateEvent e) {
        this.updateServerRotations();
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (this.setRotations) {
            if (this.serverYaw != null && !this.serverYaw.isNaN()) {
                e.setYaw(this.serverYaw);
            }
            if (this.serverPitch != null && !this.serverPitch.isNaN()) {
                e.setPitch(this.serverPitch);
            }
        }
    }

    @SubscribeEvent
    public void onRunTick(GameTickEvent e) {
        if (this.setRotations && this.serverYaw != null && this.mc.field_71439_g != null) {
            float serverYawVal = RotationUtils.serverRotations[0];
            float unwrapped = unwrapYaw(MathHelper.func_76142_g(this.mc.field_71439_g.field_70177_z), serverYawVal);
            this.mc.field_71439_g.field_70177_z = unwrapped;
            this.mc.field_71439_g.field_70126_B = unwrapped;
        }
        this.serverYaw = this.serverPitch = null;
        this.setRotations = this.forceMovementFix = false;
        this.serverRelativeMovement = false;
        this.rotationsUpdatedThisTick = false;
        this.swappedForMouseOver = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (this.setRotations && this.mc.field_71439_g != null) {
            this.mc.field_71439_g.field_71163_h = this.mc.field_71439_g.field_70177_z;
            this.mc.field_71439_g.field_71154_f = this.mc.field_71439_g.field_70177_z;
        }
    }

    public boolean isActive() {
        return this.setRotations && (this.serverYaw != null || this.serverPitch != null);
    }

    public void beginSwap(Entity e, float yaw, float pitch, boolean swapPitch) {
        this.savedYaw = e.field_70177_z;
        this.savedPrevYaw = e.field_70126_B;
        this.savedPitch = e.field_70125_A;
        this.savedPrevPitch = e.field_70127_C;
        e.field_70177_z = yaw;
        e.field_70126_B = yaw;
        if (swapPitch) {
            e.field_70125_A = pitch;
            e.field_70127_C = pitch;
        }
    }

    public void endSwap(Entity e) {
        e.field_70177_z = this.savedYaw;
        e.field_70126_B = this.savedPrevYaw;
        e.field_70125_A = this.savedPitch;
        e.field_70127_C = this.savedPrevPitch;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostInput(PostPlayerInputEvent event) {
        if (this.fixMovement() && !this.serverRelativeMovement) {
            float sneakMultiplier = this.mc.field_71439_g.field_71158_b.field_78899_d ? 0.3F : 1.0F;
            float yaw = this.serverYaw;
            float forward = this.mc.field_71439_g.field_71158_b.field_78900_b;
            float strafe = this.mc.field_71439_g.field_71158_b.field_78902_a;
            if (forward != 0.0F || strafe != 0.0F) {
                double angle = Math.toDegrees(getDirection(this.mc.field_71439_g.field_70177_z, forward, strafe));
                float closestForward = 0.0F;
                float closestStrafe = 0.0F;
                float closestDifference = Float.MAX_VALUE;
                for (float pfRaw = -1.0F; pfRaw <= 1.0F; pfRaw++) {
                    for (float psRaw = -1.0F; psRaw <= 1.0F; psRaw++) {
                        if (pfRaw != 0.0F || psRaw != 0.0F) {
                            float predictedForward = pfRaw * sneakMultiplier;
                            float predictedStrafe = psRaw * sneakMultiplier;
                            double predictedAngle = Math.toDegrees(getDirection(yaw, predictedForward, predictedStrafe));
                            double difference = Math.abs(MathHelper.func_76138_g(angle - predictedAngle));
                            if (difference < closestDifference) {
                                closestDifference = (float) difference;
                                closestForward = predictedForward;
                                closestStrafe = predictedStrafe;
                            }
                        }
                    }
                }
                this.mc.field_71439_g.field_71158_b.field_78900_b = closestForward;
                this.mc.field_71439_g.field_71158_b.field_78902_a = closestStrafe;
            }
        }
    }

    @SubscribeEvent
    public void onStrafe(StrafeEvent e) {
        if (this.fixMovement()) {
            e.setYaw(this.serverYaw);
        }
    }

    @SubscribeEvent
    public void onJump(JumpEvent e) {
        if (e.entity == this.mc.field_71439_g && this.fixMovement()) {
            e.setYaw(this.serverYaw);
        }
    }

    public boolean fixMovement() {
        return this.serverRelativeMovement || (ModuleManager.movementFix != null && ModuleManager.movementFix.isEnabled() || this.forceMovementFix) && this.setRotations;
    }

    public boolean ownsServerRelativeMovement() {
        return this.serverRelativeMovement;
    }

    public void releaseServerRelativeMovement() {
        this.serverRelativeMovement = false;
    }

    public static double getDirection(float rotationYaw, double moveForward, double moveStrafing) {
        if (moveForward < 0.0) {
            rotationYaw += 180.0F;
        }
        float forward = 1.0F;
        if (moveForward < 0.0) {
            forward = -0.5F;
        } else if (moveForward > 0.0) {
            forward = 0.5F;
        }
        if (moveStrafing > 0.0) {
            rotationYaw -= 90.0F * forward;
        }
        if (moveStrafing < 0.0) {
            rotationYaw += 90.0F * forward;
        }
        return Math.toRadians(rotationYaw);
    }

    public static RotationHelper get() {
        return INSTANCE;
    }

    public void setRotations(float yaw, float pitch) {
        this.serverRelativeMovement = false;
        this.serverYaw = yaw;
        this.serverPitch = pitch;
        this.setRotations = true;
    }

    public void setYaw(float yaw) {
        this.serverRelativeMovement = false;
        this.serverYaw = yaw;
        this.setRotations = true;
    }

    public void setPitch(float pitch) {
        this.serverRelativeMovement = false;
        this.serverPitch = pitch;
        this.setRotations = true;
    }

    public Float getServerYaw() {
        return this.serverYaw;
    }

    public Float getServerPitch() {
        return this.serverPitch;
    }
}
