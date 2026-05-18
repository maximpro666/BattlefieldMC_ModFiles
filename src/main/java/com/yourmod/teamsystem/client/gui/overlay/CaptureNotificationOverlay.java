package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class CaptureNotificationOverlay implements IGuiOverlay {
    private String currentText = "";
    private int displayTimer = 0;
    private static final int DISPLAY_TIME = 60;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (displayTimer <= 0) return;

        float progress = displayTimer / (float) DISPLAY_TIME;
        int alpha = (int) (255 * Math.min(1, progress * 2));

        int textW = mc.font.width(currentText);
        int x = (screenWidth - textW) / 2;
        int y = screenHeight / 2 - 40;

        String displayText = currentText;
        int color = (alpha << 24) | 0xFFFFAA00;

        g.drawString(mc.font, displayText, x, y, color);
        displayTimer--;
    }

    public void showCaptureMessage(String text) {
        this.currentText = text;
        this.displayTimer = DISPLAY_TIME;
    }
}
