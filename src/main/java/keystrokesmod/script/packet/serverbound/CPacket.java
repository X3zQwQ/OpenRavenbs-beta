package keystrokesmod.script.packet.serverbound;

import net.minecraft.network.Packet;

public class CPacket {

    public String name;

    public Packet packet;

    public CPacket(Packet packet) {
        if (packet != null) {
            this.packet = packet;
            this.name = packet.getClass().getSimpleName();
        }
    }

    public Packet convert() {
        return this.packet;
    }
}
