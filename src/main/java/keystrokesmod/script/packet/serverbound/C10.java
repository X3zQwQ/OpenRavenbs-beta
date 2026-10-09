package keystrokesmod.script.packet.serverbound;

import keystrokesmod.script.model.ItemStack;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;

public class C10 extends CPacket {

    public int slot;

    public ItemStack itemStack;

    public C10(int slot, ItemStack itemStack) {
        super(null);
        this.slot = slot;
        this.itemStack = itemStack;
    }

    public C10(C10PacketCreativeInventoryAction packet) {
        super(packet);
        this.slot = packet.func_149627_c();
        this.itemStack = ItemStack.convert(packet.func_149625_d());
    }

    public C10PacketCreativeInventoryAction convert() {
        return new C10PacketCreativeInventoryAction(this.slot, this.itemStack.itemStack);
    }
}
