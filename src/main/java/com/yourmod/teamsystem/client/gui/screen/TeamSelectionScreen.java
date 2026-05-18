package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.TeamSelectPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TeamSelectionScreen extends Screen {
    private final Screen parent;
    private static final int CARD_W = 180;
    private static final int CARD_H = 220;

    public TeamSelectionScreen(Screen parent) {
        super(Component.literal("Team Selection"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int gap = 30;
        int totalW = 2 * CARD_W + gap;
        int startX = (width - totalW) / 2;
        int cy = height / 2 - CARD_H / 2;

        addRenderableWidget(new BButton(startX, cy + CARD_H + 10, CARD_W, 30, "NATO", btn -> {
            PacketHandler.CHANNEL.sendToServer(new TeamSelectPacket(0));
            minecraft.setScreen(parent);
        }));

        addRenderableWidget(new BButton(startX + CARD_W + gap, cy + CARD_H + 10, CARD_W, 30, "RUSSIA", btn -> {
            PacketHandler.CHANNEL.sendToServer(new TeamSelectPacket(1));
            minecraft.setScreen(parent);
        }));

        addRenderableWidget(new BButton(width / 2 - 50, cy + CARD_H + 50, 100, 20, "Back", btn -> {
            minecraft.setScreen(parent);
        }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0xCC111111);

        String title = "SELECT YOUR TEAM";
        int titleW = font.width(title);
        g.drawString(font, title, (width - titleW) / 2, 20, 0xFF00AAFF);

        int gap = 30;
        int totalW = 2 * CARD_W + gap;
        int startX = (width - totalW) / 2;
        int cy = height / 2 - CARD_H / 2;

        drawCard(g, startX, cy, "NATO", ClientTeamData.getNatoTickets());
        drawCard(g, startX + CARD_W + gap, cy, "RUSSIA", ClientTeamData.getRussiaTickets());

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawCard(GuiGraphics g, int x, int y, String name, int tickets) {
        int cardColor = 0xCC222222;
        g.fill(x, y, x + CARD_W, y + CARD_H, cardColor);
        g.fill(x, y, x + CARD_W, y + 2, 0xFF555555);
        g.fill(x, y + CARD_H - 2, x + CARD_W, y + CARD_H, 0xFF555555);
        g.fill(x, y, x + 2, y + CARD_H, 0xFF555555);
        g.fill(x + CARD_W - 2, y, x + CARD_W, y + CARD_H, 0xFF555555);

        g.fill(x + 8, y + 8, x + CARD_W - 8, y + 10, 0xFF4488FF);
        g.drawString(font, name, x + 10, y + 14, 0xFFFFFFFF);

        g.drawString(font, "Tickets: " + tickets, x + 10, y + 40, 0xFFAAAAAA);
        g.drawString(font, "Click to join", x + 10, y + 60, 0xFF888888);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
