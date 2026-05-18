package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BProgressBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class VitalsOverlay {

    private static final int COLOR_TEXT   = UITheme.TEXT_SECONDARY;
    private static final int BAR_W        = 80;
    private static final int BAR_H        = 2;

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
        int y = screenHeight - 10;

        g.drawString(Minecraft.getInstance().font, "HP " + (int) hp,
            x, y, AnimationHelper.withAlpha(COLOR_TEXT, 200));

        if (hpBar == null) hpBar = new BProgressBar(x + 32, y + 2, BAR_W, BAR_H, hpColor);
        hpBar.setPosition(x + 32, y + 2);
        hpBar.setFraction(smoothHp);
        hpBar.render(g);
    }
}
