package keystrokesmod.utility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import keystrokesmod.module.setting.impl.BlockListSetting;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

public final class BlockListHighlightMatcher implements BlockHighlightMatcher {

    private final BlockListSetting setting;

    private Set<String> matcherIds;

    private Map<String, Object> matcherWildcards;

    private int lastListHash;

    public BlockListHighlightMatcher(BlockListSetting setting) {
        this.setting = setting;
        this.rebuildMatcher();
    }

    @Override
    public void beginScanPass() {
        int h = this.setting.getBlocks().hashCode();
        if (h != this.lastListHash) {
            this.rebuildMatcher();
        }
    }

    @Override
    public boolean isActive() {
        return !this.setting.getBlocks().isEmpty();
    }

    @Override
    public boolean matchesBlock(IBlockState state) {
        if (state == null) {
            return false;
        } else {
            Block block = state.func_177230_c();
            if (block == null) {
                return false;
            } else {
                Object nameObj = Block.field_149771_c.func_177774_c(block);
                if (nameObj == null) {
                    return false;
                } else {
                    String registryId = nameObj.toString();
                    if (this.matcherWildcards.containsKey(registryId)) {
                        return true;
                    } else {
                        int meta = block.func_176201_c(state);
                        return meta != 0 && this.matcherIds.contains(registryId + ":" + meta) ? true : this.matcherIds.contains(registryId);
                    }
                }
            }
        }
    }

    @Override
    public boolean shouldIndexAt(BlockPos pos, IBlockState state) {
        return this.matchesBlock(state);
    }

    private void rebuildMatcher() {
        List<String> blocks = this.setting.getBlocks();
        this.lastListHash = blocks.hashCode();
        this.matcherIds = new HashSet<>();
        this.matcherWildcards = new HashMap<>();
        for (String id : blocks) {
            if (id.endsWith(":*")) {
                String base = id.substring(0, id.length() - 2);
                this.matcherWildcards.put(base, null);
            } else {
                this.matcherIds.add(id);
            }
        }
    }
}
