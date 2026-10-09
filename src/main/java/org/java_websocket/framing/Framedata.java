package org.java_websocket.framing;

import java.nio.ByteBuffer;
import org.java_websocket.enums.Opcode;

public interface Framedata {

    boolean isFin();

    boolean isRSV1();

    boolean isRSV2();

    boolean isRSV3();

    boolean getTransfereMasked();

    Opcode getOpcode();

    ByteBuffer getPayloadData();

    void append(Framedata framedata);
}
