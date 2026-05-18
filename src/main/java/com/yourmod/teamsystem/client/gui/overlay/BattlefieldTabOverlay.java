package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.Map;
import java.util.UUID;

public class BattlefieldTabOverlay implements IGuiOverlay {
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 240;
    private static final int HEADER_H = 30;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.keyPlayerList.isDown()) return;

        int panelX = (screenWidth - PANEL_W) / 2;
        int panelY = (screenHeight - PANEL_H) / 2;

        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xCC111111);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, 0xFF555555);
        g.fill(panelX, panelY + PANEL_H - 1, panelX + PANEL_W, panelY + PANEL_H, 0xFF555555);
        g.fill(panelX, panelY, panelX + 1, panelY + PANEL_H, 0xFF555555);
        g.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xFF555555);

        String title = "BATTLEFIELD 2 - " + ClientTeamData.getCurrentMapName();
        g.drawString(mc.font, title, panelX + 10, panelY + 8, 0xFF00AAFF);

        String ticketStr = "NATO: " + ClientTeamData.getNatoTickets() + " / RUSSIA: " + ClientTeamData.getRussiaTickets();
        g.drawString(mc.font, ticketStr, panelX + 10, panelY + 20, 0xFFFFFFFF);

        String timeStr = "TIME: " + formatTime(ClientTeamData.matchTimeSeconds);
        g.drawString(mc.font, timeStr, panelX + PANEL_W - 80, panelY + 20, 0xFFFFFFFF);

        int nameX = panelX + 10;
        int killsX = panelX + 180;
        int deathsX = panelX + 220;
        int squadX = panelX + 260;

        g.drawString(mc.font, "NAME", nameX, panelY + HEADER_H + 2, 0xFFAAAAAA);
        g.drawString(mc.font, "K", killsX, panelY + HEADER_H + 2, 0xFFAAAAAA);
        g.drawString(mc.font, "D", deathsX, panelY + HEADER_H + 2, 0xFFAAAAAA);
        g.drawString(mc.font, "SQD", squadX, panelY + HEADER_H + 2, 0xFFAAAAAA);

        int yOff = panelY + HEADER_H + 12;
        for (var entry : ClientTeamData.playerDataMap.entrySet()) {
            if (yOff > panelY + PANEL_H - 10) break;
            PlayerListEntry pData = entry.getValue();
            int teamOrdinal = pData.teamOrdinal();
            int color = teamOrdinal == 0 ? 0xFF4488FF : (teamOrdinal == 1 ? 0xFFFF4444 : 0xFF888888);
            g.drawString(mc.font, pData.callsign(), nameX, yOff, color);
            g.drawString(mc.font, String.valueOf(pData.kills()), killsX, yOff, 0xFFFFFFFF);
            g.drawString(mc.font, String.valueOf(pData.deaths()), deathsX, yOff, 0xFFFFFFFF);
            g.drawString(mc.font, pData.squad(), squadX, yOff, 0xFFAAAAAA);
            yOff += 10;
        }
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
