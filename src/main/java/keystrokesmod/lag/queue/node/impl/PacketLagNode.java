package keystrokesmod.lag.queue.node.impl;

import keystrokesmod.lag.api.EnumLagDirection;
import keystrokesmod.lag.handler.AbstractFastTrackProvider;
import keystrokesmod.lag.queue.node.api.AbstractLagNode;
import net.minecraft.network.Packet;
import org.jetbrains.annotations.NotNull;

public final class PacketLagNode extends AbstractLagNode {

    @NotNull
    private final Packet<?> packet;

    @NotNull
    private final EnumLagDirection direction;

    private final long queuedAtMs;

    public PacketLagNode(@NotNull Packet<?> packet, @NotNull EnumLagDirection direction) {
        this.packet = packet;
        this.direction = direction;
        this.queuedAtMs = System.currentTimeMillis();
    }

    public long getQueuedAtMs() {
        return this.queuedAtMs;
    }

    public void goThrough(@NotNull AbstractFastTrackProvider fastTrack) {
        if (this.direction == EnumLagDirection.OUTBOUND) {
            fastTrack.forPacket(this.packet);
        }
        this.direction.passThroughChannel(this.packet);
    }
}
