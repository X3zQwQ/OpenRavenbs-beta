package keystrokesmod.module.impl.client;

import com.google.gson.JsonObject;
import java.util.Locale;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.TextSetting;
import org.lwjgl.input.Keyboard;

public class ChatCommands extends Module {

    public static final String DEFAULT_PREFIX = ".";

    public final ButtonSetting lowercase;

    public final TextSetting prefix;

    public ChatCommands() {
        super("Chat Commands", Module.category.client);
        this.liteModule = true;
        this.registerSetting(this.prefix = new TextSetting("Prefix", ".", "Type one character...", 1) {

            @Override
            public void loadProfile(JsonObject data) {
                if (data != null) {
                    String profileKey = this.getProfileKey();
                    if (data.has(profileKey) && data.get(profileKey).isJsonPrimitive()) {
                        String value = data.getAsJsonPrimitive(profileKey).getAsString();
                        if (ChatCommands.isValidPrefix(value)) {
                            this.setText(value);
                        }
                    } else {
                        if (data.has("Prefix key") && data.get("Prefix key").isJsonPrimitive()) {
                            try {
                                String text = ChatCommands.characterForKey(data.getAsJsonPrimitive("Prefix key").getAsInt());
                                if (ChatCommands.isValidPrefix(text)) {
                                    this.setText(text);
                                }
                            } catch (Exception exception) {
                            }
                        }
                    }
                }
            }
        });
        this.registerSetting(this.lowercase = new ButtonSetting("Lowercase", false));
    }

    public boolean lowercase() {
        return this.lowercase.isToggled();
    }

    public String getPrefix() {
        return isValidPrefix(this.prefix.getText()) ? this.prefix.getText() : ".";
    }

    public void setPrefix(String prefix) {
        if (isValidPrefix(prefix)) {
            this.prefix.setText(prefix);
        }
    }

    public static boolean isValidPrefix(String prefix) {
        return prefix != null && prefix.length() == 1 && !Character.isWhitespace(prefix.charAt(0));
    }

    private static String characterForKey(int keyCode) {
        if (keyCode > 0 && keyCode < 256) {
            String keyName = Keyboard.getKeyName(keyCode);
            return keyName != null && keyName.length() == 1 && !Character.isWhitespace(keyName.charAt(0)) ? keyName.toLowerCase(Locale.ROOT) : null;
        } else {
            return null;
        }
    }
}
