package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class SquadOverlay implements IGuiOverlay {
    private static final int PANEL_W = 140;
    private static final int PANEL_H = 120;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        String squad = ClientTeamData.getLocalPlayerSquad();
        if (squad == null || squad.isEmpty()) return;

        int x = screenWidth - PANEL_W - 5;
        int y = screenHeight - PANEL_H - 40;

        g.fill(x, y, x + PANEL_W, y + PANEL_H, 0x88000000);
        BButton.drawBorder(g, x, y, PANEL_W, PANEL_H, 0xFF555555);
        g.fill(x, y, x + PANEL_W, y + 1, 0xFF00AAFF);
        g.drawString(mc.font, "SQUAD " + squad, x + 5, y + 4, 0xFF00AAFF);

        int yOff = y + 16;
        for (var entry : ClientTeamData.playerDataMap.entrySet()) {
            PlayerListEntry pData = entry.getValue();
            if (!squad.equals(pData.squad())) continue;
            if (yOff > y + PANEL_H - 10) break;

            String name = pData.callsign();
            String health = pData.isDowned() ? "X" : "OK";
            int col = pData.isDowned() ? 0xFFFF4444 : 0xFFFFFFFF;
            g.drawString(mc.font, name, x + 5, yOff, col);
            g.drawString(mc.font, health, x + PANEL_W - 20, yOff, col);
            yOff += 10;
        }
    }
}
