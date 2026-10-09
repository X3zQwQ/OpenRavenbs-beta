package org.spongepowered.asm.mixin.extensibility;

import org.apache.logging.log4j.Level;

public interface IMixinErrorHandler {

    IMixinErrorHandler.ErrorAction onPrepareError(IMixinConfig iMixinConfig, Throwable exception, IMixinInfo iMixinInfo, IMixinErrorHandler.ErrorAction errorAction);

    IMixinErrorHandler.ErrorAction onApplyError(String text, Throwable exception2, IMixinInfo iMixinInfo2, IMixinErrorHandler.ErrorAction errorAction2);

    public static enum ErrorAction {

        NONE(Level.INFO), WARN(Level.WARN), ERROR(Level.FATAL);

        public final Level logLevel;

        private ErrorAction(Level logLevel) {
            this.logLevel = logLevel;
        }
    }
}
