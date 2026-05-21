package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.gui.GuiGraphics;

public class StatusDot {

    public enum Status { OK, WARN, DANGER }

    public static void draw(GuiGraphics g, int cx, int cy, Status status) {
        int color = switch (status) {
            case OK     -> UITheme.STATUS_OK;
            case WARN   -> UITheme.STATUS_WARN;
            case DANGER -> UITheme.STATUS_DANGER;
        };
        int x = cx - 3, y = cy - 3;
        g.fill(x, y, x + 6, y + 6, color);
        g.fill(x - 1, y - 1, x + 7, y + 7, AnimationHelper.withAlpha(color, 0x44));
    }

    public static void drawOk(GuiGraphics g, int cx, int cy) { draw(g, cx, cy, Status.OK); }
    public static void drawDanger(GuiGraphics g, int cx, int cy) { draw(g, cx, cy, Status.DANGER); }
}
