package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationOverlay {

    private static final int FADE_MS = 400;
    private static final int ENTRY_H = 26;

    public static class NotifEntry {
        public final String text;
        public final String type;
        public final long expireAt;
        public float alpha = 1f;
        public float slideOffset = 40f;

        public NotifEntry(String text, String type, int durationMs) {
            this.text = text;
            this.type = type != null ? type : "info";
            this.expireAt = System.currentTimeMillis() + durationMs;
        }
    }

    private static NotificationOverlay INSTANCE = null;
    private final List<NotifEntry> entries = new ArrayList<>();

    public NotificationOverlay() { INSTANCE = this; }

    public static void addNotification(String text, int durationMs) {
        addNotification(text, "info", durationMs);
    }

    public static void addNotification(String text, String type, int durationMs) {
        if (INSTANCE != null) INSTANCE.add(text, type, durationMs);
    }

    public void add(String text, String type, int durationMs) {
        entries.add(new NotifEntry(text, type, durationMs));
        if (entries.size() > 6) entries.remove(0);
    }

    public void render(GuiGraphics g, int screenWidth) {
        long now = System.currentTimeMillis();
        Iterator<NotifEntry> it = entries.iterator();
        while (it.hasNext()) {
            NotifEntry e = it.next();
            long remaining = e.expireAt - now;
            if (remaining <= 0) { it.remove(); continue; }
            e.alpha = (float) Math.min(1.0, remaining / (double) FADE_MS);
            e.slideOffset = AnimationHelper.lerp(e.slideOffset, 0f, 0.12f);
        }

        int startY = 54;
        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < entries.size(); i++) {
            NotifEntry e = entries.get(i);
            String icon = UITheme.getNotificationIcon(e.type);
            int accentColor = UITheme.getNotificationColor(e.type);
            String displayText = icon + " " + e.text;
            int tw = font.width(displayText);
            int pw = tw + 28;
            int x = screenWidth / 2 - pw / 2;
            int y = startY + i * (ENTRY_H + 4) + (int) e.slideOffset;

            int alphaInt = (int) (e.alpha * UITheme.ALPHA_HUD);

            RenderHelper.dropShadow(g, x, y, pw, ENTRY_H, 3, (int) (e.alpha * 100));
            RenderHelper.roundedRect(g, x, y, pw, ENTRY_H, 4,
                AnimationHelper.withAlpha(UITheme.BG_HUD, alphaInt));

            g.fill(x, y, x + 4, y + ENTRY_H,
                AnimationHelper.withAlpha(accentColor, (int) (e.alpha * 255)));

            g.fill(x + pw - 4, y, x + pw, y + ENTRY_H,
                AnimationHelper.withAlpha(accentColor, (int) (e.alpha * 80)));

            g.drawString(font, displayText, x + 12, y + 7,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int) (e.alpha * 255)));
        }
    }
}
