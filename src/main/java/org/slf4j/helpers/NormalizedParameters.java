package org.slf4j.helpers;

import org.slf4j.event.LoggingEvent;

public class NormalizedParameters {

    final String message;

    final Object[] arguments;

    final Throwable throwable;

    public NormalizedParameters(String message, Object[] arguments, Throwable throwable) {
        this.message = message;
        this.arguments = arguments;
        this.throwable = throwable;
    }

    public NormalizedParameters(String message, Object[] arguments) {
        this(message, arguments, null);
    }

    public String getMessage() {
        return this.message;
    }

    public Object[] getArguments() {
        return this.arguments;
    }

    public Throwable getThrowable() {
        return this.throwable;
    }

    public static Throwable getThrowableCandidate(Object[] argArray) {
        if (argArray != null && argArray.length != 0) {
            Object lastEntry = argArray[argArray.length - 1];
            return lastEntry instanceof Throwable ? (Throwable) lastEntry : null;
        } else {
            return null;
        }
    }

    public static Object[] trimmedCopy(Object[] argArray) {
        if (argArray != null && argArray.length != 0) {
            int trimmedLen = argArray.length - 1;
            Object[] trimmed = new Object[trimmedLen];
            if (trimmedLen > 0) {
                System.arraycopy(argArray, 0, trimmed, 0, trimmedLen);
            }
            return trimmed;
        } else {
            throw new IllegalStateException("non-sensical empty or null argument array");
        }
    }

    public static NormalizedParameters normalize(String msg, Object[] arguments, Throwable t) {
        if (t != null) {
            return new NormalizedParameters(msg, arguments, t);
        } else if (arguments != null && arguments.length != 0) {
            Throwable throwableCandidate = getThrowableCandidate(arguments);
            if (throwableCandidate != null) {
                Object[] trimmedArguments = MessageFormatter.trimmedCopy(arguments);
                return new NormalizedParameters(msg, trimmedArguments, throwableCandidate);
            } else {
                return new NormalizedParameters(msg, arguments);
            }
        } else {
            return new NormalizedParameters(msg, arguments, t);
        }
    }

    public static NormalizedParameters normalize(LoggingEvent event) {
        return normalize(event.getMessage(), event.getArgumentArray(), event.getThrowable());
    }
}
