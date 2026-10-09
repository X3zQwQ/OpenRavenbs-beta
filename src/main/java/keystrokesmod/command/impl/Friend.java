package keystrokesmod.command.impl;

import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.utility.PlayerRelationsManager;
import keystrokesmod.utility.Utils;

public class Friend extends Command {

    public Friend() {
        super("friend", "friend", "f");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() != 0) {
            if (input.argumentCount() != 1) {
                this.syntaxError();
            } else {
                String name = input.getArgument(0);
                if ("clear".equalsIgnoreCase(name)) {
                    int cleared = Raven.playerRelationsManager.getCount(PlayerRelationsManager.RelationType.FRIEND);
                    Raven.playerRelationsManager.clearFriends();
                    this.replyWithHeader("&b" + cleared + " &7friend" + (cleared == 1 ? "" : "s") + " cleared.");
                } else {
                    if (Utils.addFriend(name)) {
                        this.replyWithHeader("&7Added &afriend&7: &b" + name);
                    } else {
                        Utils.removeFriend(name);
                        this.replyWithHeader("&7Removed &afriend&7: &b" + name);
                    }
                }
            }
        } else {
            List<PlayerRelationsManager.PlayerEntry> entries = Raven.playerRelationsManager.getEntries(PlayerRelationsManager.RelationType.FRIEND);
            this.replyWithHeader("&b" + entries.size() + " &7friend" + (entries.size() == 1 ? "" : "s") + ".");
            for (PlayerRelationsManager.PlayerEntry entry : entries) {
                this.replyWithHeader(" &b" + entry.getDisplayName());
            }
        }
    }
}
