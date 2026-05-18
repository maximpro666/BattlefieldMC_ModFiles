package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class CompassOverlay implements IGuiOverlay {
    private static final int COMPASS_W = 200;
    private static final int COMPASS_H = 20;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        float yaw = player.getYRot();
        int x = (screenWidth - COMPASS_W) / 2;
        int y = 2;

        g.fill(x, y, x + COMPASS_W, y + COMPASS_H, 0x88000000);
        BButton.drawBorder(g, x, y, COMPASS_W, COMPASS_H, 0xFF555555);

        double degPerPx = 360.0 / COMPASS_W;
        double centerYaw = yaw;
        int markerY = y + 4;

        for (int i = 0; i < 360; i += 10) {
            double offset = ((i - centerYaw + 540) % 360) - 180;
            int px = x + (int) (COMPASS_W / 2 + offset / degPerPx);
            if (px < x || px > x + COMPASS_W) continue;

            String label = getDirectionLabel(i);
            int col = (i % 90 == 0) ? 0xFFFFFFFF : 0xFFAAAAAA;
            g.drawString(mc.font, label, px - mc.font.width(label) / 2, markerY, col);
        }
    }

    private String getDirectionLabel(int deg) {
        return switch (deg) {
            case 0 -> "N";
            case 90 -> "E";
            case 180 -> "S";
            case 270 -> "W";
            default -> String.valueOf(deg);
        };
    }
}
