package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.NotificationManager;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class NotificationOverlay implements IGuiOverlay {
    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        var notification = NotificationManager.getCurrentNotification();
        if (notification == null) return;

        Minecraft mc = Minecraft.getInstance();
        int timer = NotificationManager.getDisplayTimer();
        float alpha = Math.min(1, timer / 20F);
        int alphaBits = ((int) (alpha * 255) << 24);

        String title = notification.title;
        String subtitle = notification.subtitle;
        int titleW = mc.font.width(title);
        int subW = mc.font.width(subtitle);
        int maxW = Math.max(titleW, subW) + 20;
        int x = (screenWidth - maxW) / 2;
        int y = 20;

        g.fill(x, y, x + maxW, y + 50, 0xCC222222 | alphaBits);
        BButton.drawBorder(g, x, y, maxW, 50, 0xFF555555);
        g.fill(x, y, x + 3, y + 50, notification.color | alphaBits);

        g.drawString(mc.font, title, x + 10, y + 8, 0xFFFFFFFF);
        g.drawString(mc.font, subtitle, x + 10, y + 24, 0xFFAAAAAA);
    }
}
