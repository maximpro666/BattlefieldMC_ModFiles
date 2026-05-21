package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.UITheme;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.VehicleEntry;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.VehicleSpawnPacket;
import com.pigeostudios.pwp.client.gui.component.BButton;
import com.pigeostudios.pwp.client.gui.component.BScrollPanel;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class VehicleSelectionScreen extends Screen {

    private static final int COLOR_BG       = UITheme.BG_SCREEN;
    private static final int COLOR_CARD     = UITheme.BG_PANEL;
    private static final int COLOR_ORANGE   = UITheme.ACCENT;
    private static final int COLOR_BORDER   = UITheme.BORDER;
    private static final int COLOR_TEXT     = UITheme.TEXT_PRIMARY;
    private static final int COLOR_SUBTEXT  = UITheme.TEXT_MUTED;

    private static final int COLS   = 2;
    private static final int CELL_W = 160;
    private static final int CELL_H = 60;
    private static final int GAP    = 8;

    private String selectedVehicle = null;
    private float fadeAlpha = 0f;
    private long openTime;
    private BScrollPanel scrollPanel;
    private BButton spawnButton;
    private float[] hoverState;

    public VehicleSelectionScreen() {
        super(Component.literal("Vehicle Selection"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        List<VehicleEntry> vehicles = ClientTeamData.availableVehicles;
        hoverState = new float[vehicles == null ? 0 : vehicles.size()];

        int panelW = COLS * (CELL_W + GAP) + GAP;
        int panelH = height - 80;
        scrollPanel = new BScrollPanel(width / 2 - panelW / 2, 40, panelW, panelH);
        int rows = (vehicles == null || vehicles.isEmpty()) ? 1 : (vehicles.size() + COLS - 1) / COLS;
        scrollPanel.setContentHeight(rows * (CELL_H + GAP) + GAP);

        spawnButton = addRenderableWidget(new BButton(
            width / 2 - 60, height - 28, 120, 20,
            Component.literal("Spawn Vehicle"), btn -> spawnVehicle()
        ));
    }

    private void spawnVehicle() {
        if (selectedVehicle != null) {
            PacketHandler.CHANNEL.sendToServer(new VehicleSpawnPacket(selectedVehicle));
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xCC)));

        String title = "VEHICLE SPAWN";
        int tw = font.width(title);
        g.drawString(font, title, width / 2 - tw / 2, 14, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        List<VehicleEntry> vehicles = ClientTeamData.availableVehicles;
        if (vehicles != null && !vehicles.isEmpty()) {
            int panelW = COLS * (CELL_W + GAP) + GAP;
            int panelX = width / 2 - panelW / 2;
            int panelY = 40;
            float scrollOff = scrollPanel.getScrollOffset();

            scrollPanel.render(g);
            g.enableScissor(panelX, panelY, panelX + panelW, panelY + scrollPanel.getHeight());

            for (int i = 0; i < vehicles.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = panelX + GAP + col * (CELL_W + GAP);
                int cy = panelY + GAP + row * (CELL_H + GAP) - (int)scrollOff;
                if (cy + CELL_H < panelY || cy > panelY + scrollPanel.getHeight()) continue;

                boolean hov = mx >= cx && mx <= cx + CELL_W && my >= cy && my <= cy + CELL_H;
                hoverState[i] = AnimationHelper.lerp(hoverState[i], hov ? 1f : 0f, 0.15f);
                boolean sel = vehicles.get(i).id().equals(selectedVehicle);
                drawVehicleCell(g, cx, cy, vehicles.get(i), sel, hoverState[i], fadeAlpha);
            }

            g.disableScissor();
        } else {
            String none = "No vehicles available";
            int nw = font.width(none);
            g.drawString(font, none, width / 2 - nw / 2, height / 2,
                AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(fadeAlpha * 200)));
        }

        super.render(g, mx, my, pt);
    }

    private void drawVehicleCell(GuiGraphics g, int x, int y, VehicleEntry vehicle,
                                  boolean selected, float hover, float alpha) {
        int bg  = selected
            ? AnimationHelper.blendColors(UITheme.BG_PANEL, UITheme.ACCENT, 0.18f)
            : AnimationHelper.withAlpha(COLOR_CARD, (int)(alpha * 0xDD));
        int brd = selected
            ? AnimationHelper.withAlpha(COLOR_ORANGE, (int)(alpha * 255))
            : AnimationHelper.withAlpha(COLOR_BORDER, (int)(alpha * (0x44 + hover * 0xBB)));

        g.fill(x, y, x + CELL_W, y + CELL_H, bg);
        g.fill(x, y, x + CELL_W, y + 1, brd);
        g.fill(x, y + CELL_H - 1, x + CELL_W, y + CELL_H, brd);
        g.fill(x, y, x + 1, y + CELL_H, brd);
        g.fill(x + CELL_W - 1, y, x + CELL_W, y + CELL_H, brd);

        if (selected) {
            g.fill(x, y, x + 3, y + CELL_H, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(alpha * 255)));
        }

        String display = vehicle.name().toUpperCase();
        int tw = font.width(display);
        g.drawString(font, display, x + CELL_W / 2 - tw / 2, y + CELL_H / 2 - 4,
            AnimationHelper.withAlpha(COLOR_TEXT, (int)(alpha * 255)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        List<VehicleEntry> vehicles = ClientTeamData.availableVehicles;
        if (vehicles != null) {
            int panelW = COLS * (CELL_W + GAP) + GAP;
            int panelX = width / 2 - panelW / 2;
            int panelY = 40;
            float scrollOff = scrollPanel.getScrollOffset();
            for (int i = 0; i < vehicles.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = panelX + GAP + col * (CELL_W + GAP);
                int cy = panelY + GAP + row * (CELL_H + GAP) - (int)scrollOff;
                if (mx >= cx && mx <= cx + CELL_W && my >= cy && my <= cy + CELL_H) {
                    selectedVehicle = vehicles.get(i).id();
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
    public void tick() { scrollPanel.tick(); }

    @Override
    public boolean isPauseScreen() { return false; }
}
