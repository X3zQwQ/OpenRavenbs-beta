package keystrokesmod.module.setting.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import keystrokesmod.module.setting.Setting;

public class BlockListSetting extends Setting {

    private final List<String> blocks = new ArrayList<>();

    private final String[] legacyProfileKeys;

    public GroupSetting group;

    public BlockListSetting(String name) {
        this(name, new String[0]);
    }

    public BlockListSetting(String name, String... legacyProfileKeys) {
        super(name);
        this.legacyProfileKeys = legacyProfileKeys != null ? legacyProfileKeys : new String[0];
    }

    public BlockListSetting(GroupSetting group, String name) {
        this(group, name, new String[0]);
    }

    public BlockListSetting(GroupSetting group, String name, String... legacyProfileKeys) {
        super(name);
        this.group = group;
        this.legacyProfileKeys = legacyProfileKeys != null ? legacyProfileKeys : new String[0];
    }

    public void addBlock(String registryName) {
        if (!this.blocks.contains(registryName)) {
            this.blocks.add(registryName);
        }
    }

    public void removeBlock(String registryName) {
        this.blocks.remove(registryName);
    }

    public List<String> getBlocks() {
        return this.blocks;
    }

    public boolean contains(String storageId) {
        if (this.blocks.contains(storageId)) {
            return true;
        } else {
            String registryId = extractRegistryId(storageId);
            return registryId != null && this.blocks.contains(registryId + ":*");
        }
    }

    @Override
    public String getProfileKey() {
        return this.group == null ? this.getName() : this.group.getName() + "." + this.getName();
    }

    private static String extractRegistryId(String storageId) {
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

    @Override
    public void loadProfile(JsonObject data) {
        String key = null;
        if (data.has(this.getProfileKey())) {
            key = this.getProfileKey();
        } else if (data.has(this.getName())) {
            key = this.getName();
        } else {
            for (String legacyProfileKey : this.legacyProfileKeys) {
                if (data.has(legacyProfileKey)) {
                    key = legacyProfileKey;
                    break;
                }
            }
        }
        if (key != null) {
            this.blocks.clear();
            JsonElement el = data.get(key);
            if (el.isJsonArray()) {
                for (JsonElement entry : el.getAsJsonArray()) {
                    this.blocks.add(entry.getAsString());
                }
            }
        }
    }

    public JsonArray toJsonArray() {
        JsonArray arr = new JsonArray();
        for (String block : this.blocks) {
            arr.add(new JsonPrimitive(block));
        }
        return arr;
    }

    protected String[] getLegacyProfileKeys() {
        return this.legacyProfileKeys;
    }
}
