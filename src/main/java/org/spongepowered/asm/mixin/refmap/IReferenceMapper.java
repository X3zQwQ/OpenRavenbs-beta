package org.spongepowered.asm.mixin.refmap;

public interface IReferenceMapper {

    boolean isDefault();

    String getResourceName();

    String getStatus();

    String getContext();

    void setContext(String context);

    String remap(String text, String text2);

    String remapWithContext(String text3, String text4, String text5);
}
