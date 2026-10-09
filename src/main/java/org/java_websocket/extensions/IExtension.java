package org.java_websocket.extensions;

import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.Framedata;

public interface IExtension {

    void decodeFrame(Framedata framedata) throws InvalidDataException;

    void encodeFrame(Framedata framedata2);

    boolean acceptProvidedExtensionAsServer(String text);

    boolean acceptProvidedExtensionAsClient(String text2);

    void isFrameValid(Framedata framedata3) throws InvalidDataException;

    String getProvidedExtensionAsClient();

    String getProvidedExtensionAsServer();

    IExtension copyInstance();

    void reset();

    @Override
    String toString();
}
