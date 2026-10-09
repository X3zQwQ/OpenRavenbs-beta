package keystrokesmod.script.packet.serverbound;

import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;
import net.minecraft.network.play.client.C0BPacketEntityAction;

public class C0B extends CPacket {

    public String action;

    public int horsePower;

    public C0B(String action, int horsePower) {
        super(null);
        this.action = action;
        this.horsePower = horsePower;
    }

    public C0B(C0BPacketEntityAction packet) {
        super(packet);
        this.action = packet.func_180764_b().name();
        this.horsePower = packet.func_149512_e();
    }

    public C0BPacketEntityAction convert() {
        return new C0BPacketEntityAction(Minecraft.func_71410_x().field_71439_g, Utils.getEnum(Action.class, this.action), this.horsePower);
    }
}
