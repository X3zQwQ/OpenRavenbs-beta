package keystrokesmod.utility;

import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

public final class BedFootHighlightMatcher implements BlockHighlightMatcher {

    @Override
    public boolean matchesBlock(IBlockState state) {
        return state != null && state.func_177230_c() instanceof BlockBed;
    }

    @Override
    public boolean shouldIndexAt(BlockPos pos, IBlockState state) {
        return !this.matchesBlock(state) ? false : state.func_177229_b(BlockBed.field_176472_a) == EnumPartType.FOOT;
    }
}
