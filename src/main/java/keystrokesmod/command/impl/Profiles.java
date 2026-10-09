package keystrokesmod.command.impl;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.Raven;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.Profile;

public class Profiles extends Command {

    public Profiles() {
        super("profiles", "profile", "p");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() == 0) {
            List<Profile> profiles = Raven.profileManager.profiles;
            if (profiles.isEmpty()) {
                this.replyWithHeader("&7No profiles found");
            } else {
                this.replyWithHeader("&b" + profiles.size() + " &7profile" + (profiles.size() == 1 ? "" : "s") + " loaded.");
                for (Profile profile : profiles) {
                    this.replyWithHeader(" &7" + profile.getName() + (profile == Raven.currentProfile ? " &7(&bcurrent&7)" : ""));
                }
            }
        } else {
            String subCommand = input.getArgument(0).toLowerCase();
            switch(subCommand) {
                case "save":
                case "s":
                    this.handleSave(input);
                    return;
                case "load":
                case "l":
                    this.handleLoad(input);
                    return;
                case "delete":
                case "remove":
                case "r":
                    this.handleDelete(input);
                    return;
                case "rename":
                    this.handleRename(input);
                    return;
                default:
                    this.syntaxError();
            }
        }
    }

    @Override
    public List<String> suggest(CommandInput input) {
        if (input.argumentCount() == 1) {
            return this.filterSuggestions(input, "save", "s", "load", "l", "delete", "remove", "r", "rename");
        } else {
            String subCommand = input.getArgument(0);
            if (this.isLoadOrDeleteCommand(subCommand)) {
                return this.suggestProfileNames(input.joinArguments(1));
            } else {
                return (List<String>) (this.isRenameCommand(subCommand) && input.argumentCount() == 2 ? this.suggestProfileNames(input.joinArguments(1)) : new ArrayList<>());
            }
        }
    }

    @Override
    public int getSuggestionArgumentStart(CommandInput input) {
        String subCommand = input.getArgument(0);
        return !this.isLoadOrDeleteCommand(subCommand) && !this.isRenameCommand(subCommand) ? super.getSuggestionArgumentStart(input) : 1;
    }

    private void handleSave(CommandInput input) {
        if (input.argumentCount() < 2) {
            if (Raven.currentProfile != null) {
                Utils.sendMessage("&7Saved profile: &b" + Raven.currentProfile);
                Raven.profileManager.saveProfile(Raven.currentProfile);
            } else {
                this.syntaxError();
            }
        } else {
            String saveName = input.joinArguments(1);
            Profile savedProfile = Raven.profileManager.getProfile(saveName);
            if (savedProfile == null) {
                savedProfile = Raven.profileManager.createProfile(saveName, 0);
                if (savedProfile == null) {
                    return;
                }
            } else {
                Raven.profileManager.saveProfile(savedProfile);
            }
            this.replyWithHeader("&7Saved profile: &b" + saveName);
        }
    }

    private void handleLoad(CommandInput input) {
        if (input.argumentCount() < 2) {
            this.syntaxError();
        } else {
            String loadName = input.joinArguments(1);
            if (Raven.profileManager.getProfile(loadName) == null) {
                this.replyWithHeader("&b" + loadName + " &7does not exist");
            } else {
                Raven.profileManager.loadProfile(loadName);
                this.replyWithHeader("&7Enabled profile: &b" + loadName);
            }
        }
    }

    private void handleDelete(CommandInput input) {
        if (input.argumentCount() < 2) {
            this.syntaxError();
        } else {
            String deleteName = input.joinArguments(1);
            if (Raven.profileManager.getProfile(deleteName) == null) {
                this.replyWithHeader("&cProfile &b" + deleteName + " &7does not exist");
            } else {
                Raven.profileManager.deleteProfile(deleteName);
                this.replyWithHeader("&7Removed profile: &b" + deleteName);
                Raven.profileManager.loadProfiles(true);
            }
        }
    }

    private void handleRename(CommandInput input) {
        if (input.argumentCount() < 3) {
            this.syntaxError();
        } else {
            Profiles.RenameArguments renameArguments = this.parseRenameArguments(input.joinArguments(1));
            if (renameArguments == null) {
                this.replyWithHeader("&7Rename requires an existing profile name followed by the new name.");
            } else {
                String oldName = renameArguments.oldName;
                String newName = renameArguments.newName;
                Profile oldProfile = Raven.profileManager.getProfile(oldName);
                if (oldProfile == null) {
                    this.replyWithHeader("&b" + oldName + " &7does not exist");
                } else if (Raven.profileManager.getProfile(newName) != null) {
                    this.replyWithHeader("&b" + newName + " &7already exists");
                } else if (Raven.profileManager.renameProfile(oldProfile, newName)) {
                    this.replyWithHeader("&b" + oldName + " &7renamed to &b" + oldProfile.getName());
                }
            }
        }
    }

    private List<String> suggestProfileNames(String query) {
        String loweredQuery = query == null ? "" : query.toLowerCase();
        List<String> profileNames = new ArrayList<>();
        for (Profile profile : Raven.profileManager.profiles) {
            if (profile.getName().toLowerCase().startsWith(loweredQuery)) {
                profileNames.add(profile.getName());
            }
        }
        return profileNames;
    }

    private boolean isLoadOrDeleteCommand(String subCommand) {
        return "load".equalsIgnoreCase(subCommand) || "l".equalsIgnoreCase(subCommand) || "delete".equalsIgnoreCase(subCommand) || "remove".equalsIgnoreCase(subCommand) || "r".equalsIgnoreCase(subCommand);
    }

    private boolean isRenameCommand(String subCommand) {
        return "rename".equalsIgnoreCase(subCommand);
    }

    private Profiles.RenameArguments parseRenameArguments(String remaining) {
        if (remaining == null) {
            return null;
        } else {
            String trimmed = remaining.trim();
            if (trimmed.isEmpty()) {
                return null;
            } else {
                String bestMatch = null;
                for (Profile profile : Raven.profileManager.profiles) {
                    String profileName = profile.getName();
                    if (trimmed.length() > profileName.length() && trimmed.regionMatches(true, 0, profileName, 0, profileName.length()) && Character.isWhitespace(trimmed.charAt(profileName.length())) && (bestMatch == null || profileName.length() > bestMatch.length())) {
                        bestMatch = profileName;
                    }
                }
                if (bestMatch == null) {
                    return null;
                } else {
                    String newName = trimmed.substring(bestMatch.length()).trim();
                    return newName.isEmpty() ? null : new Profiles.RenameArguments(bestMatch, newName);
                }
            }
        }
    }

    private static final class RenameArguments {

        private final String oldName;

        private final String newName;

        private RenameArguments(String oldName, String newName) {
            this.oldName = oldName;
            this.newName = newName;
        }
    }
}
