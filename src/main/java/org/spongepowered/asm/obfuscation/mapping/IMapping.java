package org.spongepowered.asm.obfuscation.mapping;

public interface IMapping<TMapping> {

    IMapping.Type getType();

    TMapping move(String text);

    TMapping remap(String text2);

    TMapping transform(String text3);

    TMapping copy();

    String getName();

    String getSimpleName();

    String getOwner();

    String getDesc();

    TMapping getSuper();

    String serialise();

    public static enum Type {

        FIELD, METHOD, CLASS, PACKAGE
    }
}
