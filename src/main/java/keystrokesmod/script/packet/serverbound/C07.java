package keystrokesmod.script.packet.serverbound;

import keystrokesmod.script.model.Vec3;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class C07 extends CPacket {

    public Vec3 position;

    public String status;

    public String facing;

    public C07(Vec3 position, String status, String facing) {
        super(null);
        this.position = position;
        this.status = status;
        this.facing = facing;
    }

    public C07(C07PacketPlayerDigging packet) {
        super(packet);
        this.position = Vec3.convert(packet.func_179715_a());
        this.status = packet.func_180762_c().name();
        this.facing = packet.func_179714_b().name();
    }

    public C07PacketPlayerDigging convert() {
        return new C07PacketPlayerDigging(Utils.getEnum(Action.class, this.status), new BlockPos(this.position.x, this.position.y, this.position.z), Utils.getEnum(EnumFacing.class, this.facing));
    }
}
