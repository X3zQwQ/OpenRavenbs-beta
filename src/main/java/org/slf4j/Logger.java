package org.slf4j;

import org.slf4j.event.Level;
import org.slf4j.helpers.CheckReturnValue;
import org.slf4j.spi.DefaultLoggingEventBuilder;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.NOPLoggingEventBuilder;

public interface Logger {

    String ROOT_LOGGER_NAME = "ROOT";

    String getName();

    default LoggingEventBuilder makeLoggingEventBuilder(Level level) {
        return new DefaultLoggingEventBuilder(this, level);
    }

    @CheckReturnValue
    default LoggingEventBuilder atLevel(Level level) {
        return this.isEnabledForLevel(level) ? this.makeLoggingEventBuilder(level) : NOPLoggingEventBuilder.singleton();
    }

    default boolean isEnabledForLevel(Level level) {
        int levelInt = level.toInt();
        switch(levelInt) {
            case 0:
                return this.isTraceEnabled();
            case 10:
                return this.isDebugEnabled();
            case 20:
                return this.isInfoEnabled();
            case 30:
                return this.isWarnEnabled();
            case 40:
                return this.isErrorEnabled();
            default:
                throw new IllegalArgumentException("Level [" + level + "] not recognized.");
        }
    }

    boolean isTraceEnabled();

    void trace(String text);

    void trace(String text2, Object object);

    void trace(String text3, Object object2, Object object3);

    void trace(String text4, Object... object4);

    void trace(String text5, Throwable exception);

    boolean isTraceEnabled(Marker marker);

    @CheckReturnValue
    default LoggingEventBuilder atTrace() {
        return this.isTraceEnabled() ? this.makeLoggingEventBuilder(Level.TRACE) : NOPLoggingEventBuilder.singleton();
    }

    void trace(Marker marker2, String text6);

    void trace(Marker marker3, String text7, Object object5);

    void trace(Marker marker4, String text8, Object object6, Object object7);

    void trace(Marker marker5, String text9, Object... object8);

    void trace(Marker marker6, String text10, Throwable exception2);

    boolean isDebugEnabled();

    void debug(String text11);

    void debug(String text12, Object object9);

    void debug(String text13, Object object10, Object object11);

    void debug(String text14, Object... object12);

    void debug(String text15, Throwable exception3);

    boolean isDebugEnabled(Marker marker7);

    void debug(Marker marker8, String text16);

    void debug(Marker marker9, String text17, Object object13);

    void debug(Marker marker10, String text18, Object object14, Object object15);

    void debug(Marker marker11, String text19, Object... object16);

    void debug(Marker marker12, String text20, Throwable exception4);

    @CheckReturnValue
    default LoggingEventBuilder atDebug() {
        return this.isDebugEnabled() ? this.makeLoggingEventBuilder(Level.DEBUG) : NOPLoggingEventBuilder.singleton();
    }

    boolean isInfoEnabled();

    void info(String text21);

    void info(String text22, Object object17);

    void info(String text23, Object object18, Object object19);

    void info(String text24, Object... object20);

    void info(String text25, Throwable exception5);

    boolean isInfoEnabled(Marker marker13);

    void info(Marker marker14, String text26);

    void info(Marker marker15, String text27, Object object21);

    void info(Marker marker16, String text28, Object object22, Object object23);

    void info(Marker marker17, String text29, Object... object24);

    void info(Marker marker18, String text30, Throwable exception6);

    @CheckReturnValue
    default LoggingEventBuilder atInfo() {
        return this.isInfoEnabled() ? this.makeLoggingEventBuilder(Level.INFO) : NOPLoggingEventBuilder.singleton();
    }

    boolean isWarnEnabled();

    void warn(String text31);

    void warn(String text32, Object object25);

    void warn(String text33, Object... object26);

    void warn(String text34, Object object27, Object object28);

    void warn(String text35, Throwable exception7);

    boolean isWarnEnabled(Marker marker19);

    void warn(Marker marker20, String text36);

    void warn(Marker marker21, String text37, Object object29);

    void warn(Marker marker22, String text38, Object object30, Object object31);

    void warn(Marker marker23, String text39, Object... object32);

    void warn(Marker marker24, String text40, Throwable exception8);

    @CheckReturnValue
    default LoggingEventBuilder atWarn() {
        return this.isWarnEnabled() ? this.makeLoggingEventBuilder(Level.WARN) : NOPLoggingEventBuilder.singleton();
    }

    boolean isErrorEnabled();

    void error(String text41);

    void error(String text42, Object object33);

    void error(String text43, Object object34, Object object35);

    void error(String text44, Object... object36);

    void error(String text45, Throwable exception9);

    boolean isErrorEnabled(Marker marker25);

    void error(Marker marker26, String text46);

    void error(Marker marker27, String text47, Object object37);

    void error(Marker marker28, String text48, Object object38, Object object39);

    void error(Marker marker29, String text49, Object... object40);

    void error(Marker marker30, String text50, Throwable exception10);

    @CheckReturnValue
    default LoggingEventBuilder atError() {
        return this.isErrorEnabled() ? this.makeLoggingEventBuilder(Level.ERROR) : NOPLoggingEventBuilder.singleton();
    }
}
