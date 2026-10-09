package keystrokesmod.script.packet.clientbound;

import keystrokesmod.script.model.Vec3;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.EnumParticleTypes;

public class S2A extends SPacket {

    public String type;

    public Vec3 position;

    public Vec3 offset;

    public float speed;

    public int count;

    public boolean longDistance;

    public int[] args;

    public S2A(S2APacketParticles packet) {
        super(packet);
        this.type = packet.func_179749_a().name();
        this.longDistance = packet.func_179750_b();
        this.position = new Vec3(packet.func_149220_d(), packet.func_149226_e(), packet.func_149225_f());
        this.offset = new Vec3(packet.func_149221_g(), packet.func_149224_h(), packet.func_149223_i());
        this.speed = packet.func_149227_j();
        this.count = packet.func_149222_k();
        this.args = packet.func_179748_k();
    }

    public S2A(String type, boolean longDistance, Vec3 position, Vec3 offset, float speed, int count, int[] args) {
        super(new S2APacketParticles(Utils.getEnum(EnumParticleTypes.class, type), longDistance, (float) position.x, (float) position.y, (float) position.z, (float) offset.x, (float) offset.y, (float) offset.z, speed, count, args));
        this.type = type;
        this.longDistance = longDistance;
        this.position = position;
        this.offset = offset;
        this.speed = speed;
        this.count = count;
        this.args = args;
    }
}
