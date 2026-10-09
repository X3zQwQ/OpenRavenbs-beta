package org.slf4j;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.event.SubstituteLoggingEvent;
import org.slf4j.helpers.NOP_FallbackServiceProvider;
import org.slf4j.helpers.Reporter;
import org.slf4j.helpers.SubstituteLogger;
import org.slf4j.helpers.SubstituteServiceProvider;
import org.slf4j.helpers.Util;
import org.slf4j.spi.SLF4JServiceProvider;

public final class LoggerFactory {

    static final String CODES_PREFIX = "https://www.slf4j.org/codes.html";

    static final String NO_PROVIDERS_URL = "https://www.slf4j.org/codes.html#noProviders";

    static final String IGNORED_BINDINGS_URL = "https://www.slf4j.org/codes.html#ignoredBindings";

    static final String MULTIPLE_BINDINGS_URL = "https://www.slf4j.org/codes.html#multiple_bindings";

    static final String VERSION_MISMATCH = "https://www.slf4j.org/codes.html#version_mismatch";

    static final String SUBSTITUTE_LOGGER_URL = "https://www.slf4j.org/codes.html#substituteLogger";

    static final String LOGGER_NAME_MISMATCH_URL = "https://www.slf4j.org/codes.html#loggerNameMismatch";

    static final String REPLAY_URL = "https://www.slf4j.org/codes.html#replay";

    static final String UNSUCCESSFUL_INIT_URL = "https://www.slf4j.org/codes.html#unsuccessfulInit";

    static final String UNSUCCESSFUL_INIT_MSG = "org.slf4j.LoggerFactory in failed state. Original exception was thrown EARLIER. See also https://www.slf4j.org/codes.html#unsuccessfulInit";

    public static final String PROVIDER_PROPERTY_KEY = "slf4j.provider";

    static final int UNINITIALIZED = 0;

    static final int ONGOING_INITIALIZATION = 1;

    static final int FAILED_INITIALIZATION = 2;

    static final int SUCCESSFUL_INITIALIZATION = 3;

    static final int NOP_FALLBACK_INITIALIZATION = 4;

    static volatile int INITIALIZATION_STATE = 0;

    static final SubstituteServiceProvider SUBST_PROVIDER = new SubstituteServiceProvider();

    static final NOP_FallbackServiceProvider NOP_FALLBACK_SERVICE_PROVIDER = new NOP_FallbackServiceProvider();

    static final String DETECT_LOGGER_NAME_MISMATCH_PROPERTY = "slf4j.detectLoggerNameMismatch";

    static final String JAVA_VENDOR_PROPERTY = "java.vendor.url";

    static boolean DETECT_LOGGER_NAME_MISMATCH = Util.safeGetBooleanSystemProperty("slf4j.detectLoggerNameMismatch");

    static volatile SLF4JServiceProvider PROVIDER;

    private static final String[] API_COMPATIBILITY_LIST = new String[] { "2.0" };

    private static final String STATIC_LOGGER_BINDER_PATH = "org/slf4j/impl/StaticLoggerBinder.class";

    static List<SLF4JServiceProvider> findServiceProviders() {
        List<SLF4JServiceProvider> providerList = new ArrayList<>();
        ClassLoader classLoaderOfLoggerFactory = LoggerFactory.class.getClassLoader();
        SLF4JServiceProvider explicitProvider = loadExplicitlySpecified(classLoaderOfLoggerFactory);
        if (explicitProvider != null) {
            providerList.add(explicitProvider);
            return providerList;
        } else {
            ServiceLoader<SLF4JServiceProvider> serviceLoader = getServiceLoader(classLoaderOfLoggerFactory);
            Iterator<SLF4JServiceProvider> iterator = serviceLoader.iterator();
            while (iterator.hasNext()) {
                safelyInstantiate(providerList, iterator);
            }
            return providerList;
        }
    }

