package keystrokesmod.script.packet.clientbound;

import keystrokesmod.script.model.ItemStack;
import net.minecraft.network.play.server.S04PacketEntityEquipment;

public class S04 extends SPacket {

    public int entityId;

    public int slot;

    public ItemStack item;

    public S04(S04PacketEntityEquipment e) {
        super(e);
        this.entityId = e.func_149389_d();
        this.slot = e.func_149388_e();
        this.item = ItemStack.convert(e.func_149390_c());
    }

    public S04(int entityId, int slot, ItemStack item) {
        super(new S04PacketEntityEquipment(entityId, slot, item.itemStack));
        this.entityId = entityId;
        this.slot = slot;
        this.item = item;
    }
}
