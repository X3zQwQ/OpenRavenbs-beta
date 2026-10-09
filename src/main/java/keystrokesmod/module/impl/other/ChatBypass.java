package keystrokesmod.module.impl.other;

import java.util.Arrays;
import java.util.List;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatBypass extends Module {

    private ButtonSetting filterKnownWords;

    private List<String> filteredWords = Arrays.asList("kill", "retard", "anal", "beaner", "bestiality", "blowjob", "cameltoe", "chink", "clit", "cock", "coon", "cunnilingus", "cunt", "dick", "dildo", "dilf", "dyke", "ejaculate", "ejaculati", "fag", "foreskin", "gilf", "hentai", "jerkoff", "jizz", "kike", "kill yourself", "kill urself", "kys", "loli", "masturbate", "masturbati", "milf", "nazi", "nigga", "nigger", "orgy", "pedo", "penis", "porn", "pussy", "rape", "raping", "redtube", "retard", "schlong", "shemale", "sex", "swastika", "tits", "titties", "trannie", "tranny", "vagina", "whore", "xhamster", "xvideos", "end", "arse", "ass", "bastard", "bitch", "boob", "douche", "fuck", "hitler", "shit", "twat", "wank");

    private List<String> allowedCommands = Arrays.asList("ac", "achat", "pc", "pchat", "gc", "gchat", "shout", "msg", "message", "r", "reply", "t", "tell", "w", "whisper");

    private String replace_a = "á";

    private String replace_e = "é";

    private String replace_i = "¡";

    private String replace_o = "ó";

    private String replace_u = "ú";

    private String replace_y = "ÿ";

    private String replace_A = "Á";

    private String replace_E = "É";

    private String replace_I = this.replace_i;

    private String replace_O = "Ó";

    private String replace_U = "Ú";

    private String replace_Y = this.replace_y;

    public ChatBypass() {
        super("Chat Bypass", Module.category.other);
        this.registerSetting(this.filterKnownWords = new ButtonSetting("Only filter known words", true));
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (Utils.nullCheck()) {
            if (e.getPacket() instanceof C01PacketChatMessage) {
                C01PacketChatMessage c01 = (C01PacketChatMessage) e.getPacket();
                String msg = c01.func_149439_c();
                String[] split = this.splitCommand(msg);
                if (split != null && !split[1].isEmpty()) {
                    msg = split[1];
                    if (this.filterKnownWords.isToggled()) {
                        StringBuilder newMsg = new StringBuilder();
                        String[] words = msg.split(" ");
                        for (String word : words) {
                            String lowerCaseWord = word.toLowerCase();
                            for (String filteredWord : this.filteredWords) {
                                int index = lowerCaseWord.indexOf(filteredWord.toLowerCase());
                                if (index != -1) {
                                    String matched = word.substring(index, index + filteredWord.length());
                                    String replaced = this.doReplace(matched);
                                    word = word.substring(0, index) + replaced + word.substring(index + filteredWord.length());
                                }
                            }
                            newMsg.append(word).append(" ");
                        }
                        msg = newMsg.toString().trim();
                    } else {
                        msg = this.doReplace(msg);
                    }
                    if (split[0] != null) {
                        msg = split[0] + " " + msg;
                    }
                    PacketUtils.sendPacketNoEvent(new C01PacketChatMessage(msg));
                    e.setCanceled(true);
                }
            }
        }
    }

    private String[] splitCommand(String msg) {
        if (msg.startsWith("/")) {
            if (!this.isValidCommand(msg)) {
                return null;
            }
            int intValue = msg.indexOf(" ");
            if (intValue != -1) {
                return new String[] { msg.substring(0, intValue), msg.substring(intValue + 1) };
            }
        }
        return new String[] { null, msg };
    }

    private String doReplace(String text) {
        return text.replace("a", this.replace_a).replace("e", this.replace_e).replace("i", this.replace_i).replace("o", this.replace_o).replace("u", this.replace_u).replace("y", this.replace_y).replace("A", this.replace_A).replace("E", this.replace_E).replace("I", this.replace_I).replace("O", this.replace_O).replace("U", this.replace_U).replace("Y", this.replace_Y);
    }

    private boolean isValidCommand(String msg) {
        for (String cmd : this.allowedCommands) {
            String _cmd = "/" + cmd + " ";
            if (msg.startsWith(_cmd)) {
                return true;
            }
        }
        return false;
    }
}
