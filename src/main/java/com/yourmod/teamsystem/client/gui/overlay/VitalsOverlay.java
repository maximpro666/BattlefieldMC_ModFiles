package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.BProgressBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class VitalsOverlay implements IGuiOverlay {
    private BProgressBar healthBar;
    private BProgressBar armorBar;

    public VitalsOverlay() {
        healthBar = new BProgressBar(0, 0, 100, 10, 0xFF44FF44);
        armorBar = new BProgressBar(0, 0, 100, 6, 0xFF4488FF);
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        int armor = 0;
        var armorItem = mc.player.getInventory().getArmor(2);
        if (!armorItem.isEmpty()) {
            armor = armorItem.getMaxDamage() - armorItem.getDamageValue();
        }
        int maxArmor = 100;

        int barX = 10;
        int barY = screenHeight - 50;

        healthBar.setPosition(barX, barY);
        healthBar.setSize(100, 10);
        healthBar.setFraction(health / maxHealth);
        healthBar.tick();
        healthBar.renderHp(g);

        armorBar.setPosition(barX, barY + 14);
        armorBar.setSize(100, 6);
        armorBar.setFraction(Math.min(1, armor / (float) maxArmor));
        armorBar.tick();
        armorBar.render(g);
    }
}
