package keystrokesmod.event;

import keystrokesmod.script.model.MovementInput;
import net.minecraftforge.fml.common.eventhandler.Event;

public class PrePlayerInputEvent extends Event {

    private float forward;

    private float strafe;

    private boolean jump;

    private boolean sneak;

    private double sneakSlowDownMultiplier;

    public PrePlayerInputEvent(float forward, float strafe, boolean jump, boolean sneak, double sneakSlowDownMultiplier) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
        this.sneakSlowDownMultiplier = sneakSlowDownMultiplier;
    }

    public float getForward() {
        return this.forward;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    public float getStrafe() {
        return this.strafe;
    }

    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }

    public boolean isJump() {
        return this.jump;
    }

    public void setJump(boolean jump) {
        this.jump = jump;
    }

    public boolean isSneak() {
        return this.sneak;
    }

    public void setSneak(boolean sneak) {
        this.sneak = sneak;
    }

    public double getSneakSlowDownMultiplier() {
        return this.sneakSlowDownMultiplier;
    }

    public void setSneakSlowDownMultiplier(double sneakSlowDownMultiplier) {
        this.sneakSlowDownMultiplier = sneakSlowDownMultiplier;
    }

    public boolean isEquals(MovementInput e) {
        return e.sneak == this.sneak && e.jump == this.jump && e.forward == this.forward && e.strafe == this.strafe;
    }
}
