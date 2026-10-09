package keystrokesmod.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import org.lwjgl.input.Keyboard;

public class Bind extends Command {

    public Bind() {
        super("bind", "b");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() < 2) {
            this.syntaxError();
        } else {
            String action = input.getArgument(0);
            String moduleQuery;
            String keyToken;
            if (!"add".equalsIgnoreCase(action) && !"remove".equalsIgnoreCase(action)) {
                keyToken = input.getArgument(input.argumentCount() - 1);
                moduleQuery = this.joinArguments(input.getArguments(), 0, input.argumentCount() - 1);
            } else {
                if ("remove".equalsIgnoreCase(action)) {
                    String removeQuery = input.joinArguments(1);
                    Module module = this.findModule(removeQuery);
                    if (module == null && input.argumentCount() > 2) {
                        removeQuery = this.joinArguments(input.getArguments(), 1, input.argumentCount() - 1);
                        module = this.findModule(removeQuery);
                    }
                    if (module == null) {
                        this.replyWithHeader("&cModule not found.");
                        return;
                    }
                    module.setBind(0);
                    this.replyWithHeader("&7Unbound &b" + module.getName() + "&7.");
                    return;
                }
                if (input.argumentCount() < 3) {
                    this.syntaxError();
                    return;
                }
                keyToken = input.getArgument(input.argumentCount() - 1);
                moduleQuery = this.joinArguments(input.getArguments(), 1, input.argumentCount() - 1);
            }
            Module module2 = this.findModule(moduleQuery);
            if (module2 == null) {
                this.replyWithHeader("&cModule not found.");
            } else {
                int keycode = this.parseKey(keyToken);
                if (keycode == 0) {
                    this.replyWithHeader("&7Invalid key.");
                } else {
                    module2.setBind(keycode);
                    this.replyWithHeader("&7Bound &b" + module2.getName() + " &7to &b" + this.keyName(keycode) + "&7.");
                }
            }
        }
    }

    @Override
    public List<String> suggest(CommandInput input) {
        if (input.argumentCount() <= 1) {
            List<String> suggestions = new ArrayList<>(Arrays.asList("add", "remove"));
            suggestions.addAll(this.moduleSuggestions(input.argumentCount() == 0 ? "" : input.joinArguments(0)));
            return this.filterSuggestions(input, suggestions);
        } else {
            String first = input.getArgument(0);
            if ("add".equalsIgnoreCase(first)) {
                if (input.argumentCount() == 2) {
                    return this.moduleSuggestions(input.getArgument(1));
                }
                if (input.argumentCount() >= 3) {
                    return this.filterSuggestions(input, "R", "F", "G", "V", "NONE");
                }
            } else {
                if ("remove".equalsIgnoreCase(first)) {
                    return this.moduleSuggestions(input.joinArguments(1));
                }
                if (input.argumentCount() >= 2) {
                    return this.filterSuggestions(input, "R", "F", "G", "V", "NONE");
                }
            }
            return new ArrayList<>();
        }
    }

    @Override
    public int getSuggestionArgumentStart(CommandInput input) {
        return Math.max(0, input.argumentCount() - 1);
    }

    private Module findModule(String query) {
        String normalizedQuery = this.normalize(query);
        for (Module module : ModuleManager.modules) {
            if (this.normalize(module.getName()).equals(normalizedQuery)) {
                return module;
            }
        }
        return null;
    }

    private List<String> moduleSuggestions(String query) {
        List<String> completions = new ArrayList<>();
        String normalized = this.normalize(query);
        for (Module module : ModuleManager.modules) {
            String moduleName = module.getName();
            if (this.normalize(moduleName).startsWith(normalized)) {
                completions.add(moduleName);
            }
        }
        return completions;
    }

    private int parseKey(String keyName) {
        if (keyName == null) {
            return 0;
        } else {
            return !"NONE".equalsIgnoreCase(keyName) && !"UNBOUND".equalsIgnoreCase(keyName) ? Keyboard.getKeyIndex(keyName.toUpperCase()) : 0;
        }
    }

    private String keyName(int keycode) {
        return keycode == 0 ? "NONE" : Keyboard.getKeyName(keycode);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace(" ", "").toLowerCase();
    }

    private String joinArguments(String[] values, int startInclusive, int endExclusive) {
        if (values != null && startInclusive < endExclusive) {
            StringBuilder builder = new StringBuilder();
            for (int i = startInclusive; i < endExclusive; i++) {
                if (i > startInclusive) {
                    builder.append(' ');
                }
                builder.append(values[i]);
            }
            return builder.toString();
        } else {
            return "";
        }
    }
}
