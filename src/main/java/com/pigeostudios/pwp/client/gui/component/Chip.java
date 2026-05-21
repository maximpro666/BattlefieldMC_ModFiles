package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class Chip {

    public static int draw(GuiGraphics g, int x, int y, int h, String label,
                            boolean selected, boolean hovered, float alpha) {
        var font = Minecraft.getInstance().font;
        int tw = font.width(label);
        int w  = tw + 16;

        int bg = selected
            ? AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0xDD))
            : hovered
                ? AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(alpha * 0xCC))
                : AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(alpha * 0x88));

        int border = selected
            ? AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0xFF))
            : AnimationHelper.withAlpha(UITheme.BORDER, (int)(alpha * (hovered ? 0x88 : 0x44)));

        g.fill(x, y, x + w, y + h, bg);
        g.fill(x,         y,       x + w,     y + 1,     border);
        g.fill(x,         y + h-1, x + w,     y + h,     border);
        g.fill(x,         y,       x + 1,     y + h,     border);
        g.fill(x + w - 1, y,       x + w,     y + h,     border);

        int txtColor = selected ? 0xFFFFFFFF
            : AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(alpha * 0xFF));
        g.drawString(font, label, x + 8, y + (h - font.lineHeight) / 2, txtColor);

        return w + 4;
    }
}
