package keystrokesmod.module.setting.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import keystrokesmod.module.setting.Setting;

public class StringListSetting extends Setting {

    private final String placeholder;

    private final int maxLength;

    private final List<String> entries = new ArrayList<>();

    public GroupSetting group;

    public StringListSetting(String name, String placeholder, int maxLength) {
        this(null, name, placeholder, maxLength);
    }

    public StringListSetting(GroupSetting group, String name, String placeholder, int maxLength) {
        super(name);
        this.group = group;
        this.placeholder = placeholder == null ? "" : placeholder;
        this.maxLength = Math.max(1, maxLength);
    }

    public String getPlaceholder() {
        return this.placeholder;
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    public List<String> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    public boolean addEntry(String entry) {
        if (entry != null && !entry.trim().isEmpty()) {
            String trimmed = entry.trim();
            for (String existing : this.entries) {
                if (existing.equalsIgnoreCase(trimmed)) {
                    return false;
                }
            }
            this.entries.add(trimmed);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeEntry(String entry) {
        return this.entries.removeIf(e -> e.equalsIgnoreCase(entry));
    }

    public void clearEntries() {
        this.entries.clear();
    }

    public JsonArray toJsonArray() {
        JsonArray array = new JsonArray();
        for (String entry : this.entries) {
            array.add(new JsonPrimitive(entry));
        }
        return array;
    }

    public void fromJsonArray(JsonArray array) {
        this.entries.clear();
        if (array != null) {
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    String value = element.getAsString();
                    if (value != null && !value.trim().isEmpty()) {
                        this.entries.add(value.trim());
                    }
                }
            }
        }
    }

    @Override
    public String getProfileKey() {
        return this.group == null ? this.getName() : this.group.getName() + "." + this.getName();
    }

    @Override
    public void loadProfile(JsonObject data) {
        if (data != null) {
            String key = this.getProfileKey();
            if (data.has(key) && data.get(key).isJsonArray()) {
                this.fromJsonArray(data.getAsJsonArray(key));
            }
        }
    }
}
