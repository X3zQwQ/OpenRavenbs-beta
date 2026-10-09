package keystrokesmod.command.impl;

import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;

public class ShowAll extends Command {

    public ShowAll() {
        super("show", "showall");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() > 0 && !"all".equalsIgnoreCase(input.getArgument(0))) {
            this.syntaxError();
        } else {
            int count = 0;
            for (Module module : ModuleManager.modules) {
                if (module.isHidden()) {
                    module.setHidden(false);
                    count++;
                }
            }
            this.replyWithHeader("&7Made &a" + count + " &7module" + (count == 1 ? "" : "s") + " visible in HUD.");
        }
    }
}
