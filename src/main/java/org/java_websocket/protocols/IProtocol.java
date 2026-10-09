package org.java_websocket.protocols;

public interface IProtocol {

    boolean acceptProvidedProtocol(String text);

    String getProvidedProtocol();

    IProtocol copyInstance();

    @Override
    String toString();
}
