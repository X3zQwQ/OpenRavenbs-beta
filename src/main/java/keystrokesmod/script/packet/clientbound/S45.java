package keystrokesmod.script.packet.clientbound;

import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S45PacketTitle.Type;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.ChatComponentText;

public class S45 extends SPacket {

    public String type;

    public String message;

    private int fadeInTime;

    private int displayTime;

    private int fadeOutTime;

    public S45(S45PacketTitle packet) {
        super(packet);
        this.type = packet.func_179807_a().name();
        this.message = packet.func_179805_b().func_150260_c();
        this.fadeInTime = packet.func_179806_c();
        this.displayTime = packet.func_179804_d();
        this.fadeOutTime = packet.func_179803_e();
    }

    public S45(String type, String message, int fadeInTime, int displayTime, int fadeOutTime) {
        super(new S45PacketTitle(Utils.getEnum(Type.class, type), new ChatComponentText(message), fadeInTime, displayTime, fadeOutTime));
        this.type = type;
        this.message = message;
        this.fadeInTime = fadeInTime;
        this.displayTime = displayTime;
        this.fadeOutTime = fadeOutTime;
    }
}
