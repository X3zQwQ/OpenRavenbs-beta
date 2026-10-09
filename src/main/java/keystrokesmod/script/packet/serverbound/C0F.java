package keystrokesmod.script.packet.serverbound;

import net.minecraft.network.play.client.C0FPacketConfirmTransaction;

public class C0F extends CPacket {

    public int windowId;

    public short uid;

    public C0F(int windowId, short uid) {
        super(null);
        this.windowId = windowId;
        this.uid = uid;
    }

    public C0F(C0FPacketConfirmTransaction packet) {
        super(packet);
        this.windowId = packet.func_149532_c();
        this.uid = packet.func_149533_d();
    }

    public C0FPacketConfirmTransaction convert() {
        return new C0FPacketConfirmTransaction(this.windowId, this.uid, true);
    }
}
