package keystrokesmod.helper;

import keystrokesmod.utility.CommandHandler;
import keystrokesmod.utility.Utils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PingHelper {

    private static boolean sendChat = false;

    private static long sendTime = 0L;

    private static boolean sendChatCC = false;

    private static long sendTimeCC = 0L;

    @SubscribeEvent
    public void onChatMessageRecieved(ClientChatReceivedEvent event) {
        if (sendChat ^ sendChatCC && Utils.nullCheck() && Utils.stripColor(event.message.func_150260_c()).startsWith("Unknown")) {
            event.setCanceled(true);
            this.getPing(sendChatCC);
            sendChat = false;
            sendChatCC = false;
        }
    }

    public static void checkPing(boolean isChat) {
        if (isChat) {
            Utils.sendMessage("&7Checking ping...");
        } else {
            CommandHandler.print("§3Checking...", 1);
        }
        if (sendChat) {
            if (isChat) {
                Utils.sendMessage("&7Please wait before checking again.");
            } else {
                CommandHandler.print("§cPlease wait.", 0);
            }
        } else {
            Utils.mc.field_71439_g.func_71165_d("/...");
            if (isChat) {
                sendChatCC = true;
                sendTimeCC = System.currentTimeMillis();
            } else {
                sendChat = true;
                sendTime = System.currentTimeMillis();
            }
        }
    }

    private void getPing(boolean isChat) {
        int ping = (int) (System.currentTimeMillis() - (isChat ? sendTimeCC : sendTime)) - 20;
        if (ping < 0) {
            ping = 0;
        }
        if (isChat) {
            Utils.sendMessage("&7Your ping: &b" + ping + "&7ms.");
        } else {
            CommandHandler.print("Your ping: " + ping + "ms", 0);
        }
        reset(isChat);
    }

    public static void reset(boolean isChat) {
        if (isChat) {
            sendChatCC = false;
            sendTimeCC = 0L;
        } else {
            sendChat = false;
            sendTime = 0L;
        }
    }
}
