package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.network.MapVotePacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class MapVoteScreen extends Screen {
    private static final List<String> MAP_OPTIONS = List.of("Golmud Railway", "Siege of Shanghai", "Operation Locker", "Paracel Storm", "Dawnbreaker");

    public MapVoteScreen() {
        super(Component.literal("Map Vote"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY = height / 2 - (MAP_OPTIONS.size() * 30) / 2;

        for (int i = 0; i < MAP_OPTIONS.size(); i++) {
            String mapName = MAP_OPTIONS.get(i);
            addRenderableWidget(Button.builder(
                Component.literal(mapName),
                btn -> {
                    PacketHandler.CHANNEL.sendToServer(new MapVotePacket(mapName));
                    onClose();
                })
                .bounds(centerX - 120, startY + i * 30, 240, 24)
                .build());
        }

        addRenderableWidget(Button.builder(
            Component.literal("Close"),
            btn -> onClose())
            .bounds(centerX - 50, startY + MAP_OPTIONS.size() * 30 + 10, 100, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(0, 0, width, height, 0xCC000000, 0xCC222222);

        int centerX = width / 2;
        graphics.drawCenteredString(font, Component.literal("VOTE FOR NEXT MAP"), centerX, 20, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.literal("Current: " + ClientTeamData.getCurrentMapName()), centerX, 34, 0xFFAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
