package keystrokesmod.script.packet.serverbound;

import net.minecraft.network.play.client.C01PacketChatMessage;

public class C01 extends CPacket {

    public String message;

    public C01(String message) {
        super(null);
        this.message = message;
    }

    public C01(C01PacketChatMessage packet, byte f) {
        super(packet);
        this.message = packet.func_149439_c();
    }

    public C01PacketChatMessage convert() {
        return new C01PacketChatMessage(this.message);
    }
}
