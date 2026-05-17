package com.yourmod.teamsystem.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BattlefieldPauseScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_SPACING = 6;

    public BattlefieldPauseScreen() {
        super(Component.literal("Paused"));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int startY = height / 2 - ((6 * (BUTTON_HEIGHT + BUTTON_SPACING)) / 2);

        addRenderableWidget(Button.builder(
            Component.literal("Resume"),
            btn -> onClose())
            .bounds(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        addRenderableWidget(Button.builder(
            Component.literal("Team Selection"),
            btn -> Minecraft.getInstance().setScreen(new TeamSelectionScreen()))
            .bounds(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 1, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        addRenderableWidget(Button.builder(
            Component.literal("Kits"),
            btn -> Minecraft.getInstance().setScreen(new KitSelectionScreen()))
            .bounds(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        addRenderableWidget(Button.builder(
            Component.literal("Vehicles"),
            btn -> Minecraft.getInstance().setScreen(new VehicleShopScreen()))
            .bounds(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 3, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        addRenderableWidget(Button.builder(
            Component.literal("Squad"),
            btn -> Minecraft.getInstance().setScreen(new SquadScreen()))
            .bounds(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 4, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        addRenderableWidget(Button.builder(
            Component.literal("Disconnect"),
            btn -> Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        Minecraft.getInstance().level.disconnect();
                        Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.DisconnectedScreen(
                            null,
                            Component.literal("Disconnected"),
                            Component.literal("You have left the game.")));
                    } else {
                        Minecraft.getInstance().setScreen(this);
                    }
                },
                Component.literal("Are you sure?"),
                Component.literal("Disconnect from the server?"))))
            .bounds(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + BUTTON_SPACING) * 5, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fillGradient(0, 0, width, height, 0xCC000000, 0xCC111111);

        int centerX = width / 2;
        graphics.drawCenteredString(font, Component.literal("BATTLEFIELD"), centerX, 30, 0xFFFFAA00);
        graphics.drawCenteredString(font, Component.literal("PAUSED"), centerX, 44, 0xFFFFFFFF);

        graphics.drawCenteredString(font, Component.literal("teamsystem"), centerX, height - 20, 0xFF666666);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
