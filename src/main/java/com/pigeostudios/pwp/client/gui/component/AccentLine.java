package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.gui.GuiGraphics;

public class AccentLine {
    public static void draw(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 2, UITheme.ACCENT);
        int fade = w / 4;
        if (fade > 0) {
            g.fill(x + w - fade, y, x + w, y + 2,
                AnimationHelper.withAlpha(UITheme.ACCENT, 0x44));
        }
    }

    public static void draw(GuiGraphics g, int x, int y, int w, float alpha) {
        g.fill(x, y, x + w, y + 2,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0xFF)));
    }
}
