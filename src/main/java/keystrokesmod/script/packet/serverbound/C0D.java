package keystrokesmod.script.packet.serverbound;

import net.minecraft.network.play.client.C0DPacketCloseWindow;

public class C0D extends CPacket {

    public int windowId;

    public C0D(int windowId) {
        super(null);
        this.windowId = windowId;
    }

    public C0D(C0DPacketCloseWindow packet) {
        super(packet);
    }

    public C0DPacketCloseWindow convert() {
        return new C0DPacketCloseWindow(this.windowId);
    }
}
