package keystrokesmod.utility;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import keystrokesmod.Raven;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class PacketUtils implements IMinecraftInstance {

    private static final Set<Packet<?>> skipSendEvent = Collections.newSetFromMap(Collections.synchronizedMap(new IdentityHashMap<>()));

    private static final Set<Packet<?>> skipReceiveEvent = Collections.newSetFromMap(Collections.synchronizedMap(new IdentityHashMap<>()));

    public static boolean consumeSendEventSkip(Packet<?> packet) {
        return skipSendEvent.remove(packet);
    }

    public static boolean consumeReceiveEventSkip(Packet<?> packet) {
        return skipReceiveEvent.remove(packet);
    }

    public static void sendPacketNoEvent(Packet packet) {
        if (packet != null && !packet.getClass().getSimpleName().startsWith("S")) {
            skipSendEvent.add(packet);
            Raven.mc.field_71439_g.field_71174_a.func_147297_a(packet);
        }
    }

    public static void receivePacketNoEvent(Packet packet) {
        try {
            packet.func_148833_a(Raven.mc.func_147114_u());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void sendReleasePacket() {
        mc.field_71439_g.field_71174_a.func_147297_a(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.field_177992_a, EnumFacing.DOWN));
    }
}
