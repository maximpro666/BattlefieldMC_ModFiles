package com.yourmod.teamsystem.client.gui.component;

import com.yourmod.teamsystem.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class SlotBadge {

    private static final int SIZE = 20;

    public static void draw(GuiGraphics g, int x, int y, int current, int max, float fade) {
        var font = Minecraft.getInstance().font;
        boolean full = current >= max;
        String text = full ? "\u2715" : current + "/" + max;
        int bg = full
            ? AnimationHelper.withAlpha(0xCC222222, (int)(fade * 0xDD))
            : AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xBB));
        int border = full
            ? AnimationHelper.withAlpha(UITheme.STATUS_DANGER, (int)(fade * 0xAA))
            : AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0x99));
        int color = full
            ? AnimationHelper.withAlpha(UITheme.STATUS_DANGER, (int)(fade * 200))
            : AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fade * 200));

        g.fill(x, y, x + SIZE, y + SIZE, bg);
        g.fill(x, y, x + SIZE, y + 1, border);
        g.fill(x, y + SIZE - 1, x + SIZE, y + SIZE, border);
        g.fill(x, y, x + 1, y + SIZE, border);
        g.fill(x + SIZE - 1, y, x + SIZE, y + SIZE, border);

        int tw = font.width(text);
        g.drawString(font, text, x + SIZE / 2 - tw / 2, y + (SIZE - font.lineHeight) / 2, color);
    }
}
