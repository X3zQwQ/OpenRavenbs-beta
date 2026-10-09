package org.slf4j;

public interface IMarkerFactory {

    Marker getMarker(String text);

    boolean exists(String text2);

    boolean detachMarker(String text3);

    Marker getDetachedMarker(String text4);
}
