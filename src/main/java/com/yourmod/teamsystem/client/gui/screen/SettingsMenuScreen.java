package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public class SettingsMenuScreen extends Screen {

    private static final int COLOR_BG     = UITheme.BG_SCREEN;
    private static final int COLOR_PANEL  = UITheme.BG_PANEL;
    private static final int COLOR_ORANGE = UITheme.ACCENT;
    private static final int COLOR_BORDER = UITheme.BORDER;
    private static final int COLOR_TEXT   = UITheme.TEXT_PRIMARY;

    private static final List<String> CATEGORIES = Arrays.asList(
        "HUD & Overlays",
        "Audio",
        "Graphics",
        "Controls",
        "Accessibility"
    );

    private float[] catHover = new float[CATEGORIES.size()];
    private int selectedCat  = -1;
    private float fadeAlpha  = 0f;
    private long openTime;

    private static final int PANEL_W = 260;
    private static final int ROW_H   = 28;
    private static final int GAP     = 6;

    public SettingsMenuScreen() {
        super(Component.literal("Settings"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        addRenderableWidget(new BButton(
            width / 2 - 60, height - 34, 120, 20,
            Component.literal("Back"), btn -> onClose()
        ));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xCC)));

        int panelX = width / 2 - PANEL_W / 2;
        int panelY = 40;
        int panelH = CATEGORIES.size() * (ROW_H + GAP) + 30;

        g.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, AnimationHelper.withAlpha(COLOR_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 3, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        String title = "SETTINGS";
        int tw = font.width(title);
        g.drawString(font, title, width / 2 - tw / 2, panelY + 10, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        for (int i = 0; i < CATEGORIES.size(); i++) {
            int ry = panelY + 28 + i * (ROW_H + GAP);
            boolean hov = mx >= panelX + 4 && mx <= panelX + PANEL_W - 4 && my >= ry && my <= ry + ROW_H;
            catHover[i] = AnimationHelper.lerp(catHover[i], hov ? 1f : 0f, 0.15f);
            boolean sel = selectedCat == i;
            drawCategory(g, panelX + 4, ry, PANEL_W - 8, CATEGORIES.get(i), sel, catHover[i], fadeAlpha);
        }

        super.render(g, mx, my, pt);
    }

    private void drawCategory(GuiGraphics g, int x, int y, int w, String name,
                               boolean selected, float hover, float alpha) {
        int bg  = selected ? AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 0x33)) : 0;
        int brd = AnimationHelper.withAlpha(UITheme.NAMETAG_SPECTATOR, (int)(alpha * (0x44 + hover * 0xBB)));
        if (bg != 0) g.fill(x, y, x + w, y + ROW_H, bg);
        if (selected) g.fill(x, y, x + 3, y + ROW_H, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(alpha * 255)));
        g.fill(x, y + ROW_H - 1, x + w, y + ROW_H, brd);

        int tw = font.width(name);
        g.drawString(font, name, x + w / 2 - tw / 2, y + ROW_H / 2 - 4,
            AnimationHelper.withAlpha(COLOR_TEXT, (int)(alpha * 255)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int panelX = width / 2 - PANEL_W / 2;
        int panelY = 40;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            int ry = panelY + 28 + i * (ROW_H + GAP);
            if (mx >= panelX + 4 && mx <= panelX + PANEL_W - 4 && my >= ry && my <= ry + ROW_H) {
                selectedCat = (selectedCat == i) ? -1 : i;
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
