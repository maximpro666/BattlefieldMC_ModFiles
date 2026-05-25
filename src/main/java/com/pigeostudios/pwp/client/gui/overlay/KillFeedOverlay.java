package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;

public class KillFeedOverlay {

    private static final int FADE_MS = 500;
    private static final int DISPLAY_MS = 7000;
    private static final int ENTRY_H = 20;
    private static final int MAX_ENTRIES = 6;

    public static class KillEntry {
        public final String killer;
        public final String victim;
        public final long expireAt;
        public final String formattedLine;
        public final int lineWidth;
        public final int killerWidth;

        public KillEntry(String killer, String victim, String weapon, Font font) {
            this.killer = killer;
            this.victim = victim;
            this.expireAt = System.currentTimeMillis() + DISPLAY_MS;
            String line = killer + " \u279C " + victim;
            if (weapon != null && !weapon.isEmpty()) line += "  [" + weapon + "]";
            this.formattedLine = line;
            this.lineWidth = font.width(line);
            this.killerWidth = font.width(killer);
        }

        public float getAlpha(long now) {
            long remaining = expireAt - now;
            return (float) Math.max(0, Math.min(1.0, remaining / (double) FADE_MS));
        }
    }

    private final List<KillEntry> entries = new ArrayList<>();

    public void addKill(String killer, String victim, String weapon) {
        Font font = Minecraft.getInstance().font;
        entries.add(new KillEntry(killer, victim, weapon, font));
        if (entries.size() > MAX_ENTRIES) entries.remove(0);
    }

    public void render(GuiGraphics g, int screenWidth) {
        long now = System.currentTimeMillis();
        entries.removeIf(e -> e.expireAt < now);

        int x = screenWidth - 10;
        int y = 4;
        Font font = Minecraft.getInstance().font;

        for (KillEntry e : entries) {
            float alpha = e.getAlpha(now);
            int lx = x - e.lineWidth - 16;

            RenderHelper.dropShadow(g, lx, y, e.lineWidth + 16, ENTRY_H, 3, (int) (alpha * 100));
            RenderHelper.roundedRect(g, lx, y, e.lineWidth + 16, ENTRY_H, 4,
                AnimationHelper.withAlpha(UITheme.BG_HUD, (int) (alpha * UITheme.ALPHA_HUD)));

            g.fill(lx, y, lx + e.lineWidth + 16, y + 1,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int) (alpha * 150)));

            g.fill(lx, y, lx + 3, y + ENTRY_H,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int) (alpha * 255)));

            g.drawString(font, e.killer, lx + 8, y + 5,
                AnimationHelper.withAlpha(UITheme.KILLFEED_KILLER, (int) (alpha * 255)));

            String sep = " \u279C ";
            g.drawString(font, sep + e.victim, lx + 8 + e.killerWidth, y + 5,
                AnimationHelper.withAlpha(UITheme.KILLFEED_VICTIM, (int) (alpha * 220)));

            y += ENTRY_H + 4;
        }
    }
}
