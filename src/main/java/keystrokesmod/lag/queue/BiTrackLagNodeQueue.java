package keystrokesmod.lag.queue;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.lag.api.EnumLagDirection;
import keystrokesmod.lag.api.LagRequest;
import keystrokesmod.lag.handler.AbstractFastTrackProvider;
import keystrokesmod.lag.queue.node.api.AbstractLagNode;
import keystrokesmod.lag.queue.node.impl.AddRequestLagNode;
import keystrokesmod.lag.queue.node.impl.PacketLagNode;
import net.minecraft.network.Packet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BiTrackLagNodeQueue {

    @NotNull
    private final BiTrackLagNodeQueue.TrackState incomingState;

    @NotNull
    private final BiTrackLagNodeQueue.TrackState outgoingState;

    public BiTrackLagNodeQueue(@NotNull AbstractFastTrackProvider fastTrackProvider) {
        this.incomingState = new BiTrackLagNodeQueue.TrackState(new ArrayList(), fastTrackProvider);
        this.outgoingState = new BiTrackLagNodeQueue.TrackState(new ArrayList(), fastTrackProvider);
    }

    public void clear() {
        this.incomingState.clear();
        this.outgoingState.clear();
    }

    public boolean isBuffering() {
        return this.incomingState.isBuffering() || this.outgoingState.isBuffering();
    }

    public boolean tick(@Nullable Packet<?> packet, @Nullable EnumLagDirection direction) {
        if (packet == null != (direction == null)) {
            throw new NullPointerException();
        } else if (direction == null) {
            this.incomingState.tick(null, null);
            this.outgoingState.tick(null, null);
            return false;
        } else {
            switch(direction) {
                case INBOUND:
                    return this.incomingState.tick(packet, direction);
                case OUTBOUND:
                    return this.outgoingState.tick(packet, direction);
                default:
                    return false;
            }
        }
    }

    public void requestLag(@NotNull LagRequest request) {
        for (EnumLagDirection direction : request.getDirections()) {
            switch(direction) {
                case INBOUND:
                    this.incomingState.addRequest(request);
                    break;
                case OUTBOUND:
                    this.outgoingState.addRequest(request);
            }
        }
    }

    public void releaseExpiredPackets(@NotNull EnumLagDirection direction, long maxAgeMs) {
        switch(direction) {
            case INBOUND:
                this.incomingState.releaseExpiredPackets(maxAgeMs);
                break;
            case OUTBOUND:
                this.outgoingState.releaseExpiredPackets(maxAgeMs);
        }
    }

    public void releaseNextPacket(@NotNull EnumLagDirection direction) {
        switch(direction) {
            case INBOUND:
                this.incomingState.releaseNextPacket();
                break;
            case OUTBOUND:
                this.outgoingState.releaseNextPacket();
        }
    }

    private static final class TrackState {

        @NotNull
        private final List<AbstractLagNode> track;

        @NotNull
        private final AbstractFastTrackProvider fastTrackProvider;

        @Nullable
        private LagRequest currentlyAwaiting = null;

        private TrackState(@NotNull List<AbstractLagNode> track, @NotNull AbstractFastTrackProvider fastTrackProvider) {
            this.track = track;
            this.fastTrackProvider = fastTrackProvider;
        }

        private synchronized void addRequest(@NotNull LagRequest request) {
            this.track.add(new AddRequestLagNode(request));
        }

        private synchronized boolean isBuffering() {
            return !this.track.isEmpty() || this.currentlyAwaiting != null && !this.currentlyAwaiting.getTimeout().isTimedOut();
        }

        private synchronized boolean tick(@Nullable Packet<?> packet, @Nullable EnumLagDirection direction) {
            if (!this.track.isEmpty() || this.currentlyAwaiting != null && !this.currentlyAwaiting.getTimeout().isTimedOut()) {
                if (packet != null) {
                    this.track.add(new PacketLagNode(packet, direction));
                }
                LagRequest awaiting = this.currentlyAwaiting;
                try {
                    while (awaiting == null || awaiting.getTimeout().isTimedOut()) {
                        AbstractLagNode popped = this.pop();
                        if (popped == null) {
                            awaiting = null;
                            break;
                        }
                        if (popped instanceof PacketLagNode) {
                            ((PacketLagNode) popped).goThrough(this.fastTrackProvider);
                        } else if (popped instanceof AddRequestLagNode) {
                            awaiting = ((AddRequestLagNode) popped).getRequest();
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                this.currentlyAwaiting = awaiting;
                return true;
            } else {
                this.currentlyAwaiting = null;
                return false;
            }
        }

        private synchronized void releaseExpiredPackets(long maxAgeMs) {
            long cutoff = System.currentTimeMillis() - maxAgeMs;
            List<PacketLagNode> toRelease = new ArrayList<>();
            for (AbstractLagNode node : new ArrayList<>(this.track)) {
                if (node instanceof PacketLagNode) {
                    PacketLagNode pkt = (PacketLagNode) node;
                    if (pkt.getQueuedAtMs() <= cutoff) {
                        toRelease.add(pkt);
                    }
                }
            }
            if (!toRelease.isEmpty()) {
                this.track.removeAll(toRelease);
                for (PacketLagNode pkt : toRelease) {
                    pkt.goThrough(this.fastTrackProvider);
                }
            }
        }

        private synchronized void releaseNextPacket() {
            for (int i = 0; i < this.track.size(); i++) {
                AbstractLagNode node = this.track.get(i);
                if (node instanceof PacketLagNode) {
                    PacketLagNode packetLagNode = (PacketLagNode) node;
                    this.track.remove(i);
                    packetLagNode.goThrough(this.fastTrackProvider);
                    return;
                }
            }
        }

        private synchronized void clear() {
            this.track.clear();
            this.currentlyAwaiting = null;
        }

        @Nullable
        private AbstractLagNode pop() {
            return this.track.isEmpty() ? null : this.track.remove(0);
        }
    }
}
