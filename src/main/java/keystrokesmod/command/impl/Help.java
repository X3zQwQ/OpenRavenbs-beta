package keystrokesmod.command.impl;

import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;

public class Help extends Command {

    public Help() {
        super("help");
    }

    @Override
    public void execute(CommandInput input) {
        String prefix = this.prefixed("");
        this.replyWithHeader("&7Chat commands - &dGeneral");
        this.reply(" &b" + prefix + "ign/" + prefix + "name &7Copy your username.");
        this.reply(" &b" + prefix + "ping &7Estimate your ping.");
        this.reply(" &b" + prefix + "friend/" + prefix + "enemy [name/clear] &7Adds as enemy/friend.");
        this.reply(" &b" + prefix + "track [name/clear] &7Renders esp on targets.");
        this.replyWithHeader("&7Chat commands - &dModules");
        this.reply(" &b" + prefix + "t/" + prefix + "toggle [module] &7Toggle a module.");
        this.reply(" &b" + prefix + "bind [module] [key] &7Bind module to key.");
        this.reply(" &b" + prefix + "bind add/remove [module] [key] &7Astolfo bind syntax.");
        this.reply(" &b" + prefix + "unbind [module] &7Remove module bind.");
        this.reply(" &b" + prefix + "namehider [name] &7Set name hider name.");
        this.reply(" &b" + prefix + "binds (key) &7List module binds.");
        this.reply(" &b" + prefix + "prefix [char] &7Change the chat command prefix.");
        this.reply(" &b" + prefix + "show all/" + prefix + "showall &7Show all modules in HUD.");
        this.reply(" &b" + prefix + "hide all/" + prefix + "hideall &7Hide all modules from HUD.");
        this.replyWithHeader("&7Chat commands - &dProfiles");
        this.reply(" &b" + prefix + "profiles &7List loaded profiles.");
        this.reply(" &b" + prefix + "profiles save (name) &7Save current settings as a profile.");
        this.reply(" &b" + prefix + "profiles load [name] &7Load a profile.");
        this.reply(" &b" + prefix + "profiles delete [name] &7Delete a profile.");
        this.reply(" &b" + prefix + "profiles rename [oldname] [newname] &7Rename a profile.");
    }
}
