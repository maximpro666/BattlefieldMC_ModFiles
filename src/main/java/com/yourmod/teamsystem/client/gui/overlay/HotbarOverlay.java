package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class HotbarOverlay implements IGuiOverlay {
    private static final int SLOT_SIZE = 22;
    private static final int PADDING = 2;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        var inventory = mc.player.getInventory();
        int selected = inventory.selected;
        int totalW = 9 * SLOT_SIZE + 8 * PADDING;
        int x = (screenWidth - totalW) / 2;
        int y = screenHeight - SLOT_SIZE - 10;

        g.fill(x - 2, y - 2, x + totalW + 2, y + SLOT_SIZE + 2, 0x88000000);
        BButton.drawBorder(g, x - 2, y - 2, totalW + 4, SLOT_SIZE + 4, 0xFF555555);

        for (int i = 0; i < 9; i++) {
            int sx = x + i * (SLOT_SIZE + PADDING);
            int slotColor = i == selected ? 0x66FFFFFF : 0x33000000;
            boolean isSelected = i == selected;
            if (isSelected) {
                g.fill(sx - 1, y - 1, sx + SLOT_SIZE + 1, y + SLOT_SIZE + 1, 0xFF00AAFF);
            }
            g.fill(sx, y, sx + SLOT_SIZE, y + SLOT_SIZE, slotColor);

            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, sx + 3, y + 3);
                g.renderItemDecorations(mc.font, stack, sx + 3, y + 3);
            }

            if (isSelected) {
                g.drawString(mc.font, String.valueOf(i + 1), sx + 2, y + SLOT_SIZE - 9, 0xFFAAAAAA);
            }
        }
    }
}
