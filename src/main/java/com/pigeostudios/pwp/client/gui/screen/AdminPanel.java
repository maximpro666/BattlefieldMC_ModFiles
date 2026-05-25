package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.BButton;
import com.pigeostudios.pwp.client.gui.component.KitEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;



public class AdminPanel extends Screen {

    private enum Tab { MATCH, MAPS, KITS, TEAMS, CONFIG, PUNISHMENTS, REPORTS, TICKETS }

    private static final int NAV_COLLAPSED = 50;
    private static final int NAV_EXPANDED  = 160;
    private static final int NAV_ITEM_H    = 44;
    private static final int HEADER_H      = 40;
    private static final int QUICK_BAR_H   = 36;

    private static final String[] TAB_ICONS  = {"\u26A1", "\uD83D\uDDFA\uFE0F", "\u2694\uFE0F", "\uD83D\uDC65", "\u2699\uFE0F", "\uD83D\uDD28", "\uD83D\uDCE2", "\uD83C\uDFAB"};
    private static final String[] TAB_LABELS = {"Match", "Maps", "Kits", "Teams", "Config", "Punishments", "Reports", "Tickets"};

    private Tab currentTab = Tab.MATCH;
    private float fadeAlpha = 0f;
    private long openTime;
    private float navHover = 0f;
    private int activeNavIdx = 0;

    private int ticketScroll = 0;

    private final KitEditor kitEditor = new KitEditor();

    public AdminPanel() {
        super(Component.literal("Admin Panel"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int navW = NAV_COLLAPSED;
        int qy = HEADER_H;
        int btnH = 24;
        int btnY = qy + (QUICK_BAR_H - btnH) / 2;
        int x = navW + 8;

        addRenderableWidget(new BButton(x, btnY, 100, btnH,
            Component.literal("\u25B6 START"), btn -> sendAdminCommand("start"),
            BButton.Variant.GHOST));
        addRenderableWidget(new BButton(x + 104, btnY, 100, btnH,
            Component.literal("\u21BB RESTART"), btn -> sendAdminCommand("restart"),
            BButton.Variant.GHOST));
        addRenderableWidget(new BButton(x + 208, btnY, 90, btnH,
            Component.literal("\u25A0 STOP"), btn -> sendAdminCommand("stop"),
            BButton.Variant.GHOST));
        addRenderableWidget(new BButton(x + 302, btnY, 120, btnH,
            Component.literal("\uD83D\uDDF3 VOTE"), btn -> sendAdminCommand("forcevote"),
            BButton.Variant.GHOST));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));

        boolean hoveringNav = mx < NAV_EXPANDED + 10;
        navHover = AnimationHelper.lerp(navHover, hoveringNav ? 1f : 0f, 0.1f);
        int navW = (int)(NAV_COLLAPSED + (NAV_EXPANDED - NAV_COLLAPSED) * navHover);

        drawNavBar(g, mx, my, navW);
        drawHeader(g, navW);
        drawQuickBarBg(g, navW);

        int contentX = navW + 10;
        int contentY = HEADER_H + QUICK_BAR_H + 10;
        int contentW = width - navW - 20;
        int contentH = height - contentY - 10;

        switch (currentTab) {
            case MATCH -> renderMatchTab(g, contentX, contentY, contentW, contentH);
            case MAPS  -> renderMapsTab(g, contentX, contentY, contentW, contentH);
            case KITS  -> renderKitsTab(g, contentX, contentY, contentW, contentH, mx, my, pt);
            case TEAMS -> renderTeamsTab(g, contentX, contentY, contentW, contentH);
            case CONFIG -> renderConfigTab(g, contentX, contentY, contentW, contentH);
            case PUNISHMENTS -> renderPunishmentsTab(g, contentX, contentY, contentW, contentH);
            case REPORTS -> renderReportsTab(g, contentX, contentY, contentW, contentH);
            case TICKETS -> renderTicketsTab(g, contentX, contentY, contentW, contentH, mx, my);
        }

        super.render(g, mx, my, pt);
    }

    private void drawNavBar(GuiGraphics g, int mx, int my, int navW) {
        g.fill(0, 0, navW, height, AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(navW - 1, 0, navW, height, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 180)));

