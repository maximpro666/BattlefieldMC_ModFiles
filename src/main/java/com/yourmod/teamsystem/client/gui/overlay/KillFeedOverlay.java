package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class KillFeedOverlay {

    private static final int COLOR_BG      = UITheme.BG_HUD;
    private static final int COLOR_KILLER  = UITheme.ACCENT;
    private static final int COLOR_TEXT    = UITheme.TEXT_PRIMARY;
    private static final int FADE_MS       = 400;
    private static final int DISPLAY_MS    = 6000;
    private static final int ENTRY_H       = 16;
    private static final int MAX_ENTRIES   = 6;

    public static class KillEntry {
        public final String killer;
        public final String victim;
        public final String weapon;
        public final long   expireAt;
        public final String formattedLine;
        public final int    lineWidth;
        public final int    killerWidth;

        public KillEntry(String killer, String victim, String weapon, Font font) {
            this.killer   = killer;
            this.victim   = victim;
            this.weapon   = weapon;
            this.expireAt = System.currentTimeMillis() + DISPLAY_MS;
            String line = killer + " \u2715 " + victim;
            if (weapon != null && !weapon.isEmpty()) line += " [" + weapon + "]";
            this.formattedLine = line;
            this.lineWidth = font.width(line);
            this.killerWidth = font.width(killer);
        }

        public float getAlpha(long now) {
            long remaining = expireAt - now;
            return (float) Math.min(1.0, remaining / (double) FADE_MS);
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

        int x = screenWidth - 4;
        int y = 4;
        Font font = Minecraft.getInstance().font;

        for (KillEntry e : entries) {
            float alpha = e.getAlpha(now);
            int lx = x - e.lineWidth - 8;

            g.fill(lx, y, lx + e.lineWidth + 8, y + ENTRY_H,
                AnimationHelper.withAlpha(COLOR_BG, (int)(alpha * 180)));
            g.drawString(font, e.killer,
                lx + 4, y + 4, AnimationHelper.withAlpha(COLOR_KILLER, (int)(alpha * 255)));
            g.drawString(font, " \u2715 " + e.victim,
                lx + 4 + e.killerWidth, y + 4, AnimationHelper.withAlpha(COLOR_TEXT, (int)(alpha * 200)));

            y += ENTRY_H + 2;
        }
    }
}
