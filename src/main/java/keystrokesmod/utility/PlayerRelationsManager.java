package keystrokesmod.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.clickgui.components.impl.ModuleComponent;
import keystrokesmod.clickgui.components.impl.PlayerListComponent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;

public class PlayerRelationsManager implements IMinecraftInstance {

    private final File file;

    private final LinkedHashMap<String, String> friends = new LinkedHashMap<>();

    private final LinkedHashMap<String, String> enemies = new LinkedHashMap<>();

    private boolean active = true;

    private boolean middleClickFriends;

    public PlayerRelationsManager() {
        File directory = new File(mc.field_71412_D, "keystrokes");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        this.file = new File(directory, "players.json");
    }

    public void load() {
        this.friends.clear();
        this.enemies.clear();
        this.active = true;
        this.middleClickFriends = false;
        if (!this.file.exists()) {
            this.syncUtilsViews();
        } else {
            try (FileReader reader = new FileReader(this.file)) {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                if (root == null) {
                    this.syncUtilsViews();
                    return;
                }
                if (root.has("middleClickFriends")) {
                    this.middleClickFriends = root.get("middleClickFriends").getAsBoolean();
                }
                if (root.has("active")) {
                    this.active = root.get("active").getAsBoolean();
                }
                this.loadEntries(root.get("friends"), this.friends);
                this.loadEntries(root.get("enemies"), this.enemies);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            this.syncUtilsViews();
        }
    }

    public boolean addFriend(String name) {
        return this.addRelation(PlayerRelationsManager.RelationType.FRIEND, name);
    }

    public boolean addEnemy(String name) {
        return this.addRelation(PlayerRelationsManager.RelationType.ENEMY, name);
    }

    public boolean removeFriend(String name) {
        return this.removeRelation(PlayerRelationsManager.RelationType.FRIEND, name);
    }

    public boolean removeEnemy(String name) {
        return this.removeRelation(PlayerRelationsManager.RelationType.ENEMY, name);
    }

    public void clearFriends() {
        this.clearRelation(PlayerRelationsManager.RelationType.FRIEND);
    }

    public void clearEnemies() {
        this.clearRelation(PlayerRelationsManager.RelationType.ENEMY);
    }

    public boolean addRelation(PlayerRelationsManager.RelationType relationType, String name) {
        String normalized = this.normalize(name);
        if (normalized.isEmpty()) {
            return false;
        } else {
            LinkedHashMap<String, String> primary = relationType == PlayerRelationsManager.RelationType.FRIEND ? this.friends : this.enemies;
            LinkedHashMap<String, String> opposite = relationType == PlayerRelationsManager.RelationType.FRIEND ? this.enemies : this.friends;
            String displayName = this.normalizeDisplayName(name, normalized);
            boolean added = !primary.containsKey(normalized);
            boolean removedOpposite = opposite.remove(normalized) != null;
            boolean displayChanged = !displayName.equals(primary.get(normalized));
            if (!added && !removedOpposite && !displayChanged) {
                return false;
            } else {
                primary.put(normalized, displayName);
                this.persistAndNotify();
                return added;
            }
        }
    }

    public boolean removeRelation(PlayerRelationsManager.RelationType relationType, String name) {
        String normalized = this.normalize(name);
        if (normalized.isEmpty()) {
            return false;
        } else {
            LinkedHashMap<String, String> map = relationType == PlayerRelationsManager.RelationType.FRIEND ? this.friends : this.enemies;
            if (map.remove(normalized) == null) {
                return false;
            } else {
                this.persistAndNotify();
                return true;
            }
        }
    }

    public void clearRelation(PlayerRelationsManager.RelationType relationType) {
        LinkedHashMap<String, String> map = relationType == PlayerRelationsManager.RelationType.FRIEND ? this.friends : this.enemies;
        if (!map.isEmpty()) {
            map.clear();
            this.persistAndNotify();
        }
    }

    public boolean isFriend(String name) {
        return this.active && this.contains(this.friends, name);
    }

    public boolean isEnemy(String name) {
        return this.active && this.contains(this.enemies, name);
    }

    public int getCount(PlayerRelationsManager.RelationType relationType) {
        return this.getMap(relationType).size();
    }

    public List<PlayerRelationsManager.PlayerEntry> getEntries(PlayerRelationsManager.RelationType relationType) {
        LinkedHashMap<String, String> map = this.getMap(relationType);
        List<PlayerRelationsManager.PlayerEntry> entries = new ArrayList<>(map.size());
        for (Entry<String, String> entry : map.entrySet()) {
            entries.add(new PlayerRelationsManager.PlayerEntry(entry.getKey(), entry.getValue()));
        }
        return entries;
    }

    public boolean isMiddleClickFriends() {
        return this.middleClickFriends;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            this.save();
            this.syncRelationshipsModuleState();
        }
    }

    public void setMiddleClickFriends(boolean middleClickFriends) {
        if (this.middleClickFriends != middleClickFriends) {
            this.middleClickFriends = middleClickFriends;
            this.save();
            this.syncRelationshipsModuleState();
        }
    }

