package org.slf4j.helpers;

import org.slf4j.Logger;
import org.slf4j.Marker;

public class NOPLogger extends NamedLoggerBase implements Logger {

    private static final long serialVersionUID = -517220405410904473L;

    public static final NOPLogger NOP_LOGGER = new NOPLogger();

    protected NOPLogger() {
    }

    @Override
    public String getName() {
        return "NOP";
    }

    @Override
    public final boolean isTraceEnabled() {
        return false;
    }

    @Override
    public final void trace(String msg) {
    }

    @Override
    public final void trace(String format, Object arg) {
    }

    @Override
    public final void trace(String format, Object arg1, Object arg2) {
    }

    @Override
    public final void trace(String format, Object... argArray) {
    }

    @Override
    public final void trace(String msg, Throwable t) {
    }

    @Override
    public final boolean isDebugEnabled() {
        return false;
    }

    @Override
    public final void debug(String msg) {
    }

    @Override
    public final void debug(String format, Object arg) {
    }

    @Override
    public final void debug(String format, Object arg1, Object arg2) {
    }

    @Override
    public final void debug(String format, Object... argArray) {
    }

    @Override
    public final void debug(String msg, Throwable t) {
    }

    @Override
    public final boolean isInfoEnabled() {
        return false;
    }

    @Override
    public final void info(String msg) {
    }

    @Override
    public final void info(String format, Object arg1) {
    }

    @Override
    public final void info(String format, Object arg1, Object arg2) {
    }

    @Override
    public final void info(String format, Object... argArray) {
    }

    @Override
    public final void info(String msg, Throwable t) {
    }

    @Override
    public final boolean isWarnEnabled() {
        return false;
    }

    @Override
    public final void warn(String msg) {
    }

    @Override
    public final void warn(String format, Object arg1) {
    }

    @Override
    public final void warn(String format, Object arg1, Object arg2) {
    }

    @Override
    public final void warn(String format, Object... argArray) {
    }

    @Override
    public final void warn(String msg, Throwable t) {
    }

    @Override
    public final boolean isErrorEnabled() {
        return false;
    }

    @Override
    public final void error(String msg) {
    }

    @Override
    public final void error(String format, Object arg1) {
    }

    @Override
    public final void error(String format, Object arg1, Object arg2) {
    }

    @Override
    public final void error(String format, Object... argArray) {
    }

    @Override
    public final void error(String msg, Throwable t) {
    }

    @Override
    public final boolean isTraceEnabled(Marker marker) {
        return false;
    }

    @Override
    public final void trace(Marker marker, String msg) {
    }

    @Override
    public final void trace(Marker marker, String format, Object arg) {
    }

    @Override
    public final void trace(Marker marker, String format, Object arg1, Object arg2) {
    }

    @Override
    public final void trace(Marker marker, String format, Object... argArray) {
    }

    @Override
    public final void trace(Marker marker, String msg, Throwable t) {
    }

    @Override
    public final boolean isDebugEnabled(Marker marker) {
        return false;
    }

    @Override
    public final void debug(Marker marker, String msg) {
    }

    @Override
    public final void debug(Marker marker, String format, Object arg) {
    }

    @Override
    public final void debug(Marker marker, String format, Object arg1, Object arg2) {
    }

    @Override
    public final void debug(Marker marker, String format, Object... arguments) {
    }

    @Override
    public final void debug(Marker marker, String msg, Throwable t) {
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    @Override
    public final void info(Marker marker, String msg) {
    }

    @Override
    public final void info(Marker marker, String format, Object arg) {
    }

    @Override
    public final void info(Marker marker, String format, Object arg1, Object arg2) {
    }

    @Override
    public final void info(Marker marker, String format, Object... arguments) {
    }

    @Override
    public final void info(Marker marker, String msg, Throwable t) {
    }

    @Override
    public final boolean isWarnEnabled(Marker marker) {
        return false;
    }

    @Override
    public final void warn(Marker marker, String msg) {
    }

    @Override
    public final void warn(Marker marker, String format, Object arg) {
    }

    @Override
    public final void warn(Marker marker, String format, Object arg1, Object arg2) {
    }

    @Override
    public final void warn(Marker marker, String format, Object... arguments) {
    }

    @Override
    public final void warn(Marker marker, String msg, Throwable t) {
    }

    @Override
    public final boolean isErrorEnabled(Marker marker) {
        return false;
    }

    @Override
    public final void error(Marker marker, String msg) {
    }

    @Override
    public final void error(Marker marker, String format, Object arg) {
    }

    @Override
    public final void error(Marker marker, String format, Object arg1, Object arg2) {
    }

    @Override
    public final void error(Marker marker, String format, Object... arguments) {
    }

    @Override
    public final void error(Marker marker, String msg, Throwable t) {
    }
}
