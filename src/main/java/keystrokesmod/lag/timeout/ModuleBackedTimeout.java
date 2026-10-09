package keystrokesmod.lag.timeout;

import keystrokesmod.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ModuleBackedTimeout extends AbstractTimeout {

    @NotNull
    private final Module module;

    @Nullable
    private final AbstractTimeout secondaryTimeout;

    private boolean hasModuleDisabled = false;

    public ModuleBackedTimeout(@NotNull Module module, @Nullable AbstractTimeout secondaryTimeout) {
        this.module = module;
        this.secondaryTimeout = secondaryTimeout;
        if (!module.isEnabled()) {
            this.hasModuleDisabled = true;
        }
    }

    public ModuleBackedTimeout(@NotNull Module module) {
        this(module, null);
    }

    @Override
    protected boolean shouldHaveTimedOut() {
        if (!this.module.isEnabled()) {
            this.hasModuleDisabled = true;
        }
        return this.hasModuleDisabled ? true : this.secondaryTimeout != null && this.secondaryTimeout.isTimedOut();
    }
}
