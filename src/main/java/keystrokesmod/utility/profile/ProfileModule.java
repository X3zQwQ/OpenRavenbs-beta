package keystrokesmod.utility.profile;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.TextSetting;
import keystrokesmod.utility.Utils;

public class ProfileModule extends Module {

    private final Profile profile;

    private final TextSetting profileNameSetting;

    private String displayName;

    public boolean saved = true;

    public ProfileModule(Profile profile, String name, int bind) {
        super(name, Module.category.profiles, bind);
        this.profile = profile;
        this.displayName = name;
        this.registerSetting(this.profileNameSetting = new TextSetting("Profile name", name, "Type a new profile name...", 32, this::renameProfile));
        this.registerSetting(new ButtonSetting("Save profile", () -> {
            Utils.sendMessage("&7Saved profile: &b" + this.getName());
            Raven.profileManager.saveProfile(this.profile);
            this.saved = true;
        }));
        this.registerSetting(new ButtonSetting("Set default", () -> {
            if (Raven.profileManager != null) {
                Raven.profileManager.setDefaultProfile(this.profile);
                Utils.sendMessage("&7Set &b" + this.getName() + " &7as default profile.");
            }
        }));
        this.registerSetting(new ButtonSetting("Remove profile", () -> {
            String profileName = this.getName();
            if (Raven.profileManager.deleteProfile(profileName)) {
                Utils.sendMessage("&7Removed profile: &b" + profileName);
            }
        }));
    }

    @Override
    public void toggle() {
        if (mc.field_71462_r instanceof ClickGui || mc.field_71462_r == null) {
            Raven.profileManager.loadProfile(this.getName());
            Raven.currentProfile = this.profile;
            if (Settings.sendMessage.isToggled()) {
                Utils.sendMessage("&7Enabled profile: &b" + this.getName());
            }
            this.saved = true;
        }
    }

    @Override
    public boolean isEnabled() {
        return Raven.currentProfile == null ? false : Raven.currentProfile.getModule() == this;
    }

    @Override
    public String getName() {
        return this.displayName;
    }

    public void setProfileName(String profileName) {
        this.displayName = profileName;
        this.profileNameSetting.setText(profileName);
    }

    private void renameProfile() {
        if (Raven.profileManager != null) {
            String oldName = this.getName();
            if (Raven.profileManager.renameProfile(this.profile, this.profileNameSetting.getText())) {
                this.profileNameSetting.setText(this.profile.getName());
                if (!oldName.equals(this.profile.getName())) {
                    Utils.sendMessage("&7Renamed profile: &b" + oldName + " &7to &b" + this.profile.getName());
                }
            }
        }
    }
}
