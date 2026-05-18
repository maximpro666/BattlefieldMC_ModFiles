package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.VehicleEntry;
import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.BScrollPanel;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.VehicleSpawnPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VehicleSelectionScreen extends Screen {
    private final Screen parent;
    private BScrollPanel scroll;

    public VehicleSelectionScreen(Screen parent) {
        super(Component.literal("Vehicle Selection"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        scroll = new BScrollPanel(10, 40, width - 20, height - 100);
        rebuildVehicleList();

        addRenderableWidget(new BButton(width / 2 - 50, height - 35, 100, 20, "Close", btn -> {
            minecraft.setScreen(parent);
        }));
    }

    private void rebuildVehicleList() {
        scroll.clearContent();
        int[] idx = {0};
        for (VehicleEntry v : ClientTeamData.availableVehicles) {
            int yOff = idx[0] * 50;
            scroll.addContent(g -> {
                int sx = scroll.getX() + 5;
                int sy = scroll.getY() + yOff - scroll.getScrollOffset();
                boolean affordable = ClientTeamData.getNatoTickets() >= v.ticketCost() || ClientTeamData.getRussiaTickets() >= v.ticketCost();
                boolean bcAffordable = ClientTeamData.localPlayerBC >= v.bcCost();
                boolean affordableRank = ClientTeamData.localPlayerRank >= v.minRankOrdinal();
                boolean canBuy = affordable && bcAffordable && affordableRank;

                g.drawString(font, v.name(), sx, sy + 4, canBuy ? 0xFFFFFFFF : 0xFF888888);
                g.drawString(font, v.description(), sx, sy + 16, 0xFFAAAAAA);
                String costStr = v.ticketCost() + " tickets / " + v.bcCost() + " BC";
                g.drawString(font, costStr, sx, sy + 28, 0xFF00AAFF);
            });
            idx[0]++;
        }
        scroll.setContentHeight(idx[0] * 50);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int idx = (int) ((my - scroll.getY() + scroll.getScrollOffset()) / 50);
            if (idx >= 0 && idx < ClientTeamData.availableVehicles.size()) {
                VehicleEntry v = ClientTeamData.availableVehicles.get(idx);
                PacketHandler.CHANNEL.sendToServer(new VehicleSpawnPacket(v.id()));
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0xCC111111);
        String title = "VEHICLE DEPLOYMENT";
        int titleW = font.width(title);
        g.drawString(font, title, (width - titleW) / 2, 16, 0xFF00AAFF);
        String info = "BC: " + ClientTeamData.localPlayerBC + " | Tickets: " + ClientTeamData.getNatoTickets() + "/" + ClientTeamData.getRussiaTickets();
        g.drawString(font, info, (width - font.width(info)) / 2, 28, 0xFFAAAAAA);
        scroll.render(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll.onScroll(mouseX, mouseY, delta);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
