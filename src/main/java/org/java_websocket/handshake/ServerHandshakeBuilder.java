package org.java_websocket.handshake;

public interface ServerHandshakeBuilder extends HandshakeBuilder, ServerHandshake {

    void setHttpStatus(short httpStatus);

    void setHttpStatusMessage(String httpStatusMessage);
}
