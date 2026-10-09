package org.spongepowered.asm.mixin.transformer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.ArgsClassGenerator;
import org.spongepowered.asm.mixin.throwables.ClassAlreadyLoadedException;
import org.spongepowered.asm.mixin.throwables.MixinApplyError;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.throwables.MixinPrepareError;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IClassGenerator;
import org.spongepowered.asm.mixin.transformer.ext.IHotSwap;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionCheckClass;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionCheckInterfaces;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionClassExporter;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.mixin.transformer.throwables.MixinTransformerError;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.transformers.TreeTransformer;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.ReEntranceLock;
import org.spongepowered.asm.util.perf.Profiler;

public class MixinTransformer extends TreeTransformer {

    private static final String MIXIN_AGENT_CLASS = "org.spongepowered.tools.agent.MixinAgent";

    private static final String METRONOME_AGENT_CLASS = "org.spongepowered.metronome.Agent";

    static final Logger logger = LogManager.getLogger("mixin");

    private final IMixinService service = MixinService.getService();

    private final List<MixinConfig> configs = new ArrayList<>();

    private final List<MixinConfig> pendingConfigs = new ArrayList<>();

    private final ReEntranceLock lock;

    private final String sessionId = UUID.randomUUID().toString();

    private final Extensions extensions;

    private final IHotSwap hotSwapper;

    private final MixinPostProcessor postProcessor;

    private final Profiler profiler;

    private MixinEnvironment currentEnvironment;

    private Level verboseLoggingLevel = Level.DEBUG;

    private boolean errorState = false;

    private int transformedCount = 0;

    MixinTransformer() {
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        Object globalMixinTransformer = environment.getActiveTransformer();
        if (globalMixinTransformer instanceof ITransformer) {
            throw new MixinException("Terminating MixinTransformer instance " + this);
        } else {
            environment.setActiveTransformer(this);
            this.lock = this.service.getReEntranceLock();
            this.extensions = new Extensions(this);
            this.hotSwapper = this.initHotSwapper(environment);
            this.postProcessor = new MixinPostProcessor();
            this.extensions.add(new ArgsClassGenerator());
            this.extensions.add(new InnerClassGenerator());
            this.extensions.add(new ExtensionClassExporter(environment));
            this.extensions.add(new ExtensionCheckClass());
            this.extensions.add(new ExtensionCheckInterfaces());
            this.profiler = MixinEnvironment.getProfiler();
        }
    }

    private IHotSwap initHotSwapper(MixinEnvironment environment) {
        if (!environment.getOption(MixinEnvironment.Option.HOT_SWAP)) {
            return null;
        } else {
            try {
                logger.info("Attempting to load Hot-Swap agent");
                Class<? extends IHotSwap> clazz = (Class<? extends IHotSwap>) Class.forName("org.spongepowered.tools.agent.MixinAgent");
                Constructor<? extends IHotSwap> ctor = clazz.getDeclaredConstructor(MixinTransformer.class);
                return ctor.newInstance(this);
            } catch (Throwable exception) {
                logger.info("Hot-swap agent could not be loaded, hot swapping of mixins won't work. {}: {}", new Object[] { exception.getClass().getSimpleName(), exception.getMessage() });
                return null;
            }
        }
    }

    public void audit(MixinEnvironment environment) {
        Set<String> unhandled = new HashSet<>();
        for (MixinConfig config : this.configs) {
            unhandled.addAll(config.getUnhandledTargets());
        }
        Logger auditLogger = LogManager.getLogger("mixin/audit");
        for (String target : unhandled) {
            try {
                auditLogger.info("Force-loading class {}", new Object[] { target });
                this.service.getClassProvider().findClass(target, true);
            } catch (ClassNotFoundException exception2) {
                auditLogger.error("Could not force-load " + target, exception2);
            }
        }
        for (MixinConfig config : this.configs) {
            for (String target : config.getUnhandledTargets()) {
                ClassAlreadyLoadedException ex = new ClassAlreadyLoadedException(target + " was already classloaded");
                auditLogger.error("Could not force-load " + target, ex);
            }
        }
        if (environment.getOption(MixinEnvironment.Option.DEBUG_PROFILER)) {
            this.printProfilerSummary();
        }
    }