    private static ServiceLoader<SLF4JServiceProvider> getServiceLoader(ClassLoader classLoaderOfLoggerFactory) {
        SecurityManager securityManager = System.getSecurityManager();
        ServiceLoader<SLF4JServiceProvider> serviceLoader;
        if (securityManager == null) {
            serviceLoader = ServiceLoader.load(SLF4JServiceProvider.class, classLoaderOfLoggerFactory);
        } else {
            PrivilegedAction<ServiceLoader<SLF4JServiceProvider>> action = () -> ServiceLoader.load(SLF4JServiceProvider.class, classLoaderOfLoggerFactory);
            serviceLoader = AccessController.doPrivileged(action);
        }
        return serviceLoader;
    }

    private static void safelyInstantiate(List<SLF4JServiceProvider> providerList, Iterator<SLF4JServiceProvider> iterator) {
        try {
            SLF4JServiceProvider provider = iterator.next();
            providerList.add(provider);
        } catch (ServiceConfigurationError serviceConfigurationError) {
            Reporter.error("A service provider failed to instantiate:\n" + serviceConfigurationError.getMessage());
        }
    }

    private LoggerFactory() {
    }

    static void reset() {
        INITIALIZATION_STATE = 0;
    }

    private static final void performInitialization() {
        bind();
        if (INITIALIZATION_STATE == 3) {
            versionSanityCheck();
        }
    }

    private static final void bind() {
        try {
            List<SLF4JServiceProvider> providersList = findServiceProviders();
            reportMultipleBindingAmbiguity(providersList);
            if (providersList != null && !providersList.isEmpty()) {
                PROVIDER = providersList.get(0);
                PROVIDER.initialize();
                INITIALIZATION_STATE = 3;
                reportActualBinding(providersList);
            } else {
                INITIALIZATION_STATE = 4;
                Reporter.warn("No SLF4J providers were found.");
                Reporter.warn("Defaulting to no-operation (NOP) logger implementation");
                Reporter.warn("See https://www.slf4j.org/codes.html#noProviders for further details.");
                Set<URL> staticLoggerBinderPathSet = findPossibleStaticLoggerBinderPathSet();
                reportIgnoredStaticLoggerBinders(staticLoggerBinderPathSet);
            }
            postBindCleanUp();
        } catch (Exception exception) {
            failedBinding(exception);
            throw new IllegalStateException("Unexpected initialization failure", exception);
        }
    }

    static SLF4JServiceProvider loadExplicitlySpecified(ClassLoader classLoader) {
        String explicitlySpecified = System.getProperty("slf4j.provider");
        if (null != explicitlySpecified && !explicitlySpecified.isEmpty()) {
            try {
                String message = String.format("Attempting to load provider \"%s\" specified via \"%s\" system property", explicitlySpecified, "slf4j.provider");
                Reporter.info(message);
                Class<?> clazz = classLoader.loadClass(explicitlySpecified);
                Constructor<?> constructor = clazz.getConstructor();
                Object provider = constructor.newInstance();
                return (SLF4JServiceProvider) provider;
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException exception2) {
                String text = String.format("Failed to instantiate the specified SLF4JServiceProvider (%s)", explicitlySpecified);
                Reporter.error(text, exception2);
                return null;
            } catch (ClassCastException exception3) {
                String text2 = String.format("Specified SLF4JServiceProvider (%s) does not implement SLF4JServiceProvider interface", explicitlySpecified);
                Reporter.error(text2, exception3);
                return null;
            }
        } else {
            return null;
        }
    }

    private static void reportIgnoredStaticLoggerBinders(Set<URL> staticLoggerBinderPathSet) {
        if (!staticLoggerBinderPathSet.isEmpty()) {
            Reporter.warn("Class path contains SLF4J bindings targeting slf4j-api versions 1.7.x or earlier.");
            for (URL path : staticLoggerBinderPathSet) {
                Reporter.warn("Ignoring binding found at [" + path + "]");
            }
            Reporter.warn("See https://www.slf4j.org/codes.html#ignoredBindings for an explanation.");
        }
    }

