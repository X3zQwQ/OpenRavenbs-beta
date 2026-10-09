package org.java_websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

public interface WrappedByteChannel extends ByteChannel {

    boolean isNeedWrite();

    void writeMore() throws IOException;

    boolean isNeedRead();

    int readMore(ByteBuffer byteBuffer) throws IOException;

    boolean isBlocking();
}
