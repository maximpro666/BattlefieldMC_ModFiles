package com.yourmod.teamsystem.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NotificationManager {
    private static final Queue<Notification> queue = new ConcurrentLinkedQueue<>();
    private static Notification currentNotification = null;
    private static int displayTimer = 0;
    private static final int DISPLAY_DURATION = 80;

    public static class Notification {
        public final String title;
        public final String subtitle;
        public final int color;
        public final int soundType;

        public Notification(String title, String subtitle, int color, int soundType) {
            this.title = title;
            this.subtitle = subtitle;
            this.color = color;
            this.soundType = soundType;
        }
    }

    public static void showNotification(String title, String subtitle) {
        showNotification(title, subtitle, 0xFF00AAFF, 1);
    }

    public static void showNotification(String title, String subtitle, int color) {
        showNotification(title, subtitle, color, 1);
    }

    public static void showNotification(String title, String subtitle, int color, int soundType) {
        queue.add(new Notification(title, subtitle, color, soundType));
    }

    public static Notification getCurrentNotification() {
        return currentNotification;
    }

    public static int getDisplayTimer() {
        return displayTimer;
    }

    public static void tick() {
        if (currentNotification != null) {
            displayTimer--;
            if (displayTimer <= 0) {
                currentNotification = null;
            }
        }
        if (currentNotification == null && !queue.isEmpty()) {
            currentNotification = queue.poll();
            displayTimer = DISPLAY_DURATION;
            playSound(currentNotification.soundType);
        }
    }

    private static void playSound(int soundType) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        switch (soundType) {
            case 1 -> mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
            case 2 -> mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0F));
        }
    }
}
