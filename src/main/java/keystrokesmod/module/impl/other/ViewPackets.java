package keystrokesmod.module.impl.other;

import keystrokesmod.event.NoEventPacketEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.event.HoverEvent.Action;
import net.minecraft.event.HoverEvent;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.network.play.client.C15PacketClientSettings;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class ViewPackets extends Module {

    private ButtonSetting includeCancelled;

    private ButtonSetting singlePlayer;

    private ButtonSetting sent;

    private ButtonSetting ignoreC00;

    private ButtonSetting ignoreC03;

    private ButtonSetting compactC03;

    private ButtonSetting ignoreC0F;

    private ButtonSetting received;

    private Packet packet;

    public static long tick;

    public ViewPackets() {
        super("View Packets", Module.category.other);
        this.registerSetting(this.includeCancelled = new ButtonSetting("Include cancelled", true));
        this.registerSetting(this.singlePlayer = new ButtonSetting("Singleplayer", false));
        this.registerSetting(this.sent = new ButtonSetting("Sent", false));
        this.registerSetting(this.ignoreC00 = new ButtonSetting("Ignore C00", false));
        this.registerSetting(this.ignoreC03 = new ButtonSetting("Ignore C03", false));
        this.registerSetting(this.compactC03 = new ButtonSetting("Compact C03", false));
        this.registerSetting(this.ignoreC0F = new ButtonSetting("Ignore C0F", false));
        this.registerSetting(this.received = new ButtonSetting("Received", false));
    }

    @Override
    public void onDisable() {
        this.packet = null;
        tick = 0L;
    }

    private static String formatBoolean(boolean b) {
        return b ? "&atrue" : "&cfalse";
    }

    private void sendMessage(Packet packet, boolean b) {
        if (Utils.nullCheck()) {
            String s = b ? "&a" + packet.getClass().getSimpleName() : this.applyInfo(packet);
            String string = (this.compactC03.isToggled() && packet instanceof C03PacketPlayer ? "&6" : "&d") + packet.getClass().getSimpleName();
            ChatComponentText chatComponentText = new ChatComponentText(Utils.formatColor("&7[&dR&7]&r &7" + (b ? "Received" : "Sent") + " packet (t:&b" + tick + "&7): "));
            ChatStyle chatStyle = new ChatStyle();
            chatStyle.func_150209_a(new HoverEvent(Action.SHOW_TEXT, new ChatComponentText(Utils.formatColor(s))));
            chatComponentText.func_150257_a(new ChatComponentText(Utils.formatColor(string)).func_150255_a(chatStyle));
            mc.field_71439_g.func_145747_a(chatComponentText);
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (this.sent.isToggled()) {
            if (!this.singlePlayer.isToggled() || !mc.func_71356_B() || e.getPacket().getClass().getSimpleName().charAt(0) != 'S') {
                if (!e.isCanceled() || this.includeCancelled.isToggled()) {
                    if (!this.ignoreC00.isToggled() || !(e.getPacket() instanceof C00PacketKeepAlive)) {
                        if (!this.ignoreC0F.isToggled() || !(e.getPacket() instanceof C0FPacketConfirmTransaction)) {
                            if (!(e.getPacket() instanceof C03PacketPlayer) || !this.ignoreC03.isToggled() && (!this.compactC03.isToggled() || this.packet != null && !(this.packet instanceof C03PacketPlayer))) {
                                this.sendMessage(this.packet = e.getPacket(), false);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onSendPacketNoEvent(NoEventPacketEvent e) {
        if (this.sent.isToggled()) {
            if (!this.singlePlayer.isToggled() || !mc.func_71356_B() || e.getPacket().getClass().getSimpleName().charAt(0) != 'S') {
                if (!e.isCanceled() || this.includeCancelled.isToggled()) {
                    if (!this.ignoreC00.isToggled() || !(e.getPacket() instanceof C00PacketKeepAlive)) {
                        if (!this.ignoreC0F.isToggled() || !(e.getPacket() instanceof C0FPacketConfirmTransaction)) {
                            if (!(e.getPacket() instanceof C03PacketPlayer) || !this.ignoreC03.isToggled() && (!this.compactC03.isToggled() || this.packet != null && !(this.packet instanceof C03PacketPlayer))) {
                                this.sendMessage(this.packet = e.getPacket(), false);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (this.received.isToggled()) {
            if (!this.singlePlayer.isToggled() || !mc.func_71356_B() || e.getPacket().getClass().getSimpleName().charAt(0) != 'C') {
                this.sendMessage(e.getPacket(), true);
            }
        }
    }

    private String applyInfo(Packet packet) {
        String s = "&a" + packet.getClass().getSimpleName();
        if (packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging c07PacketPlayerDigging = (C07PacketPlayerDigging) packet;
            String string = s + "\n&7Status: &b" + c07PacketPlayerDigging.func_180762_c().name() + "\n&7Facing: &b" + c07PacketPlayerDigging.func_179714_b().name();
            BlockPos getPosition = c07PacketPlayerDigging.func_179715_a();
            s = string + "\n&7Position: &b" + getPosition.func_177958_n() + "&7, &b" + getPosition.func_177956_o() + "&7, &b" + getPosition.func_177952_p();
        } else if (packet instanceof C09PacketHeldItemChange) {
            s = s + "\n&7Swap to slot: &b" + ((C09PacketHeldItemChange) packet).func_149614_c();
        } else if (packet instanceof C0BPacketEntityAction) {
            s = s + "\n&7Action: &b" + ((C0BPacketEntityAction) packet).func_180764_b().name() + "\n&7Aux data: &b" + ((C0BPacketEntityAction) packet).func_149512_e();
        } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement c08PacketPlayerBlockPlacement = (C08PacketPlayerBlockPlacement) packet;
            String string2 = s + "\n&7Item: &b" + (c08PacketPlayerBlockPlacement.func_149574_g() == null ? "null" : c08PacketPlayerBlockPlacement.func_149574_g().func_77973_b().getRegistryName().replace("minecraft:", "")) + "\n&7Direction: &b" + c08PacketPlayerBlockPlacement.func_149568_f();
            BlockPos getPosition = c08PacketPlayerBlockPlacement.func_179724_a();
            s = string2 + "\n&7Position: &b" + getPosition.func_177958_n() + "&7, &b" + getPosition.func_177956_o() + "&7, &b" + getPosition.func_177952_p() + "\n&7Offset: &b" + round(c08PacketPlayerBlockPlacement.func_149573_h()) + "&7, &b" + round(c08PacketPlayerBlockPlacement.func_149569_i()) + "&7, &b" + round(c08PacketPlayerBlockPlacement.func_149575_j());
        } else if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity c02PacketUseEntity = (C02PacketUseEntity) packet;
            String string3 = s + "\n&7Action: &b" + c02PacketUseEntity.func_149565_c().name();
            Entity getEntityFromWorld = c02PacketUseEntity.func_149564_a(mc.field_71441_e);
            String string4 = string3 + "\n&7Target: &b" + (getEntityFromWorld == null ? "null" : getEntityFromWorld.func_70005_c_());
            Vec3 getHitVec = c02PacketUseEntity.func_179712_b();
            if (getHitVec == null) {
                s = string4 + "\n&7Hit vec: &bnull";
            } else {
                s = string4 + "\n&7Hit vec: &b" + round(getHitVec.field_72450_a) + "&7, &b" + round(getHitVec.field_72448_b) + "&7, &b" + round(getHitVec.field_72449_c);
            }
        } else if (packet instanceof C01PacketChatMessage) {
            s = s + "\n&7Length: &b" + ((C01PacketChatMessage) packet).func_149439_c().length();
        } else if (packet instanceof C17PacketCustomPayload) {
            s = s + "\n&7Channel: &b" + ((C17PacketCustomPayload) packet).func_149559_c();
        } else if (packet instanceof C15PacketClientSettings) {
            s = s + "\n&7Language: &b" + ((C15PacketClientSettings) packet).func_149524_c() + "\n&7Chat visibility: &b" + ((C15PacketClientSettings) packet).func_149523_e().name();
        } else if (packet instanceof C00PacketKeepAlive) {
            s = s + "\n&7Key: &b" + ((C00PacketKeepAlive) packet).func_149460_c();
        } else if (packet instanceof C16PacketClientStatus) {
            s = s + "\n&7Status: &b" + ((C16PacketClientStatus) packet).func_149435_c().name();
        } else if (packet instanceof C10PacketCreativeInventoryAction) {
            s = s + "\n&7Slot: &b" + ((C10PacketCreativeInventoryAction) packet).func_149627_c() + "\n&7Item: &b" + (((C10PacketCreativeInventoryAction) packet).func_149625_d() == null ? "null" : ((C10PacketCreativeInventoryAction) packet).func_149625_d().func_77973_b().getRegistryName().replace("minecraft:", ""));
        } else if (packet instanceof C0EPacketClickWindow) {
            C0EPacketClickWindow c0EPacketClickWindow = (C0EPacketClickWindow) packet;
            s = s + "\n&7Window: &b" + c0EPacketClickWindow.func_149548_c() + "\n&7Slot: &b" + c0EPacketClickWindow.func_149544_d() + "\n&7Button: &b" + c0EPacketClickWindow.func_149543_e() + "\n&7Action: &b" + c0EPacketClickWindow.func_149547_f() + "\n&7Mode: &b" + c0EPacketClickWindow.func_149542_h() + "\n&7Item: &b" + (c0EPacketClickWindow.func_149546_g() == null ? "null" : c0EPacketClickWindow.func_149546_g().func_77973_b().getRegistryName().replace("minecraft:", ""));
        } else if (packet instanceof C0FPacketConfirmTransaction) {
            s = s + "\n&7Window: &b" + ((C0FPacketConfirmTransaction) packet).func_149532_c() + "\n&7Uid: &b" + ((C0FPacketConfirmTransaction) packet).func_149533_d();
        } else if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer c03PacketPlayer = (C03PacketPlayer) packet;
            s = s + "\n&7Position: &b" + round(c03PacketPlayer.func_149464_c()) + "&7, &b" + round(c03PacketPlayer.func_149467_d()) + "&7, &b" + round(c03PacketPlayer.func_149472_e()) + "\n&7Rotations: &b" + round(c03PacketPlayer.func_149462_g()) + "&7, &b" + round(c03PacketPlayer.func_149470_h()) + "\n&7Ground: " + formatBoolean(c03PacketPlayer.func_149465_i()) + "\n&7Moving: " + formatBoolean(c03PacketPlayer.func_149466_j()) + "\n&7Rotating: " + formatBoolean(c03PacketPlayer.func_149463_k());
        }
        return s + "\n&7Client tick: &e" + tick;
    }

    private static double round(double n) {
        return Utils.round(n, 3);
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent e) {
        if (e.phase == Phase.START) {
            tick++;
        }
    }
}
