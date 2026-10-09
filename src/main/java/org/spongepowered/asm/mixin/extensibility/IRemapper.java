package org.spongepowered.asm.mixin.extensibility;

public interface IRemapper {

    String mapMethodName(String text, String text2, String text3);

    String mapFieldName(String text4, String text5, String text6);

    String map(String text7);

    String unmap(String text8);

    String mapDesc(String text9);

    String unmapDesc(String text10);
}
