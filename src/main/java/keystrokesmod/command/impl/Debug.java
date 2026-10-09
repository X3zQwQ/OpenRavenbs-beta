package keystrokesmod.command.impl;

import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.helper.DebugHelper;

public class Debug extends Command {

    public Debug() {
        super("debug");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() == 0) {
            Raven.DEBUG = !Raven.DEBUG;
            this.replyWithHeader("&7Debug " + (Raven.DEBUG ? "&aenabled" : "&cdisabled") + "&7.");
        } else if (input.argumentCount() != 1) {
            this.syntaxError();
        } else {
            String option = input.getArgument(0).toLowerCase();
            if ("mixin".equals(option)) {
                DebugHelper.MIXIN = !DebugHelper.MIXIN;
                this.replyWithHeader("&dMixin &7debug " + (DebugHelper.MIXIN ? "&aenabled" : "&cdisabled") + "&7.");
            } else if (!"bg".equals(option) && !"background".equals(option)) {
                this.syntaxError();
            } else {
                DebugHelper.BACKGROUND = !DebugHelper.BACKGROUND;
                this.replyWithHeader("&6Background &7debug " + (DebugHelper.BACKGROUND ? "&aenabled" : "&cdisabled") + "&7.");
            }
        }
    }

    @Override
    public List<String> suggest(CommandInput input) {
        return this.filterSuggestions(input, "mixin", "background");
    }
}