    private void printProfilerSummary() {
        DecimalFormat threedp = new DecimalFormat("(###0.000");
        DecimalFormat onedp = new DecimalFormat("(###0.0");
        PrettyPrinter printer = this.profiler.printer(false, false);
        long prepareTime = this.profiler.get("mixin.prepare").getTotalTime();
        long readTime = this.profiler.get("mixin.read").getTotalTime();
        long applyTime = this.profiler.get("mixin.apply").getTotalTime();
        long writeTime = this.profiler.get("mixin.write").getTotalTime();
        long totalMixinTime = this.profiler.get("mixin").getTotalTime();
        long loadTime = this.profiler.get("class.load").getTotalTime();
        long transformTime = this.profiler.get("class.transform").getTotalTime();
        long exportTime = this.profiler.get("mixin.debug.export").getTotalTime();
        long actualTime = totalMixinTime - loadTime - transformTime - exportTime;
        double timeSliceMixin = (double) actualTime / totalMixinTime * 100.0;
        double timeSliceLoad = (double) loadTime / totalMixinTime * 100.0;
        double timeSliceTransform = (double) transformTime / totalMixinTime * 100.0;
        double timeSliceExport = (double) exportTime / totalMixinTime * 100.0;
        long worstTransformerTime = 0L;
        Profiler.Section worstTransformer = null;
        for (Profiler.Section section : this.profiler.getSections()) {
            long transformerTime = section.getName().startsWith("class.transform.") ? section.getTotalTime() : 0L;
            if (transformerTime > worstTransformerTime) {
                worstTransformerTime = transformerTime;
                worstTransformer = section;
            }
        }
        printer.hr().add("Summary").hr().add();
        String format = "%9d ms %12s seconds)";
        printer.kv("Total mixin time", format, totalMixinTime, threedp.format(totalMixinTime * 0.001)).add();
        printer.kv("Preparing mixins", format, prepareTime, threedp.format(prepareTime * 0.001));
        printer.kv("Reading input", format, readTime, threedp.format(readTime * 0.001));
        printer.kv("Applying mixins", format, applyTime, threedp.format(applyTime * 0.001));
        printer.kv("Writing output", format, writeTime, threedp.format(writeTime * 0.001)).add();
        printer.kv("of which", "");
        printer.kv("Time spent loading from disk", format, loadTime, threedp.format(loadTime * 0.001));
        printer.kv("Time spent transforming classes", format, transformTime, threedp.format(transformTime * 0.001)).add();
        if (worstTransformer != null) {
            printer.kv("Worst transformer", worstTransformer.getName());
            printer.kv("Class", worstTransformer.getInfo());
            printer.kv("Time spent", "%s seconds", worstTransformer.getTotalSeconds());
            printer.kv("called", "%d times", worstTransformer.getTotalCount()).add();
        }
        printer.kv("   Time allocation:     Processing mixins", "%9d ms %10s%% of total)", actualTime, onedp.format(timeSliceMixin));
        printer.kv("Loading classes", "%9d ms %10s%% of total)", loadTime, onedp.format(timeSliceLoad));
        printer.kv("Running transformers", "%9d ms %10s%% of total)", transformTime, onedp.format(timeSliceTransform));
        if (exportTime > 0L) {
            printer.kv("Exporting classes (debug)", "%9d ms %10s%% of total)", exportTime, onedp.format(timeSliceExport));
        }
        printer.add();
        try {
            Class<?> agent = this.service.getClassProvider().findAgentClass("org.spongepowered.metronome.Agent", false);
            Method mdGetTimes = agent.getDeclaredMethod("getTimes");
            Map<String, Long> times = (Map<String, Long>) mdGetTimes.invoke(null);
            printer.hr().add("Transformer Times").hr().add();
            int longest = 10;
            for (Entry<String, Long> entry : times.entrySet()) {
                longest = Math.max(longest, entry.getKey().length());
            }
            for (Entry<String, Long> entry : times.entrySet()) {
                String name = entry.getKey();
                long mixinTime = 0L;
                for (Profiler.Section section2 : this.profiler.getSections()) {
                    if (name.equals(section2.getInfo())) {
                        mixinTime = section2.getTotalTime();
                        break;
                    }
                }
                if (mixinTime > 0L) {
                    printer.add("%-" + longest + "s %8s ms %8s ms in mixin)", name, entry.getValue() + mixinTime, "(" + mixinTime);
                } else {
                    printer.add("%-" + longest + "s %8s ms", name, entry.getValue());
                }
            }
            printer.add();
        } catch (Throwable exception3) {
        }
        printer.print();
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isDelegationExcluded() {
        return true;
    }

    @Override
    public synchronized byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (transformedName != null && !this.errorState) {
            MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
            if (basicClass == null) {
                for (IClassGenerator generator : this.extensions.getGenerators()) {
                    Profiler.Section genTimer = this.profiler.begin("generator", generator.getClass().getSimpleName().toLowerCase());
                    basicClass = generator.generate(transformedName);
                    genTimer.end();
                    if (basicClass != null) {
                        this.extensions.export(environment, transformedName.replace('.', '/'), false, basicClass);
                        return basicClass;
                    }
                }
                return basicClass;
            } else {
                boolean locked = this.lock.push().check();
                Profiler.Section mixinTimer = this.profiler.begin("mixin");
                if (!locked) {
                    try {
                        this.checkSelect(environment);
                    } catch (Exception exception4) {
                        this.lock.pop();
                        mixinTimer.end();
                        throw new MixinException(exception4);
                    }
                }
                byte[] th;
                try {
                    if (!this.postProcessor.canTransform(transformedName)) {
                        SortedSet<MixinInfo> mixins = null;
                        boolean invalidRef = false;
                        for (MixinConfig config : this.configs) {
                            if (config.packageMatch(transformedName)) {
                                invalidRef = true;
                            } else if (config.hasMixinsFor(transformedName)) {
                                if (mixins == null) {
                                    mixins = new TreeSet<>();
                                }
                                mixins.addAll(config.getMixinsFor(transformedName));
                            }
                        }
                        if (invalidRef) {
                            throw new NoClassDefFoundError(String.format("%s is a mixin class and cannot be referenced directly", transformedName));
                        }
                        if (mixins != null) {
                            if (locked) {
                                logger.warn("Re-entrance detected, this will cause serious problems.", new MixinException());
                                throw new MixinApplyError("Re-entrance error.");
                            }
                            if (this.hotSwapper != null) {
                                this.hotSwapper.registerTargetClass(transformedName, basicClass);
                            }
                            try {
                                Profiler.Section timer = this.profiler.begin("read");
                                ClassNode targetClassNode = this.readClass(basicClass, true);
                                TargetClassContext context = new TargetClassContext(environment, this.extensions, this.sessionId, transformedName, targetClassNode, mixins);
                                timer.end();
                                basicClass = this.applyMixins(environment, context);
                                this.transformedCount++;
                            } catch (InvalidMixinException exception5) {
                                this.dumpClassOnFailure(transformedName, basicClass, environment);
                                this.handleMixinApplyError(transformedName, exception5, environment);
                            }
                        }
                        return basicClass;
                    }
                    Profiler.Section postTimer = this.profiler.begin("postprocessor");
                    byte[] bytes = this.postProcessor.transformClassBytes(name, transformedName, basicClass);
                    postTimer.end();
                    this.extensions.export(environment, transformedName, false, bytes);
                    th = bytes;
                } catch (Throwable exception6) {
                    exception6.printStackTrace();
                    this.dumpClassOnFailure(transformedName, basicClass, environment);
                    throw new MixinTransformerError("An unexpected critical error was encountered", exception6);
                } finally {
                    this.lock.pop();
                    mixinTimer.end();
                }
                return th;
            }
        } else {
            return basicClass;
        }
    }

