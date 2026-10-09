package keystrokesmod.command.impl;

import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.system.SystemUtils;

public class Name extends Command {

    public Name() {
        super("name", "ign", "name");
    }

    @Override
    public void execute(CommandInput input) {
        if (Utils.nullCheck()) {
            SystemUtils.addToClipboard(mc.field_71439_g.func_70005_c_());
            this.replyWithHeader("&7Copied &b" + mc.field_71439_g.func_70005_c_() + " &7to clipboard");
        }
    }
}
