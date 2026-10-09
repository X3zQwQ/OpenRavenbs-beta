package keystrokesmod.lag.api;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import keystrokesmod.utility.IMinecraftInstance;
import keystrokesmod.utility.Utils;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import org.jetbrains.annotations.NotNull;

public enum EnumLagDirection implements IMinecraftInstance {

    INBOUND(packet -> {
        try {
            ((Packet<net.minecraft.network.play.INetHandlerPlayClient>) packet).func_148833_a(mc.func_147114_u());
        } catch (ThreadQuickExitException exception) {
        } catch (Exception exception2) {
            Utils.sendDebugMessage("error while handling packet: " + packet.getClass().getSimpleName());
        }
    }), OUTBOUND(packet -> mc.func_147114_u().func_147297_a(packet));

    @NotNull
    public static final Set<EnumLagDirection> ONLY_INBOUND = EnumSet.of(INBOUND);

    @NotNull
    public static final Set<EnumLagDirection> ONLY_OUTBOUND = EnumSet.of(OUTBOUND);

    @NotNull
    public static final Set<EnumLagDirection> BIDIRECTIONAL = EnumSet.allOf(EnumLagDirection.class);

    @NotNull
    private final Consumer<Packet<?>> channel;

    private EnumLagDirection(@NotNull Consumer<Packet<?>> channel) {
        this.channel = channel;
    }

    public void passThroughChannel(@NotNull Packet<?> packet) {
        this.channel.accept(packet);
    }
}
