package org.spongepowered.asm.service;

import java.io.InputStream;
import java.util.Collection;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.util.ReEntranceLock;

public interface IMixinService {

    String getName();

    boolean isValid();

    void prepare();

    MixinEnvironment.Phase getInitialPhase();

    void init();

    void beginPhase();

    void checkEnv(Object object);

    ReEntranceLock getReEntranceLock();

    IClassProvider getClassProvider();

    IClassBytecodeProvider getBytecodeProvider();

    Collection<String> getPlatformAgents();

    InputStream getResourceAsStream(String text);

    void registerInvalidClass(String text2);

    boolean isClassLoaded(String text3);

    String getClassRestrictions(String text4);

    Collection<ITransformer> getTransformers();

    String getSideName();
}
