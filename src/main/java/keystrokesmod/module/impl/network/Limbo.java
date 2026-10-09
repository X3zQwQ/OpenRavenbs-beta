package keystrokesmod.module.impl.network;

import keystrokesmod.module.Module;
import keystrokesmod.utility.Utils;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Limbo extends Module {

    private boolean waitingForDisconnectSpam;

    public Limbo() {
        super("Limbo", Module.category.network);
    }

    @Override
    public void onEnable() {
        if (!Utils.isHypixel()) {
            Utils.sendMessage("&cYou are not on hypixel.");
            this.disable();
        } else {
            this.waitingForDisconnectSpam = false;
            for (int i = 0; i < 12; i++) {
                mc.field_71439_g.func_71165_d("/");
            }
        }
    }

    @Override
    public void onDisable() {
        this.waitingForDisconnectSpam = false;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String message = StringUtils.func_76338_a(event.message.func_150260_c());
        if (message.startsWith("Unknown command. Type \"help\" for help. (")) {
            event.setCanceled(true);
            this.waitingForDisconnectSpam = true;
        } else {
            if (this.waitingForDisconnectSpam && message.contains("disconnect.spam")) {
                this.waitingForDisconnectSpam = false;
                event.setCanceled(true);
                this.disable();
            }
        }
    }
}
