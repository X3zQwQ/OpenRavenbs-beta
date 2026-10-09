package keystrokesmod.event;

import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Event;

public class CollisionEvent extends Event {

    public final BlockPos blockPos;

    public final Block block;

    public AxisAlignedBB boundingBox;

    public CollisionEvent(BlockPos position, Block block, AxisAlignedBB axisAlignedBB) {
        this.blockPos = position;
        this.block = block;
        this.boundingBox = axisAlignedBB;
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public Block getBlock() {
        return this.block;
    }

    public AxisAlignedBB getBoundingBox() {
        return this.boundingBox;
    }

    public void setBoundingBox(AxisAlignedBB boundingBox2) {
        this.boundingBox = boundingBox2;
    }
}
