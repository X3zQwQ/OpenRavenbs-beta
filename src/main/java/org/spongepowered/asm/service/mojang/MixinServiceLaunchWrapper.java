package org.spongepowered.asm.service.mojang;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.util.ReEntranceLock;
import org.spongepowered.asm.util.perf.Profiler;

public class MixinServiceLaunchWrapper implements IMixinService, IClassProvider, IClassBytecodeProvider {

    public static final String BLACKBOARD_KEY_TWEAKCLASSES = "TweakClasses";

    public static final String BLACKBOARD_KEY_TWEAKS = "Tweaks";

    private static final String LAUNCH_PACKAGE = "org.spongepowered.asm.launch.";

    private static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixin.";

    private static final String STATE_TWEAKER = "org.spongepowered.asm.mixin.EnvironmentStateTweaker";

    private static final String TRANSFORMER_PROXY_CLASS = "org.spongepowered.asm.mixin.transformer.Proxy";

    private static final Logger logger = LogManager.getLogger("mixin");

    private final LaunchClassLoaderUtil classLoaderUtil = new LaunchClassLoaderUtil(Launch.classLoader);

    private final ReEntranceLock lock = new ReEntranceLock(1);

    private IClassNameTransformer nameTransformer;

    @Override
    public String getName() {
        return "LaunchWrapper";
    }

    @Override
    public boolean isValid() {
        try {
            Launch.classLoader.hashCode();
            return true;
        } catch (Throwable exception) {
            return false;
        }
    }

    @Override
    public void prepare() {
        Launch.classLoader.addClassLoaderExclusion("org.spongepowered.asm.launch.");
    }

    @Override
    public MixinEnvironment.Phase getInitialPhase() {
        return findInStackTrace("net.minecraft.launchwrapper.Launch", "launch") > 132 ? MixinEnvironment.Phase.DEFAULT : MixinEnvironment.Phase.PREINIT;
    }

    @Override
    public void init() {
        if (findInStackTrace("net.minecraft.launchwrapper.Launch", "launch") < 4) {
            logger.error("MixinBootstrap.doInit() called during a tweak constructor!");
        }
        List<String> tweakClasses = GlobalProperties.get("TweakClasses");
        if (tweakClasses != null) {
            tweakClasses.add("org.spongepowered.asm.mixin.EnvironmentStateTweaker");
        }
    }

