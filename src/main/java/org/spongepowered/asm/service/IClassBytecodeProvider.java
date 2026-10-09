package org.spongepowered.asm.service;

import java.io.IOException;
import org.spongepowered.asm.lib.tree.ClassNode;

public interface IClassBytecodeProvider {

    byte[] getClassBytes(String text, String text2) throws IOException;

    byte[] getClassBytes(String text3, boolean enabled) throws ClassNotFoundException, IOException;

    ClassNode getClassNode(String text4) throws ClassNotFoundException, IOException;
}
