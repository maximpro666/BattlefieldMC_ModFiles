package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.I18n;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.VehicleEntry;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.VehicleSpawnPacket;
import com.pigeostudios.pwp.client.gui.component.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class VehicleSelectionScreen extends Screen {

    private static final int LEFT_RATIO = 58;
    private static final int CARD_W = 140;
    private static final int CARD_H = 100;
    private static final int GAP = 8;

    private final List<VehicleEntry> vehicles = new ArrayList<>();
    private String selectedId;
    private float fadeAlpha;
    private long openTime;
    private int tickCount;
    private float[] hoverState;
    private BScrollPanel scrollPanel;

    public VehicleSelectionScreen() {
        super(Component.literal("Vehicle Selection"));
    }

    @Override
    protected void init() {
        super.init();
        openTime = System.currentTimeMillis();
        tickCount = 0;
        vehicles.clear();
        if (ClientTeamData.availableVehicles != null) {
            vehicles.addAll(ClientTeamData.availableVehicles);
        }
        if (!vehicles.isEmpty()) selectedId = vehicles.get(0).id();
        hoverState = new float[vehicles.size()];

        int leftW = width * LEFT_RATIO / 100;
        int colW = CARD_W + GAP;
        int cols = Math.max(1, (leftW - GAP) / colW);
        int gridW = cols * colW;
        int panelX = (leftW - gridW) / 2;
        int panelY = TopBar.TOP_H + BreadcrumbNav.BREADCRUMB_H + 6;
        int panelH = height - panelY - StatusBar.STATUS_H - 8;

        scrollPanel = new BScrollPanel(panelX, panelY, gridW, panelH);
        int rows = vehicles.isEmpty() ? 1 : (vehicles.size() + cols - 1) / cols;
        scrollPanel.setContentHeight(rows * (CARD_H + GAP) + GAP);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        tickCount++;
        scrollPanel.tick();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);
        int alpha = (int)(fadeAlpha * 0xFF);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));

        renderHeader(g, mx, my, alpha);

        int leftW = width * LEFT_RATIO / 100;
        renderLeftPanel(g, mx, my, fadeAlpha, alpha, leftW);
        g.fill(leftW, TopBar.TOP_H + BreadcrumbNav.BREADCRUMB_H, leftW + 1, height - StatusBar.STATUS_H,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xAA)));
        renderRightPanel(g, leftW, mx, my, fadeAlpha, alpha);

        renderBottomBar(g, mx, my, fadeAlpha, alpha);

        super.render(g, mx, my, pt);
    }

    private void renderHeader(GuiGraphics g, int mx, int my, int alpha) {
        int topH = TopBar.TOP_H;
        g.fill(0, 0, width, topH, AnimationHelper.withAlpha(UITheme.BG_PANEL, 0xEE));
        g.fill(0, topH - 1, width, topH, AnimationHelper.withAlpha(UITheme.BORDER, alpha));

        int rank = ClientTeamData.localPlayerRank;
        int bc = ClientTeamData.localPlayerBC;
        Team team = ClientTeamData.getLocalPlayerTeam();
        int vc = team == Team.NATO ? ClientTeamData.natoVC : team == Team.RUSSIA ? ClientTeamData.russiaVC : 0;

        g.drawString(font, I18n.get("pwp.ui.vehicle_selection.title"), 16, 8, AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        String count = I18n.get("pwp.ui.vehicle_selection.count", vehicles.size());
        g.drawString(font, count, 16 + font.width(I18n.get("pwp.ui.vehicle_selection.title")) + 12, 8,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 150)));

        String bal = I18n.get("pwp.ui.vehicle_selection.balance", rank, bc, vc);
        int bw = font.width(bal);
        g.drawString(font, bal, width - bw - 16, 8,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));

        BreadcrumbNav.render(g, width, topH, List.of(I18n.get("pwp.ui.vehicle_selection.breadcrumb")), alpha);
    }

    private void renderLeftPanel(GuiGraphics g, int mx, int my, float fade, int alpha, int leftW) {
        if (vehicles.isEmpty()) {
            String msg = I18n.get("pwp.ui.vehicle_selection.none");
            g.drawString(font, msg, (leftW - font.width(msg)) / 2, height / 2,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fade * 200)));
            return;
        }

        int colW = CARD_W + GAP;
        int cols = Math.max(1, (leftW - GAP) / colW);
        int gridW = cols * colW;
        int panelX = (leftW - gridW) / 2;
        int panelY = TopBar.TOP_H + BreadcrumbNav.BREADCRUMB_H + 6;

        scrollPanel.render(g);
        float scrollOff = scrollPanel.getScrollOffset();
        g.enableScissor(panelX, panelY, panelX + gridW, panelY + scrollPanel.getHeight());

        for (int i = 0; i < vehicles.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = panelX + col * colW;
            int cy = panelY + row * (CARD_H + GAP) - Math.round(scrollOff);

            if (cy + CARD_H < panelY || cy > panelY + scrollPanel.getHeight()) continue;

            float progress = Math.min(1f, (tickCount - i * 2f) / 10f);
            progress = Math.max(0f, AnimationHelper.easeOutCubic(progress));
            int slideX = cx - (int)((1f - progress) * (leftW * 0.15f));

            boolean hov = mx >= slideX && mx <= slideX + CARD_W && my >= cy && my <= cy + CARD_H;
            if (i < hoverState.length) {
                hoverState[i] = AnimationHelper.lerp(hoverState[i], hov ? 1f : 0f, 0.12f);
            }
            boolean sel = vehicles.get(i).id().equals(selectedId);
            drawVehicleCard(g, slideX, cy, vehicles.get(i), sel, hov, fade);
        }
        g.disableScissor();

        String hint = I18n.get("pwp.ui.vehicle_selection.hint");
        g.drawString(font, hint, (leftW - font.width(hint)) / 2, height - StatusBar.STATUS_H - 16,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 100)));
    }

    private void renderRightPanel(GuiGraphics g, int rx, int mx, int my, float fade, int alpha) {
        int x = rx + 14;
        int w = width - rx - 14;
        int y = TopBar.TOP_H + BreadcrumbNav.BREADCRUMB_H + 10;

        VehicleEntry sel = findSelected();
        if (sel == null) {
            String msg = I18n.get("pwp.ui.vehicle_selection.preview");
            g.drawString(font, msg, x + (w - font.width(msg)) / 2, height / 2,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 150)));
            return;
        }

        boolean locked = !isAvailable(sel);

        String name = I18n.localize(sel.name()).toUpperCase();
        g.drawString(font, name, x, y, AnimationHelper.withAlpha(locked ? UITheme.TEXT_MUTED : UITheme.ACCENT, alpha));
        y += 14;

        Team pt = ClientTeamData.getLocalPlayerTeam();
        int tvc = pt == Team.NATO ? ClientTeamData.natoVC : pt == Team.RUSSIA ? ClientTeamData.russiaVC : 0;
        boolean rankBlocked = ClientTeamData.localPlayerRank < sel.minRankOrdinal();
        boolean bcBlocked = ClientTeamData.localPlayerBC < sel.bcCost();
        boolean vcBlocked = tvc < sel.vcCost();

        if (locked && (rankBlocked || bcBlocked || vcBlocked)) {
            String reason;
            if (rankBlocked) reason = I18n.get("pwp.ui.vehicle_selection.rank_req", sel.minRankOrdinal());
            else if (bcBlocked) reason = I18n.get("pwp.ui.vehicle_selection.bc_req", sel.bcCost());
            else reason = I18n.get("pwp.ui.vehicle_selection.vc_req", sel.vcCost());
            g.drawString(font, reason, x, y, AnimationHelper.withAlpha(UITheme.STATUS_WARN, (int)(fade * 200)));
            y += 14;
        }

        AccentLine.draw(g, x, y, Math.min(w, 200), fade);
        y += 12;

        String[][] info = {
            {I18n.get("pwp.ui.vehicle_selection.stat_rank"), String.valueOf(sel.minRankOrdinal())},
            {I18n.get("pwp.ui.vehicle_selection.stat_bc"), String.valueOf(sel.bcCost())},
            {I18n.get("pwp.ui.vehicle_selection.stat_vc"), String.valueOf(sel.vcCost())},
            {I18n.get("pwp.ui.vehicle_selection.stat_tickets"), String.valueOf(sel.ticketCost())},
        };
        int cellW = Math.max(60, (w - 8) / 2);
        for (int row = 0; row < 2; row++) {
            int cy = y + row * 44;
            for (int col = 0; col < 2; col++) {
                int idx = row * 2 + col;
                int cx = x + col * (cellW + 8);
                g.fill(cx, cy, cx + cellW, cy + 40, AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xDD)));
                g.fill(cx, cy, cx + cellW, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
                g.fill(cx, cy + 39, cx + cellW, cy + 40, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
                g.fill(cx, cy, cx + 1, cy + 40, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
                g.fill(cx + cellW - 1, cy, cx + cellW, cy + 40, AnimationHelper.withAlpha(UITheme.BORDER, alpha));

                int labelColor = idx == 0 ? (rankBlocked ? UITheme.STATUS_DANGER : UITheme.STATUS_OK)
                    : idx == 1 ? (bcBlocked ? UITheme.STATUS_DANGER : UITheme.STATUS_OK)
                    : idx == 2 ? (vcBlocked ? UITheme.STATUS_DANGER : UITheme.STATUS_OK)
                    : UITheme.TEXT_MUTED;
                g.drawString(font, info[idx][0], cx + 8, cy + 4,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
                String val = info[idx][1];
                g.drawString(font, val, cx + 8, cy + 20,
                    AnimationHelper.withAlpha(labelColor, alpha));
            }
        }

        int btnY = height - StatusBar.STATUS_H - 38;
        int btnW = w - 8;
        boolean canSpawn = isAvailable(sel);
        boolean btnHov = mx >= x && mx <= x + btnW && my >= btnY && my <= btnY + 28;
        int btnBg = canSpawn
            ? AnimationHelper.blendColors(UITheme.ACCENT, 0xFFFF8C0A, btnHov ? 1f : 0f)
            : 0xFF333333;
        g.fill(x, btnY, x + btnW, btnY + 28, AnimationHelper.withAlpha(btnBg, alpha));
        g.fill(x, btnY, x + 2, btnY + 28, AnimationHelper.withAlpha(0x33000000, alpha));
        String label = canSpawn ? I18n.get("pwp.ui.vehicle_selection.spawn", I18n.localize(sel.name()).toUpperCase()) : I18n.get("pwp.ui.vehicle_selection.locked_btn");
        g.drawString(font, label, x + btnW / 2 - font.width(label) / 2, btnY + 10,
            AnimationHelper.withAlpha(canSpawn ? 0xFFFFFFFF : UITheme.TEXT_MUTED, alpha));
    }

    private void renderBottomBar(GuiGraphics g, int mx, int my, float fade, int alpha) {
        int y0 = height - StatusBar.STATUS_H;
        g.fill(0, y0, width, height, AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fade * 0xEE)));
        g.fill(0, y0, width, y0 + 1, AnimationHelper.withAlpha(UITheme.BORDER, alpha));

        String cnt = I18n.get("pwp.ui.vehicle_selection.count", vehicles.size());
        g.drawString(font, cnt, 10, y0 + (StatusBar.STATUS_H - font.lineHeight) / 2,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
    }

    private void drawVehicleCard(GuiGraphics g, int x, int y, VehicleEntry v,
                                  boolean selected, boolean hovered, float fade) {
        boolean locked = !isAvailable(v);
        int alpha = (int)(fade * 0xFF);
        int bg;
        if (locked) {
            bg = AnimationHelper.withAlpha(0xFF222222, (int)(fade * 0xAA));
        } else if (selected) {
            bg = AnimationHelper.blendColors(UITheme.BG_PANEL, UITheme.ACCENT, 0.18f);
        } else if (hovered) {
            bg = AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xDD));
        } else {
            bg = AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fade * 0xDD));
        }
        int border = selected
            ? AnimationHelper.withAlpha(UITheme.ACCENT, alpha)
            : AnimationHelper.withAlpha(locked ? 0xFF553333 : UITheme.BORDER, (int)(fade * (0x44 + (hovered ? 0xBB : 0x44))));

        g.fill(x, y, x + CARD_W, y + CARD_H, bg);
        g.fill(x, y, x + CARD_W, y + 1, border);
        g.fill(x, y + CARD_H - 1, x + CARD_W, y + CARD_H, border);
        g.fill(x, y, x + 1, y + CARD_H, border);
        g.fill(x + CARD_W - 1, y, x + CARD_W, y + CARD_H, border);

        if (selected && !locked) {
            g.fill(x, y, x + 3, y + CARD_H, AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
        }

        String name = I18n.localize(v.name()).toUpperCase();
        g.drawString(font, name, x + 8, y + 6,
            AnimationHelper.withAlpha(locked ? UITheme.TEXT_MUTED : UITheme.TEXT_PRIMARY, alpha));

        String req = I18n.get("pwp.ui.vehicle_selection.card_rank", v.minRankOrdinal()) + "  " + I18n.get("pwp.ui.vehicle_selection.card_bc", v.bcCost()) + "  " + I18n.get("pwp.ui.vehicle_selection.card_vc", v.vcCost());
        g.drawString(font, req, x + 8, y + 20,
            AnimationHelper.withAlpha(locked ? UITheme.STATUS_DANGER : UITheme.TEXT_MUTED, (int)(fade * 160)));

        String status;
        int statusColor;
        if (locked) {
            Team pt = ClientTeamData.getLocalPlayerTeam();
            int tvc = pt == Team.NATO ? ClientTeamData.natoVC : pt == Team.RUSSIA ? ClientTeamData.russiaVC : 0;
            boolean rBlock = ClientTeamData.localPlayerRank < v.minRankOrdinal();
            boolean bcBlock = ClientTeamData.localPlayerBC < v.bcCost();
            boolean vcBlock = tvc < v.vcCost();
            if (rBlock) { status = I18n.get("pwp.ui.vehicle_selection.card_rank", v.minRankOrdinal()); statusColor = UITheme.STATUS_WARN; }
            else if (bcBlock) { status = I18n.get("pwp.ui.vehicle_selection.card_bc", v.bcCost()); statusColor = UITheme.STATUS_WARN; }
            else if (vcBlock) { status = I18n.get("pwp.ui.vehicle_selection.card_vc", v.vcCost()); statusColor = UITheme.STATUS_WARN; }
            else { status = I18n.get("pwp.ui.vehicle_selection.cooldown"); statusColor = UITheme.TEXT_MUTED; }
        } else {
            status = I18n.get("pwp.ui.vehicle_selection.available");
            statusColor = UITheme.STATUS_OK;
        }
        g.drawString(font, status, x + 8, y + 34,
            AnimationHelper.withAlpha(statusColor, (int)(fade * 180)));

        String ticket = I18n.get("pwp.ui.vehicle_selection.ticket", v.ticketCost());
        g.drawString(font, ticket, x + CARD_W - font.width(ticket) - 8, y + CARD_H - 12,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 140)));
    }

    private VehicleEntry findSelected() {
        for (VehicleEntry v : vehicles) {
            if (v.id().equals(selectedId)) return v;
        }
        return null;
    }

    private boolean isAvailable(VehicleEntry v) {
        Team pt = ClientTeamData.getLocalPlayerTeam();
        int tvc = pt == Team.NATO ? ClientTeamData.natoVC : pt == Team.RUSSIA ? ClientTeamData.russiaVC : 0;
        boolean rankOk = ClientTeamData.localPlayerRank >= v.minRankOrdinal();
        boolean bcOk = ClientTeamData.localPlayerBC >= v.bcCost();
        boolean vcOk = tvc >= v.vcCost();
        return rankOk && bcOk && vcOk;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        int rx = width * LEFT_RATIO / 100;
        if (mx >= rx) {
            int x = rx + 14;
            int w = width - rx - 14;
            int btnY = height - StatusBar.STATUS_H - 38;
            int btnW = w - 8;
            if (mx >= x && mx <= x + btnW && my >= btnY && my <= btnY + 28) {
                VehicleEntry sel = findSelected();
                if (sel != null && isAvailable(sel)) {
                    PacketHandler.CHANNEL.sendToServer(new VehicleSpawnPacket(sel.id()));
                    onClose();
                }
                return true;
            }
            return super.mouseClicked(mx, my, btn);
        }

        if (vehicles.isEmpty()) return super.mouseClicked(mx, my, btn);

        int leftW = width * LEFT_RATIO / 100;
        int colW = CARD_W + GAP;
        int cols = Math.max(1, (leftW - GAP) / colW);
        int gridW = cols * colW;
        int panelX = (leftW - gridW) / 2;
        int panelY = TopBar.TOP_H + BreadcrumbNav.BREADCRUMB_H + 6;
        float scrollOff = scrollPanel.getScrollOffset();

        for (int i = 0; i < vehicles.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = panelX + col * colW;
            int cy = panelY + row * (CARD_H + GAP) - Math.round(scrollOff);
            if (mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H) {
                selectedId = vehicles.get(i).id();
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < width * LEFT_RATIO / 100) {
            scrollPanel.onScroll(mx, my, delta);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        SpawnScreenHelper.reopen();
    }
}
