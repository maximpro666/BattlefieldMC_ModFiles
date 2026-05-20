package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class BattlefieldScreen extends Screen {

    protected float fadeAlpha;
    protected long openTime;
    protected int tickCount;
    protected int mouseX, mouseY;

    protected BattlefieldScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        tickCount = 0;
        fadeAlpha = 0f;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        tickCount++;
        mouseX = mx;
        mouseY = my;
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);
    }

    protected int currentAlpha() {
        return (int)(fadeAlpha * 0xFF);
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        g.fill(0, 0, width, height,
                AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));
    }
}
