package keystrokesmod.module.setting.impl;

import com.google.gson.JsonObject;
import keystrokesmod.module.setting.Setting;

public class TextSetting extends Setting {

    private final String placeholder;

    private final int maxLength;

    private final Runnable onSubmit;

    private String text;

    public GroupSetting group;

    public TextSetting(String name, String text, String placeholder, int maxLength) {
        this(name, text, placeholder, maxLength, null);
    }

    public TextSetting(String name, String text, String placeholder, int maxLength, Runnable onSubmit) {
        super(name);
        this.placeholder = placeholder == null ? "" : placeholder;
        this.maxLength = Math.max(1, maxLength);
        this.onSubmit = onSubmit;
        this.setText(text);
    }

    public TextSetting(GroupSetting group, String name, String text, String placeholder, int maxLength) {
        this(group, name, text, placeholder, maxLength, null);
    }

    public TextSetting(GroupSetting group, String name, String text, String placeholder, int maxLength, Runnable onSubmit) {
        this(name, text, placeholder, maxLength, onSubmit);
        this.group = group;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        String next = text == null ? "" : text;
        if (next.length() > this.maxLength) {
            next = next.substring(0, this.maxLength);
        }
        this.text = next;
    }

    public String getPlaceholder() {
        return this.placeholder;
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    @Override
    public String getProfileKey() {
        return this.group == null ? this.getName() : this.group.getName() + "." + this.getName();
    }

    public void submit() {
        if (this.onSubmit != null) {
            this.onSubmit.run();
        }
    }

    @Override
    public void loadProfile(JsonObject data) {
    }
}
