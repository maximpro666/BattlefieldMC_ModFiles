package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.KitEntry;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.KitSelectPacket;
import com.yourmod.teamsystem.client.gui.component.BButton;
import com.yourmod.teamsystem.client.gui.component.BScrollPanel;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class KitSelectionScreen extends Screen {

    private static final int COLOR_BG       = UITheme.BG_SCREEN;
    private static final int COLOR_CARD     = UITheme.BG_SURFACE;
    private static final int COLOR_SELECTED = UITheme.ACCENT;
    private static final int COLOR_BORDER   = UITheme.NAMETAG_SPECTATOR;
    private static final int COLOR_TEXT     = UITheme.TEXT_PRIMARY;
    private static final int COLOR_SUBTEXT  = UITheme.TEXT_SECONDARY;

    private static final int COLS    = 3;
    private static final int CELL_W  = 130;
    private static final int CELL_H  = 80;
    private static final int PADDING = 10;

    private String selectedKit = null;
    private float fadeAlpha    = 0f;
    private long openTime;
    private BScrollPanel scrollPanel;
    private BButton confirmButton;
    private float[] hoverState;

    public KitSelectionScreen() {
        super(Component.literal("Kit Selection"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        List<KitEntry> kits = ClientTeamData.availableKits;
        hoverState = new float[kits == null ? 0 : kits.size()];

        int panelW = COLS * (CELL_W + PADDING) + PADDING;
        int panelH = height - 80;
        int panelX = width / 2 - panelW / 2;
        int panelY = 40;

        scrollPanel = new BScrollPanel(panelX, panelY, panelW, panelH);
        int rows = (kits == null || kits.isEmpty()) ? 1 : (kits.size() + COLS - 1) / COLS;
        scrollPanel.setContentHeight(rows * (CELL_H + PADDING) + PADDING);

        confirmButton = addRenderableWidget(new BButton(
            width / 2 - 60, height - 28, 120, 20,
            Component.literal("Select Kit"), btn -> confirmSelection()
        ));
    }

    private void confirmSelection() {
        if (selectedKit != null) {
            PacketHandler.CHANNEL.sendToServer(new KitSelectPacket(selectedKit));
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xCC)));

        String title = "SELECT KIT";
        int tw = font.width(title);
        g.drawString(font, title, width / 2 - tw / 2, 14, AnimationHelper.withAlpha(COLOR_SELECTED, (int)(fadeAlpha * 255)));

        List<KitEntry> kits = ClientTeamData.availableKits;
        if (kits != null && !kits.isEmpty()) {
            int panelW = COLS * (CELL_W + PADDING) + PADDING;
            int panelX = width / 2 - panelW / 2;
            int panelY = 40;

            scrollPanel.render(g);

            float scrollOff = scrollPanel.getScrollOffset();
            g.enableScissor(panelX, panelY, panelX + panelW, panelY + scrollPanel.getHeight());

            for (int i = 0; i < kits.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = panelX + PADDING + col * (CELL_W + PADDING);
                int cy = panelY + PADDING + row * (CELL_H + PADDING) - (int)scrollOff;

                if (cy + CELL_H < panelY || cy > panelY + scrollPanel.getHeight()) continue;

                boolean hov = mx >= cx && mx <= cx + CELL_W && my >= cy && my <= cy + CELL_H;
                hoverState[i] = AnimationHelper.lerp(hoverState[i], hov ? 1f : 0f, 0.15f);

                boolean sel = kits.get(i).id().equals(selectedKit);
                drawKitCell(g, cx, cy, kits.get(i), sel, hoverState[i], fadeAlpha);
            }

            g.disableScissor();
        } else {
            String none = "No kits available";
            int nw = font.width(none);
            g.drawString(font, none, width / 2 - nw / 2, height / 2, AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(fadeAlpha * 200)));
        }

        super.render(g, mx, my, pt);
    }

    private void drawKitCell(GuiGraphics g, int x, int y, KitEntry kit, boolean selected, float hover, float alpha) {
        int bgColor  = selected ? AnimationHelper.blendColors(UITheme.BG_SURFACE, UITheme.ACCENT, 0.15f)
                                : AnimationHelper.withAlpha(COLOR_CARD, (int)(alpha * 0xDD));
        int brdColor = selected ? AnimationHelper.withAlpha(COLOR_SELECTED, (int)(alpha * 255))
                                : AnimationHelper.withAlpha(COLOR_BORDER, (int)(alpha * (0x44 + hover * 0xBB)));

        g.fill(x, y, x + CELL_W, y + CELL_H, bgColor);
        g.fill(x, y, x + CELL_W, y + 1, brdColor);
        g.fill(x, y + CELL_H - 1, x + CELL_W, y + CELL_H, brdColor);
        g.fill(x, y, x + 1, y + CELL_H, brdColor);
        g.fill(x + CELL_W - 1, y, x + CELL_W, y + CELL_H, brdColor);

        String display = kit.name().toUpperCase();
        int tw = font.width(display);
        g.drawString(font, display,
            x + CELL_W / 2 - tw / 2, y + CELL_H / 2 - 4,
            AnimationHelper.withAlpha(COLOR_TEXT, (int)(alpha * 255)));

        if (selected) {
            String sel = "\u2713 SELECTED";
            int sw = font.width(sel);
            g.drawString(font, sel, x + CELL_W / 2 - sw / 2, y + CELL_H / 2 + 8,
                AnimationHelper.withAlpha(COLOR_SELECTED, (int)(alpha * 255)));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        List<KitEntry> kits = ClientTeamData.availableKits;
        if (kits != null) {
            int panelW = COLS * (CELL_W + PADDING) + PADDING;
            int panelX = width / 2 - panelW / 2;
            int panelY = 40;
            float scrollOff = scrollPanel.getScrollOffset();

            for (int i = 0; i < kits.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = panelX + PADDING + col * (CELL_W + PADDING);
                int cy = panelY + PADDING + row * (CELL_H + PADDING) - (int)scrollOff;
                if (mx >= cx && mx <= cx + CELL_W && my >= cy && my <= cy + CELL_H) {
                    selectedKit = kits.get(i).id();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollPanel.onScroll((int)mx, (int)my, delta);
        return true;
    }

    @Override
    public void tick() {
        scrollPanel.tick();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
