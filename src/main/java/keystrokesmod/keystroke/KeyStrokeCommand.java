package keystrokesmod.keystroke;

import keystrokesmod.Raven;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class KeyStrokeCommand extends CommandBase {

    public String func_71517_b() {
        return "keystrokesmod";
    }

    public void func_71515_b(ICommandSender sender, String[] args) {
        Raven.toggleKeyStrokeConfigGui();
    }

    public String func_71518_a(ICommandSender sender) {
        return "/keystrokesmod";
    }

    public int func_82362_a() {
        return 0;
    }

    public boolean func_71519_b(ICommandSender sender) {
        return true;
    }
}
