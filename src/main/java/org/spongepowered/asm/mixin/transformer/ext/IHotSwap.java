package org.spongepowered.asm.mixin.transformer.ext;

public interface IHotSwap {

    void registerMixinClass(String text);

    void registerTargetClass(String text2, byte[] byteArray);
}