    @Override
    public ReEntranceLock getReEntranceLock() {
        return this.lock;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return ImmutableList.of("org.spongepowered.asm.launch.platform.MixinPlatformAgentFML");
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Launch.classLoader.findClass(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, Launch.classLoader);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, Launch.class.getClassLoader());
    }

    @Override
    public void beginPhase() {
        Launch.classLoader.registerTransformer("org.spongepowered.asm.mixin.transformer.Proxy");
    }

    @Override
    public void checkEnv(Object bootSource) {
        if (bootSource.getClass().getClassLoader() != Launch.class.getClassLoader()) {
            throw new MixinException("Attempted to init the mixin environment in the wrong classloader");
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return Launch.classLoader.getResourceAsStream(name);
    }

    @Override
    public void registerInvalidClass(String className) {
        this.classLoaderUtil.registerInvalidClass(className);
    }

    @Override
    public boolean isClassLoaded(String className) {
        return this.classLoaderUtil.isClassLoaded(className);
    }

    @Override
    public String getClassRestrictions(String className) {
        String restrictions = "";
        if (this.classLoaderUtil.isClassClassLoaderExcluded(className, null)) {
            restrictions = "PACKAGE_CLASSLOADER_EXCLUSION";
        }
        if (this.classLoaderUtil.isClassTransformerExcluded(className, null)) {
            restrictions = (restrictions.length() > 0 ? restrictions + "," : "") + "PACKAGE_TRANSFORMER_EXCLUSION";
        }
        return restrictions;
    }

    @Override
    public URL[] getClassPath() {
        return Launch.classLoader.getSources().toArray(new URL[0]);
    }

    @Override
    public Collection<ITransformer> getTransformers() {
        List<IClassTransformer> transformers = Launch.classLoader.getTransformers();
        List<ITransformer> wrapped = new ArrayList<>(transformers.size());
        for (IClassTransformer transformer : transformers) {
            if (transformer instanceof ITransformer) {
                wrapped.add((ITransformer) transformer);
            } else {
                wrapped.add(new LegacyTransformerHandle(transformer));
            }
            if (transformer instanceof IClassNameTransformer) {
                logger.debug("Found name transformer: {}", new Object[] { transformer.getClass().getName() });
                this.nameTransformer = (IClassNameTransformer) transformer;
            }
        }
        return wrapped;
    }

    @Override
    public byte[] getClassBytes(String name, String transformedName) throws IOException {
        byte[] classBytes = Launch.classLoader.getClassBytes(name);
        if (classBytes != null) {
            return classBytes;
        } else {
            URLClassLoader appClassLoader = (URLClassLoader) Launch.class.getClassLoader();
            InputStream classStream = null;
            Object object;
            try {
                String resourcePath = transformedName.replace('.', '/').concat(".class");
                classStream = appClassLoader.getResourceAsStream(resourcePath);
                return IOUtils.toByteArray(classStream);
            } catch (Exception exception2) {
                object = null;
            } finally {
                IOUtils.closeQuietly(classStream);
            }
            return (byte[]) object;
        }
    }

    @Override
    public byte[] getClassBytes(String className, boolean runTransformers) throws ClassNotFoundException, IOException {
        String transformedName = className.replace('/', '.');
        String name = this.unmapClassName(transformedName);
        Profiler profiler = MixinEnvironment.getProfiler();
        Profiler.Section loadTime = profiler.begin(1, "class.load");
        byte[] classBytes = this.getClassBytes(name, transformedName);
        loadTime.end();
        if (runTransformers) {
            Profiler.Section transformTime = profiler.begin(1, "class.transform");
            classBytes = this.applyTransformers(name, transformedName, classBytes, profiler);
            transformTime.end();
        }
        if (classBytes == null) {
            throw new ClassNotFoundException(String.format("The specified class '%s' was not found", transformedName));
        } else {
            return classBytes;
        }
    }

    private byte[] applyTransformers(String name, String transformedName, byte[] basicClass, Profiler profiler) {
        if (this.classLoaderUtil.isClassExcluded(name, transformedName)) {
            return basicClass;
        } else {
            MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
            for (ILegacyClassTransformer transformer : environment.getTransformers()) {
                this.lock.clear();
                int pos = transformer.getName().lastIndexOf(46);
                String simpleName = transformer.getName().substring(pos + 1);
                Profiler.Section transformTime = profiler.begin(2, simpleName.toLowerCase());
                transformTime.setInfo(transformer.getName());
                basicClass = transformer.transformClassBytes(name, transformedName, basicClass);
                transformTime.end();
                if (this.lock.isSet()) {
                    environment.addTransformerExclusion(transformer.getName());
                    this.lock.clear();
                    logger.info("A re-entrant transformer '{}' was detected and will no longer process meta class data", new Object[] { transformer.getName() });
                }
            }
            return basicClass;
        }
    }

    private String unmapClassName(String className) {
        if (this.nameTransformer == null) {
            this.findNameTransformer();
        }
        return this.nameTransformer != null ? this.nameTransformer.unmapClassName(className) : className;
    }

    private void findNameTransformer() {
        for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
            if (transformer instanceof IClassNameTransformer) {
                logger.debug("Found name transformer: {}", new Object[] { transformer.getClass().getName() });
                this.nameTransformer = (IClassNameTransformer) transformer;
            }
        }
    }

    @Override
    public ClassNode getClassNode(String className) throws ClassNotFoundException, IOException {
        return this.getClassNode(this.getClassBytes(className, true), 0);
    }

    private ClassNode getClassNode(byte[] classBytes, int flags) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(classNode, flags);
        return classNode;
    }

    @Override
    public final String getSideName() {
        for (ITweaker tweaker : (List<ITweaker>) GlobalProperties.get("Tweaks")) {
            if (tweaker.getClass().getName().endsWith(".common.launcher.FMLServerTweaker")) {
                return "SERVER";
            }
            if (tweaker.getClass().getName().endsWith(".common.launcher.FMLTweaker")) {
                return "CLIENT";
            }
        }
        String name = this.getSideName("net.minecraftforge.fml.relauncher.FMLLaunchHandler", "side");
        if (name != null) {
            return name;
        } else {
            name = this.getSideName("cpw.mods.fml.relauncher.FMLLaunchHandler", "side");
            if (name != null) {
                return name;
            } else {
                name = this.getSideName("com.mumfrey.liteloader.launch.LiteLoaderTweaker", "getEnvironmentType");
                return name != null ? name : "UNKNOWN";
            }
        }
    }

    private String getSideName(String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className, false, Launch.classLoader);
            Method method = clazz.getDeclaredMethod(methodName);
            return ((Enum) method.invoke(null)).name();
        } catch (Exception exception3) {
            return null;
        }
    }

    private static int findInStackTrace(String className, String methodName) {
        Thread currentThread = Thread.currentThread();
        if (!"main".equals(currentThread.getName())) {
            return 0;
        } else {
            StackTraceElement[] stackTrace = currentThread.getStackTrace();
            for (StackTraceElement s : stackTrace) {
                if (className.equals(s.getClassName()) && methodName.equals(s.getMethodName())) {
                    return s.getLineNumber();
                }
            }
            return 0;
        }
    }
}