    public List<String> reload(String mixinClass, byte[] bytes) {
        if (this.lock.getDepth() > 0) {
            throw new MixinApplyError("Cannot reload mixin if re-entrant lock entered");
        } else {
            List<String> targets = new ArrayList<>();
            for (MixinConfig config : this.configs) {
                targets.addAll(config.reloadMixin(mixinClass, bytes));
            }
            return targets;
        }
    }

    private void checkSelect(MixinEnvironment environment) {
        if (this.currentEnvironment != environment) {
            this.select(environment);
        } else {
            int unvisitedCount = Mixins.getUnvisitedCount();
            if (unvisitedCount > 0 && this.transformedCount == 0) {
                this.select(environment);
            }
        }
    }

    private void select(MixinEnvironment environment) {
        this.verboseLoggingLevel = environment.getOption(MixinEnvironment.Option.DEBUG_VERBOSE) ? Level.INFO : Level.DEBUG;
        if (this.transformedCount > 0) {
            logger.log(this.verboseLoggingLevel, "Ending {}, applied {} mixins", new Object[] { this.currentEnvironment, this.transformedCount });
        }
        String action = this.currentEnvironment == environment ? "Checking for additional" : "Preparing";
        logger.log(this.verboseLoggingLevel, "{} mixins for {}", new Object[] { action, environment });
        this.profiler.setActive(true);
        this.profiler.mark(environment.getPhase().toString() + ":prepare");
        Profiler.Section prepareTimer = this.profiler.begin("prepare");
        this.selectConfigs(environment);
        this.extensions.select(environment);
        int totalMixins = this.prepareConfigs(environment);
        this.currentEnvironment = environment;
        this.transformedCount = 0;
        prepareTimer.end();
        long elapsedMs = prepareTimer.getTime();
        double elapsedTime = prepareTimer.getSeconds();
        if (elapsedTime > 0.25) {
            long loadTime = this.profiler.get("class.load").getTime();
            long transformTime = this.profiler.get("class.transform").getTime();
            long pluginTime = this.profiler.get("mixin.plugin").getTime();
            String elapsed = new DecimalFormat("###0.000").format(elapsedTime);
            String perMixinTime = new DecimalFormat("###0.0").format((double) elapsedMs / totalMixins);
            logger.log(this.verboseLoggingLevel, "Prepared {} mixins in {} sec ({}ms avg) ({}ms load, {}ms transform, {}ms plugin)", new Object[] { totalMixins, elapsed, perMixinTime, loadTime, transformTime, pluginTime });
        }
        this.profiler.mark(environment.getPhase().toString() + ":apply");
        this.profiler.setActive(environment.getOption(MixinEnvironment.Option.DEBUG_PROFILER));
    }

