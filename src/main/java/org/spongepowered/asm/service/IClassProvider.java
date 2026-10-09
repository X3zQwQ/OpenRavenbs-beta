package org.spongepowered.asm.service;

import java.net.URL;

public interface IClassProvider {

    URL[] getClassPath();

    Class<?> findClass(String text) throws ClassNotFoundException;

    Class<?> findClass(String text2, boolean enabled) throws ClassNotFoundException;

    Class<?> findAgentClass(String text3, boolean enabled2) throws ClassNotFoundException;
}
