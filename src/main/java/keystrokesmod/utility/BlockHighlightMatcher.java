package keystrokesmod.utility;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

public interface BlockHighlightMatcher {

    boolean matchesBlock(IBlockState iBlockState);

    default boolean shouldIndexAt(BlockPos pos, IBlockState state) {
        return this.matchesBlock(state);
    }

    default boolean isActive() {
        return true;
    }

    default void beginScanPass() {
    }
}