    private void selectConfigs(MixinEnvironment environment) {
        Iterator<Config> iter = Mixins.getConfigs().iterator();
        while (iter.hasNext()) {
            Config handle = iter.next();
            try {
                MixinConfig config = handle.get();
                if (config.select(environment)) {
                    iter.remove();
                    logger.log(this.verboseLoggingLevel, "Selecting config {}", new Object[] { config });
                    config.onSelect();
                    this.pendingConfigs.add(config);
                }
            } catch (Exception exception7) {
                logger.warn(String.format("Failed to select mixin config: %s", handle), exception7);
            }
        }
        Collections.sort(this.pendingConfigs);
    }

    private int prepareConfigs(MixinEnvironment environment) {
        int totalMixins = 0;
        final IHotSwap hotSwapper = this.hotSwapper;
        for (MixinConfig config : this.pendingConfigs) {
            config.addListener(this.postProcessor);
            if (hotSwapper != null) {
                config.addListener(new MixinConfig.IListener() {

                    @Override
                    public void onPrepare(MixinInfo mixin) {
                        hotSwapper.registerMixinClass(mixin.getClassName());
                    }

                    @Override
                    public void onInit(MixinInfo mixin) {
                    }
                });
            }
        }
        for (MixinConfig mixinConfig : this.pendingConfigs) {
            try {
                logger.log(this.verboseLoggingLevel, "Preparing {} ({})", new Object[] { mixinConfig, mixinConfig.getDeclaredMixinCount() });
                mixinConfig.prepare();
                totalMixins += mixinConfig.getMixinCount();
            } catch (InvalidMixinException exception8) {
                this.handleMixinPrepareError(mixinConfig, exception8, environment);
            } catch (Exception exception9) {
                String message = exception9.getMessage();
                logger.error("Error encountered whilst initialising mixin config '" + mixinConfig.getName() + "': " + message, exception9);
            }
        }
        for (MixinConfig mixinConfig2 : this.pendingConfigs) {
            IMixinConfigPlugin plugin = mixinConfig2.getPlugin();
            if (plugin != null) {
                Set<String> otherTargets = new HashSet<>();
                for (MixinConfig otherConfig : this.pendingConfigs) {
                    if (!otherConfig.equals(mixinConfig2)) {
                        otherTargets.addAll(otherConfig.getTargets());
                    }
                }
                plugin.acceptTargets(mixinConfig2.getTargets(), Collections.unmodifiableSet(otherTargets));
            }
        }
        for (MixinConfig mixinConfig3 : this.pendingConfigs) {
            try {
                mixinConfig3.postInitialise();
            } catch (InvalidMixinException exception10) {
                this.handleMixinPrepareError(mixinConfig3, exception10, environment);
            } catch (Exception exception11) {
                String message = exception11.getMessage();
                logger.error("Error encountered during mixin config postInit step'" + mixinConfig3.getName() + "': " + message, exception11);
            }
        }
        this.configs.addAll(this.pendingConfigs);
        Collections.sort(this.configs);
        this.pendingConfigs.clear();
        return totalMixins;
    }

