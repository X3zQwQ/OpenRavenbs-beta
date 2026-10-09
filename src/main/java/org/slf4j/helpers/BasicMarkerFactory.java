package org.slf4j.helpers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;

public class BasicMarkerFactory implements IMarkerFactory {

    private final ConcurrentMap<String, Marker> markerMap = new ConcurrentHashMap<>();

    @Override
    public Marker getMarker(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Marker name cannot be null");
        } else {
            Marker marker = this.markerMap.get(name);
            if (marker == null) {
                marker = new BasicMarker(name);
                Marker oldMarker = this.markerMap.putIfAbsent(name, marker);
                if (oldMarker != null) {
                    marker = oldMarker;
                }
            }
            return marker;
        }
    }

    @Override
    public boolean exists(String name) {
        return name == null ? false : this.markerMap.containsKey(name);
    }

    @Override
    public boolean detachMarker(String name) {
        return name == null ? false : this.markerMap.remove(name) != null;
    }

    @Override
    public Marker getDetachedMarker(String name) {
        return new BasicMarker(name);
    }
}
