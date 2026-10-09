package keystrokesmod.module.setting.impl;

import com.google.gson.JsonObject;
import keystrokesmod.helper.MouseHelper;
import keystrokesmod.module.setting.Setting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeySetting extends Setting {

    private int key;

    public GroupSetting group;

    public KeySetting(String name, int key) {
        super(name);
        this.key = key;
    }

    public KeySetting(GroupSetting group, String name, int key) {
        super(name);
        this.group = group;
        this.key = key;
    }

    public int getKey() {
        return this.key;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public String getProfileKey() {
        return this.group == null ? this.getName() : this.group.getName() + "." + this.getName();
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isPressed() {
        if (this.getKey() == 0) {
            return false;
        } else if (this.getKey() < 1000) {
            return Keyboard.isKeyDown(this.getKey());
        } else {
            return this.getKey() != 1069 && this.getKey() != 1070 ? Mouse.isButtonDown(this.getKey() - 1000) : MouseHelper.isScrollDown(this.getKey());
        }
    }

    @Override
    public void loadProfile(JsonObject data) {
        String profileKey = this.getProfileKey();
        String legacyKey = this.getName();
        String key = data.has(profileKey) ? profileKey : legacyKey;
        if (data.has(key) && data.get(key).isJsonPrimitive()) {
            int keyValue = this.key;
            try {
                keyValue = data.getAsJsonPrimitive(key).getAsInt();
            } catch (Exception exception) {
            }
            this.key = keyValue;
        }
    }
}
