package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class LockBadge {

    private static final int OVERLAY_COLOR = 0x88000000;
    private static final String LOCK_ICON  = "\uD83D\uDD12";

    public static void draw(GuiGraphics g, int x, int y, int w, int h, String reason) {
        g.fill(x, y, x + w, y + h, OVERLAY_COLOR);

        var font = Minecraft.getInstance().font;
        int midX = x + w / 2;
        int midY = y + h / 2;

        int iconW = font.width(LOCK_ICON);
        g.drawString(font, LOCK_ICON,
            midX - iconW / 2, midY + (reason != null ? -font.lineHeight - 4 : -font.lineHeight / 2),
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, 0xCC));

        if (reason != null && !reason.isEmpty()) {
            int tw = font.width(reason);
            int bx = midX - tw / 2 - 8;
            int by = midY + 2;
            int bw = tw + 16;
            int bh = font.lineHeight + 6;
            g.fill(bx, by, bx + bw, by + bh, 0xCC330000);
            g.fill(bx, by, bx + bw, by + 1, 0x44CC3030);
            g.fill(bx, by + bh - 1, bx + bw, by + bh, 0x44CC3030);
            g.drawString(font, reason, midX - tw / 2, by + 4,
                AnimationHelper.withAlpha(0xFFFF6060, 0xFF));
        }
    }
}
