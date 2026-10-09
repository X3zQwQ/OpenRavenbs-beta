package keystrokesmod.utility;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public final class BlockHighlightSharedHandler {

    private static final Minecraft mc = Minecraft.func_71410_x();

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        SharedBlockHighlightCache.get().handleReceivePacket(e);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent e) {
        if (e.phase == Phase.END && Utils.nullCheck()) {
            SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
            if (cache.anyConsumerActive()) {
                int budget = 0;
                if (ModuleManager.blockESP != null) {
                    budget = Math.max(budget, ModuleManager.blockESP.getScanSpeedBudget());
                }
                if (ModuleManager.bedESP != null) {
                    budget = Math.max(budget, ModuleManager.bedESP.getScanSpeedBudget());
                }
                if (budget > 0) {
                    cache.tickScan(budget);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent e) {
        if (e.entity == mc.field_71439_g) {
            SharedBlockHighlightCache cache = SharedBlockHighlightCache.get();
            if (cache.anyConsumerActive()) {
                cache.clear();
                cache.enqueueLoadedChunks();
            }
        }
    }
}
