package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BattlefieldPauseScreen extends Screen {

    private static final int COLOR_BG     = UITheme.BG_SCREEN;
    private static final int COLOR_ORANGE = UITheme.ACCENT;
    private static final int COLOR_TEXT   = UITheme.TEXT_PRIMARY;
    private static final int COLOR_PANEL  = UITheme.BG_PANEL;

    private float fadeAlpha = 0f;
    private long openTime;
    private float slideIn = 0f;

    public BattlefieldPauseScreen() {
        super(Component.literal("Paused"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int cx = width / 2;
        int startY = height / 2 - 60;
        int btnW = 180;
        int btnH = 22;
        int gap  = 6;

        addRenderableWidget(new BButton(cx - btnW / 2, startY, btnW, btnH,
            Component.literal("Return to Battle"), btn -> onClose()));

        addRenderableWidget(new BButton(cx - btnW / 2, startY + (btnH + gap), btnW, btnH,
            Component.literal("Team Selection"), btn -> {
                Minecraft.getInstance().setScreen(new TeamSelectionScreen());
            }));

        addRenderableWidget(new BButton(cx - btnW / 2, startY + (btnH + gap) * 2, btnW, btnH,
            Component.literal("Settings"), btn -> {
                Minecraft.getInstance().setScreen(new SettingsMenuScreen());
            }));

        addRenderableWidget(new BButton(cx - btnW / 2, startY + (btnH + gap) * 3, btnW, btnH,
            Component.literal("Disconnect"), btn -> {
                Minecraft.getInstance().level.disconnect();
                Minecraft.getInstance().setScreen(new BattlefieldMainMenuScreen());
            }));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        float elapsed = (now - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);
        slideIn   = AnimationHelper.lerp(slideIn, 1f, 0.18f);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xBB)));

        int panelW = 220;
        int panelH = 200;
        int panelX = (int)(width / 2 - panelW / 2 + (1f - slideIn) * (-width / 2));
        int panelY = height / 2 - panelH / 2;

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH,
            AnimationHelper.withAlpha(COLOR_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(panelX, panelY, panelX + panelW, panelY + 3,
            AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        String paused = "PAUSED";
        int pw = font.width(paused);
        g.drawString(font, paused, panelX + panelW / 2 - pw / 2, panelY + 12,
            AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
