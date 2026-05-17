package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.VehicleEntry;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.VehicleSpawnPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class VehicleShopScreen extends Screen {
    private static final int ENTRY_HEIGHT = 50;
    private static final int ENTRY_WIDTH = 260;
    private int scrollOffset;

    public VehicleShopScreen() {
        super(Component.literal("Vehicle Shop"));
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
        refreshButtons();
    }

    private void refreshButtons() {
        clearWidgets();
        List<VehicleEntry> vehicles = ClientTeamData.availableVehicles;
        int centerX = width / 2;
        int startY = 60;

        for (int i = 0; i < vehicles.size(); i++) {
            VehicleEntry veh = vehicles.get(i);
            int y = startY + i * (ENTRY_HEIGHT + 4) - scrollOffset;
            int finalI = i;

            addRenderableWidget(Button.builder(
                Component.literal(veh.name()),
                btn -> PacketHandler.CHANNEL.sendToServer(new VehicleSpawnPacket(vehicles.get(finalI).id()))
                )
                .bounds(centerX - ENTRY_WIDTH / 2, y, ENTRY_WIDTH, ENTRY_HEIGHT)
                .build());
        }

        addRenderableWidget(Button.builder(
            Component.literal("Back"),
            btn -> onClose())
            .bounds(centerX - 50, height - 30, 100, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(0, 0, width, height, 0xCC000000, 0xCC222222);

        graphics.drawCenteredString(font, Component.literal("VEHICLE SHOP"), width / 2, 20, 0xFFFFFFFF);

        List<VehicleEntry> vehicles = ClientTeamData.availableVehicles;
        int startY = 60;
        int centerX = width / 2;

        graphics.drawString(font, Component.literal("BC: " + ClientTeamData.localPlayerBC + " | Tickets: " + ClientTeamData.getNatoTickets() + "/" + ClientTeamData.getRussiaTickets()), centerX - ENTRY_WIDTH / 2, 40, 0xFFAAAAAA);

        for (int i = 0; i < vehicles.size(); i++) {
            VehicleEntry veh = vehicles.get(i);
            int y = startY + i * (ENTRY_HEIGHT + 4) - scrollOffset;

            if (y < 50 || y + ENTRY_HEIGHT > height - 40) continue;

            graphics.fill(centerX - ENTRY_WIDTH / 2, y, centerX + ENTRY_WIDTH / 2, y + ENTRY_HEIGHT, 0x80000000);
            graphics.fill(centerX - ENTRY_WIDTH / 2, y, centerX - ENTRY_WIDTH / 2 + ENTRY_HEIGHT, y + ENTRY_HEIGHT, 0x88FFFFFF);

            graphics.drawString(font, Component.literal(veh.name()), centerX - ENTRY_WIDTH / 2 + ENTRY_HEIGHT + 6, y + 4, 0xFFFFFFFF);
            graphics.drawString(font, Component.literal(veh.description()), centerX - ENTRY_WIDTH / 2 + ENTRY_HEIGHT + 6, y + 16, 0xFFAAAAAA);

            String stats = "Tickets: " + veh.ticketCost() + " | BC: " + veh.bcCost() + " | Rank: " + veh.minRankOrdinal();
            graphics.drawString(font, Component.literal(stats), centerX - ENTRY_WIDTH / 2 + ENTRY_HEIGHT + 6, y + 30, 0xFFFFAA00);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<VehicleEntry> vehicles = ClientTeamData.availableVehicles;
        int maxScroll = Math.max(0, vehicles.size() * (ENTRY_HEIGHT + 4) - (height - 100));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) delta * 20));
        refreshButtons();
        return true;
    }
}
