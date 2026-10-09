package keystrokesmod.lag.handler;

import net.minecraft.network.Packet;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractFastTrackProvider {

    public abstract void forPacket(@NotNull Packet<?> packet);
}
