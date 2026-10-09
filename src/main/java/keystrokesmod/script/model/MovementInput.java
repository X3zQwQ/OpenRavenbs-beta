package keystrokesmod.script.model;

import keystrokesmod.event.PrePlayerInputEvent;

public class MovementInput {

    public float forward;

    public float strafe;

    public boolean jump;

    public boolean sneak;

    public MovementInput(PrePlayerInputEvent event, byte f1) {
        this.forward = event.getForward();
        this.strafe = event.getStrafe();
        this.jump = event.isJump();
        this.sneak = event.isSneak();
    }

    public MovementInput(float forward, float strafe, boolean jump, boolean sneak) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
    }

    public MovementInput(Object[] state) {
        this.forward = (Float) state[0];
        this.strafe = (Float) state[1];
        this.jump = (Boolean) state[2];
        this.sneak = (Boolean) state[3];
    }

    public Object[] asArray() {
        return new Object[] { this.forward, this.strafe, this.jump, this.sneak };
    }

    public boolean equals(MovementInput input) {
        return input == null ? false : this.forward == input.forward && this.strafe == input.strafe && this.jump == input.jump && this.sneak == input.sneak;
    }
}
