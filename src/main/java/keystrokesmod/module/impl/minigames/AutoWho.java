package keystrokesmod.module.impl.minigames;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoWho extends Module {

    private ButtonSetting artifical;

    private ButtonSetting hideMessage;

    private ButtonSetting removeBots;

    private int lastTrackedGameStatus = Integer.MIN_VALUE;

    public AutoWho() {
        super("Auto Who", Module.category.minigames);
        this.registerSetting(new DescriptionSetting("Automatically execute /who."));
        this.registerSetting(new DescriptionSetting(Utils.formatColor("Use '&enick [nick]&r' when nicked.")));
        this.registerSetting(this.artifical = new ButtonSetting("Artificial", false));
        this.registerSetting(this.hideMessage = new ButtonSetting("Hide message", false));
        this.registerSetting(this.removeBots = new ButtonSetting("Remove bots", true));
    }

    @Override
    public void onDisable() {
        this.lastTrackedGameStatus = Integer.MIN_VALUE;
    }

    @Override
    public void onUpdate() {
        if (!Utils.nullCheck()) {
            this.lastTrackedGameStatus = Integer.MIN_VALUE;
        } else {
            int trackedGameStatus = this.getTrackedGameStatus();
            if (trackedGameStatus == 2 && this.lastTrackedGameStatus != 2) {
                this.artificial();
            }
            this.lastTrackedGameStatus = trackedGameStatus;
        }
    }

    @SubscribeEvent
    public void onChatReceive(ClientChatReceivedEvent e) {
        if (e.type != 2 && Utils.nullCheck()) {
            String r = Utils.stripColor(e.message.func_150260_c());
            if (!r.isEmpty()) {
                if (this.hideMessage.isToggled() && r.startsWith("ONLINE: ")) {
                    e.setCanceled(true);
                    Utils.log.info("[CHAT] " + r);
                }
            }
        }
    }

    private int getTrackedGameStatus() {
        int bedwarsStatus = Utils.getBedwarsStatus();
        if (bedwarsStatus != -1) {
            return bedwarsStatus;
        } else {
            int skywarsStatus = Utils.getSkyWarsStatus();
            return skywarsStatus != -1 ? skywarsStatus : 0;
        }
    }

    private void artificial() {
        if (this.artifical.isToggled()) {
            String online = this.hideMessage.isToggled() ? "ONLINE: " : "&b&lONLINE: &r";
            for (NetworkPlayerInfo networkPlayerInfo : Utils.getTablist(true)) {
                if (!this.removeBots.isToggled() || networkPlayerInfo.func_178853_c() <= 1) {
                    if (this.hideMessage.isToggled()) {
                        online = online + networkPlayerInfo.func_178845_a().getName() + ", ";
                    } else {
                        online = online + ScorePlayerTeam.func_96667_a(networkPlayerInfo.func_178850_i(), networkPlayerInfo.func_178845_a().getName()) + "�7, ";
                    }
                }
            }
            if (this.hideMessage.isToggled()) {
                Utils.log.info("[CHAT] " + online + mc.field_71439_g.func_70005_c_());
                return;
            }
            Utils.sendRawMessage(online + mc.field_71439_g.func_145748_c_().func_150254_d());
        } else {
            mc.field_71439_g.func_71165_d("/who");
        }
    }
}
