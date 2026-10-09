package keystrokesmod.utility.profile;

import keystrokesmod.Raven;

public class Profile {

    private final ProfileModule module;

    private final int bind;

    private String profileName;

    public Profile(String profileName, int bind) {
        this.profileName = profileName;
        this.bind = bind;
        this.module = new ProfileModule(this, profileName, bind);
        this.module.ignoreOnSave = true;
    }

    public ProfileModule getModule() {
        return this.module;
    }

    public int getBind() {
        return this.bind;
    }

    public String getName() {
        return this.profileName;
    }

    public void setName(String profileName) {
        this.profileName = profileName;
        this.module.setProfileName(profileName);
    }

    public boolean isDefault() {
        return Raven.profileManager != null && Raven.profileManager.isDefaultProfile(this);
    }

    public void setDefault(boolean defaultProfile) {
        if (Raven.profileManager != null) {
            if (defaultProfile) {
                Raven.profileManager.setDefaultProfile(this);
            } else if (this.isDefault()) {
                Raven.profileManager.removeDefaultProfile();
            }
        }
    }
}
