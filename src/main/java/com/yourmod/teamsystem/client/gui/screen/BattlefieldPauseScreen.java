package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.I18n;
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
    private static final int COLOR_PANEL  = UITheme.BG_PANEL;

    private float fadeAlpha = 0f;
    private long openTime;
    private float slideIn = 0f;
    private BButton[] menuButtons;

    public BattlefieldPauseScreen() {
        super(Component.literal(I18n.get("teamsystem.ui.paused")));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int cx = width / 2;
        int startY = height / 2 - 70;
        int btnW = 180;
        int btnH = 22;
        int gap = 4;
        int x = cx - btnW / 2;

        menuButtons = new BButton[5];

        menuButtons[0] = new BButton(x, startY, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.return_to_battle")), btn -> onClose());
        addRenderableWidget(menuButtons[0]);

        menuButtons[1] = new BButton(x, startY + (btnH + gap), btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.team_selection")), btn -> {
                Minecraft.getInstance().setScreen(new TeamSelectionScreen());
            });
        addRenderableWidget(menuButtons[1]);

        menuButtons[2] = new BButton(x, startY + (btnH + gap) * 2, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.settings")), btn -> {
                Minecraft.getInstance().setScreen(new SettingsMenuScreen());
            });
        addRenderableWidget(menuButtons[2]);

        menuButtons[3] = new BButton(x, startY + (btnH + gap) * 3, btnW, btnH,
            Component.literal("\u2620 Redeploy"), btn -> {
                if (Minecraft.getInstance().player != null && Minecraft.getInstance().getConnection() != null) {
                    Minecraft.getInstance().getConnection().sendCommand("redeploy");
                }
                onClose();
            });
        addRenderableWidget(menuButtons[3]);

        menuButtons[4] = new BButton(x, startY + (btnH + gap) * 4, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.disconnect")), btn -> {
                Minecraft.getInstance().level.disconnect();
                Minecraft.getInstance().setScreen(new BattlefieldMainMenuScreen());
            });
        addRenderableWidget(menuButtons[4]);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        float elapsed = (now - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);
        slideIn   = AnimationHelper.lerp(slideIn, 1f, 0.18f);

        int slideOffset = (int)((1f - slideIn) * (-width / 2));
        int panelW = 220;
        int panelH = 200;
        int panelX = width / 2 - panelW / 2 + slideOffset;
        int panelY = height / 2 - panelH / 2;

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xBB)));

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH,
            AnimationHelper.withAlpha(COLOR_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(panelX, panelY, panelX + panelW, panelY + 3,
            AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        String paused = I18n.get("teamsystem.ui.paused_uppercase");
        int pw = font.width(paused);
        g.drawString(font, paused, panelX + panelW / 2 - pw / 2, panelY + 12,
            AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        int baseX = width / 2 - 90 + slideOffset;
        if (menuButtons != null) {
            for (int i = 0; i < menuButtons.length; i++) {
                menuButtons[i].setX(baseX);
                menuButtons[i].setY((height / 2 - 70) + i * 26);
            }
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
