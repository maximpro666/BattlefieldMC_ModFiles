package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class HotbarOverlay {

    private static final int COLOR_BG       = UITheme.BG_HUD;
    private static final int COLOR_SELECTED = UITheme.ACCENT;
    private static final int COLOR_BORDER   = UITheme.BORDER;
    private static final int SLOT_SIZE      = 20;
    private static final int SLOTS          = 9;
    private static final int PADDING        = 2;

    private float fadeAlpha = 1f;
    private long  lastMove  = System.currentTimeMillis();

    public void onSlotChanged() {
        lastMove   = System.currentTimeMillis();
        fadeAlpha  = 1f;
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        long idle = System.currentTimeMillis() - lastMove;
        if (idle > 3000) {
            fadeAlpha = AnimationHelper.lerp(fadeAlpha, 0.3f, 0.05f);
        } else {
            fadeAlpha = AnimationHelper.lerp(fadeAlpha, 1f, 0.15f);
        }

        int totalW = SLOTS * (SLOT_SIZE + PADDING) - PADDING;
        int x = screenWidth / 2 - totalW / 2;
        int y = screenHeight - SLOT_SIZE - 8;

        g.fill(x - 4, y - 2, x + totalW + 4, y + SLOT_SIZE + 2,
            AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 180)));

        Inventory inv = mc.player.getInventory();
        int selected  = inv.selected;

        for (int i = 0; i < SLOTS; i++) {
            int sx = x + i * (SLOT_SIZE + PADDING);
            ItemStack stack = inv.getItem(i);
            boolean isSel = i == selected;

            int slotBg = isSel
                ? AnimationHelper.withAlpha(COLOR_SELECTED, (int)(fadeAlpha * 80))
                : AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(fadeAlpha * 180));
            g.fill(sx, y, sx + SLOT_SIZE, y + SLOT_SIZE, slotBg);

            int brd = isSel
                ? AnimationHelper.withAlpha(COLOR_SELECTED, (int)(fadeAlpha * 255))
                : AnimationHelper.withAlpha(COLOR_BORDER, (int)(fadeAlpha * 150));
            g.fill(sx, y, sx + SLOT_SIZE, y + 1, brd);
            g.fill(sx, y + SLOT_SIZE - 1, sx + SLOT_SIZE, y + SLOT_SIZE, brd);
            g.fill(sx, y, sx + 1, y + SLOT_SIZE, brd);
            g.fill(sx + SLOT_SIZE - 1, y, sx + SLOT_SIZE, y + SLOT_SIZE, brd);

            if (!stack.isEmpty()) {
                g.renderItem(stack, sx + 2, y + 2);
                g.renderItemDecorations(mc.font, stack, sx + 2, y + 2);
            }

            String num = String.valueOf(i + 1 == 10 ? 0 : i + 1);
            g.drawString(mc.font, num, sx + 2, y - 8,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 180)));
        }
    }
}
