package keystrokesmod.command.impl;

import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.utility.PlayerRelationsManager;
import keystrokesmod.utility.Utils;

public class Enemy extends Command {

    public Enemy() {
        super("enemy", "enemy", "e");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() != 0) {
            if (input.argumentCount() != 1) {
                this.syntaxError();
            } else {
                String name = input.getArgument(0);
                if ("clear".equalsIgnoreCase(name)) {
                    int cleared = Raven.playerRelationsManager.getCount(PlayerRelationsManager.RelationType.ENEMY);
                    Raven.playerRelationsManager.clearEnemies();
                    this.replyWithHeader("&b" + cleared + " &7enem" + (cleared == 1 ? "y" : "ies") + " cleared.");
                } else {
                    if (Utils.addEnemy(name)) {
                        this.replyWithHeader("&7Added enemy&7: &b" + name);
                    } else {
                        Utils.removeEnemy(name);
                        this.replyWithHeader("&7Removed enemy&7: &b" + name);
                    }
                }
            }
        } else {
            List<PlayerRelationsManager.PlayerEntry> entries = Raven.playerRelationsManager.getEntries(PlayerRelationsManager.RelationType.ENEMY);
            this.replyWithHeader("&b" + entries.size() + " &7enem" + (entries.size() == 1 ? "y" : "ies") + ".");
            for (PlayerRelationsManager.PlayerEntry entry : entries) {
                this.replyWithHeader(" &b" + entry.getDisplayName());
            }
        }
    }
}
