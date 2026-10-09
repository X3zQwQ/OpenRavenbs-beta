package keystrokesmod.script.packet.clientbound;

import keystrokesmod.script.model.Vec3;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class S12 extends SPacket {

    public int entityId;

    public Vec3 motion;

    public S12(S12PacketEntityVelocity packet) {
        super(packet);
        this.entityId = packet.func_149412_c();
        this.motion = new Vec3(packet.func_149411_d(), packet.func_149410_e(), packet.func_149409_f());
    }

    public S12(int entityId, Vec3 motion) {
        super(new S12PacketEntityVelocity(entityId, motion.x, motion.y, motion.z));
        this.entityId = entityId;
        this.motion = motion;
    }
}
