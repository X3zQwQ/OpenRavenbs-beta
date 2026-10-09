package keystrokesmod.script.packet.serverbound;

import keystrokesmod.script.model.Entity;
import keystrokesmod.script.model.Vec3;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C02PacketUseEntity;

public class C02 extends CPacket {

    public Entity entity;

    public String action;

    public Vec3 hitVec;

    public C02(Entity entity, String action, Vec3 hitVec) {
        super(null);
        this.entity = entity;
        this.action = action;
        this.hitVec = hitVec;
    }

    public C02(C02PacketUseEntity packet) {
        super(packet);
        if (packet.func_149564_a(Minecraft.func_71410_x().field_71441_e) == null) {
            this.entity = null;
        } else {
            this.entity = Entity.convert(packet.func_149564_a(Minecraft.func_71410_x().field_71441_e));
        }
        this.action = packet.func_149565_c().name();
        if (packet.func_179712_b() != null) {
            this.hitVec = new Vec3(packet.func_179712_b().field_72450_a, packet.func_179712_b().field_72448_b, packet.func_179712_b().field_72449_c);
        }
    }

    public C02PacketUseEntity convert() {
        Action action = Utils.getEnum(Action.class, this.action);
        return this.hitVec != null && action == Action.INTERACT_AT ? new C02PacketUseEntity(this.entity.entity, new net.minecraft.util.Vec3(this.hitVec.x, this.hitVec.y, this.hitVec.z)) : new C02PacketUseEntity(this.entity.entity, action);
    }
}
