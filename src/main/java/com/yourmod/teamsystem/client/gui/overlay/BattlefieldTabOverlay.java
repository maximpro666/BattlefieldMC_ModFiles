package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.scoreboard.ScoreboardRenderer;
import net.minecraft.client.gui.GuiGraphics;

public class BattlefieldTabOverlay {

    private final ScoreboardRenderer scoreboardRenderer = new ScoreboardRenderer();

    public void setVisible(boolean v) {
        scoreboardRenderer.setVisible(v);
    }

    public void tick() {
        scoreboardRenderer.tick();
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        scoreboardRenderer.render(g, screenWidth, screenHeight, 0f);
    }
}
