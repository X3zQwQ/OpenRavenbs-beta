package keystrokesmod.command.impl;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;

public class Toggle extends Command {

    public Toggle() {
        super("toggle", "toggle", "t");
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
                module.toggle();
                this.replyWithHeader("&7" + module.getName() + " " + (module.isEnabled() ? "&aenabled" : "&cdisabled") + "&7.");
            }
        }
    }

    @Override
    public List<String> suggest(CommandInput input) {
        List<String> completions = new ArrayList<>();
        String query = input.argumentCount() == 0 ? "" : input.joinArguments(0).toLowerCase();
        for (Module module : ModuleManager.modules) {
            if (module.getName().toLowerCase().startsWith(query)) {
                completions.add(module.getName());
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
