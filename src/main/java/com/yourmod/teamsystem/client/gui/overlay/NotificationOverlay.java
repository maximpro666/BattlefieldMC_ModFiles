package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationOverlay {

    private static final int COLOR_BG     = UITheme.BG_HUD;
    private static final int COLOR_ORANGE = UITheme.ACCENT;
    private static final int COLOR_TEXT   = UITheme.TEXT_PRIMARY;
    private static final int FADE_MS      = 300;
    private static final int ENTRY_H      = 18;

    public static class NotifEntry {
        public final String text;
        public final long   expireAt;
        public float        alpha = 1f;

        public NotifEntry(String text, int durationMs) {
            this.text     = text;
            this.expireAt = System.currentTimeMillis() + durationMs;
        }
    }

    private static NotificationOverlay INSTANCE = null;
    private final List<NotifEntry> entries = new ArrayList<>();

    public NotificationOverlay() {
        INSTANCE = this;
    }

    public static void addNotification(String text, int durationMs) {
        if (INSTANCE != null) {
            INSTANCE.add(text, durationMs);
        }
    }

    public void add(String text, int durationMs) {
        entries.add(new NotifEntry(text, durationMs));
        if (entries.size() > 5) entries.remove(0);
    }

    public void render(GuiGraphics g, int screenWidth) {
        long now = System.currentTimeMillis();
        Iterator<NotifEntry> it = entries.iterator();
        while (it.hasNext()) {
            NotifEntry e = it.next();
            long remaining = e.expireAt - now;
            if (remaining <= 0) { it.remove(); continue; }
            e.alpha = (float) Math.min(1.0, remaining / (double) FADE_MS);
        }

        int startY = 52;
        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < entries.size(); i++) {
            NotifEntry e = entries.get(i);
            int tw = font.width(e.text);
            int x  = screenWidth / 2 - tw / 2 - 4;
            int y  = startY + i * (ENTRY_H + 2);

            g.fill(x, y, x + tw + 8, y + ENTRY_H, AnimationHelper.withAlpha(COLOR_BG, (int)(e.alpha * 200)));
            g.fill(x, y, x + 2, y + ENTRY_H, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(e.alpha * 255)));
            g.drawString(font, e.text, x + 6, y + 5,
                AnimationHelper.withAlpha(COLOR_TEXT, (int)(e.alpha * 255)));
        }
    }
}
