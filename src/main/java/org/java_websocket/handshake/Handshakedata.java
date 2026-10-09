package org.java_websocket.handshake;

import java.util.Iterator;

public interface Handshakedata {

    Iterator<String> iterateHttpFields();

    String getFieldValue(String text);

    boolean hasFieldValue(String text2);

    byte[] getContent();
}
