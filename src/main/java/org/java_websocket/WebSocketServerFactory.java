package org.java_websocket;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import org.java_websocket.drafts.Draft;

public interface WebSocketServerFactory extends WebSocketFactory {

    WebSocketImpl createWebSocket(WebSocketAdapter webSocketAdapter, Draft draft);

    WebSocketImpl createWebSocket(WebSocketAdapter webSocketAdapter2, List<Draft> list);

    ByteChannel wrapChannel(SocketChannel socketChannel, SelectionKey selectionKey) throws IOException;

    void close();
}
