package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class BreadcrumbNav {

    public static final int BREADCRUMB_H = 18;

    public static void render(GuiGraphics g, int screenW, int topY,
                               List<String> history, int alpha) {
        g.fill(0, topY, screenW, topY + BREADCRUMB_H,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(alpha * 0.9f)));
        g.fill(0, topY + BREADCRUMB_H - 1, screenW, topY + BREADCRUMB_H,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));

        var font = Minecraft.getInstance().font;
        int ty   = topY + (BREADCRUMB_H - font.lineHeight) / 2;
        int x    = 10;

        for (int i = 0; i < history.size(); i++) {
            String crumb = history.get(i).toUpperCase();
            boolean current = i == history.size() - 1;

            if (i > 0) {
                g.drawString(font, "\u203A", x, ty,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(alpha * 0.4f)));
                x += font.width("\u203A") + 4;
            }

            int color = current ? UITheme.ACCENT : UITheme.TEXT_MUTED;
            g.drawString(font, crumb, x, ty,
                AnimationHelper.withAlpha(color, alpha));
            x += font.width(crumb) + 4;
        }
    }
}
