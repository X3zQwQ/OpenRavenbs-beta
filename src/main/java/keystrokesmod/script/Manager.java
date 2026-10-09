package keystrokesmod.script;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.TextSetting;
import keystrokesmod.script.model.Entity;
import keystrokesmod.script.model.Image;
import keystrokesmod.script.model.NetworkPlayer;
import keystrokesmod.utility.Utils;
import org.lwjgl.Sys;

public class Manager extends Module {

    public static ButtonSetting enableHttpRequests;

    public static ButtonSetting enableWebSockets;

    private final TextSetting createScriptName;

    public final String DOCUMENTATION_URL = "https://blowsy.gitbook.io/raven";

    private final String CONFIG_DIR = mc.field_71412_D + File.separator + "keystrokes" + File.separator + "settings.txt";

    private final String SEPARATOR = ":";

    private final String SEPARATOR_FULL = ": ";

    private long lastLoad = 0L;

    public Manager() {
        super("Manager", Module.category.scripts);
        this.registerSetting(this.createScriptName = new TextSetting("Script name", "", "Type a script name...", 32, this::createScript));
        this.registerSetting(new ButtonSetting("Create script", this::createScript));
        this.registerSetting(new ButtonSetting("Load scripts", () -> {
            if (Raven.scriptManager.compiler == null) {
                Utils.sendMessage("&cCompiler error, JDK not found");
            } else {
                long currentTimeMillis = System.currentTimeMillis();
                if (Utils.timeBetween(this.lastLoad, currentTimeMillis) > 1500L) {
                    this.lastLoad = currentTimeMillis;
                    this.loadScripts(currentTimeMillis);
                } else {
                    Utils.sendMessage("&cYou are on cooldown.");
                }
            }
        }));
        this.registerSetting(new ButtonSetting("Open folder", () -> {
            try {
                Desktop.getDesktop().open(Raven.scriptManager.directory);
            } catch (IOException exception) {
                Raven.scriptManager.directory.mkdirs();
                Utils.sendMessage("&cError locating folder, recreated.");
            }
        }));
        this.registerSetting(new ButtonSetting("View documentation", () -> {
            try {
                Desktop.getDesktop().browse(new URI("https://blowsy.gitbook.io/raven"));
            } catch (Throwable exception2) {
                Sys.openURL("https://blowsy.gitbook.io/raven");
            }
        }));
        this.registerSetting(new DescriptionSetting("Privacy"));
        this.registerSetting(enableHttpRequests = new ButtonSetting("Enable http requests", true));
        this.registerSetting(enableWebSockets = new ButtonSetting("Enable websockets", true));
        this.canBeEnabled = false;
        this.ignoreOnSave = true;
        this.retrieveSettings();
    }

    @Override
    public void guiButtonToggled(ButtonSetting s) {
        this.updateSettingFile();
    }

    private boolean updateSettingFile() {
        return this.set("enable-http-requests", String.valueOf(enableHttpRequests.isToggled())) & this.set("enable-websockets", String.valueOf(enableWebSockets.isToggled()));
    }

    public void loadScripts(long ms) {
        Raven.scriptManager.loadScripts();
        if (Raven.scriptManager.scripts.isEmpty()) {
            Utils.sendMessage("&7No scripts found.");
        } else {
            double timeTaken = Utils.round((System.currentTimeMillis() - ms) / 1000.0, 1);
            Utils.sendMessage("&7Loaded &b" + Raven.scriptManager.scripts.size() + " &7script" + (Raven.scriptManager.scripts.size() == 1 ? "" : "s") + " in &b" + Utils.asWholeNum(timeTaken) + "&7s.");
        }
        Entity.clearCache();
        NetworkPlayer.clearCache();
        Image.clearCache();
        ScriptDefaults.reloadModules();
        if (Raven.currentProfile != null && Raven.currentProfile.getModule() != null) {
            Raven.currentProfile.getModule().saved = false;
        }
    }

    private void ensureConfigFileExists() throws IOException {
        Path configPath = Paths.get(this.CONFIG_DIR);
        if (Files.notExists(configPath)) {
            Files.createDirectories(configPath.getParent());
            Files.createFile(configPath);
        }
    }

    private boolean set(String key, String value) {
        if (key != null && !key.isEmpty()) {
            key = key.replace(":", "");
            String entry = key + ": " + value;
            try {
                this.ensureConfigFileExists();
                Path configPath = new File(this.CONFIG_DIR).toPath();
                List<String> lines = new ArrayList<>(Files.readAllLines(configPath));
                boolean keyExists = false;
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.startsWith(key + ": ")) {
                        lines.set(i, entry);
                        keyExists = true;
                        break;
                    }
                }
                if (!keyExists) {
                    lines.add(entry);
                }
                Files.write(configPath, lines);
                return true;
            } catch (IOException exception3) {
                return false;
            }
        } else {
            return false;
        }
    }

    private void retrieveSettings() {
        String requestState = this.retrieveSetting("enable-http-requests");
        String webSocketsState = this.retrieveSetting("enable-websockets");
        if (requestState != null) {
            enableHttpRequests.setEnabled(this.parseBoolean(requestState, true));
        }
        if (webSocketsState != null) {
            enableWebSockets.setEnabled(this.parseBoolean(webSocketsState, true));
        }
    }

    private boolean parseBoolean(String parse, boolean defaultVal) {
        try {
            return Boolean.parseBoolean(parse);
        } catch (Exception exception4) {
            return defaultVal;
        }
    }

    private String retrieveSetting(String key) {
        try {
            this.ensureConfigFileExists();
            Path configPath = new File(this.CONFIG_DIR).toPath();
            for (String line : Files.readAllLines(configPath)) {
                if (line.startsWith(key + ": ")) {
                    return line.substring((key + ": ").length());
                }
            }
        } catch (IOException exception5) {
        }
        return null;
    }

    private void createScript() {
        if (Raven.scriptManager != null) {
            String scriptName = Raven.scriptManager.createScript(this.createScriptName.getText());
            if (scriptName != null) {
                this.createScriptName.setText("");
                Utils.sendMessage("&7Created script: &b" + scriptName);
            }
        }
    }
}
