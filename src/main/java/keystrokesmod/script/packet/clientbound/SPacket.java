package keystrokesmod.script.packet.clientbound;

import net.minecraft.network.Packet;

public class SPacket {

    public String name;

    public Packet packet;

    public SPacket(Packet packet) {
        this.packet = packet;
        this.name = packet.getClass().getSimpleName();
    }
}