        int startY = HEADER_H + 10;

        for (int i = 0; i < TAB_ICONS.length; i++) {
            int iy = startY + i * (NAV_ITEM_H + 4);
            boolean active = i == currentTab.ordinal();
            boolean hov = mx >= 0 && mx <= navW && my >= iy && my <= iy + NAV_ITEM_H;

            if (active) {
                g.fill(0, iy, 3, iy + NAV_ITEM_H, AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
            }

            int bg = active
                ? AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0x66))
                : (hov ? AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0x33)) : 0);
            if (bg != 0) {
                g.fill(4, iy, navW - 2, iy + NAV_ITEM_H, bg);
            }

            g.drawCenteredString(font, TAB_ICONS[i], navW / 2, iy + (NAV_ITEM_H - font.lineHeight) / 2,
                active ? AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255))
                       : AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));

            if (navHover > 0.1f) {
                int labelAlpha = (int)(fadeAlpha * 255 * navHover);
                g.drawString(font, TAB_LABELS[i], 52, iy + (NAV_ITEM_H - font.lineHeight) / 2,
                    AnimationHelper.withAlpha(active ? UITheme.ACCENT : UITheme.TEXT_PRIMARY, labelAlpha));
            }
        }
    }

    private void drawHeader(GuiGraphics g, int navW) {
        g.fill(navW, 0, width, HEADER_H, AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fadeAlpha * 0xDD)));
        g.fill(navW, HEADER_H - 1, width, HEADER_H, AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 200)));

        String title = "ADMIN PANEL";
        int tw = font.width(title);
        g.drawString(font, title, navW + 12, (HEADER_H - font.lineHeight) / 2,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
    }

    private void drawQuickBarBg(GuiGraphics g, int navW) {
        int qy = HEADER_H;
        g.fill(navW, qy, width, qy + QUICK_BAR_H, AnimationHelper.withAlpha(UITheme.BG_BLACK, (int)(fadeAlpha * 0x44)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int navW = (int)(NAV_COLLAPSED + (NAV_EXPANDED - NAV_COLLAPSED) * navHover);
        int startY = HEADER_H + 10;
        for (int i = 0; i < TAB_ICONS.length; i++) {
            int iy = startY + i * (NAV_ITEM_H + 4);
            if (mx >= 0 && mx <= navW && my >= iy && my <= iy + NAV_ITEM_H) {
                currentTab = Tab.values()[i];
                activeNavIdx = i;
                if (currentTab == Tab.KITS) {
                    kitEditor.activate();
                }
                return true;
            }
        }
        if (currentTab == Tab.KITS) {
            int navW2 = (int)(NAV_COLLAPSED + (NAV_EXPANDED - NAV_COLLAPSED) * navHover);
            int contentX2 = navW2 + 10;
            int contentY2 = HEADER_H + QUICK_BAR_H + 10;
            int contentW2 = width - navW2 - 20;
            int contentH2 = height - contentY2 - 10;
            if (kitEditor.handleClick(mx, my, btn, navW2, contentX2, contentY2, contentW2, contentH2)) return true;
        }
        if (currentTab == Tab.TICKETS) {
            var tickets = ClientTeamData.ticketList;
            int tx = (int)(NAV_COLLAPSED + (NAV_EXPANDED - NAV_COLLAPSED) * navHover) + 4;
            int ty = HEADER_H + QUICK_BAR_H + 10;
            int listX = tx + 4;
            int listY = ty + 36;
            int rowH = 20;
            int vis = Math.max(1, (height - listY - 24) / rowH);
            for (int i = ticketScroll; i < Math.min(ticketScroll + vis, tickets.size()); i++) {
                int ry = listY + (i - ticketScroll) * rowH;
                if (mx >= listX - 4 && mx <= listX + width - tx - 20 && my >= ry && my <= ry + rowH) {
                    Minecraft.getInstance().setScreen(new TicketScreen(tickets.get(i).id));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (currentTab == Tab.KITS) {
            if (kitEditor.keyPressed(key, scan, mods)) return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        if (currentTab == Tab.KITS) {
            if (kitEditor.charTyped(ch, mods)) return true;
        }
        return super.charTyped(ch, mods);
    }

    private void sendAdminCommand(String action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        switch (action) {
            case "start" -> mc.player.connection.sendCommand("game start");
            case "stop" -> mc.player.connection.sendCommand("game end");
            case "restart" -> mc.player.connection.sendCommand("game countdown 30");
            case "forcevote" -> mc.player.connection.sendCommand("game start");
        }
    }

    private void renderMatchTab(GuiGraphics g, int x, int y, int w, int h) {
        int cardH = 100;
        g.fill(x, y, x + w, y + cardH, AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fadeAlpha * 0xDD)));
        g.fill(x, y, x + w, y + 3, AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 200)));

        int ly = y + 12;
        drawLValue(g, x + 12, ly, "Status", phaseName(ClientTeamData.getGamePhase()), UITheme.STATUS_OK);
        ly += 18;
        drawLValue(g, x + 12, ly, "Map", ClientTeamData.getCurrentMapName(), UITheme.TEXT_PRIMARY);
        ly += 18;
        drawLValue(g, x + 12, ly, "Timer", formatTime(ClientTeamData.matchTimeSeconds), UITheme.TEXT_PRIMARY);

        int tx = x + w / 2;
        g.drawString(font, "NATO: " + ClientTeamData.getNatoTickets(), tx, y + 12,
            AnimationHelper.withAlpha(UITheme.TEAM_NATO, (int)(fadeAlpha * 255)));
        g.drawString(font, "RUSSIA: " + ClientTeamData.getRussiaTickets(), tx, y + 30,
            AnimationHelper.withAlpha(UITheme.TEAM_RUSSIA, (int)(fadeAlpha * 255)));
        g.drawString(font, "Players: " + (ClientTeamData.playerDataMap != null ? ClientTeamData.playerDataMap.size() : 0),
            tx, y + 48, AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
    }

    private void renderMapsTab(GuiGraphics g, int x, int y, int w, int h) {
        g.drawString(font, "MAP MANAGEMENT", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));

        cy += 10;
        drawLValue(g, x, cy, "Current Map", ClientTeamData.getCurrentMapName(), UITheme.TEXT_PRIMARY);
        cy += 18;
        drawLValue(g, x, cy, "Phase", phaseName(ClientTeamData.getGamePhase()), UITheme.TEXT_SECONDARY);

        cy += 30;
        g.drawString(font, "Vote Settings", x, cy,
            AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fadeAlpha * 200)));
        cy += 18;
        drawLValue(g, x, cy, "Vote Duration", "30s", UITheme.TEXT_SECONDARY);
        cy += 18;
        drawLValue(g, x, cy, "Maps Per Vote", "4", UITheme.TEXT_SECONDARY);

        cy += 30;
        g.drawString(font, "Use /pwp admin addmap <name> <path>", x, cy,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 180)));
    }

    private void renderKitsTab(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float pt) {
        g.drawString(font, "KIT EDITOR", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));

        kitEditor.render(g, x, y, w, h, mx, my, fadeAlpha, font);
    }

    private void renderTeamsTab(GuiGraphics g, int x, int y, int w, int h) {
        g.drawString(font, "TEAM SETTINGS", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));

        cy += 10;
        int natoCount = 0, rusCount = 0;
        if (ClientTeamData.playerTeamMap != null) {
            for (var entry : ClientTeamData.playerTeamMap.entrySet()) {
                if (entry.getValue() == 0) natoCount++;
                else if (entry.getValue() == 1) rusCount++;
            }
        }

        g.drawString(font, "NATO: " + natoCount + " players | Tickets: " + ClientTeamData.getNatoTickets(),
            x + 8, cy, AnimationHelper.withAlpha(UITheme.TEAM_NATO, (int)(fadeAlpha * 255)));
        cy += 20;
        g.drawString(font, "RUSSIA: " + rusCount + " players | Tickets: " + ClientTeamData.getRussiaTickets(),
            x + 8, cy, AnimationHelper.withAlpha(UITheme.TEAM_RUSSIA, (int)(fadeAlpha * 255)));
        cy += 20;
        g.drawString(font, "Total: " + (natoCount + rusCount) + " players",
            x + 8, cy, AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));

        if (ClientTeamData.playerDataMap != null) {
            cy += 30;
            g.drawString(font, "Player List:", x, cy,
                AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fadeAlpha * 200)));
            cy += 16;
            for (var entry : ClientTeamData.playerDataMap.entrySet()) {
                if (cy > y + h - 20) break;
                var ple = entry.getValue();
                String teamTag = "[" + (ple.teamOrdinal() == 0 ? "NATO" : ple.teamOrdinal() == 1 ? "RUS" : "SPECT") + "]";
                String line = teamTag + " " + (ple.callsign() != null ? ple.callsign() : "Unknown");
                g.drawString(font, line, x + 8, cy,
                    AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
                cy += 14;
            }
        }
    }

    private void renderConfigTab(GuiGraphics g, int x, int y, int w, int h) {
        g.drawString(font, "SERVER CONFIG", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));

        cy += 10;
        drawLValue(g, x + 8, cy, "Language", ClientTeamData.language, UITheme.TEXT_PRIMARY);
        cy += 18;
        drawLValue(g, x + 8, cy, "Max Tickets", String.valueOf(ClientTeamData.maxTickets), UITheme.TEXT_PRIMARY);
        cy += 18;
        drawLValue(g, x + 8, cy, "GUI Scale", String.format("%.1f", ClientTeamData.guiScale), UITheme.TEXT_PRIMARY);
        cy += 18;
        drawLValue(g, x + 8, cy, "GUI Opacity", String.format("%.1f", ClientTeamData.guiOpacity), UITheme.TEXT_PRIMARY);
        cy += 18;
        drawLValue(g, x + 8, cy, "GUI Volume", String.format("%.1f", ClientTeamData.guiVolume), UITheme.TEXT_PRIMARY);

        cy += 30;
        g.drawString(font, "Config editing: /pwp admin config <key> <value>",
            x + 8, cy, AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 180)));
    }

    private void renderPunishmentsTab(GuiGraphics g, int x, int y, int w, int h) {
        g.drawString(font, "PUNISHMENTS", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));
        cy += 16;
        g.drawString(font, "Commands:", x + 8, cy,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        cy += 18;
        String[] cmds = {
            "§6/warnchat <player> <reason>",
            "§6/warngame <player> <reason>",
            "§6/warnvoice <player> <reason>",
            "§6/warngeneral <player> <reason>",
            "§6/kick <player> <reason>",
            "§6/mute <player> <hours> <reason>",
            "§6/voicemute <player> <hours> <reason>",
            "§6/tempban <player> <hours> <reason>",
            "§6/ban <player> <reason>",
            "§6/unban <uuid>",
            "§6/unmute <player>",
            "§6/punishments <player>",
            "§6/staff add/remove/list"
        };
        for (String cmd : cmds) {
            if (cy > y + h - 10) break;
            g.drawString(font, cmd, x + 12, cy,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
            cy += 16;
        }
    }

    private void renderTicketsTab(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        g.drawString(font, "TICKETS", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        var tickets = ClientTeamData.ticketList;
        int headerY = y + 18;
        g.fill(x, headerY, x + w, headerY + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));

        int[] cw = {32, 100, 80, 70, 80};
        String[] headers = {"ID", "Reporter", "Target", "Type", "Status"};
        int hx = x + 6;
        for (int i = 0; i < cw.length; i++) {
            g.drawString(font, "\u00A78" + headers[i], hx, headerY + 6,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 180)));
            hx += cw[i];
        }

        // scrollable list area
        int listX = x + 4;
        int listY = headerY + 18;
        int listW = w - 8;
        int listH = h - (listY - y) - 14;

        g.fill(listX - 4, listY, listX + listW + 4, listY + listH,
            AnimationHelper.withAlpha(UITheme.BG_BLACK, (int)(fadeAlpha * 0x33)));

        int rowH = 20;
        int vis = Math.max(1, listH / rowH);
        int maxScroll = Math.max(0, tickets.size() - vis);
        if (ticketScroll > maxScroll) ticketScroll = maxScroll;

        for (int i = ticketScroll; i < Math.min(ticketScroll + vis, tickets.size()); i++) {
            var t = tickets.get(i);
            int ry = listY + (i - ticketScroll) * rowH;
            boolean hov = mx >= listX && mx <= listX + listW && my >= ry && my <= ry + rowH;

            if (hov) {
                g.fill(listX, ry, listX + listW, ry + rowH,
                    AnimationHelper.withAlpha(UITheme.ACCENT_GHOST, (int)(fadeAlpha * 255)));
            }
            g.fill(listX, ry + rowH - 1, listX + listW, ry + rowH,
                AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 40)));

            int cx2 = listX + 2;
            g.drawString(font, "\u00A7f#" + t.id, cx2, ry + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
            cx2 += cw[0];

            String rep = t.reporterName;
            if (rep.startsWith("UUID:")) rep = rep.substring(5);
            if (rep.length() > 10) rep = rep.substring(0, 8) + "..";
            g.drawString(font, "\u00A7e" + rep, cx2, ry + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
            cx2 += cw[1];

            String target = t.targetName.length() > 10 ? t.targetName.substring(0, 8) + ".." : t.targetName;
            g.drawString(font, "\u00A7f" + target, cx2, ry + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
            cx2 += cw[2];

            g.drawString(font, "\u00A77" + t.type, cx2, ry + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
            cx2 += cw[3];

            // assigned badge
            if (t.assignedTo != null && !t.assignedTo.isEmpty()) {
                String a = t.assignedTo.length() > 8 ? t.assignedTo.substring(0, 6) + ".." : t.assignedTo;
                g.drawString(font, "\u00A76\u2691" + a, cx2, ry + 4,
                    AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 220)));
            } else {
                g.drawString(font, "\u00A78\u2713 open", cx2, ry + 4,
                    AnimationHelper.withAlpha(UITheme.STATUS_OK, (int)(fadeAlpha * 180)));
            }
        }

        if (tickets.isEmpty()) {
            g.drawCenteredString(font, "\u00A77No tickets", listX + listW / 2, listY + listH / 2 - 4,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 200)));
        }

        // scrollbar hint
        if (maxScroll > 0) {
            float pct = (float) ticketScroll / maxScroll;
            int barH = Math.max(8, listH * vis / Math.max(1, tickets.size()));
            int barY = listY + (int)((listH - barH) * pct);
            g.fill(listX + listW + 2, barY, listX + listW + 4, barY + barH,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 150)));
        }
    }

    private void renderReportsTab(GuiGraphics g, int x, int y, int w, int h) {
        g.drawString(font, "REPORTS", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));
        cy += 16;
        g.drawString(font, "Commands:", x + 8, cy,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        cy += 18;
        String[] cmds = {
            "§6/report <player> <type> <desc>",
            "§6/reports",
            "§6/report view <id>",
            "§6/report claim <id>",
            "§6/report dismiss <id> <reason>",
            "§6/report punish <id> <punishment_id>",
            "",
            "§7Report types:",
            " TEAM_KILL, CHEATING, TOXIC_CHAT, VOICE_ABUSE,",
            " GRIEFING, EXPLOITING, AFK, INAPPROPRIATE_NAME,",
            " TEAM_STACKING, COALITION, OTHER"
        };
        for (String cmd : cmds) {
            if (cy > y + h - 10) break;
            g.drawString(font, cmd, x + 12, cy,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
            cy += 16;
        }
    }

    private void drawLValue(GuiGraphics g, int x, int y, String label, String value, int valueColor) {
        g.drawString(font, label + ":", x, y,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        int lw = font.width(label + ":  ");
        g.drawString(font, value, x + lw, y,
            AnimationHelper.withAlpha(valueColor, (int)(fadeAlpha * 255)));
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private String phaseName(int phase) {
        return switch (phase) {
            case 0 -> "LOBBY";
            case 1 -> "VOTING";
            case 2 -> "PLAYING";
            case 3 -> "ENDED";
            default -> "UNKNOWN";
        };
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (currentTab == Tab.TICKETS) {
            int listH = height - HEADER_H - QUICK_BAR_H - 10 - 36 - 14;
            int rowH = 20;
            int vis = Math.max(1, listH / rowH);
            int maxScroll = Math.max(0, ClientTeamData.ticketList.size() - vis);
            ticketScroll = (int) Math.max(0, Math.min(maxScroll, ticketScroll - delta * 3));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
