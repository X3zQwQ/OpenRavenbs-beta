package keystrokesmod.command.impl;

import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.Utils;

public class Script extends Command {

    public Script() {
        super("script");
    }

    @Override
    public void execute(CommandInput input) {
        this.execute(input.getArguments());
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("load")) {
            long currentTimeMillis = System.currentTimeMillis();
            ModuleManager.scriptManager.loadScripts(currentTimeMillis);
        } else {
            Utils.sendMessage("&cUsage: .script load");
        }
    }
}
