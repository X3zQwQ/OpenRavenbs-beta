package keystrokesmod.script.packet.clientbound;

import keystrokesmod.script.model.Block;
import keystrokesmod.script.model.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;

public class S23 extends SPacket {

    public Vec3 position;

    public Block block;

    public S23(S23PacketBlockChange packet, byte f) {
        super(packet);
        this.position = Vec3.convert(packet.func_179827_b());
        this.block = new Block(packet.func_180728_a(), new BlockPos(this.position.x, this.position.y, this.position.z));
    }

    public S23(Vec3 position) {
        super(new S23PacketBlockChange(Minecraft.func_71410_x().field_71441_e, Vec3.getBlockPos(position)));
        this.position = position;
        this.block = new Block(position);
    }
}
