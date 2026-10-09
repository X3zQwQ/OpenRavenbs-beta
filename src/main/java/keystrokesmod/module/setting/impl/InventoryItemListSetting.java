package keystrokesmod.module.setting.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryItemListSetting extends ItemListSetting {

    private static final int DEFAULT_ASSIGNED_SLOT = 1;

    private final Map<String, Integer> assignedSlots = new HashMap<>();

    public InventoryItemListSetting(String name) {
        super(name);
    }

    public InventoryItemListSetting(String name, String... legacyProfileKeys) {
        super(name, legacyProfileKeys);
    }

    public InventoryItemListSetting(GroupSetting group, String name) {
        super(group, name);
    }

    public InventoryItemListSetting(GroupSetting group, String name, String... legacyProfileKeys) {
        super(group, name, legacyProfileKeys);
    }

    @Override
    public void addItem(String storageId) {
        if (storageId != null && !storageId.isEmpty() && !this.containsItem(storageId)) {
            super.addItem(storageId);
            this.assignedSlots.put(storageId, 1);
        }
    }

    @Override
    public void removeItem(String storageId) {
        super.removeItem(storageId);
        this.assignedSlots.remove(storageId);
    }

    public Integer getAssignedSlot(String storageId) {
        if (storageId != null && this.getItems().contains(storageId)) {
            Integer slot = this.assignedSlots.get(storageId);
            return slot != null ? slot : 1;
        } else {
            return null;
        }
    }

    public void setAssignedSlot(String storageId, Integer slot) {
        if (storageId != null && this.getItems().contains(storageId)) {
            this.assignedSlots.put(storageId, slot != null && slot >= 1 && slot <= 9 ? slot : 1);
        }
    }

    public void moveItem(String storageId, int intValue) {
        List<String> items = this.getItems();
        int intValue2 = items.indexOf(storageId);
        if (intValue2 >= 0) {
            int intValue3 = Math.max(0, Math.min(intValue, items.size() - 1));
            if (intValue2 != intValue3) {
                items.remove(intValue2);
                if (intValue3 > items.size()) {
                    intValue3 = items.size();
                }
                items.add(intValue3, storageId);
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
            for (String legacyProfileKey : this.getLegacyProfileKeys()) {
                if (data.has(legacyProfileKey)) {
                    key = legacyProfileKey;
                    break;
                }
            }
        }
        if (key != null) {
            this.getItems().clear();
            this.assignedSlots.clear();
            JsonElement element = data.get(key);
            if (element.isJsonArray()) {
                for (JsonElement entry : element.getAsJsonArray()) {
                    if (entry != null && !entry.isJsonNull()) {
                        if (entry.isJsonPrimitive()) {
                            String storageId = entry.getAsString();
                            if (storageId != null && !storageId.isEmpty() && !this.containsItem(storageId)) {
                                super.addItem(storageId);
                                this.assignedSlots.put(storageId, 1);
                            }
                        } else if (entry.isJsonObject()) {
                            JsonObject object = entry.getAsJsonObject();
                            if (object.has("id")) {
                                String storageId = object.get("id").getAsString();
                                if (storageId != null && !storageId.isEmpty() && !this.containsItem(storageId)) {
                                    super.addItem(storageId);
                                    int slot = 1;
                                    if (object.has("slot") && object.get("slot").isJsonPrimitive()) {
                                        int configuredSlot = object.get("slot").getAsInt();
                                        if (configuredSlot >= 1 && configuredSlot <= 9) {
                                            slot = configuredSlot;
                                        }
                                    }
                                    this.assignedSlots.put(storageId, slot);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public JsonArray toJsonArray() {
        JsonArray array = new JsonArray();
        for (String storageId : this.getItems()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", storageId);
            object.add("slot", new JsonPrimitive(this.getAssignedSlot(storageId)));
            array.add(object);
        }
        return array;
    }
}
