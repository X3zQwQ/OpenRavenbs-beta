package keystrokesmod.command.impl;

import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.impl.other.NameHider;

public class Cname extends Command {

    public Cname() {
        super("namehider");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() == 0) {
            this.syntaxError();
        } else {
            NameHider.setFakeName(input.joinArguments(0));
            this.replyWithHeader("&7Name has been set to &b" + NameHider.fakeName + "&7.");
        }
    }
}
