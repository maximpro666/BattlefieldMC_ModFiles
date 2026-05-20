package com.yourmod.teamsystem.client.gui.component;

import com.yourmod.teamsystem.client.gui.I18n;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.data.LockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class KitCard {

    public static final int CARD_W = 130;
    public static final int CARD_H = 86;

    public static void draw(GuiGraphics g, int x, int y,
                             KitConfig.KitDef kit, String kitKey,
                             LockState lockState, String lockReason,
                             boolean selected, float hover, float fade) {
        boolean locked = !lockState.isSelectable();
        int alpha = (int)(fade * 0xFF);

        int bg = locked
            ? AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fade * 0x88))
            : selected
                ? AnimationHelper.withAlpha(UITheme.ACCENT_GHOST, (int)(fade * 0xFF))
                : AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fade * (0xDD + hover * 0x22)));

        int border = locked
            ? AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0x55))
            : selected
                ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, alpha)
                : hover > 0.01f
                    ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fade * (0x55 + hover * 0x88)))
                    : AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0x88));

        int accentBar = locked
            ? AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0x33))
            : selected
                ? AnimationHelper.withAlpha(UITheme.ACCENT, alpha)
                : AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * (0x88 + hover * 0x55)));

        g.fill(x, y, x + CARD_W, y + CARD_H, bg);
        g.fill(x,              y,              x + CARD_W, y + 1,          border);
        g.fill(x,              y + CARD_H - 1, x + CARD_W, y + CARD_H,    border);
        g.fill(x + CARD_W - 1, y,              x + CARD_W, y + CARD_H,    border);
        g.fill(x,              y,              x + CARD_W, y + 3,    accentBar);

        var font = Minecraft.getInstance().font;

        String displayName = kit.display_name != null
            ? I18n.localize(kit.display_name).toUpperCase()
            : kitKey.toUpperCase();
        int dw = font.width(displayName);
        g.drawString(font, displayName,
            x + CARD_W / 2 - dw / 2, y + CARD_H / 2 - font.lineHeight / 2 - 6,
            locked ? AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 180))
                   : selected ? AnimationHelper.withAlpha(UITheme.ACCENT, alpha)
                              : AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));

        if (kit.description != null && !kit.description.isEmpty()) {
            String desc = I18n.localize(kit.description);
            int maxDescW = CARD_W - 16;
            if (font.width(desc) > maxDescW) {
                int ellipsisW = font.width("...");
                desc = font.plainSubstrByWidth(desc, maxDescW - ellipsisW) + "...";
            }
            int ddw = font.width(desc);
            g.drawString(font, desc,
                x + CARD_W / 2 - ddw / 2, y + CARD_H / 2 + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 160)));
        }

        if (kit.requirements != null) {
            StringBuilder req = new StringBuilder();
            if (kit.requirements.rank > 0)    req.append("R").append(kit.requirements.rank);
            if (kit.requirements.sp_cost > 0) { if (req.length()>0) req.append(" "); req.append(kit.requirements.sp_cost).append("SP"); }
            if (kit.requirements.bc_cost > 0) { if (req.length()>0) req.append(" "); req.append(kit.requirements.bc_cost).append("BC"); }
            if (req.length() > 0) {
                String rs = req.toString();
                g.drawString(font, rs,
                    x + CARD_W / 2 - font.width(rs) / 2, y + CARD_H - 16,
                    AnimationHelper.withAlpha(UITheme.STATUS_WARN, (int)(fade * 180)));
            }
        }

        if (!locked && hover > 0.01f) {
            g.fill(x + 3, y + CARD_H - 2, x + CARD_W, y + CARD_H,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(hover * 100)));
        }

        if (locked) {
            LockBadge.draw(g, x, y, CARD_W, CARD_H, lockReason);
        }
    }
}
