package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.KitEntry;
import com.yourmod.teamsystem.network.KitSelectPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class KitSelectionScreen extends Screen {
    private static final int ENTRY_HEIGHT = 40;
    private static final int ENTRY_WIDTH = 220;
    private int scrollOffset;

    public KitSelectionScreen() {
        super(Component.literal("Kit Selection"));
    }

    @Override
    protected void init() {
        super.init();
        scrollOffset = 0;
        refreshButtons();
    }

    private void refreshButtons() {
        clearWidgets();
        List<KitEntry> kits = ClientTeamData.availableKits;
        int centerX = width / 2;
        int startY = 60;

        for (int i = 0; i < kits.size(); i++) {
            KitEntry kit = kits.get(i);
            int y = startY + i * (ENTRY_HEIGHT + 4) - scrollOffset;
            int finalI = i;

            addRenderableWidget(Button.builder(
                Component.literal(kit.name()),
                btn -> PacketHandler.CHANNEL.sendToServer(new KitSelectPacket(kits.get(finalI).id()))
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

        graphics.drawCenteredString(font, Component.literal("SELECT KIT"), width / 2, 20, 0xFFFFFFFF);

        List<KitEntry> kits = ClientTeamData.availableKits;
        int startY = 60;
        int centerX = width / 2;

        for (int i = 0; i < kits.size(); i++) {
            KitEntry kit = kits.get(i);
            int y = startY + i * (ENTRY_HEIGHT + 4) - scrollOffset;

            if (y < 50 || y + ENTRY_HEIGHT > height - 40) continue;

            graphics.fill(centerX - ENTRY_WIDTH / 2, y, centerX + ENTRY_WIDTH / 2, y + ENTRY_HEIGHT, 0x80000000);
            graphics.fill(centerX - ENTRY_WIDTH / 2, y, centerX - ENTRY_WIDTH / 2 + ENTRY_HEIGHT, y + ENTRY_HEIGHT, 0x88FFFFFF);

            graphics.drawString(font, Component.literal(kit.name()), centerX - ENTRY_WIDTH / 2 + ENTRY_HEIGHT + 6, y + 4, 0xFFFFFFFF);
            graphics.drawString(font, Component.literal(kit.description()), centerX - ENTRY_WIDTH / 2 + ENTRY_HEIGHT + 6, y + 16, 0xFFAAAAAA);
            graphics.drawString(font, Component.literal("Rank: " + kit.minRankOrdinal()), centerX - ENTRY_WIDTH / 2 + ENTRY_HEIGHT + 6, y + 28, 0xFFFFAA00);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<KitEntry> kits = ClientTeamData.availableKits;
        int maxScroll = Math.max(0, kits.size() * (ENTRY_HEIGHT + 4) - (height - 100));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) delta * 20));
        refreshButtons();
        return true;
    }
}
