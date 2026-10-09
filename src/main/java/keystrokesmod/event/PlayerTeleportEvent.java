package keystrokesmod.event;

import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PlayerTeleportEvent extends Event {

    public S08PacketPlayerPosLook packet;

    public PlayerTeleportEvent(S08PacketPlayerPosLook packetIn) {
        this.packet = packetIn;
    }
}
