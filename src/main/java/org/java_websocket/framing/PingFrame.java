package org.java_websocket.framing;

import org.java_websocket.enums.Opcode;

public class PingFrame extends ControlFrame {

    public PingFrame() {
        super(Opcode.PING);
    }
}
