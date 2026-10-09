package keystrokesmod.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import keystrokesmod.module.setting.impl.PotionListSetting;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.StatCollector;

public final class PotionSearchIndex {

    private static final int MAX_SEARCH_RESULTS = 100;

    private static final int MAX_POTION_META = 16384;

    private static List<PotionSearchIndex.PotionEntry> allPotionEntries;

    private static Map<String, PotionSearchIndex.PotionEntry> entriesByKey;

    private static final Map<Integer, ItemStack> itemStacksByPotionId = new HashMap<>();

    private PotionSearchIndex() {
    }

    public static List<PotionSearchIndex.PotionEntry> search(String query, PotionListSetting setting) {
        ensurePotionList();
        String trimmedQuery = query == null ? "" : query.trim();
        String lowerQuery = trimmedQuery.toLowerCase(Locale.ROOT);
        List<PotionSearchIndex.ScoredPotionEntry> scoredEntries = new ArrayList<>();
        for (PotionSearchIndex.PotionEntry entry : allPotionEntries) {
            if (!setting.containsPotion(entry.key)) {
                int score = getScore(entry, lowerQuery);
                if (score > 0) {
                    scoredEntries.add(new PotionSearchIndex.ScoredPotionEntry(entry, score));
                }
            }
        }
        scoredEntries.sort(Comparator.<PotionSearchIndex.ScoredPotionEntry>comparingInt(scoredEntry -> -scoredEntry.score).thenComparing(scoredEntry -> scoredEntry.entry.displayName, String.CASE_INSENSITIVE_ORDER).thenComparing(scoredEntry -> scoredEntry.entry.key));
        List<PotionSearchIndex.PotionEntry> results = new ArrayList<>(Math.min(100, scoredEntries.size()));
        for (int i = 0; i < Math.min(100, scoredEntries.size()); i++) {
            results.add(scoredEntries.get(i).entry);
        }
        return results;
    }

    public static String getDisplayName(String potionKey) {
        ensurePotionList();
        PotionSearchIndex.PotionEntry entry = entriesByKey.get(potionKey);
        return entry != null ? entry.displayName : potionKey;
    }

    public static ItemStack getItemStack(String potionKey) {
        ensurePotionList();
        PotionSearchIndex.PotionEntry entry = entriesByKey.get(potionKey);
        if (entry == null) {
            return null;
        } else if (itemStacksByPotionId.containsKey(entry.potionId)) {
            ItemStack cachedStack = itemStacksByPotionId.get(entry.potionId);
            return cachedStack != null ? cachedStack.func_77946_l() : null;
        } else {
            ItemStack resolvedStack = resolvePotionItemStack(entry.potionId);
            itemStacksByPotionId.put(entry.potionId, resolvedStack);
            return resolvedStack != null ? resolvedStack.func_77946_l() : null;
        }
    }

    private static int getScore(PotionSearchIndex.PotionEntry entry, String lowerQuery) {
        if (lowerQuery.isEmpty()) {
            return 1;
        } else {
            String displayName = entry.displayName.toLowerCase(Locale.ROOT);
            String key = entry.key.toLowerCase(Locale.ROOT);
            String simpleKey = getSimpleKey(entry.key).toLowerCase(Locale.ROOT);
            if (displayName.equals(lowerQuery)) {
                return 1000;
            } else if (simpleKey.equals(lowerQuery)) {
                return 900;
            } else if (displayName.startsWith(lowerQuery)) {
                return 800;
            } else if (simpleKey.startsWith(lowerQuery)) {
                return 700;
            } else {
                for (String token : displayName.split("\\s+")) {
                    if (token.startsWith(lowerQuery)) {
                        return 600;
                    }
                }
                if (displayName.contains(lowerQuery)) {
                    return 500;
                } else {
                    return !simpleKey.contains(lowerQuery) && !key.contains(lowerQuery) ? 0 : 400;
                }
            }
        }
    }

    private static String getSimpleKey(String potionKey) {
        if (potionKey != null && !potionKey.isEmpty()) {
            int intValue = potionKey.lastIndexOf(46);
            return intValue >= 0 && intValue < potionKey.length() - 1 ? potionKey.substring(intValue + 1) : potionKey;
        } else {
            return "";
        }
    }

    private static void ensurePotionList() {
        if (allPotionEntries == null || entriesByKey == null) {
            Map<String, PotionSearchIndex.PotionEntry> keyedEntries = new LinkedHashMap<>();
            for (int potionId = 0; potionId < Potion.field_76425_a.length; potionId++) {
                Potion potion = Potion.field_76425_a[potionId];
                if (potion != null && potion.func_76393_a() != null && !potion.func_76393_a().isEmpty()) {
                    String potionKey = potion.func_76393_a();
                    if (!keyedEntries.containsKey(potionKey)) {
                        String displayName = StatCollector.func_74838_a(potionKey);
                        if (displayName == null || displayName.isEmpty() || displayName.equals(potionKey)) {
                            displayName = getSimpleKey(potionKey);
                        }
                        keyedEntries.put(potionKey, new PotionSearchIndex.PotionEntry(potionId, potionKey, displayName));
                    }
                }
            }
            ArrayList<PotionSearchIndex.PotionEntry> sortedEntries = new ArrayList<>(keyedEntries.values());
            sortedEntries.sort(Comparator.<PotionSearchIndex.PotionEntry, String>comparing(entry -> entry.displayName, String.CASE_INSENSITIVE_ORDER).thenComparing(entry -> entry.key));
            allPotionEntries = Collections.unmodifiableList(sortedEntries);
            entriesByKey = keyedEntries;
        }
    }

    private static ItemStack resolvePotionItemStack(int potionId) {
        if (!(Items.field_151068_bn instanceof ItemPotion)) {
            return null;
        } else {
            ItemPotion potionItem = Items.field_151068_bn;
            ItemStack bestStack = null;
            int bestScore = Integer.MIN_VALUE;
            for (int metadata = 0; metadata <= 16384; metadata++) {
                List<PotionEffect> effects = potionItem.func_77834_f(metadata);
                if (effects != null && !effects.isEmpty()) {
                    int score = 0;
                    boolean matchesPotion = false;
                    for (PotionEffect listedEffect : effects) {
                        if (listedEffect.func_76456_a() == potionId) {
                            matchesPotion = true;
                            score += 100;
                            if (listedEffect.func_76458_c() == 0) {
                                score += 10;
                            }
                        } else {
                            score -= 20;
                        }
                    }
                    if (matchesPotion) {
                        if (effects.size() == 1) {
                            score += 50;
                        }
                        if (!ItemPotion.func_77831_g(metadata)) {
                            score += 25;
                        }
                        if (score > bestScore) {
                            bestScore = score;
                            bestStack = new ItemStack(Items.field_151068_bn, 1, metadata);
                        }
                    }
                }
            }
            return bestStack != null ? bestStack : new ItemStack(Items.field_151068_bn);
        }
    }

    public static final class PotionEntry {

        public final int potionId;

        public final String key;

        public final String displayName;

        private PotionEntry(int potionId, String key, String displayName) {
            this.potionId = potionId;
            this.key = key;
            this.displayName = displayName != null ? displayName : "";
        }
    }

    private static final class ScoredPotionEntry {

        private final PotionSearchIndex.PotionEntry entry;

        private final int score;

        private ScoredPotionEntry(PotionSearchIndex.PotionEntry entry, int score) {
            this.entry = entry;
            this.score = score;
        }
    }
}
