package org.slf4j.spi;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;

public interface SLF4JServiceProvider {

    ILoggerFactory getLoggerFactory();

    IMarkerFactory getMarkerFactory();

    MDCAdapter getMDCAdapter();

    String getRequestedApiVersion();

    void initialize();
}
