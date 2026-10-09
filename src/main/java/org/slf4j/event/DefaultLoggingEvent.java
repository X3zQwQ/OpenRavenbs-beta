package org.slf4j.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.Marker;

public class DefaultLoggingEvent implements LoggingEvent {

    Logger logger;

    Level level;

    String message;

    List<Marker> markers;

    List<Object> arguments;

    List<KeyValuePair> keyValuePairs;

    Throwable throwable;

    String threadName;

    long timeStamp;

    String callerBoundary;

    public DefaultLoggingEvent(Level level, Logger logger) {
        this.logger = logger;
        this.level = level;
    }

    public void addMarker(Marker marker) {
        if (this.markers == null) {
            this.markers = new ArrayList<>(2);
        }
        this.markers.add(marker);
    }

    @Override
    public List<Marker> getMarkers() {
        return this.markers;
    }

    public void addArgument(Object p) {
        this.getNonNullArguments().add(p);
    }

    public void addArguments(Object... args) {
        this.getNonNullArguments().addAll(Arrays.asList(args));
    }

    private List<Object> getNonNullArguments() {
        if (this.arguments == null) {
            this.arguments = new ArrayList<>(3);
        }
        return this.arguments;
    }

    @Override
    public List<Object> getArguments() {
        return this.arguments;
    }

    @Override
    public Object[] getArgumentArray() {
        return this.arguments == null ? null : this.arguments.toArray();
    }

    public void addKeyValue(String key, Object value) {
        this.getNonnullKeyValuePairs().add(new KeyValuePair(key, value));
    }

    private List<KeyValuePair> getNonnullKeyValuePairs() {
        if (this.keyValuePairs == null) {
            this.keyValuePairs = new ArrayList<>(4);
        }
        return this.keyValuePairs;
    }

    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        return this.keyValuePairs;
    }

    public void setThrowable(Throwable cause) {
        this.throwable = cause;
    }

    @Override
    public Level getLevel() {
        return this.level;
    }

    @Override
    public String getLoggerName() {
        return this.logger.getName();
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Throwable getThrowable() {
        return this.throwable;
    }

    @Override
    public String getThreadName() {
        return this.threadName;
    }

    @Override
    public long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setCallerBoundary(String fqcn) {
        this.callerBoundary = fqcn;
    }

    @Override
    public String getCallerBoundary() {
        return this.callerBoundary;
    }
}
