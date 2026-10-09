package keystrokesmod.script.model;

import keystrokesmod.event.PreMotionEvent;

public class PlayerState {

    public double x;

    public double y;

    public double z;

    public float yaw;

    public float pitch;

    public boolean onGround;

    public boolean isSprinting;

    public boolean isSneaking;

    public PlayerState(PreMotionEvent e, byte f1) {
        this.x = e.getPosX();
        this.y = e.getPosY();
        this.z = e.getPosZ();
        this.yaw = e.getYaw();
        this.pitch = e.getPitch();
        this.onGround = e.isOnGround();
        this.isSprinting = e.isSprinting();
        this.isSneaking = e.isSneaking();
    }

    public PlayerState(Object[] state) {
        this.x = (Double) state[0];
        this.y = (Double) state[1];
        this.z = (Double) state[2];
        this.yaw = (Float) state[3];
        this.pitch = (Float) state[4];
        this.onGround = (Boolean) state[5];
        this.isSprinting = (Boolean) state[6];
        this.isSneaking = (Boolean) state[7];
    }

    public Object[] asArray() {
        return new Object[] { this.x, this.y, this.z, this.yaw, this.pitch, this.onGround, this.isSprinting, this.isSneaking };
    }

    public boolean equals(PlayerState playerState) {
        return playerState == null ? false : this.x == playerState.x && this.y == playerState.y && this.z == playerState.z && this.yaw == playerState.yaw && this.pitch == playerState.pitch && this.onGround == playerState.onGround && this.isSprinting == playerState.isSprinting && this.isSneaking == playerState.isSneaking;
    }
}
