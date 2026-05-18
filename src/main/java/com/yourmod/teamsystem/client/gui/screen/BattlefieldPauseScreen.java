package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.component.BButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BattlefieldPauseScreen extends Screen {
    private static final int BTN_W = 160;
    private static final int BTN_H = 24;

    public BattlefieldPauseScreen() {
        super(Component.literal("Pause"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2 - 40;

        addRenderableWidget(new BButton(cx - BTN_W / 2, cy, BTN_W, BTN_H, "Resume", btn -> onClose()));
        addRenderableWidget(new BButton(cx - BTN_W / 2, cy + 30, BTN_W, BTN_H, "Team Selection", btn -> {
            minecraft.setScreen(new TeamSelectionScreen(this));
        }));
        addRenderableWidget(new BButton(cx - BTN_W / 2, cy + 60, BTN_W, BTN_H, "Kit Selection", btn -> {
            minecraft.setScreen(new KitSelectionScreen(this));
        }));
        addRenderableWidget(new BButton(cx - BTN_W / 2, cy + 90, BTN_W, BTN_H, "Settings", btn -> {
            minecraft.setScreen(new SettingsMenuScreen(this));
        }));
        addRenderableWidget(new BButton(cx - BTN_W / 2, cy + 120, BTN_W, BTN_H, "Main Menu", btn -> {
            minecraft.setScreen(new BattlefieldMainMenuScreen());
        }));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0x88000000);
        String title = "PAUSE";
        int titleW = font.width(title);
        g.drawString(font, title, (width - titleW) / 2, height / 2 - 80, 0xFF00AAFF);
        super.render(g, mouseX, mouseY, partialTick);
    }
}
