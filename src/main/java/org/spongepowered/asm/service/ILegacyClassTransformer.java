package org.spongepowered.asm.service;

public interface ILegacyClassTransformer extends ITransformer {

    String getName();

    boolean isDelegationExcluded();

    byte[] transformClassBytes(String text, String text2, byte[] byteArray);
}
