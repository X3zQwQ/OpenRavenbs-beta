package keystrokesmod.command.impl;

import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.helper.PingHelper;

public class Ping extends Command {

    public Ping() {
        super("ping");
    }

    @Override
    public void execute(CommandInput input) {
        PingHelper.checkPing(true);
    }
}