    static Set<URL> findPossibleStaticLoggerBinderPathSet() {
        Set<URL> staticLoggerBinderPathSet = new LinkedHashSet<>();
        try {
            ClassLoader loggerFactoryClassLoader = LoggerFactory.class.getClassLoader();
            Enumeration<URL> paths;
            if (loggerFactoryClassLoader == null) {
                paths = ClassLoader.getSystemResources("org/slf4j/impl/StaticLoggerBinder.class");
            } else {
                paths = loggerFactoryClassLoader.getResources("org/slf4j/impl/StaticLoggerBinder.class");
            }
            while (paths.hasMoreElements()) {
                URL path = paths.nextElement();
                staticLoggerBinderPathSet.add(path);
            }
        } catch (IOException exception4) {
            Reporter.error("Error getting resources from path", exception4);
        }
        return staticLoggerBinderPathSet;
    }

    private static void postBindCleanUp() {
        fixSubstituteLoggers();
        replayEvents();
        SUBST_PROVIDER.getSubstituteLoggerFactory().clear();
    }

    private static void fixSubstituteLoggers() {
        synchronized (SUBST_PROVIDER) {
            SUBST_PROVIDER.getSubstituteLoggerFactory().postInitialization();
            for (SubstituteLogger substLogger : SUBST_PROVIDER.getSubstituteLoggerFactory().getLoggers()) {
                Logger logger = getLogger(substLogger.getName());
                substLogger.setDelegate(logger);
            }
        }
    }

    static void failedBinding(Throwable t) {
        INITIALIZATION_STATE = 2;
        Reporter.error("Failed to instantiate SLF4J LoggerFactory", t);
    }

    private static void replayEvents() {
        LinkedBlockingQueue<SubstituteLoggingEvent> queue = SUBST_PROVIDER.getSubstituteLoggerFactory().getEventQueue();
        int queueSize = queue.size();
        int count = 0;
        int maxDrain = 128;
        List<SubstituteLoggingEvent> eventList = new ArrayList<>(128);
        while (true) {
            int numDrained = queue.drainTo(eventList, 128);
            if (numDrained == 0) {
                return;
            }
            for (SubstituteLoggingEvent event : eventList) {
                replaySingleEvent(event);
                if (count++ == 0) {
                    emitReplayOrSubstituionWarning(event, queueSize);
                }
            }
            eventList.clear();
        }
    }

    private static void emitReplayOrSubstituionWarning(SubstituteLoggingEvent event, int queueSize) {
        if (event.getLogger().isDelegateEventAware()) {
            emitReplayWarning(queueSize);
        } else if (!event.getLogger().isDelegateNOP()) {
            emitSubstitutionWarning();
        }
    }

    private static void replaySingleEvent(SubstituteLoggingEvent event) {
        if (event != null) {
            SubstituteLogger substLogger = event.getLogger();
            String loggerName = substLogger.getName();
            if (substLogger.isDelegateNull()) {
                throw new IllegalStateException("Delegate logger cannot be null at this state.");
            } else {
                if (!substLogger.isDelegateNOP()) {
                    if (substLogger.isDelegateEventAware()) {
                        if (substLogger.isEnabledForLevel(event.getLevel())) {
                            substLogger.log(event);
                        }
                    } else {
                        Reporter.warn(loggerName);
                    }
                }
            }
        }
    }

    private static void emitSubstitutionWarning() {
        Reporter.warn("The following set of substitute loggers may have been accessed");
        Reporter.warn("during the initialization phase. Logging calls during this");
        Reporter.warn("phase were not honored. However, subsequent logging calls to these");
        Reporter.warn("loggers will work as normally expected.");
        Reporter.warn("See also https://www.slf4j.org/codes.html#substituteLogger");
    }

