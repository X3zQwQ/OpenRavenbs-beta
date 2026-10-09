package org.java_websocket.framing;

import org.java_websocket.enums.Opcode;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.exceptions.InvalidFrameException;

public abstract class ControlFrame extends FramedataImpl1 {

    public ControlFrame(Opcode opcode) {
        super(opcode);
    }

    @Override
    public void isValid() throws InvalidDataException {
        if (!this.isFin()) {
            throw new InvalidFrameException("Control frame can't have fin==false set");
        } else if (this.isRSV1()) {
            throw new InvalidFrameException("Control frame can't have rsv1==true set");
        } else if (this.isRSV2()) {
            throw new InvalidFrameException("Control frame can't have rsv2==true set");
        } else if (this.isRSV3()) {
            throw new InvalidFrameException("Control frame can't have rsv3==true set");
        }
    }
}
