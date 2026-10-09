package org.slf4j.spi;

import org.slf4j.Logger;
import org.slf4j.Marker;

public interface LocationAwareLogger extends Logger {

    int TRACE_INT = 0;

    int DEBUG_INT = 10;

    int INFO_INT = 20;

    int WARN_INT = 30;

    int ERROR_INT = 40;

    void log(Marker marker, String text, int intValue, String text2, Object[] objectArray, Throwable exception);
}
