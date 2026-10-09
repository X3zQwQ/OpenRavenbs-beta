package keystrokesmod.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ClientRotationEvent extends Event {

    public Float yaw;

    public Float pitch;

    public boolean scriptRotations;

    private Float exactYaw;

    private Float exactPitch;

    private boolean serverRelativeMovement;

    public void claimExactRotations(float yaw, float pitch, boolean serverRelativeMovement) {
        this.yaw = this.exactYaw = yaw;
        this.pitch = this.exactPitch = pitch;
        this.serverRelativeMovement = serverRelativeMovement;
    }

    public boolean hasExactRotations() {
        return this.exactYaw != null && this.exactYaw.equals(this.yaw) && this.exactPitch.equals(this.pitch);
    }

    public boolean hasServerRelativeMovement() {
        return this.hasExactRotations() && this.serverRelativeMovement;
    }

    public ClientRotationEvent(Float yaw, Float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setYaw(Float yaw) {
        this.exactYaw = null;
        this.yaw = yaw;
    }

    public void setPitch(Float pitch) {
        this.exactYaw = null;
        this.pitch = pitch;
    }
}
