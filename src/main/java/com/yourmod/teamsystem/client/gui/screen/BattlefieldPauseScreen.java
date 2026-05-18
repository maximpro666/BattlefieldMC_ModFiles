package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.I18n;
import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.OptionsScreen;
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
    private int[] buttonBaseX;
    private int[] buttonY;
    private int btnW;
    private int btnH;
    private int gap;

    public BattlefieldPauseScreen() {
        super(Component.literal(I18n.get("teamsystem.ui.paused")));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int cx = width / 2;
        int startY = height / 2 - 80;
        btnW = 160;
        btnH = 20;
        gap  = 4;
        int leftX = cx - btnW - 4;
        int rightX = cx + 4;

        buttonBaseX = new int[10];
        buttonY     = new int[10];

        buttonBaseX[0] = leftX; buttonY[0] = startY;
        addRenderableWidget(new BButton(leftX, startY, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.return_to_battle")), btn -> onClose()));

        buttonBaseX[1] = rightX; buttonY[1] = startY;
        addRenderableWidget(new BButton(rightX, startY, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.team_selection")), btn -> {
                Minecraft.getInstance().setScreen(new TeamSelectionScreen());
            }));

        buttonBaseX[2] = leftX; buttonY[2] = startY + (btnH + gap);
        addRenderableWidget(new BButton(leftX, startY + (btnH + gap), btnW, btnH,
            Component.literal("Change Class"), btn -> {
                Minecraft.getInstance().setScreen(new ClassSelectionScreen());
            }));

        buttonBaseX[3] = rightX; buttonY[3] = startY + (btnH + gap);
        addRenderableWidget(new BButton(rightX, startY + (btnH + gap), btnW, btnH,
            Component.literal("Squad"), btn -> {
                Minecraft.getInstance().setScreen(new SquadScreen());
            }));

        buttonBaseX[4] = leftX; buttonY[4] = startY + (btnH + gap) * 2;
        addRenderableWidget(new BButton(leftX, startY + (btnH + gap) * 2, btnW, btnH,
            Component.literal("Vehicle Spawn"), btn -> {
                Minecraft.getInstance().setScreen(new VehicleSelectionScreen());
            }));

        buttonBaseX[5] = rightX; buttonY[5] = startY + (btnH + gap) * 2;
        addRenderableWidget(new BButton(rightX, startY + (btnH + gap) * 2, btnW, btnH,
            Component.literal("Admin Panel"), btn -> {
                Minecraft.getInstance().setScreen(new AdminPanel());
            }));

        buttonBaseX[6] = leftX; buttonY[6] = startY + (btnH + gap) * 3;
        addRenderableWidget(new BButton(leftX, startY + (btnH + gap) * 3, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.settings")), btn -> {
                Minecraft.getInstance().setScreen(new SettingsMenuScreen());
            }));

        buttonBaseX[7] = rightX; buttonY[7] = startY + (btnH + gap) * 3;
        addRenderableWidget(new BButton(rightX, startY + (btnH + gap) * 3, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.vanilla_settings")), btn -> {
                Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options));
            }));

        buttonBaseX[8] = leftX; buttonY[8] = startY + (btnH + gap) * 4;
        addRenderableWidget(new BButton(leftX, startY + (btnH + gap) * 4, btnW, btnH,
            Component.literal(I18n.get("teamsystem.ui.disconnect")), btn -> {
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

        int panelW = 340;
        int panelH = 200;
        int slideOffset = (int)((1f - slideIn) * (-width / 2));
        int panelX = width / 2 - panelW / 2 + slideOffset;
        int panelY = height / 2 - panelH / 2;

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH,
            AnimationHelper.withAlpha(COLOR_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(panelX, panelY, panelX + panelW, panelY + 3,
            AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        String paused = I18n.get("teamsystem.ui.paused_uppercase");
        int pw = font.width(paused);
        g.drawString(font, paused, panelX + panelW / 2 - pw / 2, panelY + 12,
            AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        for (int i = 0; i < buttonBaseX.length && i < children().size(); i++) {
            AbstractWidget w = (AbstractWidget) children().get(i);
            w.setX(buttonBaseX[i] + slideOffset);
            w.setY(buttonY[i]);
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return true; }
}
