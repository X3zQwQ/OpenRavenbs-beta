package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.CommandHandler;
import keystrokesmod.utility.Timer;

public class CommandLine extends Module {

    public static boolean opened = false;

    public static boolean closed = false;

    public static Timer animation;

    public static ButtonSetting animate;

    public CommandLine() {
        super("Command line", Module.category.client);
        this.registerSetting(animate = new ButtonSetting("Animate", true));
    }

    @Override
    public void onEnable() {
        CommandHandler.setBackgroundColor();
        opened = true;
        closed = false;
        (animation = new Timer(500.0F)).start();
    }

    @Override
    public void onDisable() {
        closed = true;
        if (animation != null) {
            animation.start();
        }
        CommandHandler.onDisable();
    }
}
