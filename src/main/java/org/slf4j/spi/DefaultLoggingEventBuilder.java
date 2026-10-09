package org.slf4j.spi;

import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.DefaultLoggingEvent;
import org.slf4j.event.KeyValuePair;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;

public class DefaultLoggingEventBuilder implements LoggingEventBuilder, CallerBoundaryAware {

    static String DLEB_FQCN = DefaultLoggingEventBuilder.class.getName();

    protected DefaultLoggingEvent loggingEvent;

    protected Logger logger;

    public DefaultLoggingEventBuilder(Logger logger, Level level) {
        this.logger = logger;
        this.loggingEvent = new DefaultLoggingEvent(level, logger);
    }

    @Override
    public LoggingEventBuilder addMarker(Marker marker) {
        this.loggingEvent.addMarker(marker);
        return this;
    }

    @Override
    public LoggingEventBuilder setCause(Throwable t) {
        this.loggingEvent.setThrowable(t);
        return this;
    }

    @Override
    public LoggingEventBuilder addArgument(Object p) {
        this.loggingEvent.addArgument(p);
        return this;
    }

    @Override
    public LoggingEventBuilder addArgument(Supplier<?> objectSupplier) {
        this.loggingEvent.addArgument(objectSupplier.get());
        return this;
    }

    @Override
    public LoggingEventBuilder addKeyValue(String key, Object value) {
        this.loggingEvent.addKeyValue(key, value);
        return this;
    }

    @Override
    public LoggingEventBuilder addKeyValue(String key, Supplier<Object> value) {
        this.loggingEvent.addKeyValue(key, value.get());
        return this;
    }

    @Override
    public void setCallerBoundary(String fqcn) {
        this.loggingEvent.setCallerBoundary(fqcn);
    }

    @Override
    public void log() {
        this.log(this.loggingEvent);
    }

    @Override
    public LoggingEventBuilder setMessage(String message) {
        this.loggingEvent.setMessage(message);
        return this;
    }

    @Override
    public LoggingEventBuilder setMessage(Supplier<String> messageSupplier) {
        this.loggingEvent.setMessage(messageSupplier.get());
        return this;
    }

    @Override
    public void log(String message) {
        this.loggingEvent.setMessage(message);
        this.log(this.loggingEvent);
    }

    @Override
    public void log(String message, Object arg) {
        this.loggingEvent.setMessage(message);
        this.loggingEvent.addArgument(arg);
        this.log(this.loggingEvent);
    }

    @Override
    public void log(String message, Object arg0, Object arg1) {
        this.loggingEvent.setMessage(message);
        this.loggingEvent.addArgument(arg0);
        this.loggingEvent.addArgument(arg1);
        this.log(this.loggingEvent);
    }

    @Override
    public void log(String message, Object... args) {
        this.loggingEvent.setMessage(message);
        this.loggingEvent.addArguments(args);
        this.log(this.loggingEvent);
    }

    @Override
    public void log(Supplier<String> messageSupplier) {
        if (messageSupplier == null) {
            this.log((String) null);
        } else {
            this.log(messageSupplier.get());
        }
    }

    protected void log(LoggingEvent aLoggingEvent) {
        if (aLoggingEvent.getCallerBoundary() == null) {
            this.setCallerBoundary(DLEB_FQCN);
        }
        if (this.logger instanceof LoggingEventAware) {
            ((LoggingEventAware) this.logger).log(aLoggingEvent);
        } else if (this.logger instanceof LocationAwareLogger) {
            this.logViaLocationAwareLoggerAPI((LocationAwareLogger) this.logger, aLoggingEvent);
        } else {
            this.logViaPublicSLF4JLoggerAPI(aLoggingEvent);
        }
    }

    private void logViaLocationAwareLoggerAPI(LocationAwareLogger locationAwareLogger, LoggingEvent aLoggingEvent) {
        String msg = aLoggingEvent.getMessage();
        List<Marker> markerList = aLoggingEvent.getMarkers();
        String mergedMessage = this.mergeMarkersAndKeyValuePairsAndMessage(aLoggingEvent);
        locationAwareLogger.log(null, aLoggingEvent.getCallerBoundary(), aLoggingEvent.getLevel().toInt(), mergedMessage, aLoggingEvent.getArgumentArray(), aLoggingEvent.getThrowable());
    }

    private void logViaPublicSLF4JLoggerAPI(LoggingEvent aLoggingEvent) {
        Object[] argArray = aLoggingEvent.getArgumentArray();
        int argLen = argArray == null ? 0 : argArray.length;
        Throwable t = aLoggingEvent.getThrowable();
        int tLen = t == null ? 0 : 1;
        Object[] combinedArguments = new Object[argLen + tLen];
        if (argArray != null) {
            System.arraycopy(argArray, 0, combinedArguments, 0, argLen);
        }
        if (t != null) {
            combinedArguments[argLen] = t;
        }
        String mergedMessage = this.mergeMarkersAndKeyValuePairsAndMessage(aLoggingEvent);
        switch(aLoggingEvent.getLevel()) {
            case TRACE:
                this.logger.trace(mergedMessage, combinedArguments);
                break;
            case DEBUG:
                this.logger.debug(mergedMessage, combinedArguments);
                break;
            case INFO:
                this.logger.info(mergedMessage, combinedArguments);
                break;
            case WARN:
                this.logger.warn(mergedMessage, combinedArguments);
                break;
            case ERROR:
                this.logger.error(mergedMessage, combinedArguments);
        }
    }

    private String mergeMarkersAndKeyValuePairsAndMessage(LoggingEvent aLoggingEvent) {
        StringBuilder sb = this.mergeMarkers(aLoggingEvent.getMarkers(), null);
        sb = this.mergeKeyValuePairs(aLoggingEvent.getKeyValuePairs(), sb);
        return this.mergeMessage(aLoggingEvent.getMessage(), sb);
    }

    private StringBuilder mergeMarkers(List<Marker> markerList, StringBuilder sb) {
        if (markerList != null && !markerList.isEmpty()) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            for (Marker marker : markerList) {
                sb.append(marker);
                sb.append(' ');
            }
            return sb;
        } else {
            return sb;
        }
    }

    private StringBuilder mergeKeyValuePairs(List<KeyValuePair> keyValuePairList, StringBuilder sb) {
        if (keyValuePairList != null && !keyValuePairList.isEmpty()) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            for (KeyValuePair kvp : keyValuePairList) {
                sb.append(kvp.key);
                sb.append('=');
                sb.append(kvp.value);
                sb.append(' ');
            }
            return sb;
        } else {
            return sb;
        }
    }

    private String mergeMessage(String msg, StringBuilder sb) {
        if (sb != null) {
            sb.append(msg);
            return sb.toString();
        } else {
            return msg;
        }
    }
}
