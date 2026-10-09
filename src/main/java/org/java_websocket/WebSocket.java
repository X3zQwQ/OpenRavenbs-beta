package org.java_websocket;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import javax.net.ssl.SSLSession;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.Opcode;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.Framedata;
import org.java_websocket.protocols.IProtocol;

public interface WebSocket {

    void close(int intValue, String text);

    void close(int intValue2);

    void close();

    void closeConnection(int intValue3, String text2);

    void send(String text3);

    void send(ByteBuffer byteBuffer);

    void send(byte[] byteArray);

    void sendFrame(Framedata framedata);

    void sendFrame(Collection<Framedata> collection);

    void sendPing();

    void sendFragmentedFrame(Opcode opcode, ByteBuffer byteBuffer2, boolean enabled);

    boolean hasBufferedData();

    InetSocketAddress getRemoteSocketAddress();

    InetSocketAddress getLocalSocketAddress();

    boolean isOpen();

    boolean isClosing();

    boolean isFlushAndClose();

    boolean isClosed();

    Draft getDraft();

    ReadyState getReadyState();

    String getResourceDescriptor();

    <T> void setAttachment(T attachment);

    <T> T getAttachment();

    boolean hasSSLSupport();

    SSLSession getSSLSession() throws IllegalArgumentException;

    IProtocol getProtocol();
}
