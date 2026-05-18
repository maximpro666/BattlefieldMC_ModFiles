package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class TicketOverlay implements IGuiOverlay {
    private static final int BAR_W = 120;
    private static final int BAR_H = 14;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        int x = screenWidth - BAR_W - 5;
        int y = 25;

        int phase = ClientTeamData.getGamePhase();
        if (phase == 0) return;

        g.fill(x, y, x + BAR_W, y + 5, 0x88000000);
        BButton.drawBorder(g, x, y, BAR_W, 5, 0xFF555555);

        String natoTitle = "NATO";
        String russiaTitle = "RUSSIA";
        g.drawString(mc.font, natoTitle, x, y + 6, 0xFF4488FF);
        g.drawString(mc.font, russiaTitle, x + BAR_W - mc.font.width(russiaTitle), y + 6, 0xFFFF4444);

        int barY = y + 18;
        g.fill(x, barY, x + BAR_W, barY + BAR_H, 0x88000000);
        BButton.drawBorder(g, x, barY, BAR_W, BAR_H, 0xFF555555);

        float natoFill = Math.min(1, ClientTeamData.getNatoTickets() / 150F);
        if (natoFill > 0) {
            g.fill(x + 1, barY + 1, x + 1 + (int) ((BAR_W - 2) * natoFill), barY + BAR_H - 1, 0xFF4488FF);
        }
        String natoStr = String.valueOf(ClientTeamData.getNatoTickets());
        g.drawString(mc.font, natoStr, x + (BAR_W - mc.font.width(natoStr)) / 2, barY + 3, 0xFFFFFFFF);

        barY += BAR_H + 2;
        g.fill(x, barY, x + BAR_W, barY + BAR_H, 0x88000000);
        BButton.drawBorder(g, x, barY, BAR_W, BAR_H, 0xFF555555);

        float rusFill = Math.min(1, ClientTeamData.getRussiaTickets() / 150F);
        if (rusFill > 0) {
            g.fill(x + 1, barY + 1, x + 1 + (int) ((BAR_W - 2) * rusFill), barY + BAR_H - 1, 0xFFFF4444);
        }
        String rusStr = String.valueOf(ClientTeamData.getRussiaTickets());
        g.drawString(mc.font, rusStr, x + (BAR_W - mc.font.width(rusStr)) / 2, barY + 3, 0xFFFFFFFF);
    }
}
