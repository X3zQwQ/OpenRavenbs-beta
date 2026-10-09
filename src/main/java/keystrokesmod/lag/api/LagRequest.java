package keystrokesmod.lag.api;

import java.util.Set;
import keystrokesmod.lag.timeout.AbstractTimeout;
import org.jetbrains.annotations.NotNull;

public final class LagRequest {

    @NotNull
    private final Set<EnumLagDirection> directions;

    @NotNull
    private final AbstractTimeout timeout;

    public LagRequest(@NotNull Set<EnumLagDirection> directions, @NotNull AbstractTimeout timeout) {
        this.directions = directions;
        this.timeout = timeout;
    }

    @NotNull
    public Set<EnumLagDirection> getDirections() {
        return this.directions;
    }

    @NotNull
    public AbstractTimeout getTimeout() {
        return this.timeout;
    }
}
