package keystrokesmod.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class PrePlayerMovementInputEvent extends Event {

    public float forward;

    public float strafe;

    public PrePlayerMovementInputEvent(float forward, float strafe) {
        this.forward = forward;
        this.strafe = strafe;
    }
}