    private byte[] applyMixins(MixinEnvironment environment, TargetClassContext context) {
        Profiler.Section timer = this.profiler.begin("preapply");
        this.extensions.preApply(context);
        timer = timer.next("apply");
        this.apply(context);
        timer = timer.next("postapply");
        try {
            this.extensions.postApply(context);
        } catch (ExtensionCheckClass.ValidationFailedException exception12) {
            logger.info(exception12.getMessage());
            if (context.isExportForced() || environment.getOption(MixinEnvironment.Option.DEBUG_EXPORT)) {
                this.writeClass(context);
            }
        }
        timer.end();
        return this.writeClass(context);
    }

    private void apply(TargetClassContext context) {
        context.applyMixins();
    }

    private void handleMixinPrepareError(MixinConfig config, InvalidMixinException ex, MixinEnvironment environment) throws MixinPrepareError {
        this.handleMixinError(config.getName(), ex, environment, MixinTransformer.ErrorPhase.PREPARE);
    }

    private void handleMixinApplyError(String targetClass, InvalidMixinException ex, MixinEnvironment environment) throws MixinApplyError {
        this.handleMixinError(targetClass, ex, environment, MixinTransformer.ErrorPhase.APPLY);
    }

    private void handleMixinError(String context, InvalidMixinException ex, MixinEnvironment environment, MixinTransformer.ErrorPhase errorPhase) throws Error {
        this.errorState = true;
        IMixinInfo mixin = ex.getMixin();
        if (mixin == null) {
            logger.error("InvalidMixinException has no mixin!", ex);
            throw ex;
        } else {
            IMixinConfig config = mixin.getConfig();
            MixinEnvironment.Phase phase = mixin.getPhase();
            IMixinErrorHandler.ErrorAction action = config.isRequired() ? IMixinErrorHandler.ErrorAction.ERROR : IMixinErrorHandler.ErrorAction.WARN;
            if (environment.getOption(MixinEnvironment.Option.DEBUG_VERBOSE)) {
                new PrettyPrinter().add("Invalid Mixin").centre().hr('-').kvWidth(10).kv("Action", errorPhase.name()).kv("Mixin", mixin.getClassName()).kv("Config", config.getName()).kv("Phase", phase).hr('-').add("    %s", ex.getClass().getName()).hr('-').addWrapped("    %s", ex.getMessage()).hr('-').add(ex, 8).trace(action.logLevel);
            }
            for (IMixinErrorHandler handler : this.getErrorHandlers(mixin.getPhase())) {
                IMixinErrorHandler.ErrorAction newAction = errorPhase.onError(handler, context, ex, mixin, action);
                if (newAction != null) {
                    action = newAction;
                }
            }
            logger.log(action.logLevel, errorPhase.getLogMessage(context, ex, mixin), ex);
            this.errorState = false;
            if (action == IMixinErrorHandler.ErrorAction.ERROR) {
                throw new MixinApplyError(errorPhase.getErrorMessage(mixin, config, phase), ex);
            }
        }
    }

