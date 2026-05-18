package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.component.BButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BattlefieldMainMenuScreen extends Screen {
    private static final int BTN_W = 160;
    private static final int BTN_H = 24;

    protected BattlefieldMainMenuScreen() {
        super(Component.literal("Battlefield 2"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2 - 40;

        addRenderableWidget(new BButton(cx - BTN_W / 2, cy, BTN_W, BTN_H, "Join Game", btn -> {
            minecraft.setScreen(new TeamSelectionScreen(this));
        }));
        addRenderableWidget(new BButton(cx - BTN_W / 2, cy + 30, BTN_W, BTN_H, "Kit Selection", btn -> {
            minecraft.setScreen(new KitSelectionScreen(this));
        }));
        addRenderableWidget(new BButton(cx - BTN_W / 2, cy + 60, BTN_W, BTN_H, "Settings", btn -> {
            minecraft.setScreen(new SettingsMenuScreen(this));
        }));
        addRenderableWidget(new BButton(cx - BTN_W / 2, cy + 90, BTN_W, BTN_H, "Disconnect", btn -> {
            minecraft.setScreen(null);
        }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0xFF111111);

        String title = "BATTLEFIELD 2";
        int titleW = font.width(title);
        g.drawString(font, title, (width - titleW) / 2, height / 2 - 80, 0xFF00AAFF);

        String sub = "Multiplayer Modification";
        int subW = font.width(sub);
        g.drawString(font, sub, (width - subW) / 2, height / 2 - 65, 0xFF888888);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
