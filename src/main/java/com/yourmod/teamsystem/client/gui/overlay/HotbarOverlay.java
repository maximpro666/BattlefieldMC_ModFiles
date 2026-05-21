package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class HotbarOverlay {

    private static final int COLOR_BG       = UITheme.BG_HUD;
    private static final int COLOR_SELECTED = UITheme.ACCENT;
    private static final int COLOR_BORDER   = UITheme.BORDER;
    private static final int SLOT_SIZE      = 22;
    private static final int SLOTS          = 9;
    private static final int PADDING        = 3;
    private static final int BOTTOM_OFFSET  = 10;

    private float fadeAlpha = 1f;
    private long  lastMove  = System.currentTimeMillis();
    private float[] slotGlow = new float[SLOTS];
    private static final String[] NUM_STRINGS = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };

    public void onSlotChanged() {
        lastMove   = System.currentTimeMillis();
        fadeAlpha  = 1f;
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        long idle = System.currentTimeMillis() - lastMove;
        if (idle > 3000) {
            fadeAlpha = AnimationHelper.lerp(fadeAlpha, 0.55f, 0.05f);
        } else {
            fadeAlpha = AnimationHelper.lerp(fadeAlpha, 1f, 0.15f);
        }

        int totalW = SLOTS * SLOT_SIZE + (SLOTS - 1) * PADDING;
        int x = screenWidth / 2 - totalW / 2;
        int y = screenHeight - SLOT_SIZE - BOTTOM_OFFSET;

        int bgAlpha = (int)(fadeAlpha * 230);
        
        g.fill(x - 5, y - 3, x + totalW + 5, y + SLOT_SIZE + 3,
            AnimationHelper.withAlpha(COLOR_BG, bgAlpha));
        
        g.fill(x - 5, y + SLOT_SIZE + 2, x + totalW + 5, y + SLOT_SIZE + 3,
            AnimationHelper.withAlpha(UITheme.ACCENT_DIM, bgAlpha / 2));

        Inventory inv = mc.player.getInventory();
        int selected  = inv.selected;

        for (int i = 0; i < SLOTS; i++) {
            int sx = x + i * (SLOT_SIZE + PADDING);
            ItemStack stack = inv.getItem(i);
            boolean isSel = i == selected;
            slotGlow[i] = AnimationHelper.lerp(slotGlow[i], isSel ? 1f : 0f, 0.15f);

            int slotBg = AnimationHelper.blendColors(
                AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(fadeAlpha * 230)),
                UITheme.ACCENT,
                slotGlow[i] * 0.2f * fadeAlpha);
            
            RenderHelper.roundedRect(g, sx, y, SLOT_SIZE, SLOT_SIZE, 2, slotBg);

            if (isSel) {
                RenderHelper.glow(g, sx, y, SLOT_SIZE, SLOT_SIZE, COLOR_SELECTED, 3, fadeAlpha);
                
                int selAlpha = (int)(fadeAlpha * 255);
                g.fill(sx - 1, y - 1, sx + SLOT_SIZE + 1, y, 
                    AnimationHelper.withAlpha(COLOR_SELECTED, selAlpha));
                g.fill(sx - 1, y + SLOT_SIZE, sx + SLOT_SIZE + 1, y + SLOT_SIZE + 1, 
                    AnimationHelper.withAlpha(COLOR_SELECTED, selAlpha));
                g.fill(sx - 1, y, sx, y + SLOT_SIZE, 
                    AnimationHelper.withAlpha(COLOR_SELECTED, selAlpha));
                g.fill(sx + SLOT_SIZE, y, sx + SLOT_SIZE + 1, y + SLOT_SIZE, 
                    AnimationHelper.withAlpha(COLOR_SELECTED, selAlpha));
            } else {
                int brdAlpha = (int)(fadeAlpha * 120);
                g.fill(sx, y + SLOT_SIZE - 1, sx + SLOT_SIZE, y + SLOT_SIZE,
                    AnimationHelper.withAlpha(COLOR_BORDER, brdAlpha));
                g.fill(sx, y, sx + SLOT_SIZE, y + 1,
                    AnimationHelper.withAlpha(COLOR_BORDER, brdAlpha));
                g.fill(sx, y, sx + 1, y + SLOT_SIZE,
                    AnimationHelper.withAlpha(COLOR_BORDER, brdAlpha));
                g.fill(sx + SLOT_SIZE - 1, y, sx + SLOT_SIZE, y + SLOT_SIZE,
                    AnimationHelper.withAlpha(COLOR_BORDER, brdAlpha));
            }

            if (!stack.isEmpty()) {
                g.renderItem(stack, sx + 3, y + 3);
                g.renderItemDecorations(mc.font, stack, sx + 3, y + 3);
            }

            g.drawString(mc.font, NUM_STRINGS[i], sx + 3, y - 9,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 160)));
        }

        renderAmmoDisplay(g, screenWidth, screenHeight, mc);
    }

    private void renderAmmoDisplay(GuiGraphics g, int screenWidth, int screenHeight, Minecraft mc) {
        if (mc.player == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) return;

        int maxAmmo = 0;
        int currentAmmo = 0;

        var tag = held.getTag();
        if (tag != null && tag.contains("ammo")) {
            var ammoTag = tag.getCompound("ammo");
            currentAmmo = ammoTag.getInt("current");
            maxAmmo = ammoTag.getInt("max");
        }

        if (maxAmmo <= 0) {
            if (held.getMaxStackSize() > 1) {
                currentAmmo = held.getCount();
                maxAmmo = held.getMaxStackSize();
            } else {
                return;
            }
        }

        int totalW = SLOTS * SLOT_SIZE + (SLOTS - 1) * PADDING;
        int hx = screenWidth / 2 - totalW / 2;
        int hy = screenHeight - SLOT_SIZE - BOTTOM_OFFSET;

        String ammoStr = currentAmmo + " / " + maxAmmo;
        int ammoW = mc.font.width(ammoStr);
        int ammoX = hx + totalW - ammoW - 8;
        int ammoY = hy - 16;

        RenderHelper.roundedRect(g, ammoX - 4, ammoY - 2, ammoW + 8, 12, 2,
            AnimationHelper.withAlpha(UITheme.HUD_AMMO_BG, (int)(fadeAlpha * 230)));

        g.drawString(mc.font, ammoStr, ammoX, ammoY,
            AnimationHelper.withAlpha(UITheme.HUD_AMMO_TEXT, (int)(fadeAlpha * 255)));
    }
}
