package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.TeamSelectPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TeamSelectionScreen extends Screen {
    public static boolean isOpen = false;

    private Button natoButton;
    private Button russiaButton;
    private Button spectatorButton;
    private Button closeButton;

    public TeamSelectionScreen() {
        super(Component.literal("Team Selection"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;

        natoButton = addRenderableWidget(Button.builder(
            Component.literal("NATO"),
            btn -> {
                PacketHandler.CHANNEL.sendToServer(new TeamSelectPacket(0));
                isOpen = false;
                onClose();
            })
            .bounds(centerX - 100, centerY - 60, 200, 30)
            .build());

        russiaButton = addRenderableWidget(Button.builder(
            Component.literal("RUSSIA"),
            btn -> {
                PacketHandler.CHANNEL.sendToServer(new TeamSelectPacket(1));
                isOpen = false;
                onClose();
            })
            .bounds(centerX - 100, centerY - 20, 200, 30)
            .build());

        spectatorButton = addRenderableWidget(Button.builder(
            Component.literal("SPECTATOR"),
            btn -> {
                PacketHandler.CHANNEL.sendToServer(new TeamSelectPacket(2));
                isOpen = false;
                onClose();
            })
            .bounds(centerX - 100, centerY + 20, 200, 30)
            .build());

        closeButton = addRenderableWidget(Button.builder(
            Component.literal("Close"),
            btn -> {
                isOpen = false;
                onClose();
            })
            .bounds(centerX - 50, centerY + 70, 100, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(0, 0, width, height, 0xCC000000, 0xCC222222);

        int centerX = width / 2;

        graphics.drawCenteredString(font, Component.literal("SELECT YOUR TEAM"), centerX, 30, 0xFFFFFFFF);

        graphics.drawString(font, Component.literal("NATO Tickets: " + ClientTeamData.getNatoTickets()), centerX - 140, 80, 0x5555FF);
        graphics.drawString(font, Component.literal("Russia Tickets: " + ClientTeamData.getRussiaTickets()), centerX + 20, 80, 0xFF5555);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        isOpen = false;
        super.onClose();
    }
}
