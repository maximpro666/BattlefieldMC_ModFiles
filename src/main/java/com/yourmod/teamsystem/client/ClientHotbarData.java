package com.yourmod.teamsystem.client;

public class ClientHotbarData {
    private static int selectedSlot = 0;
    private static long lastChangeTime = 0;

    public static void setSelectedSlot(int slot) {
        if (slot != selectedSlot) {
            selectedSlot = slot;
            lastChangeTime = System.currentTimeMillis();
        }
    }

    public static int getSelectedSlot() {
        return selectedSlot;
    }

    public static long getLastChangeTime() {
        return lastChangeTime;
    }

    public static float getFadeAlpha() {
        long elapsed = System.currentTimeMillis() - lastChangeTime;
        if (elapsed > 500) return 1.0f;
        return (float) elapsed / 500.0f;
    }
}
