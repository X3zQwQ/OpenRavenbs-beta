package keystrokesmod.command.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Track extends Command {

    public final List<EntityPlayer> trackedPlayers = new ArrayList<>();

    public Track() {
        super("track");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() != 0) {
            if (input.argumentCount() != 1) {
                this.syntaxError();
            } else {
                String playerName = input.getArgument(0);
                if ("clear".equalsIgnoreCase(playerName)) {
                    this.replyWithHeader("&b" + this.trackedPlayers.size() + " &7player" + (this.trackedPlayers.size() == 1 ? "" : "s") + " cleared.");
                    this.trackedPlayers.clear();
                } else {
                    EntityPlayer player = mc.field_71441_e.func_72924_a(playerName);
                    if (player == mc.field_71439_g) {
                        this.replyWithHeader("&cYou cannot track yourself.");
                    } else if (player == null) {
                        this.replyWithHeader("&b" + playerName + " &7not found.");
                    } else if (this.trackedPlayers.contains(player)) {
                        this.trackedPlayers.remove(player);
                        this.replyWithHeader("&7Stopped tracking &b" + playerName);
                    } else {
                        this.trackedPlayers.add(player);
                        this.replyWithHeader("&7Started tracking &b" + playerName);
                    }
                }
            }
        } else if (this.trackedPlayers.isEmpty()) {
            this.replyWithHeader("&b0 &7players tracked.");
        } else {
            this.replyWithHeader("&7Tracking &b" + this.trackedPlayers.size() + " &7player" + (this.trackedPlayers.size() == 1 ? "" : "s") + ".");
            for (EntityPlayer player : this.trackedPlayers) {
                this.replyWithHeader(" &b" + player.func_70005_c_());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (Utils.nullCheck() && !this.trackedPlayers.isEmpty()) {
            for (EntityPlayer player : new ArrayList<>(this.trackedPlayers)) {
                if (player != null && !player.field_70128_L && player.field_70725_aQ == 0 && (mc.field_71439_g == player || !AntiBot.isBot(player)) && (ModuleManager.murderMystery == null || !ModuleManager.murderMystery.isEnabled() || ModuleManager.murderMystery.isEmpty())) {
                    RenderUtils.renderEntity(player, 2, 0.0, 0.0, Color.RED.getRGB(), false);
                }
            }
        }
    }
}
