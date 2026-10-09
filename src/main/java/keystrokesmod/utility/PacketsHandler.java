package keystrokesmod.utility;

import java.util.concurrent.atomic.AtomicInteger;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PacketsHandler implements IMinecraftInstance {

    public AtomicInteger playerSlot = new AtomicInteger(-1);

    public AtomicInteger serverSlot = new AtomicInteger(-1);

    private final boolean handleSlots = true;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSendPacket(SendPacketEvent e) {
        if (!e.isCanceled()) {
            Packet<?> packet = e.getPacket();
            if (packet instanceof C09PacketHeldItemChange) {
                C09PacketHeldItemChange slotPacket = (C09PacketHeldItemChange) packet;
                int slotId = slotPacket.func_149614_c();
                this.playerSlot.set(slotId);
                this.serverSlot.set(slotId);
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S09PacketHeldItemChange) {
            S09PacketHeldItemChange packet = (S09PacketHeldItemChange) e.getPacket();
            int index = packet.func_149385_c();
            if (index >= 0 && index < InventoryPlayer.func_70451_h()) {
                this.serverSlot.set(index);
            }
        } else if (e.getPacket() instanceof S0CPacketSpawnPlayer && Minecraft.func_71410_x().field_71439_g != null) {
            S0CPacketSpawnPlayer packet = (S0CPacketSpawnPlayer) e.getPacket();
            if (packet.func_148943_d() != Minecraft.func_71410_x().field_71439_g.func_145782_y()) {
                return;
            }
            this.playerSlot.set(-1);
        }
    }
}
