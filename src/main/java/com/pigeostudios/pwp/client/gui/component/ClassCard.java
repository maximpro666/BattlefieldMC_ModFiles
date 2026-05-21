package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.I18n;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.data.ItemResolver;
import com.pigeostudios.pwp.data.KitConfig;
import com.pigeostudios.pwp.data.LockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class ClassCard {

    public static final int CARD_W = 140;
    public static final int CARD_H = 112;

    public static void draw(GuiGraphics g, int x, int y,
                             KitConfig.ClassConfig cl, String key,
                             LockState lockState, String lockReason,
                             float hover, float fade) {
        boolean locked = !lockState.isSelectable();
        int alpha = (int)(fade * 0xFF);

        int bg = locked
            ? AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fade * 0x88))
            : AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fade * (0xDD + hover * 0x22)));

        int border = locked
            ? AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0x66))
            : hover > 0.01f
                ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fade * (0x77 + hover * 0x88)))
                : AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xAA));

        int accentBar = locked
            ? AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0x44))
            : AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * (0xAA + hover * 0x55)));

        g.fill(x, y, x + CARD_W, y + CARD_H, bg);

        g.fill(x,              y,              x + CARD_W, y + 1,          border);
        g.fill(x,              y + CARD_H - 1, x + CARD_W, y + CARD_H,    border);
        g.fill(x + CARD_W - 1, y,              x + CARD_W, y + CARD_H,    border);

        g.fill(x, y, x + 3, y + CARD_H, accentBar);

        if (!locked && hover > 0.01f) {
            g.fill(x + 3, y + CARD_H - 2, x + CARD_W, y + CARD_H,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(hover * 120)));
        }

        var font = Minecraft.getInstance().font;

        if (cl.icon != null && !cl.icon.isEmpty()) {
            ItemStack stack = ItemResolver.resolve(cl.icon);
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + CARD_W / 2 - 8, y + 16);
            }
        }

        String displayName = cl.display_name != null
            ? I18n.localize(cl.display_name).toUpperCase()
            : key.toUpperCase();
        int maxNameW = CARD_W - 16;
        if (font.width(displayName) > maxNameW) {
            int ellipsisW = font.width("...");
            displayName = font.plainSubstrByWidth(displayName, maxNameW - ellipsisW) + "...";
        }
        int dw = font.width(displayName);
        g.drawString(font, displayName, x + CARD_W / 2 - dw / 2, y + CARD_H / 2 + 2,
            locked ? AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 180))
                   : AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));

        int kitCount = cl.kits != null ? cl.kits.size() : 0;
        String sub = kitCount + " kit" + (kitCount != 1 ? "s" : "");
        int sw = font.width(sub);
        g.drawString(font, sub, x + CARD_W / 2 - sw / 2, y + CARD_H / 2 + 16,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 180)));

        if (locked) {
            LockBadge.draw(g, x, y, CARD_W, CARD_H, lockReason);
        }
    }
}
