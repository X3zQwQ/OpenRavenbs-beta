package keystrokesmod.utility.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import keystrokesmod.Raven;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.event.PostProfileLoadEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.impl.client.Relationships;
import keystrokesmod.module.impl.minigames.BedWars;
import keystrokesmod.module.impl.minigames.SkyWars;
import keystrokesmod.module.impl.player.FastPlace;
import keystrokesmod.module.impl.player.HideWindow;
import keystrokesmod.module.impl.player.Scaffold;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.impl.render.PotionHUD;
import keystrokesmod.module.impl.render.Radar;
import keystrokesmod.module.impl.render.TargetHUD;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.BlockListSetting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.StringListSetting;
import keystrokesmod.module.setting.impl.TextSetting;
import keystrokesmod.utility.IMinecraftInstance;
import keystrokesmod.utility.Utils;
import net.minecraftforge.common.MinecraftForge;

public class ProfileManager implements IMinecraftInstance {

    private static final String DEFAULT_PROFILE_NAME = "default";

    private static final char[] INVALID_PROFILE_NAME_CHARS = new char[] { '\\', '/', ':', '*', '?', '"', '<', '>', '|' };

    private static final String CONFIG_DIR = mc.field_71412_D + File.separator + "keystrokes" + File.separator + "settings.txt";

    private static final String SEPARATOR = ":";

    private static final String SEPARATOR_FULL = ": ";

    private static final String DEFAULT_PROFILE_KEY = "default-profile";

    public File directory;

    public List<Profile> profiles = new ArrayList<>();

    public ProfileManager() {
        this.directory = new File(mc.field_71412_D + File.separator + "keystrokes", "profiles");
        if (!this.directory.exists()) {
            boolean success = this.directory.mkdirs();
            if (!success) {
                System.out.println("There was an issue creating profiles directory.");
                return;
            }
        }
        if (this.getProfileFiles().isEmpty()) {
            this.saveProfile(new Profile("default", 0));
        }
    }

