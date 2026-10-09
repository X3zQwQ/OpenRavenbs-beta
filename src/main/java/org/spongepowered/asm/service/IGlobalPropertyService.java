package org.spongepowered.asm.service;

public interface IGlobalPropertyService {

    <T> T getProperty(String text);

    void setProperty(String text2, Object object);

    <T> T getProperty(String text3, T value);

    String getPropertyString(String text4, String text5);
}
