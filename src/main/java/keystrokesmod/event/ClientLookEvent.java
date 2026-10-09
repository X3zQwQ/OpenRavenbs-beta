package keystrokesmod.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ClientLookEvent extends Event {

    public float yaw;

    public float pitch;

    public ClientLookEvent(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
