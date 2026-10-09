package keystrokesmod.module.impl.other;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.TextSetting;
import keystrokesmod.utility.TextUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class NameHider extends Module {

    private static final long PROFILE_CACHE_REFRESH_MS = 250L;

    private static final int MIN_ALIAS_NUMBER = 1;

    private static final String DEFAULT_FAKE_NAME = "You";

    private static final String DEFAULT_HIDE_ALL_PREFIX = "Player";

    public static String fakeName = "You";

    public static ButtonSetting hideAllNames;

    public static TextSetting fakeNameSetting;

    public static TextSetting hideAllPrefixSetting;

    private static final LinkedHashSet<String> detectedSelfNames = new LinkedHashSet<>();

    private static String formattedFakeName = "You";

    private static String formattedHideAllPrefix = "Player";

    private static String cachedSelfKey;

    private static boolean cachedHideAllState;

    private static List<String> cachedSelfNames = Collections.emptyList();

    private static Map<String, List<String>> cachedVisibleNamesByKey = Collections.emptyMap();

    private static List<Entry<String, String>> cachedOtherNameReplacements = Collections.emptyList();

    private static Map<String, Integer> cachedAliasNumbers = Collections.emptyMap();

    private static long lastProfileCacheRefresh;

    private static int transformVersion;

    private static int lastTransformVersion = -1;

    private static String lastInput;

    private static String lastOutput;

    public NameHider() {
        super("Name Hider", Module.category.other);
        this.liteModule = true;
        this.registerSetting(fakeNameSetting = new TextSetting("Fake name", fakeName, "Type a fake name...", 48) {

            @Override
            public void setText(String text) {
                super.setText(NameHider.normalizeFakeName(text));
                NameHider.applyFakeName(this.getText());
            }

            @Override
            public void loadProfile(JsonObject data) {
                if (data != null) {
                    String profileKey = this.getProfileKey();
                    if (data.has(profileKey) && data.get(profileKey).isJsonPrimitive()) {
                        this.setText(data.getAsJsonPrimitive(profileKey).getAsString());
                    }
                }
            }
        });
        this.registerSetting(hideAllNames = new ButtonSetting("Hide all names", false));
        this.registerSetting(hideAllPrefixSetting = new TextSetting("Hide-all prefix", "Player", "Type a prefix...", 24) {

            @Override
            public void setText(String text) {
                super.setText(NameHider.normalizeHideAllPrefix(text));
                NameHider.applyHideAllPrefix(this.getText());
            }

            @Override
            public void loadProfile(JsonObject data) {
                if (data != null) {
                    String profileKey = this.getProfileKey();
                    if (data.has(profileKey) && data.get(profileKey).isJsonPrimitive()) {
                        this.setText(data.getAsJsonPrimitive(profileKey).getAsString());
                    }
                }
            }
        });
        hideAllPrefixSetting.visible = false;
    }

    public static String getFakeName(String input) {
        if (!shouldProcessText(input)) {
            return input;
        } else {
            refreshCaches();
            if (lastTransformVersion == transformVersion && input.equals(lastInput)) {
                return lastOutput;
            } else {
                String replaced = replaceNames(input, cachedSelfNames, formattedFakeName);
                if (cachedHideAllState) {
                    for (Entry<String, String> entry : cachedOtherNameReplacements) {
                        replaced = TextUtils.replaceKeepingFormatting(replaced, entry.getKey(), entry.getValue());
                    }
                }
                lastInput = input;
                lastOutput = replaced;
                lastTransformVersion = transformVersion;
                return replaced;
            }
        }
    }

    public static IChatComponent getPlayerDisplayName(EntityPlayer player, IChatComponent original) {
        if (original != null && shouldProcessText(original.func_150254_d())) {
            refreshCaches();
            String originalText = original.func_150254_d();
            String replaced = getDisplayTextForPlayer(player, originalText);
            if (Objects.equals(originalText, replaced)) {
                return original;
            } else {
                ChatComponentText component = new ChatComponentText(replaced);
                component.func_150255_a(original.func_150256_b().func_150232_l());
                return component;
            }
        } else {
            return original;
        }
    }

    public static String getTabName(NetworkPlayerInfo playerInfo, String original) {
        if (!shouldProcessText(original)) {
            return original;
        } else {
            refreshCaches();
            String key = getIdentityKey(playerInfo);
            if (key == null) {
                return original;
            } else if (isSelfKey(key)) {
                return replaceNames(original, getTabVisibleNames(playerInfo, key), formattedFakeName);
            } else if (!cachedHideAllState) {
                return original;
            } else {
                String replacement = getFormattedAlias(key);
                return replacement == null ? original : replaceNames(original, getTabVisibleNames(playerInfo, key), replacement);
            }
        }
    }

    public static void setFakeName(String name) {
        String normalized = normalizeFakeName(name);
        if (fakeNameSetting != null && !normalized.equals(fakeNameSetting.getText())) {
            fakeNameSetting.setText(normalized);
        } else {
            applyFakeName(normalized);
        }
    }

    private static String getDisplayTextForPlayer(EntityPlayer player, String original) {
        if (player == null) {
            return original;
        } else {
            String key = getIdentityKey(player);
            if (isSelfPlayer(player, key)) {
                return replaceNames(original, getEntityVisibleNames(player, key), formattedFakeName);
            } else if (!cachedHideAllState) {
                return original;
            } else {
                String replacement = getFormattedAlias(key);
                return replacement == null ? original : replaceNames(original, getEntityVisibleNames(player, key), replacement);
            }
        }
    }

    private static void refreshCaches() {
        if (mc.field_71439_g != null) {
            boolean hideEveryone = hideAllNames != null && hideAllNames.isToggled();
            if (cachedHideAllState != hideEveryone) {
                cachedHideAllState = hideEveryone;
                clearHideAllCaches();
                markCacheDirty();
            }
            String selfKey = getSelfKey();
            if (!Objects.equals(cachedSelfKey, selfKey)) {
                cachedSelfKey = selfKey;
                markCacheDirty();
            }
            List<String> selfNames = buildSelfNames();
            if (!selfNames.equals(cachedSelfNames)) {
                cachedSelfNames = selfNames;
                markCacheDirty();
            }
            if (hideEveryone) {
                long now = System.currentTimeMillis();
                if (now - lastProfileCacheRefresh >= 250L) {
                    lastProfileCacheRefresh = now;
                    NameHider.HideAllCache cache = buildHideAllCache(selfNames);
                    if (!cache.visibleNamesByKey.equals(cachedVisibleNamesByKey) || !cache.aliasNumbers.equals(cachedAliasNumbers) || !cache.replacements.equals(cachedOtherNameReplacements)) {
                        cachedVisibleNamesByKey = cache.visibleNamesByKey;
                        cachedAliasNumbers = cache.aliasNumbers;
                        cachedOtherNameReplacements = cache.replacements;
                        markCacheDirty();
                    }
                }
            }
        }
    }

    private static NameHider.HideAllCache buildHideAllCache(List<String> selfNames) {
        LinkedHashMap<String, LinkedHashSet<String>> targets = new LinkedHashMap<>();
        HashSet<String> protectedNames = new HashSet<>();
        for (String selfName : selfNames) {
            protectedNames.add(normalizeName(selfName));
        }
        for (Entry<String, List<String>> entry : cachedVisibleNamesByKey.entrySet()) {
            for (String visibleName : entry.getValue()) {
                addTargetName(targets, protectedNames, entry.getKey(), visibleName);
            }
        }
        collectTargetsFromTab(targets, protectedNames);
        collectTargetsFromWorld(targets, protectedNames);
        if (targets.isEmpty()) {
            return NameHider.HideAllCache.EMPTY;
        } else {
            LinkedHashMap<String, List<String>> visibleNamesByKey = new LinkedHashMap<>();
            for (Entry<String, LinkedHashSet<String>> entry : targets.entrySet()) {
                visibleNamesByKey.put(entry.getKey(), sortNames(entry.getValue()));
            }
            LinkedHashMap<String, Integer> aliasNumbers = assignAliasNumbers(visibleNamesByKey.keySet());
            List<Entry<String, String>> replacements = new ArrayList<>();
            for (Entry<String, List<String>> entry : visibleNamesByKey.entrySet()) {
                Integer aliasNumber = aliasNumbers.get(entry.getKey());
                if (aliasNumber != null) {
                    String replacement = formattedHideAllPrefix + aliasNumber;
                    for (String visibleName : entry.getValue()) {
                        replacements.add(new SimpleEntry<>(visibleName, replacement));
                    }
                }
            }
            replacements.sort((first, second) -> {
                int lengthCompare = Integer.compare(second.getKey().length(), first.getKey().length());
                return lengthCompare != 0 ? lengthCompare : first.getKey().compareToIgnoreCase(second.getKey());
            });
            return new NameHider.HideAllCache(visibleNamesByKey, aliasNumbers, replacements);
        }
    }

    private static void collectTargetsFromTab(Map<String, LinkedHashSet<String>> targets, Set<String> protectedNames) {
        if (mc.func_147114_u() != null && mc.func_147114_u().func_175106_d() != null) {
            NetworkPlayerInfo self = getSelfPlayerInfo();
            for (NetworkPlayerInfo playerInfo : mc.func_147114_u().func_175106_d()) {
                if (playerInfo != null && playerInfo != self) {
                    addTargetName(targets, protectedNames, getIdentityKey(playerInfo), getProfileName(playerInfo));
                }
            }
        }
    }

    private static void collectTargetsFromWorld(Map<String, LinkedHashSet<String>> targets, Set<String> protectedNames) {
        if (mc.field_71441_e != null && mc.field_71441_e.field_73010_i != null) {
            for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                if (player != null && player != mc.field_71439_g) {
                    NetworkPlayerInfo playerInfo = getPlayerInfo(player);
                    String key = playerInfo != null ? getIdentityKey(playerInfo) : getIdentityKey(player.func_110124_au(), player.func_70005_c_());
                    addTargetName(targets, protectedNames, key, player.func_70005_c_());
                    if (playerInfo != null) {
                        addTargetName(targets, protectedNames, key, getProfileName(playerInfo));
                    }
                }
            }
        }
    }

    private static void addTargetName(Map<String, LinkedHashSet<String>> targets, Set<String> protectedNames, String key, String candidate) {
        String normalized = sanitizeName(candidate);
        if (key != null && !normalized.isEmpty() && !protectedNames.contains(normalizeName(normalized))) {
            LinkedHashSet<String> names = targets.computeIfAbsent(key, k -> new LinkedHashSet<>());
            names.add(normalized);
        }
    }

    private static LinkedHashMap<String, Integer> assignAliasNumbers(Set<String> keys) {
        LinkedHashMap<String, Integer> aliasNumbers = new LinkedHashMap<>();
        HashSet<Integer> usedAliasNumbers = new HashSet<>();
        for (String key : keys) {
            Integer existing = cachedAliasNumbers.get(key);
            if (existing != null && usedAliasNumbers.add(existing)) {
                aliasNumbers.put(key, existing);
            }
        }
        for (String text2 : keys) {
            if (!aliasNumbers.containsKey(text2)) {
                int aliasNumber = 1;
                while (usedAliasNumbers.contains(aliasNumber)) {
                    aliasNumber++;
                }
                aliasNumbers.put(text2, aliasNumber);
                usedAliasNumbers.add(aliasNumber);
            }
        }
        return aliasNumbers;
    }

    private static List<String> buildSelfNames() {
        rememberSelfName(mc.field_71439_g.func_70005_c_());
        NetworkPlayerInfo selfPlayerInfo = getSelfPlayerInfo();
        if (selfPlayerInfo != null) {
            rememberSelfName(getProfileName(selfPlayerInfo));
        }
        return sortNames(detectedSelfNames);
    }

    private static boolean rememberSelfName(String candidate) {
        String normalized = sanitizeName(candidate);
        return !normalized.isEmpty() && detectedSelfNames.add(normalized);
    }

    private static List<String> getEntityVisibleNames(EntityPlayer player, String key) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        addNames(names, cachedVisibleNamesByKey.get(key));
        addName(names, player.func_70005_c_());
        NetworkPlayerInfo playerInfo = getPlayerInfo(player);
        if (playerInfo != null) {
            addName(names, getProfileName(playerInfo));
        }
        if (isSelfPlayer(player, key)) {
            addNames(names, cachedSelfNames);
        }
        return sortNames(names);
    }

    private static List<String> getTabVisibleNames(NetworkPlayerInfo playerInfo, String key) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        addNames(names, cachedVisibleNamesByKey.get(key));
        addName(names, getProfileName(playerInfo));
        if (isSelfKey(key)) {
            addNames(names, cachedSelfNames);
        }
        return sortNames(names);
    }

    private static void addNames(Set<String> target, Iterable<String> names) {
        if (names != null) {
            for (String name : names) {
                addName(target, name);
            }
        }
    }

    private static void addName(Set<String> target, String name) {
        String normalized = sanitizeName(name);
        if (!normalized.isEmpty()) {
            target.add(normalized);
        }
    }

    private static List<String> sortNames(Iterable<String> names) {
        ArrayList<String> sorted = new ArrayList<>();
        if (names != null) {
            for (String name : names) {
                String normalized = sanitizeName(name);
                if (!normalized.isEmpty() && !sorted.contains(normalized)) {
                    sorted.add(normalized);
                }
            }
        }
        Collections.sort(sorted, (first, second) -> {
            int lengthCompare = Integer.compare(second.length(), first.length());
            return lengthCompare != 0 ? lengthCompare : first.compareToIgnoreCase(second);
        });
        return sorted;
    }

    private static String replaceNames(String input, Iterable<String> names, String replacement) {
        String replaced = TextUtils.replaceAllKeepingFormatting(input, names, replacement);
        return replaced == null ? input : replaced;
    }

    private static NetworkPlayerInfo getSelfPlayerInfo() {
        return mc.func_147114_u() != null && mc.field_71439_g != null ? mc.func_147114_u().func_175102_a(mc.field_71439_g.func_110124_au()) : null;
    }

    private static NetworkPlayerInfo getPlayerInfo(EntityPlayer player) {
        return mc.func_147114_u() != null && player != null ? mc.func_147114_u().func_175102_a(player.func_110124_au()) : null;
    }

    private static String getSelfKey() {
        NetworkPlayerInfo selfPlayerInfo = getSelfPlayerInfo();
        if (selfPlayerInfo != null) {
            return getIdentityKey(selfPlayerInfo);
        } else {
            return mc.field_71439_g == null ? null : getIdentityKey(mc.field_71439_g.func_110124_au(), mc.field_71439_g.func_70005_c_());
        }
    }

    private static boolean isSelfPlayer(EntityPlayer player, String key) {
        return player == mc.field_71439_g || isSelfKey(key);
    }

    private static boolean isSelfKey(String key) {
        return key != null && key.equals(cachedSelfKey);
    }

    private static String getFormattedAlias(String key) {
        Integer aliasNumber = cachedAliasNumbers.get(key);
        return aliasNumber == null ? null : formattedHideAllPrefix + aliasNumber;
    }

    private static String getProfileName(NetworkPlayerInfo playerInfo) {
        GameProfile profile = playerInfo == null ? null : playerInfo.func_178845_a();
        return profile == null ? "" : profile.getName();
    }

    private static String getIdentityKey(NetworkPlayerInfo playerInfo) {
        GameProfile profile = playerInfo == null ? null : playerInfo.func_178845_a();
        return profile == null ? null : getIdentityKey(profile.getId(), profile.getName());
    }

    private static String getIdentityKey(EntityPlayer player) {
        NetworkPlayerInfo playerInfo = getPlayerInfo(player);
        if (playerInfo != null) {
            return getIdentityKey(playerInfo);
        } else {
            return player == null ? null : getIdentityKey(player.func_110124_au(), player.func_70005_c_());
        }
    }

    private static String getIdentityKey(UUID uniqueId, String fallbackName) {
        if (uniqueId != null) {
            return uniqueId.toString();
        } else {
            String normalized = normalizeName(fallbackName);
            return normalized.isEmpty() ? null : normalized;
        }
    }

    private static String normalizeFakeName(String name) {
        String normalized = name == null ? "" : name.trim();
        return normalized.isEmpty() ? "You" : normalized;
    }

    private static String normalizeHideAllPrefix(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? "Player" : normalized;
    }

    private static String sanitizeName(String value) {
        return Utils.stripColor(value == null ? "" : value).trim();
    }

    private static String normalizeName(String value) {
        return sanitizeName(value).toLowerCase(Locale.ROOT);
    }

    private static boolean shouldProcessText(String input) {
        return input != null && !input.isEmpty() && mc.field_71439_g != null;
    }

    private static void applyFakeName(String name) {
        String normalized = normalizeFakeName(name);
        String nextFormattedFakeName = Utils.formatColor(normalized);
        if (!Objects.equals(fakeName, normalized) || !Objects.equals(formattedFakeName, nextFormattedFakeName)) {
            fakeName = normalized;
            formattedFakeName = nextFormattedFakeName;
            markCacheDirty();
        }
    }

    private static void applyHideAllPrefix(String value) {
        String normalized = normalizeHideAllPrefix(value);
        String text3 = Utils.formatColor(normalized);
        if (!Objects.equals(formattedHideAllPrefix, text3)) {
            formattedHideAllPrefix = text3;
            markCacheDirty();
        }
    }

    private static void clearHideAllCaches() {
        cachedVisibleNamesByKey = Collections.emptyMap();
        cachedOtherNameReplacements = Collections.emptyList();
        cachedAliasNumbers = Collections.emptyMap();
        lastProfileCacheRefresh = 0L;
    }

    private static void markCacheDirty() {
        transformVersion++;
        lastTransformVersion = -1;
        lastInput = null;
        lastOutput = null;
        lastProfileCacheRefresh = 0L;
    }

    @Override
    public void guiUpdate() {
        hideAllPrefixSetting.setVisible(hideAllNames.isToggled(), this);
    }

    private static final class HideAllCache {

        private static final NameHider.HideAllCache EMPTY = new NameHider.HideAllCache(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());

        private final Map<String, List<String>> visibleNamesByKey;

        private final Map<String, Integer> aliasNumbers;

        private final List<Entry<String, String>> replacements;

        private HideAllCache(Map<String, List<String>> visibleNamesByKey, Map<String, Integer> aliasNumbers, List<Entry<String, String>> replacements) {
            this.visibleNamesByKey = visibleNamesByKey;
            this.aliasNumbers = aliasNumbers;
            this.replacements = replacements;
        }
    }
}
