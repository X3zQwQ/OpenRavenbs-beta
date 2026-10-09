package keystrokesmod.script.packet.clientbound;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.script.model.Vec3;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;

public class S27 extends SPacket {

    public float strength;

    public Vec3 position;

    public Vec3 motion;

    public List<Vec3> affectedBlockPositions;

    public S27(S27PacketExplosion packet) {
        super(packet);
        this.strength = packet.func_149146_i();
        this.position = new Vec3(packet.func_149148_f(), packet.func_149143_g(), packet.func_149145_h());
        this.motion = new Vec3(packet.func_149149_c(), packet.func_149144_d(), packet.func_149147_e());
        List<Vec3> affectedBlockPositions = new ArrayList<>();
        for (BlockPos blockPos : packet.func_149150_j()) {
            affectedBlockPositions.add(Vec3.convert(blockPos));
        }
        this.affectedBlockPositions = affectedBlockPositions;
    }

    public S27(float strength, Vec3 position, Vec3 motion, List<Vec3> affectedBlockPositions) {
        super(create(strength, position, motion, affectedBlockPositions));
        this.strength = strength;
        this.position = position;
        this.motion = motion;
        this.affectedBlockPositions = affectedBlockPositions;
    }

    private static S27PacketExplosion create(float strength, Vec3 position, Vec3 motion, List<Vec3> affectedBlockPositions) {
        List<BlockPos> list = new ArrayList<>();
        for (Vec3 vec3 : affectedBlockPositions) {
            list.add(Vec3.getBlockPos(vec3));
        }
        return new S27PacketExplosion(position.x, position.y, position.z, strength, list, Vec3.getVec3(motion));
    }
}
