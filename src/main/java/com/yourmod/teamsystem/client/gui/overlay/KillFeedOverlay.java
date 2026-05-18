package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Iterator;
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

        public KillEntry(String killer, String victim, String weapon) {
            this.killer   = killer;
            this.victim   = victim;
            this.weapon   = weapon;
            this.expireAt = System.currentTimeMillis() + DISPLAY_MS;
        }

        public float getAlpha() {
            long remaining = expireAt - System.currentTimeMillis();
            return (float) Math.min(1.0, remaining / (double) FADE_MS);
        }
    }

    private final List<KillEntry> entries = new ArrayList<>();

    public void addKill(String killer, String victim, String weapon) {
        entries.add(new KillEntry(killer, victim, weapon));
        if (entries.size() > MAX_ENTRIES) entries.remove(0);
    }

    public void render(GuiGraphics g, int screenWidth) {
        long now = System.currentTimeMillis();
        entries.removeIf(e -> e.expireAt < now);

        int x = screenWidth - 4;
        int y = 4;

        for (KillEntry e : entries) {
            float alpha = e.getAlpha();
            String line = e.killer + " \u2715 " + e.victim;
            if (e.weapon != null && !e.weapon.isEmpty()) line += " [" + e.weapon + "]";
            int tw = Minecraft.getInstance().font.width(line);
            int lx = x - tw - 8;

            g.fill(lx, y, lx + tw + 8, y + ENTRY_H,
                AnimationHelper.withAlpha(COLOR_BG, (int)(alpha * 180)));
            g.drawString(Minecraft.getInstance().font, e.killer,
                lx + 4, y + 4, AnimationHelper.withAlpha(COLOR_KILLER, (int)(alpha * 255)));
            int killerW = Minecraft.getInstance().font.width(e.killer);
            g.drawString(Minecraft.getInstance().font, " \u2715 " + e.victim,
                lx + 4 + killerW, y + 4, AnimationHelper.withAlpha(COLOR_TEXT, (int)(alpha * 200)));

            y += ENTRY_H + 2;
        }
    }
}
