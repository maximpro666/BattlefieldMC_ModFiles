package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.data.ItemResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class SlotRow {

    public static final int SLOT_H = 32;

    private final String   slotKey;
    private final String   icon;
    private final List<String> options;
    private int   selectedIndex = 0;

    private Runnable onChanged;

    public SlotRow(String slotKey, String icon, List<String> options) {
        this.slotKey = slotKey;
        this.icon    = icon;
        this.options = options;
    }

    public void setOnChanged(Runnable r) { this.onChanged = r; }

    public int    getSelectedIndex()  { return selectedIndex; }
    public String getSelectedValue()  { return options.isEmpty() ? "" : options.get(selectedIndex); }
    public String getSlotKey()        { return slotKey; }

    public void cycle(int dir) {
        if (options.size() <= 1) return;
        selectedIndex = (selectedIndex + dir + options.size()) % options.size();
        if (onChanged != null) onChanged.run();
    }

    public void render(GuiGraphics g, int x, int y, int w,
                       boolean active, int mx, int my, float alpha) {
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + SLOT_H;

        int bg = active
            ? AnimationHelper.blendColors(UITheme.BG_SLOT, UITheme.ACCENT, 0.12f)
            : AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(alpha * 0xDD));

        int border = active
            ? AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0xFF))
            : AnimationHelper.withAlpha(UITheme.BORDER, (int)(alpha * (hov ? 0xAA : 0x55)));

        g.fill(x, y, x + w, y + SLOT_H, bg);
        g.fill(x,     y,           x + w, y + 1,          border);
        g.fill(x,     y + SLOT_H - 1, x + w, y + SLOT_H,  border);
        g.fill(x + w - 1, y,      x + w, y + SLOT_H,      border);

        if (active) {
            g.fill(x, y, x + 3, y + SLOT_H,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0xFF)));
        } else {
            g.fill(x, y, x + 1, y + SLOT_H, border);
        }

        var font = Minecraft.getInstance().font;
        int iy = y + (SLOT_H - font.lineHeight * 2 - 2) / 2;

        g.drawString(font, icon, x + 8, iy + 1,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(alpha * 150)));

        g.drawString(font, slotKey.toUpperCase(), x + 26, iy,
            AnimationHelper.withAlpha(active ? UITheme.ACCENT : UITheme.TEXT_MUTED,
                (int)(alpha * (active ? 0xFF : 0xB0))));

        String sel = getSelectedValue();
        String display = sel.isEmpty() ? "\u2014"
            : ItemResolver.getDisplayName(sel).isEmpty()
                ? sel.replace("_", " ").toUpperCase()
                : ItemResolver.getDisplayName(sel).toUpperCase();
        g.drawString(font, display, x + 26, iy + font.lineHeight + 2,
            AnimationHelper.withAlpha(sel.isEmpty() ? UITheme.TEXT_MUTED : UITheme.TEXT_PRIMARY,
                (int)(alpha * 0xFF)));

        boolean canCycle = options.size() > 1;
        int arrowY = y + SLOT_H / 2 - font.lineHeight / 2;
        int leftX  = x + w - 48;

        g.drawString(font, "\u2039", leftX, arrowY,
            AnimationHelper.withAlpha(canCycle ? UITheme.ACCENT : UITheme.TEXT_MUTED,
                (int)(alpha * (canCycle ? 0xCC : 0x55))));

        String count = (selectedIndex + 1) + "/" + options.size();
        int cw = font.width(count);
        g.drawString(font, count, leftX + 12 + (20 - cw) / 2, arrowY,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(alpha * 0x99)));

        g.drawString(font, "\u203A", leftX + 36, arrowY,
            AnimationHelper.withAlpha(canCycle ? UITheme.ACCENT : UITheme.TEXT_MUTED,
                (int)(alpha * (canCycle ? 0xCC : 0x55))));
    }

    public boolean handleClick(double mx, int x, int y, int w) {
        if (mx < x || mx > x + w) return false;
        int leftX = x + w - 48;
        if (mx >= leftX && mx <= leftX + 10) {
            cycle(-1); return true;
        }
        if (mx >= leftX + 34 && mx <= leftX + 46) {
            cycle(1); return true;
        }
        cycle(1);
        return true;
    }
}
