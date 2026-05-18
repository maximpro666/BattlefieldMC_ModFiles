package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.BScrollPanel;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.SquadActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SquadScreen extends Screen {
    private final Screen parent;
    private BScrollPanel leftPanel;
    private BScrollPanel rightPanel;

    private static final int PANEL_W = 200;

    public SquadScreen(Screen parent) {
        super(Component.literal("Squad Management"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int h = height - 80;

        leftPanel = new BScrollPanel(cx - PANEL_W - 10, 60, PANEL_W, h);
        rightPanel = new BScrollPanel(cx + 10, 60, PANEL_W, h);

        rebuildPanels();

        addRenderableWidget(new BButton(cx - 50, height - 35, 100, 20, "Close", btn -> {
            minecraft.setScreen(parent);
        }));
    }

    private void rebuildPanels() {
        leftPanel.clearContent();
        leftPanel.setPosition(width / 2 - PANEL_W - 10, 60);
        leftPanel.setSize(PANEL_W, height - 80);

        rightPanel.clearContent();
        rightPanel.setPosition(width / 2 + 10, 60);
        rightPanel.setSize(PANEL_W, height - 80);

        String localSquad = ClientTeamData.getLocalPlayerSquad();
        int[] playerIdx = {0};

        for (var entry : ClientTeamData.playerDataMap.entrySet()) {
            PlayerListEntry player = entry.getValue();
            if (localSquad != null && localSquad.equals(player.squad())) continue;

            int yOff = playerIdx[0] * 28;
            leftPanel.addContent(g -> {
                int sx = leftPanel.getX() + 5;
                int sy = leftPanel.getY() + yOff - leftPanel.getScrollOffset();
                String name = player.callsign();
                int col = player.teamOrdinal() == 0 ? 0xFF4488FF : (player.teamOrdinal() == 1 ? 0xFFFF4444 : 0xFF888888);
                g.drawString(font, name, sx, sy + 6, col);
            });
            playerIdx[0]++;
        }
        leftPanel.setContentHeight(playerIdx[0] * 28);

        if (localSquad != null && !localSquad.isEmpty()) {
            int[] squadIdx = {0};
            for (var entry : ClientTeamData.playerDataMap.entrySet()) {
                PlayerListEntry player = entry.getValue();
                if (!localSquad.equals(player.squad())) continue;

                int yOff = squadIdx[0] * 28;
                rightPanel.addContent(g -> {
                    int sx = rightPanel.getX() + 5;
                    int sy = rightPanel.getY() + yOff - rightPanel.getScrollOffset();
                    String name = player.callsign();
                    int col = player.isDowned() ? 0xFFFF4444 : 0xFFFFFFFF;
                    g.drawString(font, name, sx, sy + 6, col);
                });
                squadIdx[0]++;
            }
            rightPanel.setContentHeight(squadIdx[0] * 28);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0xCC111111);

        String title = "SQUAD MANAGEMENT";
        int titleW = font.width(title);
        g.drawString(font, title, (width - titleW) / 2, 16, 0xFF00AAFF);

        g.drawString(font, "Available Players", width / 2 - PANEL_W - 5, 50, 0xFFAAAAAA);
        g.drawString(font, "My Squad (" + ClientTeamData.getLocalPlayerSquad() + ")", width / 2 + 15, 50, 0xFFAAAAAA);

        leftPanel.render(g);
        rightPanel.render(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= leftPanel.getX() && mx <= leftPanel.getX() + leftPanel.getWidth()) {
            int idx = (int) ((my - leftPanel.getY() + leftPanel.getScrollOffset()) / 28);
            int i = 0;
            for (var entry : ClientTeamData.playerDataMap.entrySet()) {
                PlayerListEntry player = entry.getValue();
                String localSquad = ClientTeamData.getLocalPlayerSquad();
                if (localSquad != null && localSquad.equals(player.squad())) continue;
                if (i == idx) {
                    PacketHandler.CHANNEL.sendToServer(new SquadActionPacket("invite", entry.getKey()));
                    break;
                }
                i++;
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < width / 2) {
            leftPanel.onScroll(mouseX, mouseY, delta);
        } else {
            rightPanel.onScroll(mouseX, mouseY, delta);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
