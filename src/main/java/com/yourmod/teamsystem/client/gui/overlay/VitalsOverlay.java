package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BProgressBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public class VitalsOverlay {

    private static final int COLOR_TEXT   = UITheme.HUD_AMMO_TEXT;
    private static final int BAR_W        = 80;
    private static final int BAR_H        = 3;
    private static final int ARMOR_BAR_W  = 50;
    private static final int ARMOR_BAR_H  = 2;

    private BProgressBar hpBar;
    private BProgressBar armorBar;
    private float smoothHp = 1f;
    private float smoothArmor = 0f;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        float maxHp = player.getMaxHealth();
        float hp    = player.getHealth();
        float frac  = maxHp > 0 ? hp / maxHp : 0f;
        smoothHp    = AnimationHelper.lerp(smoothHp, frac, 0.1f);

        int hpColor;
        if (smoothHp > 0.6f) hpColor = UITheme.HUD_HP_FULL;
        else if (smoothHp > 0.3f) hpColor = UITheme.HUD_HP_MID;
        else hpColor = UITheme.HUD_HP_LOW;

        int armorVal = player.getArmorValue();
        float armorFrac = Math.min(1f, armorVal / 20f);
        smoothArmor = AnimationHelper.lerp(smoothArmor, armorFrac, 0.1f);

        int x = 8;
        int y = screenHeight - 14;

        g.drawString(Minecraft.getInstance().font, "HP " + (int) hp,
            x, y, AnimationHelper.withAlpha(COLOR_TEXT, 220));

        if (hpBar == null) hpBar = new BProgressBar(x + 32, y, BAR_W, BAR_H, hpColor);
        hpBar.setPosition(x + 32, y);
        hpBar.setFraction(smoothHp);
        hpBar.render(g);

        g.drawString(Minecraft.getInstance().font, "AR " + (int)(armorVal),
            x, y + 8, AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, 180));

        if (armorBar == null) armorBar = new BProgressBar(x + 32, y + 8, ARMOR_BAR_W, ARMOR_BAR_H, UITheme.TEXT_MUTED);
        armorBar.setPosition(x + 32, y + 8);
        armorBar.setFraction(smoothArmor);
        armorBar.render(g);
    }
}