    private static void emitReplayWarning(int eventCount) {
        Reporter.warn("A number (" + eventCount + ") of logging calls during the initialization phase have been intercepted and are");
        Reporter.warn("now being replayed. These are subject to the filtering rules of the underlying logging system.");
        Reporter.warn("See also https://www.slf4j.org/codes.html#replay");
    }

    private static final void versionSanityCheck() {
        try {
            String requested = PROVIDER.getRequestedApiVersion();
            boolean match = false;
            for (String aAPI_COMPATIBILITY_LIST : API_COMPATIBILITY_LIST) {
                if (requested.startsWith(aAPI_COMPATIBILITY_LIST)) {
                    match = true;
                }
            }
            if (!match) {
                Reporter.warn("The requested version " + requested + " by your slf4j provider is not compatible with " + Arrays.asList(API_COMPATIBILITY_LIST).toString());
                Reporter.warn("See https://www.slf4j.org/codes.html#version_mismatch for further details.");
            }
        } catch (Throwable exception5) {
            Reporter.error("Unexpected problem occurred during version sanity check", exception5);
        }
    }

    private static boolean isAmbiguousProviderList(List<SLF4JServiceProvider> providerList) {
        return providerList.size() > 1;
    }

    private static void reportMultipleBindingAmbiguity(List<SLF4JServiceProvider> providerList) {
        if (isAmbiguousProviderList(providerList)) {
            Reporter.warn("Class path contains multiple SLF4J providers.");
            for (SLF4JServiceProvider provider : providerList) {
                Reporter.warn("Found provider [" + provider + "]");
            }
            Reporter.warn("See https://www.slf4j.org/codes.html#multiple_bindings for an explanation.");
        }
    }

    private static void reportActualBinding(List<SLF4JServiceProvider> providerList) {
        if (!providerList.isEmpty() && isAmbiguousProviderList(providerList)) {
            Reporter.info("Actual provider is of type [" + providerList.get(0) + "]");
        }
    }

    public static Logger getLogger(String name) {
        ILoggerFactory iLoggerFactory = getILoggerFactory();
        return iLoggerFactory.getLogger(name);
    }

    public static Logger getLogger(Class<?> clazz) {
        Logger logger = getLogger(clazz.getName());
        if (DETECT_LOGGER_NAME_MISMATCH) {
            Class<?> autoComputedCallingClass = Util.getCallingClass();
            if (autoComputedCallingClass != null && nonMatchingClasses(clazz, autoComputedCallingClass)) {
                Reporter.warn(String.format("Detected logger name mismatch. Given name: \"%s\"; computed name: \"%s\".", logger.getName(), autoComputedCallingClass.getName()));
                Reporter.warn("See https://www.slf4j.org/codes.html#loggerNameMismatch for an explanation");
            }
        }
        return logger;
    }

    private static boolean nonMatchingClasses(Class<?> clazz, Class<?> autoComputedCallingClass) {
        return !autoComputedCallingClass.isAssignableFrom(clazz);
    }

    public static ILoggerFactory getILoggerFactory() {
        return getProvider().getLoggerFactory();
    }

    static SLF4JServiceProvider getProvider() {
        if (INITIALIZATION_STATE == 0) {
            synchronized (LoggerFactory.class) {
                if (INITIALIZATION_STATE == 0) {
                    INITIALIZATION_STATE = 1;
                    performInitialization();
                }
            }
        }
        switch(INITIALIZATION_STATE) {
            case 1:
                return SUBST_PROVIDER;
            case 2:
                throw new IllegalStateException("org.slf4j.LoggerFactory in failed state. Original exception was thrown EARLIER. See also https://www.slf4j.org/codes.html#unsuccessfulInit");
            case 3:
                return PROVIDER;
            case 4:
                return NOP_FALLBACK_SERVICE_PROVIDER;
            default:
                throw new IllegalStateException("Unreachable code");
        }
    }
}
