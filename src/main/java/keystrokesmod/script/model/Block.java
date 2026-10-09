package keystrokesmod.script.model;

import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

public class Block {

    public String type;

    public String name;

    public boolean interactable;

    public int variant;

    public double height;

    public double width;

    public double length;

    public double x;

    public double y;

    public double z;

    public Block(net.minecraft.block.Block block, BlockPos blockPos) {
        this.type = block.getClass().getSimpleName();
        this.name = block.getRegistryName().replace("minecraft:", "");
        this.interactable = BlockUtils.isInteractable(block);
        this.variant = block.func_176201_c(BlockUtils.getBlockState(blockPos));
        this.height = block.func_149669_A() - block.func_149665_z();
        this.width = block.func_149753_y() - block.func_149704_x();
        this.length = block.func_149693_C() - block.func_149706_B();
        this.x = blockPos.func_177958_n();
        this.y = blockPos.func_177956_o();
        this.z = blockPos.func_177952_p();
    }

    public Block(IBlockState state, BlockPos blockPos) {
        net.minecraft.block.Block block = state.func_177230_c();
        this.type = block.getClass().getSimpleName();
        this.name = block.getRegistryName().replace("minecraft:", "");
        this.interactable = BlockUtils.isInteractable(block);
        this.variant = block.func_176201_c(state);
        this.height = block.func_149669_A() - block.func_149665_z();
        this.width = block.func_149753_y() - block.func_149704_x();
        this.length = block.func_149693_C() - block.func_149706_B();
        this.x = blockPos.func_177958_n();
        this.y = blockPos.func_177956_o();
        this.z = blockPos.func_177952_p();
    }

    public Block(int x, int y, int z) {
        this(BlockUtils.getBlock(x, y, z), new BlockPos(x, y, z));
    }

    protected Block(String name, Vec3 position) {
        this(Utils.getBlockFromName(name), Vec3.getBlockPos(position));
    }

    public Block(String name) {
        this(name, new Vec3(-1.0, -1.0, -1.0));
    }

    public Block(Vec3 position) {
        this(BlockUtils.getBlock(position.x, position.y, position.z), new BlockPos(position.x, position.y, position.z));
    }
}
