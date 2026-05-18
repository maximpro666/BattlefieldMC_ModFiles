package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.List;
import java.util.ArrayList;

public class KillFeedOverlay implements IGuiOverlay {
    private static final int MAX_ENTRIES = 10;
    private static final int DISPLAY_TIME = 100;
    private static final int ENTRY_H = 12;

    private final List<KillFeedEntry> entries = new ArrayList<>();

    public static class KillFeedEntry {
        public final String killer;
        public final String victim;
        public final String weapon;
        public int timer;

        public KillFeedEntry(String killer, String victim, String weapon) {
            this.killer = killer;
            this.victim = victim;
            this.weapon = weapon;
            this.timer = DISPLAY_TIME;
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        int x = screenWidth - 200;
        int y = screenHeight / 2 - 100;

        List<KillFeedEntry> toRemove = new ArrayList<>();
        for (var entry : entries) {
            entry.timer--;
            if (entry.timer <= 0) {
                toRemove.add(entry);
                continue;
            }

            float alpha = Math.min(1, entry.timer / 20F);
            int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
            String text = entry.killer + " [" + entry.weapon + "] " + entry.victim;
            g.drawString(mc.font, text, x, y, color);
            y += ENTRY_H;
        }
        entries.removeAll(toRemove);
    }

    public void addEntry(String killer, String victim, String weapon) {
        entries.add(0, new KillFeedEntry(killer, victim, weapon));
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(entries.size() - 1);
        }
    }
}
