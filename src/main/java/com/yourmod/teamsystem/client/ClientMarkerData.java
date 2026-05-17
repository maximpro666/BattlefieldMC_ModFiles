package com.yourmod.teamsystem.client;

import com.yourmod.teamsystem.core.MarkerData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientMarkerData {
    private static final List<MarkerData> markers = new ArrayList<>();

    public static void setMarkers(List<MarkerData> list) {
        synchronized (markers) {
            markers.clear();
            markers.addAll(list);
        }
    }

    public static List<MarkerData> getMarkers() {
        synchronized (markers) {
            return Collections.unmodifiableList(new ArrayList<>(markers));
        }
    }

    public static MarkerData getMarkerByName(String name) {
        synchronized (markers) {
            for (MarkerData m : markers) {
                if (m.getName().equals(name)) return m;
            }
        }
        return null;
    }
}
