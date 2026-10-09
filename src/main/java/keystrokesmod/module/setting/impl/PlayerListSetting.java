package keystrokesmod.module.setting.impl;

import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.utility.PlayerRelationsManager;

public class PlayerListSetting extends Setting {

    private final PlayerRelationsManager.RelationType relationType;

    private final String placeholder;

    private final int maxLength;

    public GroupSetting group;

    public PlayerListSetting(GroupSetting group, String name, PlayerRelationsManager.RelationType relationType, String placeholder, int maxLength) {
        super(name);
        this.group = group;
        this.relationType = relationType;
        this.placeholder = placeholder == null ? "" : placeholder;
        this.maxLength = Math.max(1, maxLength);
    }

    public PlayerRelationsManager.RelationType getRelationType() {
        return this.relationType;
    }

    public String getPlaceholder() {
        return this.placeholder;
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    public boolean addPlayer(String name) {
        return Raven.playerRelationsManager != null && Raven.playerRelationsManager.addRelation(this.relationType, name);
    }

    public boolean removePlayer(String name) {
        return Raven.playerRelationsManager != null && Raven.playerRelationsManager.removeRelation(this.relationType, name);
    }

    public void clearPlayers() {
        if (Raven.playerRelationsManager != null) {
            Raven.playerRelationsManager.clearRelation(this.relationType);
        }
    }

    public List<PlayerRelationsManager.PlayerEntry> getEntries() {
        return Raven.playerRelationsManager == null ? Collections.emptyList() : Raven.playerRelationsManager.getEntries(this.relationType);
    }

    @Override
    public String getProfileKey() {
        return this.group == null ? this.getName() : this.group.getName() + "." + this.getName();
    }

    @Override
    public void loadProfile(JsonObject data) {
    }
}