    public void refreshDisplayName(String name) {
        String normalized = this.normalize(name);
        if (!normalized.isEmpty()) {
            boolean changed = this.refreshDisplayName(this.friends, normalized, name);
            changed = this.refreshDisplayName(this.enemies, normalized, name) || changed;
            if (changed) {
                this.persistAndNotify();
            }
        }
    }

    private boolean refreshDisplayName(LinkedHashMap<String, String> map, String normalized, String displayName) {
        if (!map.containsKey(normalized)) {
            return false;
        } else {
            String next = this.normalizeDisplayName(displayName, normalized);
            if (next.equals(map.get(normalized))) {
                return false;
            } else {
                map.put(normalized, next);
                return true;
            }
        }
    }

    private void persistAndNotify() {
        this.save();
        this.syncUtilsViews();
        this.syncRelationshipsModuleState();
        this.refreshRelationshipsModuleUi();
    }

    private void syncUtilsViews() {
        Utils.friends.clear();
        Utils.friends.addAll(this.friends.keySet());
        Utils.enemies.clear();
        Utils.enemies.addAll(this.enemies.keySet());
    }

    private void syncRelationshipsModuleState() {
        if (ModuleManager.relationships != null) {
            ModuleManager.relationships.middleClickFriends.setEnabled(this.middleClickFriends);
            if (this.active && !ModuleManager.relationships.isEnabled()) {
                ModuleManager.relationships.enable();
            } else if (!this.active && ModuleManager.relationships.isEnabled()) {
                ModuleManager.relationships.disable();
            }
        }
    }

    private void refreshRelationshipsModuleUi() {
        if (Raven.clickGui != null && ClickGui.categories != null && ModuleManager.relationships != null) {
            for (CategoryComponent categoryComponent : ClickGui.categories) {
                if (categoryComponent.category == Module.category.client) {
                    for (ModuleComponent moduleComponent : categoryComponent.modules) {
                        if (moduleComponent.mod == ModuleManager.relationships) {
                            for (Component component : moduleComponent.settings) {
                                if (component instanceof PlayerListComponent) {
                                    ((PlayerListComponent) component).onExternalDataChanged();
                                }
                            }
                            moduleComponent.updateSettingPositions();
                            return;
                        }
                    }
                }
            }
        }
    }

    private void save() {
        JsonObject root = new JsonObject();
        root.addProperty("active", this.active);
        root.addProperty("middleClickFriends", this.middleClickFriends);
        root.add("friends", this.toJsonArray(this.friends));
        root.add("enemies", this.toJsonArray(this.enemies));
        try (FileWriter writer = new FileWriter(this.file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(root, writer);
        } catch (Exception exception2) {
            exception2.printStackTrace();
        }
    }

    private void loadEntries(JsonElement element, LinkedHashMap<String, String> target) {
        if (element != null && element.isJsonArray()) {
            for (JsonElement entryElement : element.getAsJsonArray()) {
                if (entryElement != null && !entryElement.isJsonNull()) {
                    if (entryElement.isJsonPrimitive()) {
                        String raw = entryElement.getAsString();
                        String normalized = this.normalize(raw);
                        if (!normalized.isEmpty()) {
                            target.put(normalized, this.normalizeDisplayName(raw, normalized));
                        }
                    } else if (entryElement.isJsonObject()) {
                        JsonObject entryObject = entryElement.getAsJsonObject();
                        String key = entryObject.has("key") ? entryObject.get("key").getAsString() : "";
                        String displayName = entryObject.has("displayName") ? entryObject.get("displayName").getAsString() : key;
                        String normalized = this.normalize(key);
                        if (!normalized.isEmpty()) {
                            target.put(normalized, this.normalizeDisplayName(displayName, normalized));
                        }
                    }
                }
            }
        }
    }

    private JsonArray toJsonArray(LinkedHashMap<String, String> map) {
        JsonArray array = new JsonArray();
        for (Entry<String, String> entry : map.entrySet()) {
            JsonObject object = new JsonObject();
            object.addProperty("key", entry.getKey());
            object.addProperty("displayName", entry.getValue());
            array.add(object);
        }
        return array;
    }

    private LinkedHashMap<String, String> getMap(PlayerRelationsManager.RelationType relationType) {
        return relationType == PlayerRelationsManager.RelationType.FRIEND ? this.friends : this.enemies;
    }

    private boolean contains(LinkedHashMap<String, String> map, String name) {
        String normalized = this.normalize(name);
        return !normalized.isEmpty() && map.containsKey(normalized);
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplayName(String displayName, String normalized) {
        String trimmed = displayName == null ? "" : displayName.trim();
        return trimmed.isEmpty() ? normalized : trimmed;
    }

    public static final class PlayerEntry {

        private final String key;

        private final String displayName;

        public PlayerEntry(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        public String getKey() {
            return this.key;
        }

        public String getDisplayName() {
            return this.displayName;
        }
    }

    public static enum RelationType {

        FRIEND("friends"), ENEMY("enemies");

        private final String jsonKey;

        private RelationType(String jsonKey) {
            this.jsonKey = jsonKey;
        }

        public String getJsonKey() {
            return this.jsonKey;
        }
    }
}
