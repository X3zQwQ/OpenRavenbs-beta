package keystrokesmod.module.impl.client;

import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.PlayerListSetting;
import keystrokesmod.utility.PlayerRelationsManager;

public class Relationships extends Module {

    public final GroupSetting friendsGroup = new GroupSetting("Friends");

    public final GroupSetting enemiesGroup = new GroupSetting("Enemies");

    public final GroupSetting middleClickGroup = new GroupSetting("Middle click");

    public final PlayerListSetting friends = new PlayerListSetting(this.friendsGroup, "Players", PlayerRelationsManager.RelationType.FRIEND, "Type a username", 32);

    public final PlayerListSetting enemies = new PlayerListSetting(this.enemiesGroup, "Players", PlayerRelationsManager.RelationType.ENEMY, "Type a username", 32);

    public final ButtonSetting middleClickFriends = new ButtonSetting(this.middleClickGroup, "Middle click friends", Raven.playerRelationsManager != null && Raven.playerRelationsManager.isMiddleClickFriends());

    public Relationships() {
        super("Relationships", Module.category.client, 0);
        this.friendsGroup.setOpened(true);
        this.enemiesGroup.setOpened(true);
        this.middleClickGroup.setOpened(true);
        this.registerSetting(this.friendsGroup);
        this.registerSetting(this.friends);
        this.registerSetting(this.enemiesGroup);
        this.registerSetting(this.enemies);
        this.registerSetting(this.middleClickGroup);
        this.registerSetting(this.middleClickFriends);
        this.ignoreOnSave = true;
        this.hidden = true;
    }

    @Override
    public void guiButtonToggled(ButtonSetting buttonSetting) {
        if (buttonSetting == this.middleClickFriends && Raven.playerRelationsManager != null) {
            Raven.playerRelationsManager.setMiddleClickFriends(buttonSetting.isToggled());
        }
    }

    @Override
    public void onEnable() {
        if (Raven.playerRelationsManager != null) {
            Raven.playerRelationsManager.setActive(true);
        }
    }

    @Override
    public void onDisable() {
        if (Raven.playerRelationsManager != null) {
            Raven.playerRelationsManager.setActive(false);
        }
    }
}
