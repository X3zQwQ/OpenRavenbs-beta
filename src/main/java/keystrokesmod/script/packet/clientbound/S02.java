package keystrokesmod.script.packet.clientbound;

import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.ChatComponentText;

public class S02 extends SPacket {

    public byte type;

    public String message;

    public S02(S02PacketChat packet) {
        super(packet);
        this.type = packet.func_179841_c();
        this.message = packet.func_148915_c().func_150260_c();
    }

    public S02(byte type, String message) {
        super(new S02PacketChat(new ChatComponentText(message), type));
        this.type = type;
        this.message = message;
    }
}
