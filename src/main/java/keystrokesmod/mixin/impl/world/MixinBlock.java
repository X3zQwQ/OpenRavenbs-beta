package keystrokesmod.mixin.impl.world;

import java.util.List;
import keystrokesmod.event.CollisionEvent;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Block.class)
public abstract class MixinBlock {

    @Shadow
    public abstract AxisAlignedBB func_180640_a(World world, BlockPos blockPos, IBlockState iBlockState);

    @Overwrite
    public void func_180638_a(World worldIn, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        AxisAlignedBB axisalignedbb = this.func_180640_a(worldIn, pos, state);
        CollisionEvent event = new CollisionEvent(pos, (Block) (Object) this, axisalignedbb);
        MinecraftForge.EVENT_BUS.post(event);
        axisalignedbb = event.boundingBox;
        if (axisalignedbb != null && mask.func_72326_a(axisalignedbb)) {
            list.add(axisalignedbb);
        }
    }
}
