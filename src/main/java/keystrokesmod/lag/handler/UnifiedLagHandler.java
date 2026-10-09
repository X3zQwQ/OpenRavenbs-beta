package keystrokesmod.lag.handler;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.lag.api.EnumLagDirection;
import keystrokesmod.lag.api.LagRequest;
import keystrokesmod.lag.queue.BiTrackLagNodeQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class UnifiedLagHandler extends AbstractFastTrackProvider {

    @NotNull
    private final BiTrackLagNodeQueue queue = new BiTrackLagNodeQueue(this);

    @NotNull
    private final Set<Packet<?>> packetFastTrack = Collections.newSetFromMap(Collections.synchronizedMap(new IdentityHashMap<>()));

    @Nullable
    private volatile Vec3 serverPosition;

    public void requestLag(@NotNull LagRequest request) {
        this.queue.requestLag(request);
    }

    public boolean isBuffering() {
        return this.queue.isBuffering();
    }

    public void releaseExpiredPackets(@NotNull EnumLagDirection direction, long maxAgeMs) {
        this.queue.releaseExpiredPackets(direction, maxAgeMs);
    }

    public void releaseNextPacket(@NotNull EnumLagDirection direction) {
        this.queue.releaseNextPacket(direction);
    }

    @Nullable
    public Vec3 getLastReleasedServerPosition() {
        return this.serverPosition;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSendPacket(@NotNull SendPacketEvent event) {
        if (Minecraft.func_71410_x().func_147114_u() == null) {
            this.queue.clear();
            this.clearServerPositions();
        } else {
            Packet<?> packet = event.getPacket();
            boolean fastTracked = this.consumeFastTrack(packet);
            if (!event.isCanceled()) {
                if (fastTracked) {
                    this.updateServerPosition(packet);
                } else if (this.queue.tick(packet, EnumLagDirection.OUTBOUND)) {
                    event.setCanceled(true);
                } else {
                    this.updateServerPosition(packet);
                }
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(@NotNull ReceivePacketEvent event) {
        if (Minecraft.func_71410_x().func_147114_u() == null) {
            this.queue.clear();
            this.clearServerPositions();
        } else {
            Packet<?> packet = event.getPacket();
            boolean fastTracked = this.consumeFastTrack(packet);
            if (!event.isCanceled()) {
                if (!fastTracked) {
                    if (this.queue.tick(packet, EnumLagDirection.INBOUND)) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onGameTick(@NotNull GameTickEvent event) {
        if (Minecraft.func_71410_x().func_147114_u() == null) {
            this.queue.clear();
            this.clearServerPositions();
        } else {
            this.queue.tick(null, null);
        }
    }

    @Override
    public void forPacket(@NotNull Packet<?> packet) {
        this.packetFastTrack.add(packet);
    }

    private boolean consumeFastTrack(@NotNull Packet<?> packet) {
        return this.packetFastTrack.remove(packet);
    }

    private void updateServerPosition(@NotNull Packet<?> packet) {
        if (packet instanceof C03PacketPlayer) {
            C03PacketPlayer movementPacket = (C03PacketPlayer) packet;
            if (movementPacket.func_149466_j()) {
                this.serverPosition = new Vec3(movementPacket.func_149464_c(), movementPacket.func_149467_d(), movementPacket.func_149472_e());
            }
        }
    }

    private void clearServerPositions() {
        this.serverPosition = null;
    }
}
