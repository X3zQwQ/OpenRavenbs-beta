package keystrokesmod.script.model;

import keystrokesmod.utility.SimulatedPlayer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;

public class Simulation {

    private final SimulatedPlayer raw;

    private Simulation(SimulatedPlayer raw) {
        this.raw = raw;
    }

    public static Simulation create() {
        Minecraft mc = Minecraft.func_71410_x();
        if (mc != null && mc.field_71439_g != null && mc.field_71439_g.field_71158_b != null) {
            net.minecraft.util.MovementInput input = mc.field_71439_g.field_71158_b;
            SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(input);
            return new Simulation(sim);
        } else {
            throw new IllegalStateException("Simulation requires an initialized client player");
        }
    }

    public void setForward(float forward) {
        this.raw.movementInput.field_78900_b = forward;
    }

    public void setStrafe(float strafe) {
        this.raw.movementInput.field_78902_a = strafe;
    }

    public void setJump(boolean jump) {
        this.raw.movementInput.field_78901_c = jump;
    }

    public void setSneak(boolean sneak) {
        this.raw.movementInput.field_78899_d = sneak;
    }

    public void setYaw(float yaw) {
        this.raw.rotationYaw = yaw;
    }

    public void setPitch(float pitch) {
        this.raw.rotationPitch = pitch;
    }

    public void setSprinting(boolean sprinting) {
        this.raw.setSprintRequested(sprinting);
    }

    public float getAirAcceleration() {
        return this.raw.getAirAcceleration();
    }

    public void resetState(double x, double y, double z, boolean onGround) {
        this.raw.resetSimulationState(x, y, z, onGround);
    }

    public void tick() {
        this.raw.tick();
    }

    public Vec3 getPosition() {
        return new Vec3(this.raw.getPos());
    }

    public Vec3 getMotion() {
        return new Vec3(this.raw.motionX, this.raw.motionY, this.raw.motionZ);
    }

    public void setBlockState(BlockPos pos, IBlockState state) {
        this.raw.setBlockState(pos, state);
    }

    public boolean isInLiquid() {
        return this.raw.isInWater() || this.raw.isInLava();
    }

    public boolean isOnLadder() {
        return this.raw.isOnLadder();
    }

    public boolean onGround() {
        return this.raw.onGround;
    }
}
