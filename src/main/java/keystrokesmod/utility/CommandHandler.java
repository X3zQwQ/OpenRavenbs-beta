package keystrokesmod.utility;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.helper.PingHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.impl.minigames.DuelsStats;
import keystrokesmod.module.impl.movement.BHop;
import keystrokesmod.module.impl.movement.Fly;
import keystrokesmod.module.impl.movement.Speed;
import keystrokesmod.module.impl.other.FakeChat;
import keystrokesmod.module.impl.other.NameHider;
import keystrokesmod.utility.profile.Profile;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

public class CommandHandler implements IMinecraftInstance {

    private static final int GL_SCISSOR_TEST = 3089;

    private static final int COMMAND_PANEL_X_OFFSET = 195;

    private static final int COMMAND_PANEL_Y_OFFSET = 130;

    private static final int COMMAND_PANEL_START_Y = 345;

    private static final int COMMAND_PANEL_HEIGHT = 230;

    private static final int MIN_SCISSOR_WIDTH = 2;

    private static boolean hasShownWelcome = true;

    private static final List<Integer> backgroundColors = Arrays.asList(new Color(170, 107, 148, 50).getRGB(), new Color(122, 158, 134, 50).getRGB(), new Color(16, 16, 16, 50).getRGB(), new Color(64, 114, 148, 50).getRGB());

    private static int currentBackgroundColor = 0;

    private static int lastBackgroundColorIndex = -1;

    public static List<String> responseLines = new ArrayList<>();

    private static final String INVALID_SYNTAX = "&cInvalid syntax.";

    private static final String INVALID_COMMAND = "&cInvalid command.";

    private static boolean isArgsValid(String[] args, int requiredCount) {
        return args != null && args.length == requiredCount;
    }

    private static void handleSetKeyCommand(String[] args) {
        if (!isArgsValid(args, 2)) {
            print("&cInvalid syntax.", 1);
        } else {
            print("Setting...", 1);
            String apiKey = args[1];
            Raven.getScheduledExecutor().execute(() -> {
                if (NetworkUtils.isHypixelKeyValid(apiKey)) {
                    NetworkUtils.API_KEY = apiKey;
                    print("&asuccess!", 0);
                } else {
                    print("&cInvalid key.", 0);
                }
            });
        }
    }

    private static void handleNickCommand(String[] args) {
        if (!isArgsValid(args, 2)) {
            print("&cInvalid syntax.", 1);
        } else if (args[1].equals("reset")) {
            print("&aNick reset.", 1);
        } else {
            DuelsStats.nick = args[1];
            print("&aNick has been set to:", 1);
            print("\"" + DuelsStats.nick + "\"", 0);
        }
    }

