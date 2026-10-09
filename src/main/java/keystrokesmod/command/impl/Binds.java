package keystrokesmod.command.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import keystrokesmod.Raven;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.profile.Profile;
import org.lwjgl.input.Keyboard;

public class Binds extends Command {

    public Binds() {
        super("binds");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() == 0) {
            Map<String, List<String>> binds = this.getBindsModulesMap(0);
            int size = this.getTotalModules(binds);
            this.replyWithHeader("&b" + size + " &7module" + (size == 1 ? "" : "s") + " have keybinds.");
            for (Entry<String, List<String>> entry : binds.entrySet()) {
                for (String moduleName : entry.getValue()) {
                    this.replyWithHeader(" &b" + entry.getKey() + " &7" + moduleName);
                }
            }
        } else if (input.argumentCount() != 1) {
            this.syntaxError();
        } else {
            int keycode = Keyboard.getKeyIndex(input.getArgument(0).toUpperCase());
            if (keycode == 0) {
                this.replyWithHeader("&7Invalid key.");
            } else {
                Map<String, List<String>> binds = this.getBindsModulesMap(keycode);
                int size = this.getTotalModules(binds);
                this.replyWithHeader("&b" + size + " &7module" + (size == 1 ? "" : "s") + " has keybind &b" + input.getArgument(0).toUpperCase() + "&7.");
                for (Entry<String, List<String>> entry : binds.entrySet()) {
                    for (String moduleName : entry.getValue()) {
                        this.replyWithHeader(" &b" + entry.getKey() + " &7" + moduleName);
                    }
                }
            }
        }
    }

    private Map<String, List<String>> getBindsModulesMap(int keycode) {
        Map<String, List<String>> binds = new HashMap<>();
        for (Module module : ModuleManager.modules) {
            this.addModuleIfMatches(binds, module, keycode);
        }
        for (Profile profile : Raven.profileManager.profiles) {
            this.addModuleIfMatches(binds, profile.getModule(), keycode);
        }
        for (Module scriptModule : Raven.scriptManager.scripts.values()) {
            this.addModuleIfMatches(binds, scriptModule, keycode);
        }
        return binds;
    }

    private void addModuleIfMatches(Map<String, List<String>> bindsMap, Module module, int keycode) {
        if (module.getKeycode() != 0) {
            if (keycode == 0 || module.getKeycode() == keycode) {
                String keyName = module.getKeycode() >= 1000 ? "M" + (module.getKeycode() - 1000) : Keyboard.getKeyName(module.getKeycode());
                List<String> moduleNames = bindsMap.get(keyName);
                if (moduleNames == null) {
                    moduleNames = new ArrayList<>();
                    bindsMap.put(keyName, moduleNames);
                }
                moduleNames.add(module.getName());
            }
        }
    }

    private int getTotalModules(Map<String, List<String>> binds) {
        int total = 0;
        for (List<String> modules : binds.values()) {
            total += modules.size();
        }
        return total;
    }
}
