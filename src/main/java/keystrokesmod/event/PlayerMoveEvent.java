package keystrokesmod.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class PlayerMoveEvent extends Event {

    public double x;

    public double y;

    public double z;

    public PlayerMoveEvent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
