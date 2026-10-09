package keystrokesmod.event;

import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.Event;

public class DispatchPacketEvent extends Event {

    private Packet<?> packet;

    private final NetworkManager networkManager;

    public DispatchPacketEvent(Packet<?> packet) {
        this(null, packet);
    }

    public DispatchPacketEvent(NetworkManager networkManager, Packet<?> packet) {
        this.networkManager = networkManager;
        this.packet = packet;
    }

    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }
}
