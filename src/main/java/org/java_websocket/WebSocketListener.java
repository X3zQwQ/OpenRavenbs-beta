package org.java_websocket;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.framing.PingFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.Handshakedata;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;

public interface WebSocketListener {

    ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket webSocket, Draft draft, ClientHandshake clientHandshake) throws InvalidDataException;

    void onWebsocketHandshakeReceivedAsClient(WebSocket webSocket2, ClientHandshake clientHandshake2, ServerHandshake serverHandshake) throws InvalidDataException;

    void onWebsocketHandshakeSentAsClient(WebSocket webSocket3, ClientHandshake clientHandshake3) throws InvalidDataException;

    void onWebsocketMessage(WebSocket webSocket4, String text);

    void onWebsocketMessage(WebSocket webSocket5, ByteBuffer byteBuffer);

    void onWebsocketOpen(WebSocket webSocket6, Handshakedata handshakedata);

    void onWebsocketClose(WebSocket webSocket7, int intValue, String text2, boolean enabled);

    void onWebsocketClosing(WebSocket webSocket8, int intValue2, String text3, boolean enabled2);

    void onWebsocketCloseInitiated(WebSocket webSocket9, int intValue3, String text4);

    void onWebsocketError(WebSocket webSocket10, Exception exception);

    void onWebsocketPing(WebSocket webSocket11, Framedata framedata);

    PingFrame onPreparePing(WebSocket webSocket12);

    void onWebsocketPong(WebSocket webSocket13, Framedata framedata2);

    void onWriteDemand(WebSocket webSocket14);

    InetSocketAddress getLocalSocketAddress(WebSocket webSocket15);

    InetSocketAddress getRemoteSocketAddress(WebSocket webSocket16);
}
