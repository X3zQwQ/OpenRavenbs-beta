package keystrokesmod.script.packet.serverbound;

import keystrokesmod.script.model.ItemStack;
import net.minecraft.network.play.client.C0EPacketClickWindow;

public class C0E extends CPacket {

    public int windowId;

    public int slot;

    public int button;

    public int mode;

    public short action;

    public ItemStack itemStack;

    public C0E(int windowId, int slot, int button, int mode, ItemStack itemStack) {
        super(null);
        this.windowId = windowId;
        this.slot = slot;
        this.button = button;
        this.mode = mode;
        this.itemStack = itemStack;
    }

    public C0E(C0EPacketClickWindow packet) {
        super(packet);
        this.windowId = packet.func_149548_c();
        this.slot = packet.func_149544_d();
        this.button = packet.func_149543_e();
        this.mode = packet.func_149542_h();
        this.action = packet.func_149547_f();
        this.itemStack = new ItemStack(packet.func_149546_g(), (byte) 0);
    }

    public C0EPacketClickWindow convert() {
        return new C0EPacketClickWindow(this.windowId, this.slot, this.button, this.mode, this.itemStack.itemStack, this.action);
    }
}
