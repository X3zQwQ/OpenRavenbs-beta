package org.slf4j.spi;

import java.util.function.Supplier;
import org.slf4j.Marker;
import org.slf4j.helpers.CheckReturnValue;

public interface LoggingEventBuilder {

    @CheckReturnValue
    LoggingEventBuilder setCause(Throwable cause);

    @CheckReturnValue
    LoggingEventBuilder addMarker(Marker marker);

    @CheckReturnValue
    LoggingEventBuilder addArgument(Object object);

    @CheckReturnValue
    LoggingEventBuilder addArgument(Supplier<?> supplier);

    @CheckReturnValue
    LoggingEventBuilder addKeyValue(String text, Object object2);

    @CheckReturnValue
    LoggingEventBuilder addKeyValue(String text2, Supplier<Object> supplier2);

    @CheckReturnValue
    LoggingEventBuilder setMessage(String message);

    @CheckReturnValue
    LoggingEventBuilder setMessage(Supplier<String> message2);

    void log();

    void log(String text3);

    void log(String text4, Object object3);

    void log(String text5, Object object4, Object object5);

    void log(String text6, Object... object6);

    void log(Supplier<String> supplier3);
}
