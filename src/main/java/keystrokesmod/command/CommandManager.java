package keystrokesmod.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import keystrokesmod.command.impl.Bind;
import keystrokesmod.command.impl.Binds;
import keystrokesmod.command.impl.Cname;
import keystrokesmod.command.impl.Debug;
import keystrokesmod.command.impl.Enemy;
import keystrokesmod.command.impl.Friend;
import keystrokesmod.command.impl.HideAll;
import keystrokesmod.command.impl.Name;
import keystrokesmod.command.impl.Ping;
import keystrokesmod.command.impl.Prefix;
import keystrokesmod.command.impl.Profiles;
import keystrokesmod.command.impl.Script;
import keystrokesmod.command.impl.ShowAll;
import keystrokesmod.command.impl.Timer;
import keystrokesmod.command.impl.Toggle;
import keystrokesmod.command.impl.Track;
import keystrokesmod.command.impl.Unbind;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.ChatCommands;
import keystrokesmod.utility.Utils;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();

    public final Track trackCommand;

    public CommandManager() {
        this.register(new Ping());
        this.register(new Name());
        this.register(new Toggle());
        this.register(new Bind());
        this.register(new Unbind());
        this.register(new Binds());
        this.register(new Cname());
        this.register(new Debug());
        this.register(new Friend());
        this.register(new Enemy());
        this.register(new Prefix());
        this.register(new Timer());
        this.register(this.trackCommand = new Track());
        this.register(new Script());
        this.register(new Profiles());
        this.register(new ShowAll());
        this.register(new HideAll());
    }

    public boolean handleChatMessage(String message) {
        if (!this.isCommand(message)) {
            return false;
        } else {
            CommandManager.ParsedCommand parsed = this.parse(message, false);
            if (parsed == null) {
                return true;
            } else if (parsed.command == null) {
                Utils.sendMessage(this.formatOutput("&cUnknown command. Use tab to browse command suggestions."));
                return true;
            } else {
                parsed.command.execute(parsed.input);
                return true;
            }
        }
    }

    public String[] getAutoComplete(String message) {
        if (!this.isCommand(message)) {
            return new String[0];
        } else {
            CommandManager.ParsedCommand parsed = this.parse(message, true);
            if (parsed == null) {
                return this.prefixSuggestions("");
            } else if (parsed.command == null) {
                return this.prefixSuggestions(parsed.input.getLabel());
            } else {
                String[] labelSuggestions = this.getCommandLabelSuggestions(message, parsed);
                return labelSuggestions.length > 0 ? labelSuggestions : this.buildAutoCompleteSuggestions(parsed, parsed.command.suggest(parsed.input));
            }
        }
    }

    public String[] getPreviewSuggestions(String message) {
        if (!this.isCommand(message)) {
            return new String[0];
        } else {
            CommandManager.ParsedCommand parsed = this.parse(message, true);
            if (parsed == null) {
                return this.prefixSuggestions("");
            } else if (parsed.command == null) {
                return this.prefixSuggestions(parsed.input.getLabel());
            } else {
                String[] labelSuggestions = this.getCommandLabelSuggestions(message, parsed);
                if (labelSuggestions.length > 0) {
                    return labelSuggestions;
                } else {
                    List<String> suggestions = parsed.command.suggest(parsed.input);
                    if (suggestions != null && !suggestions.isEmpty()) {
                        Set<String> preview = new LinkedHashSet<>();
                        for (String suggestion : suggestions) {
                            if (suggestion != null && !suggestion.isEmpty()) {
                                preview.add(suggestion);
                            }
                        }
                        return preview.toArray(new String[0]);
                    } else {
                        return new String[0];
                    }
                }
            }
        }
    }

    private String[] getCommandLabelSuggestions(String message, CommandManager.ParsedCommand parsed) {
        if (parsed != null && parsed.input.argumentCount() == 0 && !endsWithWhitespace(message)) {
            String[] suggestions = this.prefixSuggestions(parsed.input.getLabel());
            for (String suggestion : suggestions) {
                if (!suggestion.equalsIgnoreCase(message)) {
                    return suggestions;
                }
            }
            return new String[0];
        } else {
            return new String[0];
        }
    }

    public boolean isEnabled() {
        ChatCommands settings = this.getSettings();
        return settings != null && settings.isEnabled();
    }

    public boolean isCommand(String message) {
        return message != null && this.isEnabled() && message.startsWith(this.getPrefix());
    }

    public String getPrefix() {
        ChatCommands settings = this.getSettings();
        return settings != null ? settings.getPrefix() : ".";
    }

    public String formatOutput(String message) {
        ChatCommands settings = this.getSettings();
        return settings != null && settings.lowercase() ? message.toLowerCase() : message;
    }

    private void register(Command command) {
        this.commands.add(command);
    }

    private ChatCommands getSettings() {
        return ModuleManager.chatCommands;
    }

    private CommandManager.ParsedCommand parse(String message, boolean preserveTrailingArgument) {
        String body = this.stripPrefix(message);
        if (body == null) {
            return null;
        } else {
            String normalizedBody = trimLeadingWhitespace(body);
            if (normalizedBody.isEmpty()) {
                return null;
            } else {
                String[] tokens = tokenize(normalizedBody, preserveTrailingArgument && endsWithWhitespace(body));
                if (tokens.length == 0) {
                    return null;
                } else {
                    String label = tokens[0];
                    String[] arguments = tokens.length > 1 ? Arrays.copyOfRange(tokens, 1, tokens.length) : new String[0];
                    return new CommandManager.ParsedCommand(this.findCommand(label), new CommandInput(message, label, arguments));
                }
            }
        }
    }

    private String[] prefixSuggestions(String query) {
        Set<String> suggestions = new LinkedHashSet<>();
        String loweredQuery = query == null ? "" : query.toLowerCase();
        for (Command command : this.commands) {
            if (command.getName().toLowerCase().startsWith(loweredQuery)) {
                suggestions.add(this.getPrefix() + command.getName());
            }
            for (String alias : command.getAliases()) {
                if (alias.toLowerCase().startsWith(loweredQuery)) {
                    suggestions.add(this.getPrefix() + alias);
                }
            }
        }
        return suggestions.toArray(new String[0]);
    }

    private Command findCommand(String label) {
        for (Command command : this.commands) {
            if (command.matches(label)) {
                return command;
            }
        }
        return null;
    }

    private String[] buildAutoCompleteSuggestions(CommandManager.ParsedCommand parsed, List<String> suggestions) {
        if (suggestions != null && !suggestions.isEmpty()) {
            Set<String> normalized = new LinkedHashSet<>();
            int replacementStart = Math.max(0, Math.min(parsed.input.argumentCount(), parsed.command.getSuggestionArgumentStart(parsed.input)));
            for (String suggestion : suggestions) {
                if (suggestion != null && !suggestion.isEmpty()) {
                    StringBuilder completed = new StringBuilder(this.getPrefix()).append(parsed.input.getLabel());
                    for (int index = 0; index < replacementStart; index++) {
                        completed.append(' ').append(parsed.input.getArgument(index));
                    }
                    if (completed.length() > 0) {
                        completed.append(' ');
                    }
                    completed.append(suggestion);
                    normalized.add(completed.toString());
                }
            }
            return normalized.toArray(new String[0]);
        } else {
            return new String[0];
        }
    }

    private String stripPrefix(String message) {
        return !this.isCommand(message) ? null : message.substring(this.getPrefix().length());
    }

    private static String trimLeadingWhitespace(String value) {
        int index = 0;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return value.substring(index);
    }

    private static boolean endsWithWhitespace(String value) {
        return !value.isEmpty() && Character.isWhitespace(value.charAt(value.length() - 1));
    }

    private static String[] tokenize(String value, boolean preserveTrailingEmptyArgument) {
        List<String> tokens = new ArrayList<>();
        int length = value.length();
        int index = 0;
        while (index < length) {
            while (index < length && Character.isWhitespace(value.charAt(index))) {
                index++;
            }
            if (index >= length) {
                break;
            }
            int start = index;
            while (index < length && !Character.isWhitespace(value.charAt(index))) {
                index++;
            }
            tokens.add(value.substring(start, index));
        }
        if (preserveTrailingEmptyArgument) {
            tokens.add("");
        }
        return tokens.toArray(new String[0]);
    }

    private static final class ParsedCommand {

        private final Command command;

        private final CommandInput input;

        private ParsedCommand(Command command, CommandInput input) {
            this.command = command;
            this.input = input;
        }
    }
}
