package org.java_websocket;

import java.util.List;
import org.java_websocket.drafts.Draft;

public interface WebSocketFactory {

    WebSocket createWebSocket(WebSocketAdapter webSocketAdapter, Draft draft);

    WebSocket createWebSocket(WebSocketAdapter webSocketAdapter2, List<Draft> list);
}
