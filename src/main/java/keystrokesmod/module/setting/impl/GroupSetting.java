package keystrokesmod.module.setting.impl;

import com.google.gson.JsonObject;
import java.util.List;
import keystrokesmod.module.setting.Setting;

public class GroupSetting extends Setting {

    private List<Setting> settings;

    private boolean opened;

    public GroupSetting(String name) {
        super(name);
    }

    public List<Setting> getSettings() {
        return this.settings;
    }

    public void addSetting(Setting setting) {
        this.settings.add(setting);
    }

    public void removeSetting(Setting setting) {
        this.settings.remove(setting);
    }

    public boolean isOpened() {
        return this.opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    @Override
    public void loadProfile(JsonObject data) {
    }
}
