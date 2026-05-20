package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.scoreboard.ScoreboardRenderer;
import net.minecraft.client.gui.GuiGraphics;

public class BattlefieldTabOverlay {

    private final ScoreboardRenderer scoreboardRenderer = new ScoreboardRenderer();

    public boolean isVisible() {
        return scoreboardRenderer.isVisible();
    }

    public void setVisible(boolean v) {
        scoreboardRenderer.setVisible(v);
    }

    public void scrollBy(int delta) {
        scoreboardRenderer.scrollBy(delta);
    }

    public void tick() {
        scoreboardRenderer.tick();
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        scoreboardRenderer.render(g, screenWidth, screenHeight, 0f);
    }
}
