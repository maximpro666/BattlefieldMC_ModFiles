package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.SquadActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class SquadScreen extends Screen {
    private static final int MEMBER_HEIGHT = 24;
    private int selectedIndex = -1;

    public SquadScreen() {
        super(Component.literal("Squad Management"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;

        addRenderableWidget(Button.builder(
            Component.literal("Create Squad"),
            btn -> {
                PacketHandler.CHANNEL.sendToServer(new SquadActionPacket("CREATE", new UUID(0, 0)));
            })
            .bounds(centerX - 120, 50, 110, 20)
            .build());

        addRenderableWidget(Button.builder(
            Component.literal("Leave Squad"),
            btn -> {
                PacketHandler.CHANNEL.sendToServer(new SquadActionPacket("LEAVE", new UUID(0, 0)));
            })
            .bounds(centerX + 10, 50, 110, 20)
            .build());

        addRenderableWidget(Button.builder(
            Component.literal("Invite Player"),
            btn -> {
                PacketHandler.CHANNEL.sendToServer(new SquadActionPacket("INVITE", UUID.randomUUID()));
            })
            .bounds(centerX - 120, 80, 110, 20)
            .build());

        addRenderableWidget(Button.builder(
            Component.literal("Kick"),
            btn -> {
                if (selectedIndex >= 0) {
                    PacketHandler.CHANNEL.sendToServer(new SquadActionPacket("KICK", UUID.randomUUID()));
                }
            })
            .bounds(centerX + 10, 80, 110, 20)
            .build());

        addRenderableWidget(Button.builder(
            Component.literal("Promote"),
            btn -> {
                if (selectedIndex >= 0) {
                    PacketHandler.CHANNEL.sendToServer(new SquadActionPacket("PROMOTE", UUID.randomUUID()));
                }
            })
            .bounds(centerX + 130, 80, 80, 20)
            .build());

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

        int centerX = width / 2;
        graphics.drawCenteredString(font, Component.literal("SQUAD MANAGEMENT"), centerX, 20, 0xFFFFFFFF);

        String squadName = ClientTeamData.localPlayerSquad;
        graphics.drawCenteredString(font, Component.literal("Squad: " + (squadName.isEmpty() ? "None" : squadName)), centerX, 34, 0xFFAAAAAA);

        graphics.drawString(font, Component.literal("Members:"), centerX - 150, 115, 0xFFFFAA00);

        int startY = 130;
        for (int i = 0; i < 4; i++) {
            int y = startY + i * (MEMBER_HEIGHT + 2);
            int color = (selectedIndex == i) ? 0x664444FF : 0x66000000;
            graphics.fill(centerX - 150, y, centerX + 150, y + MEMBER_HEIGHT, color);
            graphics.drawString(font, Component.literal("Slot " + (i + 1)), centerX - 140, y + 6, 0xFF888888);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startY = 130;
        int centerX = width / 2;
        for (int i = 0; i < 4; i++) {
            int y = startY + i * (MEMBER_HEIGHT + 2);
            if (mouseX >= centerX - 150 && mouseX <= centerX + 150 && mouseY >= y && mouseY <= y + MEMBER_HEIGHT) {
                selectedIndex = (selectedIndex == i) ? -1 : i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
