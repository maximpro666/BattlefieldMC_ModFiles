package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BProgressBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class VitalsOverlay {

    private static final int COLOR_BG     = UITheme.BG_HUD;
    private static final int COLOR_BORDER = UITheme.BORDER;
    private static final int COLOR_TEXT   = UITheme.TEXT_PRIMARY;
    private static final int BAR_W        = 120;
    private static final int BAR_H        = 8;

    private BProgressBar hpBar;
    private float smoothHp = 1f;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        float maxHp = player.getMaxHealth();
        float hp    = player.getHealth();
        float frac  = maxHp > 0 ? hp / maxHp : 0f;
        smoothHp    = AnimationHelper.lerp(smoothHp, frac, 0.1f);
        int hpColor = AnimationHelper.hpColor(smoothHp);

        int x = 8;
        int y = screenHeight - 30;

        g.fill(x, y, x + BAR_W + 16, y + 22, AnimationHelper.withAlpha(COLOR_BG, 200));
        g.fill(x, y, x + BAR_W + 16, y + 1, AnimationHelper.withAlpha(COLOR_BORDER, 180));

        g.drawString(Minecraft.getInstance().font, "HP  " + (int) hp,
            x + 4, y + 5, AnimationHelper.withAlpha(COLOR_TEXT, 230));

        if (hpBar == null) hpBar = new BProgressBar(x + 4, y + 14, BAR_W, BAR_H, hpColor);
        hpBar.setFraction(smoothHp);
        hpBar.render(g);
    }
}