    private static void handleNameHiderCommand(String[] args) {
        if (args != null && args.length >= 2) {
            NameHider.setFakeName(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            print("&aName has been set to:", 1);
            print("\"" + NameHider.fakeName + "\"", 0);
        } else {
            print("&cInvalid syntax.", 1);
        }
    }

    private static void handleFakeChatCommand(String contents) {
        String message = contents.replaceFirst("fakechat", "").substring(1);
        if (!message.isEmpty() && !message.equals("\\n")) {
            FakeChat.msg = message;
            print("&aMessage set!", 1);
        } else {
            print("&cInvalid message.", 1);
        }
    }

    private static void handleDuelsCommand(String[] args) {
        if (!isArgsValid(args, 2)) {
            print("&cInvalid syntax.", 1);
        } else if (NetworkUtils.API_KEY.isEmpty()) {
            print("&cAPI Key is empty!", 1);
            print("Use \"setkey [api_key]\".", 0);
        } else {
            String playerName = args[1];
            print("Retrieving data...", 1);
            Raven.getScheduledExecutor().execute(() -> {
                int[] stats = ProfileUtils.getHypixelStats(playerName, ProfileUtils.DM.OVERALL);
                if (stats != null) {
                    if (stats[0] == -1) {
                        String displayName = playerName.length() > 16 ? playerName.substring(0, 16) + "..." : playerName;
                        print("&c" + displayName + " does not exist!", 0);
                    } else {
                        double winLossRatio = stats[1] != 0 ? Utils.round((double) stats[0] / stats[1], 2) : stats[0];
                        print("&e" + playerName + " stats:", 1);
                        print("Wins: " + stats[0], 0);
                        print("Losses: " + stats[1], 0);
                        print("WLR: " + winLossRatio, 0);
                        print("Winstreak: " + stats[2], 0);
                        print("Threat: " + DuelsStats.gtl(stats[0], stats[1], winLossRatio, stats[2]).substring(2), 0);
                    }
                } else {
                    print("&cThere was an error.", 0);
                }
            });
        }
    }

    private static void handleSetSpeedCommand(String[] args) {
        if (args != null && args.length == 3) {
            double value;
            try {
                value = Double.parseDouble(args[2]);
            } catch (Exception exception) {
                print("&cInvalid value. [0 - 100]", 1);
                return;
            }
            if (!(value > 100.0) && !(value < 0.0)) {
                String e = args[1];
                switch(e) {
                    case "fly":
                        Fly.horizontalSpeed.setValueRawWithEvent(value);
                        break;
                    case "bhop":
                        BHop.speedSetting.setValueRawWithEvent(value);
                        break;
                    case "speed":
                        Speed.multiplier.setValueRawWithEvent(value);
                        break;
                    default:
                        print("&cInvalid syntax.", 1);
                        return;
                }
                print("&aSet speed to ", 1);
                print(args[2], 0);
            } else {
                print("&cInvalid value. [0 - 100]", 1);
            }
        } else {
            print("&cInvalid syntax.", 1);
        }
    }

    private static void handleSetVelocityCommand(String[] args) {
        if (args != null && args.length == 3) {
            double value;
            try {
                value = Double.parseDouble(args[2]);
            } catch (Exception exception2) {
                print("&cInvalid value. [-100 - 300]", 1);
                return;
            }
            if (!(value > 300.0) && !(value < -100.0)) {
                String e = args[1];
                switch(e) {
                    case "horizontal":
                    case "h":
                        Velocity.horizontal.setValueRawWithEvent(value);
                        break;
                    case "vertical":
                    case "v":
                        Velocity.vertical.setValueRawWithEvent(value);
                        break;
                    default:
                        print("&cInvalid syntax.", 1);
                        return;
                }
                print("&aSet " + args[1] + " velocity to ", 1);
                print(args[2], 0);
            } else {
                print("&cInvalid value. [-100 - 300]", 1);
            }
        } else {
            print("&cInvalid syntax.", 1);
        }
    }

    private static void handleHideCommand(String[] args) {
        if (args != null && args.length == 2) {
            for (Module module : Raven.getModuleManager().getModules()) {
                String moduleName = module.getName().toLowerCase().replace(" ", "");
                if (moduleName.equals(args[1].toLowerCase())) {
                    module.setHidden(true);
                    print("&a" + module.getName() + " is now hidden in HUD", 1);
                }
            }
        } else {
            print("&cInvalid syntax.", 1);
        }
    }

    private static void handleShowCommand(String[] args) {
        if (!isArgsValid(args, 2)) {
            print("&cInvalid syntax.", 1);
        } else {
            for (Module module : Raven.getModuleManager().getModules()) {
                String moduleName = module.getName().toLowerCase().replace(" ", "");
                if (moduleName.equals(args[1].toLowerCase())) {
                    module.setHidden(false);
                    print("&a" + module.getName() + " is now visible in HUD", 1);
                }
            }
        }
    }

    private static void handleFriendCommand(String[] args) {
        if (!isArgsValid(args, 2)) {
            print("&cInvalid syntax.", 1);
        } else if (args[1].equals("clear")) {
            Raven.playerRelationsManager.clearFriends();
            print("&aFriends cleared.", 1);
        } else {
            boolean added = Utils.addFriend(args[1]);
            if (added) {
                print("&aAdded friend: " + args[1], 1);
            } else {
                print("&aRemoved friend: " + args[1], 1);
                Utils.removeFriend(args[1]);
            }
        }
    }

    private static void handleEnemyCommand(String[] args) {
        if (!isArgsValid(args, 2)) {
            print("&cInvalid syntax.", 1);
        } else if (args[1].equals("clear")) {
            Raven.playerRelationsManager.clearEnemies();
            print("&aEnemies cleared.", 1);
        } else {
            boolean added = Utils.addEnemy(args[1]);
            if (!added) {
                print("&aRemoved enemy: " + args[1], 1);
                Utils.removeEnemy(args[1]);
            } else {
                print("&aAdded enemy: " + args[1], 1);
            }
        }
    }

    private static void handleProfilesCommand(String[] args, boolean hasArgs) {
        if (!hasArgs) {
            print("&aAvailable profiles:", 1);
            if (Raven.profileManager.profiles.isEmpty()) {
                print("None", 0);
                return;
            }
            for (int i = 0; i < Raven.profileManager.profiles.size(); i++) {
                print(i + 1 + ". " + Raven.profileManager.profiles.get(i).getName(), 0);
            }
        } else if (args != null && args.length > 1) {
            String text = args[1];
            switch(text) {
                case "save":
                case "s":
                    if (args.length != 3) {
                        print("&cInvalid syntax.", 1);
                        return;
                    }
                    String profileName = args[2];
                    if (profileName.length() < 2 || profileName.length() > 10) {
                        print("&cInvalid name.", 1);
                        return;
                    }
                    Raven.profileManager.saveProfile(new Profile(profileName, 0));
                    print("&aSaved profile:", 1);
                    print(profileName, 0);
                    Raven.profileManager.loadProfiles(true);
                    break;
                case "load":
                case "l":
                    if (args.length != 3) {
                        print("&cInvalid syntax.", 1);
                        return;
                    }
                    String loadedProfileName = args[2];
                    for (Profile loadedProfile : Raven.profileManager.profiles) {
                        if (loadedProfile.getName().equals(loadedProfileName)) {
                            Raven.profileManager.loadProfile(loadedProfile.getName());
                            print("&aLoaded profile:", 1);
                            print(loadedProfileName, 0);
                            if (Settings.sendMessage.isToggled()) {
                                Utils.sendMessage("&7Enabled profile: &b" + loadedProfileName);
                            }
                            return;
                        }
                    }
                    print("&cInvalid profile.", 1);
                    break;
                case "remove":
                case "r":
                    if (args.length != 3) {
                        print("&cInvalid syntax.", 1);
                        return;
                    }
                    String removedProfileName = args[2];
                    for (Profile profile : Raven.profileManager.profiles) {
                        if (profile.getName().equals(removedProfileName)) {
                            Raven.profileManager.deleteProfile(profile.getName());
                            print("&aRemoved profile:", 1);
                            print(removedProfileName, 0);
                            Raven.profileManager.loadProfiles(true);
                            return;
                        }
                    }
                    print("&cInvalid profile.", 1);
            }
        }
    }

    public static void runCommand(String contents) {
        if (!contents.isEmpty()) {
            String command = contents.toLowerCase();
            boolean hasArgs = contents.contains(" ");
            String[] args = hasArgs ? contents.split(" ") : null;
            String commandName = args != null && args.length > 0 ? args[0] : command;
            switch(commandName) {
                case "setkey":
                    handleSetKeyCommand(args);
                    break;
                case "nick":
                    handleNickCommand(args);
                    break;
                case "namehider":
                    handleNameHiderCommand(args);
                    break;
                case "fakechat":
                    handleFakeChatCommand(contents);
                    break;
                case "duels":
                    handleDuelsCommand(args);
                    break;
                case "setspeed":
                    handleSetSpeedCommand(args);
                    break;
                case "setvelocity":
                    handleSetVelocityCommand(args);
                    break;
                case "ping":
                    PingHelper.checkPing(false);
                    break;
                case "clear":
                    responseLines.clear();
                    break;
                case "hide":
                    handleHideCommand(args);
                    break;
                case "show":
                    handleShowCommand(args);
                    break;
                case "friend":
                case "f":
                    handleFriendCommand(args);
                    break;
                case "enemy":
                case "e":
                    handleEnemyCommand(args);
                    break;
                case "debug":
                    Raven.DEBUG = !Raven.DEBUG;
                    print("Debug " + (Raven.DEBUG ? "enabled" : "disabled") + ".", 1);
                    break;
                case "profiles":
                case "p":
                    handleProfilesCommand(args, hasArgs);
                    break;
                case "help":
                case "?":
                    print("&eAvailable commands:", 1);
                    print("1 setkey [key]", 0);
                    print("2 friend/enemy [name/clear]", 0);
                    print("3 duels [player]", 0);
                    print("4 nick [name/reset]", 0);
                    print("5 ping", 0);
                    print("6 hide/show [module]", 0);
                    print("&eProfiles:", 0);
                    print("1 profiles", 0);
                    print("2 profiles save [profile]", 0);
                    print("3 profiles load [profile]", 0);
                    print("4 profiles remove [profile]", 0);
                    print("&eModule-specific:", 0);
                    print("1 namehider [name]", 0);
                    print("2 fakechat [msg]", 0);
                    print("3 setspeed [fly/bhop/speed] [value]", 0);
                    print("4 setvelocity [h/v] [value]", 0);
                    break;
                default:
                    String commandPreview = command.length() > 5 ? command.substring(0, 5) + "..." : command;
                    print("&cInvalid command. (" + commandPreview + ")", 1);
            }
        }
    }

    public static void print(String message, int type) {
        if (type == 1 || type == 2) {
            responseLines.add("");
        }
        responseLines.add(message);
        if (type == 2 || type == 3) {
            responseLines.add("");
        }
    }

    public static void renderCommandOutput(FontRenderer fontRenderer, int height, int width, double scale) {
        int x = width - 195;
        int y = height - 130;
        int startY = height - 345;
        int panelHeight = 230;
        GL11.glEnable(3089);
        double maxWidth = width * scale;
        int scissorWidth = (int) (maxWidth - (maxWidth < 2.0 ? 0 : 2));
        int scissorHeight = (int) (panelHeight * scale - 2.0);
        int scissorY = (int) (mc.field_71440_d - (startY + panelHeight) * scale);
        GL11.glScissor(0, scissorY, scissorWidth, scissorHeight);
        RenderUtils.db(1000, 1000, currentBackgroundColor);
        renderResponseStrings(fontRenderer, responseLines, x, y);
        GL11.glDisable(3089);
    }

    private static void renderResponseStrings(FontRenderer fontRenderer, List<String> responseLines, int x, int y) {
        if (hasShownWelcome) {
            hasShownWelcome = false;
            print("Welcome,", 0);
            print("Use \"help\" for help.", 0);
        }
        if (!responseLines.isEmpty()) {
            for (int i = responseLines.size() - 1; i >= 0; i--) {
                String line = responseLines.get(i);
                int color = -1;
                if (line.contains("&a")) {
                    line = line.replace("&a", "");
                    color = Color.green.getRGB();
                } else if (line.contains("&c")) {
                    line = line.replace("&c", "");
                    color = Color.red.getRGB();
                } else if (line.contains("&e")) {
                    line = line.replace("&e", "");
                    color = Color.yellow.getRGB();
                }
                fontRenderer.func_78276_b(line, x, y, color);
                y -= fontRenderer.field_78288_b + 5;
            }
        }
    }

    public static void setBackgroundColor() {
        int intValue = Utils.getRandom().nextInt(backgroundColors.size());
        if (intValue == lastBackgroundColorIndex) {
            intValue += intValue == 3 ? -3 : 1;
        }
        lastBackgroundColorIndex = intValue;
        currentBackgroundColor = backgroundColors.get(intValue);
    }

    public static void onDisable() {
        PingHelper.reset(false);
    }
}
