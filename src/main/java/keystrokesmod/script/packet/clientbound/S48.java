package keystrokesmod.script.packet.clientbound;

import net.minecraft.network.play.server.S48PacketResourcePackSend;

public class S48 extends SPacket {

    public String url;

    public String hash;

    public S48(S48PacketResourcePackSend packet) {
        super(packet);
        this.url = packet.func_179783_a();
        this.hash = packet.func_179784_b();
    }

    public S48(String url, String hash) {
        super(new S48PacketResourcePackSend(url, hash));
        this.url = url;
        this.hash = hash;
    }
}
