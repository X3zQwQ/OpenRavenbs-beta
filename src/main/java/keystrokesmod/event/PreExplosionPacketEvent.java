package keystrokesmod.event;

import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PreExplosionPacketEvent extends Event {

    public S27PacketExplosion packet;

    public PreExplosionPacketEvent(S27PacketExplosion packet) {
        this.packet = packet;
    }
}