    public void saveProfile(Profile profile) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("keybind", profile.getModule().getKeycode());
        JsonArray jsonArray = new JsonArray();
        for (Module module : Raven.moduleManager.getModules()) {
            if (!module.ignoreOnSave || shouldSaveModuleStateOnly(module)) {
                JsonObject moduleInformation = module.ignoreOnSave ? getModuleStateObject(module) : getJsonObject(module);
                jsonArray.add(moduleInformation);
            }
        }
        if (Raven.scriptManager != null && Raven.scriptManager.scripts != null) {
            for (Module module2 : Raven.scriptManager.scripts.values()) {
                if (!module2.ignoreOnSave) {
                    JsonObject moduleInformation = getJsonObject(module2);
                    jsonArray.add(moduleInformation);
                }
            }
        }
        jsonObject.add("modules", jsonArray);
        try (FileWriter fileWriter = new FileWriter(new File(this.directory, profile.getName() + ".json"))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonObject, fileWriter);
        } catch (Exception exception) {
            this.failedMessage("save", profile.getName());
            exception.printStackTrace();
        }
    }

    public Profile createProfile(String requestedName, int bind) {
        String profileName = this.normalizeProfileName(requestedName);
        String validationError = this.validateProfileName(profileName, null);
        if (validationError != null) {
            Utils.sendMessage("&c" + validationError);
            return null;
        } else {
            Profile profile = new Profile(profileName, bind);
            this.saveProfile(profile);
            this.profiles.add(profile);
            this.refreshProfileModules();
            return profile;
        }
    }

    private static JsonObject getJsonObject(Module module) {
        JsonObject moduleInformation = new JsonObject();
        moduleInformation.addProperty("name", module.moduleCategory() == Module.category.scripts && !(module instanceof keystrokesmod.script.Manager) ? "sc-" + module.getName() : module.getName());
        if (module.canBeEnabled) {
            moduleInformation.addProperty("enabled", module.isEnabled());
            moduleInformation.addProperty("hidden", module.isHidden());
            moduleInformation.addProperty("keybind", module.getKeycode());
        }
        if (module instanceof HUD) {
            moduleInformation.addProperty("posX", HUD.posX);
            moduleInformation.addProperty("posY", HUD.posY);
            moduleInformation.addProperty("relPosX", HUD.getRelativePosX());
            moduleInformation.addProperty("relPosY", HUD.getRelativePosY());
        } else if (module instanceof TargetHUD) {
            moduleInformation.addProperty("posX", ModuleManager.targetHUD.posX);
            moduleInformation.addProperty("posY", ModuleManager.targetHUD.posY);
        } else if (module instanceof PotionHUD) {
            PotionHUD potionHUD = (PotionHUD) module;
            moduleInformation.addProperty("posX", potionHUD.getPosX());
            moduleInformation.addProperty("posY", potionHUD.getPosY());
            moduleInformation.addProperty("relPosX", potionHUD.getRelativePosX());
            moduleInformation.addProperty("relPosY", potionHUD.getRelativePosY());
        } else if (module instanceof Radar) {
            Radar radar = (Radar) module;
            moduleInformation.addProperty("posX", radar.getPosX());
            moduleInformation.addProperty("posY", radar.getPosY());
            moduleInformation.addProperty("relPosX", radar.getRelativePosX());
            moduleInformation.addProperty("relPosY", radar.getRelativePosY());
        } else if (module instanceof HideWindow) {
            HideWindow hw = (HideWindow) module;
            moduleInformation.addProperty("posX", hw.getPosX());
            moduleInformation.addProperty("posY", hw.getPosY());
            moduleInformation.addProperty("relPosX", hw.getRelativePosX());
            moduleInformation.addProperty("relPosY", hw.getRelativePosY());
        } else if (module instanceof FastPlace) {
            FastPlace fp = (FastPlace) module;
            moduleInformation.addProperty("posX", fp.getPosX());
            moduleInformation.addProperty("posY", fp.getPosY());
            moduleInformation.addProperty("relPosX", fp.getRelativePosX());
            moduleInformation.addProperty("relPosY", fp.getRelativePosY());
        } else if (module instanceof Scaffold) {
            Scaffold scaffold = (Scaffold) module;
            moduleInformation.addProperty("posX", scaffold.getPosX());
            moduleInformation.addProperty("posY", scaffold.getPosY());
            moduleInformation.addProperty("relPosX", scaffold.getRelativePosX());
            moduleInformation.addProperty("relPosY", scaffold.getRelativePosY());
        } else if (module instanceof BedWars) {
            BedWars bedWars = (BedWars) module;
            moduleInformation.addProperty("closestEnemyPosX", bedWars.getClosestEnemyPosX());
            moduleInformation.addProperty("closestEnemyPosY", bedWars.getClosestEnemyPosY());
            moduleInformation.addProperty("closestEnemyRelPosX", bedWars.getClosestEnemyRelativePosX());
            moduleInformation.addProperty("closestEnemyRelPosY", bedWars.getClosestEnemyRelativePosY());
            moduleInformation.addProperty("magicMilkPosX", bedWars.getMagicMilkPosX());
            moduleInformation.addProperty("magicMilkPosY", bedWars.getMagicMilkPosY());
            moduleInformation.addProperty("magicMilkRelPosX", bedWars.getMagicMilkRelativePosX());
            moduleInformation.addProperty("magicMilkRelPosY", bedWars.getMagicMilkRelativePosY());
        } else if (module instanceof SkyWars) {
            SkyWars skyWars = (SkyWars) module;
            moduleInformation.addProperty("closestEnemyPosX", skyWars.getClosestEnemyPosX());
            moduleInformation.addProperty("closestEnemyPosY", skyWars.getClosestEnemyPosY());
            moduleInformation.addProperty("closestEnemyRelPosX", skyWars.getClosestEnemyRelativePosX());
            moduleInformation.addProperty("closestEnemyRelPosY", skyWars.getClosestEnemyRelativePosY());
        } else if (module instanceof Gui) {
            for (CategoryComponent c : ClickGui.categories) {
                moduleInformation.addProperty(c.category.name(), c.x + "," + c.y + "," + c.opened);
            }
        }
        for (Setting setting : module.getSettings()) {
            if (setting instanceof ButtonSetting && !((ButtonSetting) setting).isMethodButton) {
                moduleInformation.addProperty(setting.getProfileKey(), ((ButtonSetting) setting).isToggled());
            } else if (setting instanceof SliderSetting) {
                SliderSetting sliderSetting = (SliderSetting) setting;
                if (sliderSetting.isString && sliderSetting.canBeDisabled && sliderSetting.getInput() == -1.0) {
                    moduleInformation.addProperty(setting.getProfileKey(), -1);
                } else if (sliderSetting.isString) {
                    moduleInformation.addProperty(setting.getProfileKey(), sliderSetting.getSelectedOption());
                } else {
                    moduleInformation.addProperty(setting.getProfileKey(), sliderSetting.getInput());
                }
            } else if (setting instanceof KeySetting) {
                moduleInformation.addProperty(setting.getProfileKey(), ((KeySetting) setting).getKey());
            } else if (setting instanceof TextSetting) {
                moduleInformation.addProperty(setting.getProfileKey(), ((TextSetting) setting).getText());
            } else if (setting instanceof ColorSetting) {
                ColorSetting cs = (ColorSetting) setting;
                moduleInformation.addProperty(setting.getProfileKey(), cs.getRed() + "," + cs.getGreen() + "," + cs.getBlue() + "," + cs.getAlpha());
            } else if (setting instanceof BlockListSetting) {
                moduleInformation.add(setting.getProfileKey(), ((BlockListSetting) setting).toJsonArray());
            } else if (setting instanceof StringListSetting) {
                moduleInformation.add(setting.getProfileKey(), ((StringListSetting) setting).toJsonArray());
            }
        }
        return moduleInformation;
    }

    private static JsonObject getModuleStateObject(Module module) {
        JsonObject moduleInformation = new JsonObject();
        moduleInformation.addProperty("name", module.getName());
        if (module.canBeEnabled) {
            moduleInformation.addProperty("enabled", module.isEnabled());
            moduleInformation.addProperty("hidden", module.isHidden());
            moduleInformation.addProperty("keybind", module.getKeycode());
        }
        return moduleInformation;
    }

    private static boolean shouldSaveModuleStateOnly(Module module) {
        return module instanceof Relationships;
    }

    public void loadProfile(String name) {
        Profile existingProfile = this.getProfile(name);
        String profileName = existingProfile != null ? existingProfile.getName() : this.normalizeProfileName(name);
        boolean foundProfile = false;
        for (File file : this.getProfileFiles()) {
            if (!file.exists()) {
                this.failedMessage("load", profileName);
                System.out.println("Failed to load " + profileName);
                return;
            }
            if (file.getName().equals(profileName + ".json")) {
                try (FileReader fileReader = new FileReader(file)) {
                    JsonParser jsonParser = new JsonParser();
                    JsonObject profileJson = jsonParser.parse(fileReader).getAsJsonObject();
                    if (profileJson == null) {
                        this.failedMessage("load", profileName);
                        return;
                    }
                    JsonArray modules = profileJson.getAsJsonArray("modules");
                    if (modules != null) {
                        List<Module> loadableModules = this.getLoadableModules();
                        Map<Module, ProfileManager.RequestedModuleState> requestedModuleStates = this.createDefaultRequestedModuleStates(loadableModules);
                        Map<Module, JsonObject> loadedModuleData = new LinkedHashMap<>();
                        boolean loadedRelationshipsState = false;
                        Map<String, ProfileManager.SavedCategoryState> savedGuiCategoryState = new HashMap<>();
                        for (JsonElement moduleJson : modules) {
                            JsonObject moduleInformation = moduleJson.getAsJsonObject();
                            String moduleName = moduleInformation.get("name").getAsString();
                            if (moduleName != null && !moduleName.isEmpty()) {
                                Module module = ModuleManager.getModule(moduleName);
                                if (module == null && moduleName.startsWith("sc-") && Raven.scriptManager != null) {
                                    for (Module module1 : Raven.scriptManager.scripts.values()) {
                                        if (module1.getName().equals(moduleName.substring(3))) {
                                            module = module1;
                                        }
                                    }
                                }
                                if (module != null) {
                                    loadedModuleData.put(module, moduleInformation);
                                    if (module instanceof Relationships) {
                                        loadedRelationshipsState = true;
                                    }
                                    if (module.canBeEnabled()) {
                                        ProfileManager.RequestedModuleState requestedState = requestedModuleStates.get(module);
                                        if (requestedState == null) {
                                            requestedState = new ProfileManager.RequestedModuleState(false, 0);
                                            requestedModuleStates.put(module, requestedState);
                                        }
                                        if (moduleInformation.has("enabled")) {
                                            requestedState.enabled = moduleInformation.get("enabled").getAsBoolean();
                                        }
                                        if (moduleInformation.has("hidden")) {
                                            requestedState.hidden = moduleInformation.get("hidden").getAsBoolean();
                                        }
                                        if (moduleInformation.has("keybind")) {
                                            requestedState.keybind = moduleInformation.get("keybind").getAsInt();
                                        }
                                    }
                                }
                            }
                        }
                        if (!loadedRelationshipsState && ModuleManager.relationships != null && Raven.playerRelationsManager != null) {
                            ProfileManager.RequestedModuleState relationshipsState = requestedModuleStates.get(ModuleManager.relationships);
                            if (relationshipsState != null) {
                                relationshipsState.enabled = Raven.playerRelationsManager.isActive();
                            }
                        }
                        for (Module module3 : loadableModules) {
                            ProfileManager.RequestedModuleState requestedModuleState = requestedModuleStates.get(module3);
                            if (requestedModuleState != null && !requestedModuleState.enabled && module3.isEnabled()) {
                                module3.disable();
                            }
                        }
                        for (Module module4 : loadableModules) {
                            ProfileManager.RequestedModuleState requestedModuleState2 = requestedModuleStates.get(module4);
                            if (requestedModuleState2 != null) {
                                module4.setBind(requestedModuleState2.keybind);
                                if (requestedModuleState2.hidden != null) {
                                    module4.setHidden(requestedModuleState2.hidden);
                                }
                            }
                        }
                        for (Entry<Module, JsonObject> entry : loadedModuleData.entrySet()) {
                            Module key2 = entry.getKey();
                            JsonObject moduleInformation = entry.getValue();
                            if (key2.getName().equals("HUD")) {
                                if (moduleInformation.has("relPosX") && moduleInformation.has("relPosY")) {
                                    HUD.setRelativePosition(moduleInformation.get("relPosX").getAsFloat(), moduleInformation.get("relPosY").getAsFloat());
                                } else if (moduleInformation.has("posX") || moduleInformation.has("posY")) {
                                    float hudX = moduleInformation.has("posX") ? moduleInformation.get("posX").getAsFloat() : HUD.posX;
                                    float hudY = moduleInformation.has("posY") ? moduleInformation.get("posY").getAsFloat() : HUD.posY;
                                    HUD.setAbsolutePosition(hudX, hudY);
                                }
                            } else if (key2.getName().equals("TargetHUD")) {
                                if (moduleInformation.has("posX")) {
                                    int posX = moduleInformation.get("posX").getAsInt();
                                    ModuleManager.targetHUD.posX = posX;
                                }
                                if (moduleInformation.has("posY")) {
                                    int posY = moduleInformation.get("posY").getAsInt();
                                    ModuleManager.targetHUD.posY = posY;
                                }
                            } else if (key2.getName().equals("Potion HUD")) {
                                PotionHUD potionHUD = (PotionHUD) key2;
                                if (moduleInformation.has("relPosX") && moduleInformation.has("relPosY")) {
                                    potionHUD.setRelativePosition(moduleInformation.get("relPosX").getAsFloat(), moduleInformation.get("relPosY").getAsFloat());
                                } else if (moduleInformation.has("posX") || moduleInformation.has("posY")) {
                                    float posX = moduleInformation.has("posX") ? moduleInformation.get("posX").getAsFloat() : potionHUD.getPosX();
                                    float posY = moduleInformation.has("posY") ? moduleInformation.get("posY").getAsFloat() : potionHUD.getPosY();
                                    potionHUD.setAbsolutePosition(posX, posY);
                                }
                            } else if (key2.getName().equals("Radar")) {
                                Radar radar = (Radar) key2;
                                if (moduleInformation.has("relPosX") && moduleInformation.has("relPosY")) {
                                    radar.setRelativePosition(moduleInformation.get("relPosX").getAsFloat(), moduleInformation.get("relPosY").getAsFloat());
                                } else if (moduleInformation.has("posX") || moduleInformation.has("posY")) {
                                    float posX = moduleInformation.has("posX") ? moduleInformation.get("posX").getAsFloat() : radar.getPosX();
                                    float posY = moduleInformation.has("posY") ? moduleInformation.get("posY").getAsFloat() : radar.getPosY();
                                    radar.setAbsolutePosition(posX, posY);
                                }
                            } else if (key2.getName().equals("Hide Window")) {
                                HideWindow hw = (HideWindow) key2;
                                if (moduleInformation.has("relPosX") && moduleInformation.has("relPosY")) {
                                    hw.setRelativePosition(moduleInformation.get("relPosX").getAsFloat(), moduleInformation.get("relPosY").getAsFloat());
                                } else if (moduleInformation.has("posX") || moduleInformation.has("posY")) {
                                    float posX = moduleInformation.has("posX") ? moduleInformation.get("posX").getAsFloat() : hw.getPosX();
                                    float posY = moduleInformation.has("posY") ? moduleInformation.get("posY").getAsFloat() : hw.getPosY();
                                    hw.setAbsolutePosition(posX, posY);
                                }
                            } else if (key2.getName().equals("Fast Place")) {
                                FastPlace fp = (FastPlace) key2;
                                if (moduleInformation.has("relPosX") && moduleInformation.has("relPosY")) {
                                    fp.setRelativePosition(moduleInformation.get("relPosX").getAsFloat(), moduleInformation.get("relPosY").getAsFloat());
                                } else if (moduleInformation.has("posX") || moduleInformation.has("posY")) {
                                    float posX = moduleInformation.has("posX") ? moduleInformation.get("posX").getAsFloat() : fp.getPosX();
                                    float posY = moduleInformation.has("posY") ? moduleInformation.get("posY").getAsFloat() : fp.getPosY();
                                    fp.setAbsolutePosition(posX, posY);
                                }
                            } else if (key2 instanceof Scaffold) {
                                Scaffold scaffold = (Scaffold) key2;
                                if (moduleInformation.has("relPosX") && moduleInformation.has("relPosY")) {
                                    scaffold.setRelativePosition(moduleInformation.get("relPosX").getAsFloat(), moduleInformation.get("relPosY").getAsFloat());
                                } else if (moduleInformation.has("posX") || moduleInformation.has("posY")) {
                                    float posX = moduleInformation.has("posX") ? moduleInformation.get("posX").getAsFloat() : scaffold.getPosX();
                                    float posY = moduleInformation.has("posY") ? moduleInformation.get("posY").getAsFloat() : scaffold.getPosY();
                                    scaffold.setAbsolutePosition(posX, posY);
                                }
                            } else if (key2 instanceof BedWars) {
                                BedWars bedWars = (BedWars) key2;
                                if (moduleInformation.has("closestEnemyRelPosX") && moduleInformation.has("closestEnemyRelPosY")) {
                                    bedWars.setClosestEnemyRelativePosition(moduleInformation.get("closestEnemyRelPosX").getAsFloat(), moduleInformation.get("closestEnemyRelPosY").getAsFloat());
                                } else if (moduleInformation.has("closestEnemyPosX") || moduleInformation.has("closestEnemyPosY")) {
                                    float posX = moduleInformation.has("closestEnemyPosX") ? moduleInformation.get("closestEnemyPosX").getAsFloat() : bedWars.getClosestEnemyPosX();
                                    float posY = moduleInformation.has("closestEnemyPosY") ? moduleInformation.get("closestEnemyPosY").getAsFloat() : bedWars.getClosestEnemyPosY();
                                    bedWars.setClosestEnemyAbsolutePosition(posX, posY);
                                }
                                if (moduleInformation.has("magicMilkRelPosX") && moduleInformation.has("magicMilkRelPosY")) {
                                    bedWars.setMagicMilkRelativePosition(moduleInformation.get("magicMilkRelPosX").getAsFloat(), moduleInformation.get("magicMilkRelPosY").getAsFloat());
                                } else if (moduleInformation.has("magicMilkPosX") || moduleInformation.has("magicMilkPosY")) {
                                    float posX = moduleInformation.has("magicMilkPosX") ? moduleInformation.get("magicMilkPosX").getAsFloat() : bedWars.getMagicMilkPosX();
                                    float posY = moduleInformation.has("magicMilkPosY") ? moduleInformation.get("magicMilkPosY").getAsFloat() : bedWars.getMagicMilkPosY();
                                    bedWars.setMagicMilkAbsolutePosition(posX, posY);
                                }
                            } else if (key2 instanceof SkyWars) {
                                SkyWars skyWars = (SkyWars) key2;
                                if (moduleInformation.has("closestEnemyRelPosX") && moduleInformation.has("closestEnemyRelPosY")) {
                                    skyWars.setClosestEnemyRelativePosition(moduleInformation.get("closestEnemyRelPosX").getAsFloat(), moduleInformation.get("closestEnemyRelPosY").getAsFloat());
                                } else if (moduleInformation.has("closestEnemyPosX") || moduleInformation.has("closestEnemyPosY")) {
                                    float posX = moduleInformation.has("closestEnemyPosX") ? moduleInformation.get("closestEnemyPosX").getAsFloat() : skyWars.getClosestEnemyPosX();
                                    float posY = moduleInformation.has("closestEnemyPosY") ? moduleInformation.get("closestEnemyPosY").getAsFloat() : skyWars.getClosestEnemyPosY();
                                    skyWars.setClosestEnemyAbsolutePosition(posX, posY);
                                }
                            } else if (key2.getName().equals("Gui")) {
                                for (Entry<String, JsonElement> setting : moduleInformation.entrySet()) {
                                    String settingName = setting.getKey();
                                    if (Module.categoriesString.contains(settingName)) {
                                        String element = setting.getValue().getAsString();
                                        String[] statesStr = element.split(",");
                                        float posX = Float.parseFloat(statesStr[0]);
                                        float posY = Float.parseFloat(statesStr[1]);
                                        boolean opened = statesStr.length > 2 && Boolean.parseBoolean(statesStr[2]);
                                        savedGuiCategoryState.put(settingName, new ProfileManager.SavedCategoryState(posX, posY, opened));
                                    }
                                }
                            }
                            for (Setting setting2 : key2.getSettings()) {
                                setting2.loadProfile(moduleInformation);
                            }
                        }
                        for (Module module5 : loadableModules) {
                            ProfileManager.RequestedModuleState requestedModuleState3 = requestedModuleStates.get(module5);
                            if (requestedModuleState3 != null && requestedModuleState3.enabled && !module5.isEnabled()) {
                                module5.enable();
                            }
                        }
                        Raven.currentProfile = this.getProfile(profileName);
                        boolean loadGuiPositions = Gui.loadGuiPositions.isToggled();
                        Raven.clickGui.refreshAfterProfileLoad();
                        if (loadGuiPositions) {
                            for (CategoryComponent c : ClickGui.categories) {
                                ProfileManager.SavedCategoryState state = savedGuiCategoryState.get(c.category.name());
                                if (state != null) {
                                    c.applySavedState(state.x, state.y, state.opened, true);
                                }
                            }
                        }
                        MinecraftForge.EVENT_BUS.post(new PostProfileLoadEvent(Raven.currentProfile.getName()));
                        return;
                    }
                    this.failedMessage("load", profileName);
                    return;
                } catch (Exception exception2) {
                    this.failedMessage("load", profileName);
                    exception2.printStackTrace();
                    return;
                }
            }
        }
        if (!foundProfile) {
            this.failedMessage("load", profileName);
        }
    }

    private List<Module> getLoadableModules() {
        List<Module> loadableModules = new ArrayList<>(Raven.getModuleManager().getModules());
        if (Raven.scriptManager != null && Raven.scriptManager.scripts != null) {
            loadableModules.addAll(Raven.scriptManager.scripts.values());
        }
        return loadableModules;
    }

    private Map<Module, ProfileManager.RequestedModuleState> createDefaultRequestedModuleStates(List<Module> modules) {
        Map<Module, ProfileManager.RequestedModuleState> requestedModuleStates = new HashMap<>();
        for (Module module : modules) {
            if (module.canBeEnabled()) {
                requestedModuleStates.put(module, new ProfileManager.RequestedModuleState(false, 0));
            }
        }
        return requestedModuleStates;
    }

    public boolean deleteProfile(String name) {
        Profile removedProfile = this.getProfile(name);
        String profileName = removedProfile != null ? removedProfile.getName() : this.normalizeProfileName(name);
        File profileFile = new File(this.directory, profileName + ".json");
        if (profileFile.exists() && !profileFile.delete() && profileFile.exists()) {
            return false;
        } else {
            boolean wasCurrentProfile = removedProfile != null && Raven.currentProfile == removedProfile;
            if (this.isDefaultProfile(profileName)) {
                this.removeDefaultProfile();
            }
            if (removedProfile != null) {
                this.profiles.remove(removedProfile);
            }
            if (wasCurrentProfile) {
                Raven.currentProfile = null;
            }
            if (this.profiles.isEmpty()) {
                Profile fallbackProfile = this.createProfile("default", 0);
                if (fallbackProfile == null) {
                    return false;
                }
            } else {
                this.refreshProfileModules();
                if (wasCurrentProfile) {
                    Profile fallbackProfile = this.getDefaultOrFirstProfile();
                    if (fallbackProfile != null) {
                        this.loadProfile(fallbackProfile.getName());
                    }
                }
            }
            return removedProfile != null;
        }
    }

    public void loadProfiles(boolean sendMessage) {
        String currentProfileName = Raven.currentProfile != null ? Raven.currentProfile.getName() : null;
        boolean currentProfileSaved = Raven.currentProfile == null || Raven.currentProfile.getModule().saved;
        this.profiles.clear();
        if (!this.directory.exists() && !this.directory.mkdirs()) {
            Utils.sendMessage("&cFailed to load profiles.");
        } else {
            List<File> profileFiles = this.getProfileFiles();
            if (profileFiles.isEmpty()) {
                this.saveProfile(new Profile("default", 0));
                profileFiles = this.getProfileFiles();
            }
            for (File file : profileFiles) {
                try (FileReader fileReader = new FileReader(file)) {
                    JsonParser jsonParser = new JsonParser();
                    JsonObject profileJson = jsonParser.parse(fileReader).getAsJsonObject();
                    String profileName = file.getName().replace(".json", "");
                    if (profileJson == null) {
                        this.failedMessage("load", profileName);
                        return;
                    }
                    int keybind = 0;
                    if (profileJson.has("keybind")) {
                        keybind = profileJson.get("keybind").getAsInt();
                    }
                    Profile profile = new Profile(profileName, keybind);
                    this.profiles.add(profile);
                } catch (Exception exception3) {
                    Utils.sendMessage("&cFailed to load profiles.");
                    exception3.printStackTrace();
                }
            }
            if (currentProfileName != null) {
                Raven.currentProfile = this.getProfile(currentProfileName);
                if (Raven.currentProfile != null) {
                    Raven.currentProfile.getModule().saved = currentProfileSaved;
                }
            }
            this.refreshProfileModules();
            if (sendMessage) {
                Utils.sendMessage("&b" + profileFiles.size() + " &7profiles loaded.");
            }
            if (this.getDefaultProfile() != null) {
                this.loadProfile(this.getDefaultProfile().getName());
                Raven.currentProfile = this.getDefaultProfile();
            }
        }
    }

    public List<File> getProfileFiles() {
        List<File> profileFiles = new ArrayList<>();
        if (this.directory.exists()) {
            File[] files = this.directory.listFiles();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    profileFiles.add(file);
                }
            }
        }
        return profileFiles;
    }

    public Profile getProfile(String name) {
        for (Profile profile : this.profiles) {
            if (profile.getName().equalsIgnoreCase(name)) {
                return profile;
            }
        }
        return null;
    }

    public void loadInitialProfile() {
        Profile defaultProfile = this.getDefaultProfile();
        if (defaultProfile != null) {
            this.loadProfile(defaultProfile.getName());
        } else {
            Raven.currentProfile = null;
        }
    }

    public void failedMessage(String reason, String name) {
        Utils.sendMessage("&cFailed to " + reason + ": &b" + name);
    }

    public boolean renameProfile(Profile profile, String requestedName) {
        if (profile == null) {
            Utils.sendMessage("&cFailed to rename profile.");
            return false;
        } else {
            String oldName = profile.getName();
            String newName = this.normalizeProfileName(requestedName);
            String validationError = this.validateProfileName(newName, oldName);
            if (validationError != null) {
                Utils.sendMessage("&c" + validationError);
                return false;
            } else if (oldName.equals(newName)) {
                profile.setName(newName);
                return true;
            } else {
                File oldFile = new File(this.directory, oldName + ".json");
                File newFile = new File(this.directory, newName + ".json");
                if (!oldFile.exists()) {
                    this.failedMessage("rename", oldName);
                    return false;
                } else {
                    boolean wasDefault = this.isDefaultProfile(oldName);
                    try {
                        Files.move(oldFile.toPath(), newFile.toPath());
                        profile.setName(newName);
                        if (wasDefault) {
                            this.setDefaultProfile(newName);
                        }
                        return true;
                    } catch (Exception exception4) {
                        this.failedMessage("rename", oldName);
                        exception4.printStackTrace();
                        return false;
                    }
                }
            }
        }
    }

    private void refreshProfileModules() {
        if (Raven.clickGui != null && ClickGui.categories != null) {
            for (CategoryComponent categoryComponent : ClickGui.categories) {
                if (categoryComponent.category == Module.category.profiles) {
                    categoryComponent.reloadModules(true);
                    break;
                }
            }
        }
    }

    private Profile getDefaultOrFirstProfile() {
        Profile defaultProfile = this.getDefaultProfile();
        if (defaultProfile != null) {
            return defaultProfile;
        } else {
            Profile fallback = this.getProfile("default");
            if (fallback != null) {
                return fallback;
            } else {
                return this.profiles.isEmpty() ? null : this.profiles.get(0);
            }
        }
    }

    public String getDefaultProfileName() {
        return this.retrieveSetting("default-profile");
    }

    public Profile getDefaultProfile() {
        String defaultName = this.getDefaultProfileName();
        return defaultName != null && !defaultName.isEmpty() ? this.getProfile(defaultName) : null;
    }

    public boolean isDefaultProfile(Profile profile) {
        return profile != null && this.isDefaultProfile(profile.getName());
    }

    public boolean isDefaultProfile(String name) {
        if (name != null && !name.isEmpty()) {
            String defaultName = this.getDefaultProfileName();
            return defaultName != null && defaultName.equalsIgnoreCase(name);
        } else {
            return false;
        }
    }

    public void setDefaultProfile(Profile profile) {
        if (profile != null) {
            this.setDefaultProfile(profile.getName());
        }
    }

    public void setDefaultProfile(String profileName) {
        if (profileName != null && !profileName.isEmpty()) {
            this.setSetting("default-profile", profileName);
        }
    }

    public void removeDefaultProfile() {
        this.setSetting("default-profile", null);
    }

    private void ensureConfigFileExists() throws IOException {
        Path configPath = Paths.get(CONFIG_DIR);
        if (Files.notExists(configPath)) {
            Files.createDirectories(configPath.getParent());
            Files.createFile(configPath);
        }
    }

    private boolean setSetting(String key, String value) {
        if (key != null && !key.isEmpty()) {
            key = key.replace(":", "");
            String entry = key + ": " + (value == null ? "" : value);
            try {
                this.ensureConfigFileExists();
                Path configPath = new File(CONFIG_DIR).toPath();
                List<String> lines = new ArrayList<>(Files.readAllLines(configPath));
                boolean keyExists = false;
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.startsWith(key + ": ")) {
                        if (value != null && !value.isEmpty()) {
                            lines.set(i, entry);
                        } else {
                            lines.remove(i);
                        }
                        keyExists = true;
                        break;
                    }
                }
                if (!keyExists && value != null && !value.isEmpty()) {
                    lines.add(entry);
                }
                Files.write(configPath, lines);
                return true;
            } catch (IOException exception5) {
                return false;
            }
        } else {
            return false;
        }
    }

    private String retrieveSetting(String key) {
        try {
            this.ensureConfigFileExists();
            Path configPath = new File(CONFIG_DIR).toPath();
            for (String line : Files.readAllLines(configPath)) {
                if (line.startsWith(key + ": ")) {
                    return line.substring((key + ": ").length());
                }
            }
        } catch (IOException exception6) {
        }
        return null;
    }

    private String validateProfileName(String profileName, String currentName) {
        if (profileName.isEmpty()) {
            return "Profile name cannot be empty.";
        } else if (!profileName.endsWith(".") && !profileName.endsWith(" ")) {
            for (char c : profileName.toCharArray()) {
                if (c < ' ' || this.containsInvalidProfileChar(c)) {
                    return "Profile name contains invalid characters.";
                }
            }
            for (Profile profile : this.profiles) {
                if (profile.getName().equalsIgnoreCase(profileName) && (currentName == null || !profile.getName().equalsIgnoreCase(currentName))) {
                    return "Profile already exists: " + profileName;
                }
            }
            return null;
        } else {
            return "Profile name cannot end with a space or period.";
        }
    }

    private boolean containsInvalidProfileChar(char c) {
        for (char invalidChar : INVALID_PROFILE_NAME_CHARS) {
            if (invalidChar == c) {
                return true;
            }
        }
        return false;
    }

    private String normalizeProfileName(String name) {
        return name == null ? "" : name.trim();
    }

    private static final class RequestedModuleState {

        boolean enabled;

        int keybind;

        Boolean hidden;

        RequestedModuleState(boolean enabled, int keybind) {
            this.enabled = enabled;
            this.keybind = keybind;
        }
    }

    private static final class SavedCategoryState {

        final float x;

        final float y;

        final boolean opened;

        SavedCategoryState(float x, float y, boolean opened) {
            this.x = x;
            this.y = y;
            this.opened = opened;
        }
    }
}
