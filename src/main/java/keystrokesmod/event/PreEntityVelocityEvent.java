package keystrokesmod.event;

import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PreEntityVelocityEvent extends Event {

    public S12PacketEntityVelocity packet;

    public PreEntityVelocityEvent(S12PacketEntityVelocity packet) {
        this.packet = packet;
    }
}
