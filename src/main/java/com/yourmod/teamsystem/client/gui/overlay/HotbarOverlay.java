package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class HotbarOverlay {

    private static final int SLOT_SIZE = 24;
    private static final int SLOTS = 9;
    private static final int PADDING = 3;
    private static final int BOTTOM_OFFSET = 10;

    private float fadeAlpha = 1f;
    private long lastMove = System.currentTimeMillis();
    private float[] slotGlow = new float[SLOTS];
    private static final String[] NUM_STRINGS = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "0" };

    public void onSlotChanged() {
        lastMove = System.currentTimeMillis();
        fadeAlpha = 1f;
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        long idle = System.currentTimeMillis() - lastMove;
        fadeAlpha = AnimationHelper.lerp(fadeAlpha, idle > 3000 ? 0.6f : 1f, 0.05f);

        int totalW = SLOTS * SLOT_SIZE + (SLOTS - 1) * PADDING;
        int x = screenWidth / 2 - totalW / 2;
        int y = screenHeight - SLOT_SIZE - BOTTOM_OFFSET;

        int bgAlpha = (int) (fadeAlpha * 255);

        RenderHelper.dropShadow(g, x - 6, y - 4, totalW + 12, SLOT_SIZE + 8, 3, (int) (fadeAlpha * 100));
        RenderHelper.roundedRect(g, x - 6, y - 4, totalW + 12, SLOT_SIZE + 8, 4,
            AnimationHelper.withAlpha(UITheme.BG_HUD, (int) (fadeAlpha * 220)));

        g.fill(x - 6, y + SLOT_SIZE + 3, x + totalW + 6, y + SLOT_SIZE + 4,
            AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int) (fadeAlpha * 80)));

        Inventory inv = mc.player.getInventory();
        int selected = inv.selected;

        for (int i = 0; i < SLOTS; i++) {
            int sx = x + i * (SLOT_SIZE + PADDING);
            ItemStack stack = inv.getItem(i);
            boolean isSel = i == selected;
            slotGlow[i] = AnimationHelper.lerp(slotGlow[i], isSel ? 1f : 0f, 0.15f);

            int slotBg = AnimationHelper.blendColors(
                AnimationHelper.withAlpha(UITheme.BG_SLOT, (int) (fadeAlpha * 220)),
                UITheme.ACCENT,
                slotGlow[i] * 0.25f * fadeAlpha);

            RenderHelper.roundedRect(g, sx, y, SLOT_SIZE, SLOT_SIZE, 3, slotBg);

            if (isSel) {
                RenderHelper.glow(g, sx, y, SLOT_SIZE, SLOT_SIZE, UITheme.ACCENT, 4, fadeAlpha * 0.6f);

                int sa = (int) (fadeAlpha * 255);
                g.fill(sx - 1, y - 2, sx + SLOT_SIZE + 1, y,
                    AnimationHelper.withAlpha(UITheme.ACCENT, sa));
                g.fill(sx - 1, y + SLOT_SIZE, sx + SLOT_SIZE + 1, y + SLOT_SIZE + 2,
                    AnimationHelper.withAlpha(UITheme.ACCENT, sa));
                g.fill(sx - 2, y, sx, y + SLOT_SIZE,
                    AnimationHelper.withAlpha(UITheme.ACCENT, sa));
                g.fill(sx + SLOT_SIZE, y, sx + SLOT_SIZE + 2, y + SLOT_SIZE,
                    AnimationHelper.withAlpha(UITheme.ACCENT, sa));
            } else {
                int ba = (int) (fadeAlpha * 100);
                g.fill(sx, y + SLOT_SIZE - 1, sx + SLOT_SIZE, y + SLOT_SIZE,
                    AnimationHelper.withAlpha(UITheme.BORDER, ba));
                g.fill(sx, y, sx + SLOT_SIZE, y + 1,
                    AnimationHelper.withAlpha(UITheme.BORDER, ba));
                g.fill(sx, y, sx + 1, y + SLOT_SIZE,
                    AnimationHelper.withAlpha(UITheme.BORDER, ba));
                g.fill(sx + SLOT_SIZE - 1, y, sx + SLOT_SIZE, y + SLOT_SIZE,
                    AnimationHelper.withAlpha(UITheme.BORDER, ba));
            }

            if (!stack.isEmpty()) {
                g.renderItem(stack, sx + 4, y + 4);
                g.renderItemDecorations(mc.font, stack, sx + 4, y + 4);
            }

            g.drawString(mc.font, NUM_STRINGS[i], sx + 4, y - 10,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int) (fadeAlpha * 140)));
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
        int ammoX = hx + totalW - ammoW - 12;
        int ammoY = hy - 20;

        RenderHelper.roundedRect(g, ammoX - 6, ammoY - 3, ammoW + 14, 14, 3,
            AnimationHelper.withAlpha(UITheme.HUD_AMMO_BG, (int) (fadeAlpha * 240)));

        g.fill(ammoX - 6, ammoY - 3, ammoX - 3, ammoY + 11,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int) (fadeAlpha * 200)));

        g.drawString(mc.font, ammoStr, ammoX, ammoY + 1,
            AnimationHelper.withAlpha(UITheme.HUD_AMMO_TEXT, (int) (fadeAlpha * 255)));
    }
}
