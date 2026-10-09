package keystrokesmod.script.packet.clientbound;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.S0BPacketAnimation;

public class S0B extends SPacket {

    public int entityId;

    public int type;

    public S0B(S0BPacketAnimation packet) {
        super(packet);
        this.entityId = packet.func_148978_c();
        this.type = packet.func_148977_d();
    }

    public S0B(int entityId, int type) {
        super(new S0BPacketAnimation(createEntityWithCustomID(entityId), type));
        this.entityId = entityId;
        this.type = type;
    }

    private static Entity createEntityWithCustomID(int id) {
        EntityOtherPlayerMP entity = new EntityOtherPlayerMP(Minecraft.func_71410_x().field_71441_e, new GameProfile(UUID.fromString("ae7a42d0-31b1-4926-ba4c-f28d6efad4a9"), ""));
        entity.func_145769_d(id);
        return entity;
    }
}
