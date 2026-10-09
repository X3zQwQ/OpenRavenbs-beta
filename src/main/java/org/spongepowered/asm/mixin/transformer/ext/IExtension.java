package org.spongepowered.asm.mixin.transformer.ext;

import org.spongepowered.asm.mixin.MixinEnvironment;

public interface IExtension {

    boolean checkActive(MixinEnvironment mixinEnvironment);

    void preApply(ITargetClassContext iTargetClassContext);

    void postApply(ITargetClassContext iTargetClassContext2);

    void export(MixinEnvironment mixinEnvironment2, String text, boolean enabled, byte[] byteArray);
}
