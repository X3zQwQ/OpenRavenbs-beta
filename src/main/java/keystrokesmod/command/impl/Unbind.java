package keystrokesmod.command.impl;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;

public class Unbind extends Command {

    public Unbind() {
        super("unbind", "ub");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() == 0) {
            this.syntaxError();
        } else {
            Module module = this.findModule(input.joinArguments(0));
            if (module == null) {
                this.replyWithHeader("&cModule not found.");
            } else {
                module.setBind(0);
                this.replyWithHeader("&7Unbound &b" + module.getName() + "&7.");
            }
        }
    }

    @Override
    public List<String> suggest(CommandInput input) {
        String query = input.argumentCount() == 0 ? "" : input.joinArguments(0);
        List<String> completions = new ArrayList<>();
        String normalized = this.normalize(query);
        for (Module module : ModuleManager.modules) {
            String moduleName = module.getName();
            if (this.normalize(moduleName).startsWith(normalized)) {
                completions.add(moduleName);
            }
        }
        return completions;
    }

    @Override
    public int getSuggestionArgumentStart(CommandInput input) {
        return 0;
    }

    private Module findModule(String query) {
        String normalizedQuery = this.normalize(query);
        for (Module module : ModuleManager.modules) {
            if (this.normalize(module.getName()).equals(normalizedQuery)) {
                return module;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace(" ", "").toLowerCase();
    }
}
