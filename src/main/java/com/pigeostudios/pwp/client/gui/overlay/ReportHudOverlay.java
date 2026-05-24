package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ReportHudOverlay {

    private static int pendingCount = 0;
    private static boolean visible = false;

    public static void setPendingCount(int count) {
        pendingCount = count;
        visible = count > 0;
    }

    public void render(GuiGraphics g, int screenWidth) {
        if (!visible || pendingCount <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        String text = "§c\u2691 " + pendingCount;
        int tw = font.width(text);
        int x = screenWidth / 2 + 240;
        int y = 8;

        RenderHelper.dropShadow(g, x, y, tw + 12, 16, 3, 80);
        RenderHelper.roundedRect(g, x, y, tw + 12, 16, 4,
            AnimationHelper.withAlpha(0xCCFF4444, 200));

        g.drawString(font, text, x + 6, y + 3, 0xFFFFFFFF);
    }

    public static void onNewMessage(int ticketId, String senderName, String message) {
        visible = true;
    }

    public void tick() {
        if (visible && pendingCount <= 0) {
            visible = false;
        }
    }
}
