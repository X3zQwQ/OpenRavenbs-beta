package keystrokesmod.command.impl;

import java.util.Arrays;
import java.util.List;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.ChatCommands;

public class Prefix extends Command {

    private static final List<String> EXAMPLE_PREFIXES = Arrays.asList(".", ",", ";", "/", "-", "=", "[", "]", "\\", "'", "`");

    public Prefix() {
        super("prefix");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() == 0) {
            this.replyWithHeader("&7Current prefix: &b" + ModuleManager.chatCommands.getPrefix());
        } else if (input.argumentCount() != 1) {
            this.syntaxError();
        } else {
            String prefix = input.getArgument(0);
            if (!ChatCommands.isValidPrefix(prefix)) {
                this.replyWithHeader("&7Prefix must be a single non-space character.");
            } else {
                ModuleManager.chatCommands.setPrefix(prefix);
                this.replyWithHeader("&7Chat command prefix set to &b" + ModuleManager.chatCommands.getPrefix() + "&7.");
            }
        }
    }

    @Override
    public List<String> suggest(CommandInput input) {
        return this.filterSuggestions(input, EXAMPLE_PREFIXES);
    }
}