    private List<IMixinErrorHandler> getErrorHandlers(MixinEnvironment.Phase phase) {
        List<IMixinErrorHandler> handlers = new ArrayList<>();
        for (String handlerClassName : Mixins.getErrorHandlerClasses()) {
            try {
                logger.info("Instancing error handler class {}", new Object[] { handlerClassName });
                Class<?> handlerClass = this.service.getClassProvider().findClass(handlerClassName, true);
                IMixinErrorHandler handler = (IMixinErrorHandler) handlerClass.newInstance();
                if (handler != null) {
                    handlers.add(handler);
                }
            } catch (Throwable exception13) {
            }
        }
        return handlers;
    }

    private byte[] writeClass(TargetClassContext context) {
        return this.writeClass(context.getClassName(), context.getClassNode(), context.isExportForced());
    }

    private byte[] writeClass(String transformedName, ClassNode targetClass, boolean forceExport) {
        Profiler.Section writeTimer = this.profiler.begin("write");
        byte[] bytes = this.writeClass(targetClass);
        writeTimer.end();
        this.extensions.export(this.currentEnvironment, transformedName, forceExport, bytes);
        return bytes;
    }

    private void dumpClassOnFailure(String className, byte[] bytes, MixinEnvironment env) {
        if (env.getOption(MixinEnvironment.Option.DUMP_TARGET_ON_FAILURE)) {
            ExtensionClassExporter exporter = this.extensions.getExtension(ExtensionClassExporter.class);
            exporter.dumpClass(className.replace('.', '/') + ".target", bytes);
        }
    }

    static enum ErrorPhase {

        PREPARE {

            @Override
            IMixinErrorHandler.ErrorAction onError(IMixinErrorHandler handler, String context, InvalidMixinException ex, IMixinInfo mixin, IMixinErrorHandler.ErrorAction action) {
                try {
                    return handler.onPrepareError(mixin.getConfig(), ex, mixin, action);
                } catch (AbstractMethodError abstractMethodError) {
                    return action;
                }
            }

            @Override
            protected String getContext(IMixinInfo mixin, String context) {
                return String.format("preparing %s in %s", mixin.getName(), context);
            }
        }
        , APPLY {

            @Override
            IMixinErrorHandler.ErrorAction onError(IMixinErrorHandler handler, String context, InvalidMixinException ex, IMixinInfo mixin, IMixinErrorHandler.ErrorAction action) {
                try {
                    return handler.onApplyError(context, ex, mixin, action);
                } catch (AbstractMethodError abstractMethodError2) {
                    return action;
                }
            }

            @Override
            protected String getContext(IMixinInfo mixin, String context) {
                return String.format("%s -> %s", mixin, context);
            }
        }
        ;

        private final String text = this.name().toLowerCase();

        private ErrorPhase() {
        }

        abstract IMixinErrorHandler.ErrorAction onError(IMixinErrorHandler iMixinErrorHandler, String text2, InvalidMixinException exception14, IMixinInfo iMixinInfo, IMixinErrorHandler.ErrorAction errorAction);

        protected abstract String getContext(IMixinInfo iMixinInfo2, String text3);

        public String getLogMessage(String context, InvalidMixinException ex, IMixinInfo mixin) {
            return String.format("Mixin %s failed %s: %s %s", this.text, this.getContext(mixin, context), ex.getClass().getName(), ex.getMessage());
        }

        public String getErrorMessage(IMixinInfo mixin, IMixinConfig config, MixinEnvironment.Phase phase) {
            return String.format("Mixin [%s] from phase [%s] in config [%s] FAILED during %s", mixin, phase, config, this.name());
        }
    }
}
