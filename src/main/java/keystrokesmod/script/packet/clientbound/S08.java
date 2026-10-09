package keystrokesmod.script.packet.clientbound;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import keystrokesmod.script.model.Vec3;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.server.S08PacketPlayerPosLook.EnumFlags;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class S08 extends SPacket {

    public Vec3 position;

    public float yaw;

    public float pitch;

    public Set<String> enumFlags;

    public S08(S08PacketPlayerPosLook packet) {
        super(packet);
        this.position = new Vec3(packet.func_148932_c(), packet.func_148928_d(), packet.func_148933_e());
        this.yaw = packet.func_148931_f();
        this.pitch = packet.func_148930_g();
        Set<EnumFlags> flags = packet.func_179834_f();
        Set<String> flagsStr = new HashSet<>(flags.size());
        for (EnumFlags flag : flags) {
            flagsStr.add(flag.name());
        }
        this.enumFlags = flagsStr;
    }

    public S08(Vec3 position, float yaw, float pitch, Set<String> enumFlags) {
        super(create(position, yaw, pitch, enumFlags));
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.enumFlags = enumFlags;
    }

    private static S08PacketPlayerPosLook create(Vec3 position, float yaw, float pitch, Set<String> enumFlags) {
        Set<EnumFlags> enumSet = EnumSet.noneOf(EnumFlags.class);
        for (String flag : enumFlags) {
            EnumFlags enumFlag = Utils.getEnum(EnumFlags.class, flag);
            if (enumFlag != null) {
                enumSet.add(enumFlag);
            }
        }
        return new S08PacketPlayerPosLook(position.x, position.y, position.z, yaw, pitch, enumSet);
    }
}
