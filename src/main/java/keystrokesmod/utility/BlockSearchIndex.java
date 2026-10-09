package keystrokesmod.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import keystrokesmod.module.setting.impl.BlockListSetting;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class BlockSearchIndex {

    private static final int MAX_SEARCH_RESULTS = 100;

    private static List<BlockSearchIndex.BlockEntry> allBlockEntries = null;

    private static Map<String, List<BlockSearchIndex.BlockEntry>> variantsByRegistryId = null;

    private static final Map<Block, Item> BLOCK_TO_ITEM_FALLBACK = new HashMap<>();

    public static List<BlockSearchIndex.BlockEntry> search(String query, BlockListSetting setting) {
        ensureBlockList();
        if (query != null && !(query = query.trim()).isEmpty()) {
            String lq = query.toLowerCase();
            List<BlockSearchIndex.ScoredBlock> scored = new ArrayList<>();
            for (BlockSearchIndex.BlockEntry entry : allBlockEntries) {
                String locLower = entry.displayName.toLowerCase();
                String reg = entry.storageId.toLowerCase();
                String regPath = reg.indexOf(58) >= 0 ? reg.substring(reg.indexOf(58) + 1) : reg;
                int score = 0;
                if (locLower.equals(lq)) {
                    score = 1000;
                } else if (regPath.equals(lq)) {
                    score = 900;
                } else if (locLower.startsWith(lq)) {
                    score = 800;
                } else if (regPath.startsWith(lq)) {
                    score = 700;
                } else {
                    String[] tokens = locLower.split("\\s+");
                    for (String t : tokens) {
                        if (t.startsWith(lq)) {
                            score = 600;
                            break;
                        }
                    }
                    if (score == 0 && locLower.contains(lq)) {
                        score = 500;
                    } else if (score == 0 && regPath.contains(lq)) {
                        score = 400;
                    }
                }
                if (score > 0 && !setting.contains(entry.storageId)) {
                    scored.add(new BlockSearchIndex.ScoredBlock(entry, score));
                }
            }
            scored.sort(Comparator.<BlockSearchIndex.ScoredBlock>comparingInt(s -> -s.score).thenComparing(s -> s.entry.displayName, String.CASE_INSENSITIVE_ORDER).thenComparing(s -> s.entry.storageId));
            List<BlockSearchIndex.BlockEntry> results = new ArrayList<>(Math.min(100, scored.size()));
            for (int i = 0; i < Math.min(100, scored.size()); i++) {
                results.add(scored.get(i).entry);
            }
            return results;
        } else {
            return Collections.emptyList();
        }
    }

    public static List<BlockSearchIndex.GroupedBlockResult> searchGrouped(String query, BlockListSetting setting) {
        ensureBlockList();
        if (query != null && !(query = query.trim()).isEmpty()) {
            String lq = query.toLowerCase();
            List<BlockSearchIndex.ScoredBlock> scored = new ArrayList<>();
            for (BlockSearchIndex.BlockEntry entry : allBlockEntries) {
                if (!setting.contains(entry.storageId)) {
                    String wildcard = getRegistryId(entry.storageId);
                    if (wildcard == null || !setting.contains(wildcard + ":*")) {
                        String locLower = entry.displayName.toLowerCase();
                        String reg = entry.storageId.toLowerCase();
                        String regPath = reg.indexOf(58) >= 0 ? reg.substring(reg.indexOf(58) + 1) : reg;
                        int score = 0;
                        if (locLower.equals(lq)) {
                            score = 1000;
                        } else if (regPath.equals(lq)) {
                            score = 900;
                        } else if (locLower.startsWith(lq)) {
                            score = 800;
                        } else if (regPath.startsWith(lq)) {
                            score = 700;
                        } else {
                            String[] tokens = locLower.split("\\s+");
                            for (String t : tokens) {
                                if (t.startsWith(lq)) {
                                    score = 600;
                                    break;
                                }
                            }
                            if (score == 0 && locLower.contains(lq)) {
                                score = 500;
                            } else if (score == 0 && regPath.contains(lq)) {
                                score = 400;
                            }
                        }
                        if (score > 0) {
                            scored.add(new BlockSearchIndex.ScoredBlock(entry, score));
                        }
                    }
                }
            }
            scored.sort(Comparator.<BlockSearchIndex.ScoredBlock>comparingInt(s -> -s.score).thenComparing(s -> s.entry.displayName, String.CASE_INSENSITIVE_ORDER).thenComparing(s -> s.entry.storageId));
            Map<String, Integer> bestScoreByRegistry = new HashMap<>();
            for (BlockSearchIndex.ScoredBlock sb : scored) {
                String rid = getRegistryId(sb.entry.storageId);
                if (rid != null && (!bestScoreByRegistry.containsKey(rid) || bestScoreByRegistry.get(rid) < sb.score)) {
                    bestScoreByRegistry.put(rid, sb.score);
                }
            }
            List<BlockSearchIndex.GroupedBlockResult> results = new ArrayList<>();
            List<String> seen = new ArrayList<>();
            for (BlockSearchIndex.ScoredBlock scoredBlock : scored) {
                String rid = getRegistryId(scoredBlock.entry.storageId);
                if (rid != null && !seen.contains(rid)) {
                    seen.add(rid);
                    List<BlockSearchIndex.BlockEntry> variants = getVariants(rid);
                    if (!variants.isEmpty()) {
                        int groupScore = bestScoreByRegistry.containsKey(rid) ? bestScoreByRegistry.get(rid) : scoredBlock.score;
                        results.add(new BlockSearchIndex.GroupedBlockResult(rid, variants, groupScore));
                        if (results.size() >= 100) {
                            break;
                        }
                    }
                }
            }
            results.sort(Comparator.<BlockSearchIndex.GroupedBlockResult>comparingInt(g -> -g.score).thenComparing(g -> g.variants.get(0).displayName, String.CASE_INSENSITIVE_ORDER));
            return results;
        } else {
            return Collections.emptyList();
        }
    }

    public static List<BlockSearchIndex.BlockEntry> getVariants(String registryId) {
        ensureBlockList();
        if (variantsByRegistryId == null) {
            return Collections.emptyList();
        } else {
            List<BlockSearchIndex.BlockEntry> list = variantsByRegistryId.get(registryId);
            return list != null ? list : Collections.emptyList();
        }
    }

    public static boolean isWildcard(String storageId) {
        return storageId != null && storageId.endsWith(":*");
    }

    public static String getRegistryId(String storageId) {
        if (storageId == null || storageId.isEmpty()) {
            return null;
        } else if (storageId.endsWith(":*")) {
            return storageId.substring(0, storageId.length() - 2);
        } else {
            String[] p = storageId.split(":");
            if (p.length >= 3) {
                return p[0] + ":" + p[1];
            } else {
                return p.length == 2 ? storageId : null;
            }
        }
    }

    public static ItemStack getItemStack(String storageId) {
        String id = isWildcard(storageId) ? getRegistryId(storageId) : storageId;
        Block block = getBlockForName(id);
        if (block == null) {
            return null;
        } else {
            int meta = isWildcard(storageId) ? 0 : getMetaFromStorageId(storageId);
            Item fallback = BLOCK_TO_ITEM_FALLBACK.get(block);
            return fallback != null ? new ItemStack(fallback, 1, meta) : new ItemStack(block, 1, meta);
        }
    }

    public static String getDisplayName(String storageId) {
        if (isWildcard(storageId)) {
            ItemStack stack = getItemStack(getRegistryId(storageId));
            return stack != null ? stack.func_82833_r() + " (All)" : storageId;
        } else {
            ItemStack stack = getItemStack(storageId);
            return stack != null ? stack.func_82833_r() : storageId;
        }
    }

    private static void ensureBlockList() {
        if (allBlockEntries == null) {
            allBlockEntries = new ArrayList<>();
            Map<String, List<BlockSearchIndex.BlockEntry>> variantMap = new HashMap<>();
            for (Object obj : Block.field_149771_c) {
                Block block = (Block) obj;
                String registryId = Block.field_149771_c.func_177774_c(block) != null ? ((ResourceLocation) Block.field_149771_c.func_177774_c(block)).toString() : null;
                if (registryId != null) {
                    Item item = Item.func_150898_a(block);
                    if (item == null) {
                        Item fallback = BLOCK_TO_ITEM_FALLBACK.get(block);
                        if (fallback != null) {
                            ItemStack displayStack = new ItemStack(fallback, 1, 0);
                            String displayName = displayStack.func_82833_r();
                            if (displayName != null && !displayName.isEmpty()) {
                                BlockSearchIndex.BlockEntry entry = new BlockSearchIndex.BlockEntry(block, 0, displayName, registryId, fallback);
                                allBlockEntries.add(entry);
                                variantMap.put(registryId, Collections.singletonList(entry));
                            }
                        }
                    } else {
                        List<ItemStack> sub = new ArrayList<>();
                        block.func_149666_a(item, CreativeTabs.field_78030_b, sub);
                        if (sub.isEmpty()) {
                            sub.add(new ItemStack(block, 1, 0));
                        }
                        List<BlockSearchIndex.BlockEntry> group = new ArrayList<>();
                        for (ItemStack stack : sub) {
                            String displayName = stack.func_82833_r();
                            if (displayName != null && !displayName.isEmpty()) {
                                int meta = stack.func_77960_j();
                                String storageId = meta != 0 ? registryId + ":" + meta : registryId;
                                BlockSearchIndex.BlockEntry entry = new BlockSearchIndex.BlockEntry(block, meta, displayName, storageId);
                                allBlockEntries.add(entry);
                                group.add(entry);
                            }
                        }
                        if (!group.isEmpty()) {
                            group.sort(Comparator.comparing(e -> e.meta));
                            variantMap.put(registryId, group);
                        }
                    }
                }
            }
            allBlockEntries.sort(Comparator.comparing(e -> e.displayName, String.CASE_INSENSITIVE_ORDER));
            variantsByRegistryId = variantMap;
        }
    }

    private static String parseRegistryId(String name) {
        if (name != null && !name.isEmpty()) {
            String[] p = name.split(":");
            if (p.length >= 3) {
                return p[0] + ":" + p[1];
            } else {
                return p.length == 2 ? name : null;
            }
        } else {
            return null;
        }
    }

    private static int getMetaFromStorageId(String name) {
        if (name == null) {
            return 0;
        } else {
            String[] p = name.split(":");
            if (p.length >= 3) {
                try {
                    return Integer.parseInt(p[2]);
                } catch (NumberFormatException exception) {
                    return 0;
                }
            } else {
                return 0;
            }
        }
    }

    private static Block getBlockForName(String name) {
        String registryId = parseRegistryId(name);
        if (registryId == null) {
            return null;
        } else {
            try {
                return (Block) Block.field_149771_c.func_82594_a(new ResourceLocation(registryId));
            } catch (Exception exception2) {
                return null;
            }
        }
    }

    static {
        BLOCK_TO_ITEM_FALLBACK.put(Blocks.field_150324_C, Items.field_151104_aV);
    }

    public static final class BlockEntry {

        public final Block block;

        public final int meta;

        public final String displayName;

        public final String storageId;

        private final Item displayItem;

        BlockEntry(Block block, int meta, String displayName, String storageId) {
            this(block, meta, displayName, storageId, null);
        }

        BlockEntry(Block block, int meta, String displayName, String storageId, Item displayItem) {
            this.block = block;
            this.meta = meta;
            this.displayName = displayName != null ? displayName : "";
            this.storageId = storageId;
            this.displayItem = displayItem;
        }

        public ItemStack toItemStack() {
            return this.displayItem != null ? new ItemStack(this.displayItem, 1, this.meta) : new ItemStack(this.block, 1, this.meta);
        }
    }

    public static final class GroupedBlockResult {

        public final String registryId;

        public final List<BlockSearchIndex.BlockEntry> variants;

        public final int score;

        public GroupedBlockResult(String registryId, List<BlockSearchIndex.BlockEntry> variants, int score) {
            this.registryId = registryId;
            this.variants = variants;
            this.score = score;
        }

        public boolean isSingleVariant() {
            return this.variants.size() <= 1;
        }

        public String getGroupLabel() {
            return this.variants.get(0).displayName + " (" + this.variants.size() + ")";
        }

        public ItemStack getCyclingIcon() {
            int intValue = (int) (System.currentTimeMillis() / 1000L % this.variants.size());
            return this.variants.get(intValue).toItemStack();
        }
    }

    private static final class ScoredBlock {

        final BlockSearchIndex.BlockEntry entry;

        final int score;

        ScoredBlock(BlockSearchIndex.BlockEntry entry, int score) {
            this.entry = entry;
            this.score = score;
        }
    }
}
