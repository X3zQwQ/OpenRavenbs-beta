package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.CapeManager;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.font.FontManager;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiInventory;

public class Settings extends Module {

    public static SliderSetting customCapes;

    public static SliderSetting defaultTheme;

    public static ButtonSetting addBracketsToDistance;

    public static ButtonSetting hideFirstPersonESP;

    public static ButtonSetting setChatAsInventory;

    public static ButtonSetting showHealthAsHearts;

    public static ButtonSetting showHeartSymbol;

    public static ButtonSetting weaponAxe;

    public static ButtonSetting weaponEnchanted;

    public static ButtonSetting weaponFist;

    public static ButtonSetting weaponHoe;

    public static ButtonSetting weaponRod;

    public static ButtonSetting weaponShovel;

    public static ButtonSetting weaponStick;

    public static ButtonSetting rotateBody;

    public static ButtonSetting fullBody;

    public static SliderSetting randomYawFactor;

    public static ButtonSetting sendMessage;

    public static SliderSetting offset;

    public static SliderSetting timeMultiplier;

    public Settings() {
        super("Settings", Module.category.client, 0);
        CapeManager.reloadCustomCapes();
        this.registerSetting(new DescriptionSetting("General"));
        this.registerSetting(customCapes = new SliderSetting("Custom cape", 0, CapeManager.getCapeOptions()));
        this.registerSetting(defaultTheme = new SliderSetting("Default theme", 1, Theme.THEMES_STRING));
        this.registerSetting(addBracketsToDistance = new ButtonSetting("Add brackets to distance", false));
        this.registerSetting(hideFirstPersonESP = new ButtonSetting("Hide first person self ESP", true));
        this.registerSetting(setChatAsInventory = new ButtonSetting("Set chat as inventory", false));
        this.registerSetting(showHealthAsHearts = new ButtonSetting("Show health as hearts", false));
        this.registerSetting(showHeartSymbol = new ButtonSetting("Show heart symbol", false));
        this.registerSetting(new DescriptionSetting("Extra weapons"));
        this.registerSetting(weaponAxe = new ButtonSetting("Axe", false));
        this.registerSetting(weaponEnchanted = new ButtonSetting("Enchanted", false));
        this.registerSetting(weaponFist = new ButtonSetting("Fist", false));
        this.registerSetting(weaponHoe = new ButtonSetting("Hoe", false));
        this.registerSetting(weaponRod = new ButtonSetting("Rod", false));
        this.registerSetting(weaponShovel = new ButtonSetting("Shovel", false));
        this.registerSetting(weaponStick = new ButtonSetting("Stick", true));
        this.registerSetting(new DescriptionSetting("Rotations"));
        this.registerSetting(rotateBody = new ButtonSetting("Rotate body", true));
        this.registerSetting(fullBody = new ButtonSetting("Full body", false));
        this.registerSetting(randomYawFactor = new SliderSetting("Random yaw factor", 0.0, 0.0, 10.0, 1.0));
        this.registerSetting(new DescriptionSetting("Profiles"));
        this.registerSetting(sendMessage = new ButtonSetting("Send message on enable", true));
        this.registerSetting(new DescriptionSetting("Theme colors"));
        this.registerSetting(offset = new SliderSetting("Offset", 0.5, -3.0, 3.0, 0.1));
        this.registerSetting(timeMultiplier = new SliderSetting("Time multiplier", 0.5, 0.1, 4.0, 0.1));
        this.registerSetting(new DescriptionSetting("Resources"));
        this.registerSetting(new ButtonSetting("Open fonts folder", Settings::openFontsFolder));
        this.registerSetting(new ButtonSetting("Load fonts", Settings::loadFonts));
        this.registerSetting(new ButtonSetting("Open capes folder", Settings::openCapesFolder));
        this.registerSetting(new ButtonSetting("Load capes", Settings::loadCapes));
        this.canBeEnabled = false;
    }

    private static void openFontsFolder() {
        if (!FontManager.openFontDirectory()) {
            Utils.sendMessage("&cCould not open fonts folder: &7" + FontManager.getFontDirectory().getAbsolutePath());
        }
    }

    private static void loadFonts() {
        FontManager.ReloadResult result = FontManager.reloadCustomFonts();
        if (ModuleManager.hud != null) {
            ModuleManager.sort();
        }
        sendReloadMessage("font", result.getLoaded(), result.getFailed());
    }

    private static void openCapesFolder() {
        if (!CapeManager.openCapeDirectory()) {
            Utils.sendMessage("&cCould not open capes folder: &7" + CapeManager.getCapeDirectory().getAbsolutePath());
        }
    }

    private static void loadCapes() {
        CapeManager.ReloadResult result = CapeManager.reloadCustomCapes();
        customCapes.setOptions(CapeManager.getCapeOptions());
        sendReloadMessage("cape", result.getLoaded(), result.getFailed());
    }

    private static void sendReloadMessage(String resourceName, int loaded, int failed) {
        String plural = loaded == 1 ? "" : "s";
        String message = "&7Loaded &b" + loaded + " &7custom " + resourceName + plural + ".";
        if (failed > 0) {
            message = message + " &c" + failed + " failed.";
        }
        Utils.sendMessage(message);
    }

    public static boolean inInventory() {
        return mc.field_71462_r instanceof GuiInventory ? true : mc.field_71462_r instanceof GuiChat && setChatAsInventory.isToggled();
    }
}
