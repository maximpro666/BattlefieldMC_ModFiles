package com.yourmod.teamsystem.client.gui.scoreboard;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.RenderHelper;
import com.yourmod.teamsystem.client.gui.scoreboard.data.PlayerScoreboardData;
import com.yourmod.teamsystem.client.gui.scoreboard.data.RankDefinition;
import com.yourmod.teamsystem.client.gui.scoreboard.data.ScoreboardDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;

public class ScoreboardRenderer {

    private static final int ROW_H = 15;
    private static final int SECTION_H = 17;
    private static final int PADDING = 6;
    private static final int ICON_SIZE = 11;
    private static final int TEAM_HEADER_H = 20;
    private static final int COL_HEADER_H = 14;
    private static final int COLUMNS_GAP = 24;
    private static final int PANEL_TOP = 18;

    private static final int COL_RANK = 32;
    private static final int COL_NAME = 90;
    private static final int COL_KILLS = 30;
    private static final int COL_DEATHS = 30;
    private static final int COL_KD = 36;
    private static final int COL_PING = 34;
    private static final int COL_GAP = 4;

    private boolean visible = false;
    private float prevAlpha = 0f;
    private float targetAlpha = 0f;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public void setVisible(boolean v) {
        this.visible = v;
        this.targetAlpha = v ? 1f : 0f;
        if (v) scrollOffset = 0;
    }

    public boolean isVisible() { return visible; }
    public float getAlpha() { return prevAlpha; }

    public void scrollBy(int delta) {
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + delta));
    }

    public void tick() {
        prevAlpha = AnimationHelper.lerp(prevAlpha, targetAlpha, 0.18f);
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight, float partialTick) {
        if (prevAlpha < 0.02f) return;
        int alpha = (int) (prevAlpha * 255);

        Font font = Minecraft.getInstance().font;
        List<PlayerScoreboardData> players = ScoreboardDataProvider.buildPlayerList();
        if (players.isEmpty()) return;

        List<PlayerScoreboardData> nato = new ArrayList<>();
        List<PlayerScoreboardData> russia = new ArrayList<>();
        for (PlayerScoreboardData p : players) {
            if (p.teamOrdinal == 0) nato.add(p);
            else if (p.teamOrdinal == 1) russia.add(p);
        }

        sortBySquad(nato);
        sortBySquad(russia);

        Map<String, List<PlayerScoreboardData>> natoGrouped = groupBySquad(nato);
        Map<String, List<PlayerScoreboardData>> russiaGrouped = groupBySquad(russia);

        int minColW = COL_RANK + COL_NAME + COL_KILLS + COL_DEATHS + COL_KD + COL_PING + COL_GAP * 5 + PADDING * 2;
        int availableW = screenWidth - PADDING * 2 - COLUMNS_GAP;
        int colW = Math.max(minColW, Math.min(availableW / 2, 460));
        if (colW * 2 + COLUMNS_GAP > screenWidth) {
            colW = (screenWidth - COLUMNS_GAP) / 2;
        }

        int totalW = colW * 2 + COLUMNS_GAP;
        int panelX = screenWidth / 2 - totalW / 2;
        int panelY = PANEL_TOP;

        int natoRows = countRows(natoGrouped);
        int russiaRows = countRows(russiaGrouped);
        int maxRows = Math.max(natoRows, russiaRows);
        int contentH = maxRows * ROW_H + PADDING * 2;
        int maxH = screenHeight - panelY - PADDING - TEAM_HEADER_H - COL_HEADER_H;
        if (maxH <= 0) return;
        int panelH = Math.min(contentH, maxH);

        maxScroll = Math.max(0, contentH - maxH);

        RenderHelper.dropShadow(g, panelX, panelY, totalW, panelH + TEAM_HEADER_H + COL_HEADER_H, 5, (int) (alpha * 0.7f));
        RenderHelper.roundedRect(g, panelX, panelY, totalW, panelH + TEAM_HEADER_H + COL_HEADER_H, 4,
            AnimationHelper.withAlpha(UITheme.BG_SCREEN, alpha));

        drawTeamColumn(g, font, panelX, panelY, colW, panelH, natoGrouped, "NATO", nato.size(), alpha, UITheme.TEAM_NATO);
        drawTeamColumn(g, font, panelX + colW + COLUMNS_GAP, panelY, colW, panelH, russiaGrouped, "RUSSIA", russia.size(), alpha, UITheme.TEAM_RUSSIA);

        if (maxScroll > 0) {
            String scrollHint = (scrollOffset > 0 ? "\u25B2 " : "") + "\u25BC";
            int hintW = font.width(scrollHint);
            g.drawString(font, scrollHint, screenWidth / 2 - hintW / 2,
                panelY + panelH + TEAM_HEADER_H + COL_HEADER_H + 3,
                AnimationHelper.withAlpha(0xFF808080, alpha));
        }
    }

    private void sortBySquad(List<PlayerScoreboardData> list) {
        list.sort((a, b) -> {
            int sqCmp = a.squad.compareTo(b.squad);
            if (sqCmp != 0) return sqCmp;
            int rCmp = Integer.compare(b.rankId, a.rankId);
            if (rCmp != 0) return rCmp;
            return Integer.compare(b.kills, a.kills);
        });
    }

    private Map<String, List<PlayerScoreboardData>> groupBySquad(List<PlayerScoreboardData> list) {
        Map<String, List<PlayerScoreboardData>> grouped = new LinkedHashMap<>();
        for (PlayerScoreboardData p : list) {
            String squad = p.squad != null && !p.squad.isEmpty() ? p.squad : "\u2014";
            grouped.computeIfAbsent(squad, k -> new ArrayList<>()).add(p);
        }
        return grouped;
    }

    private int countRows(Map<String, List<PlayerScoreboardData>> grouped) {
        int rows = 0;
        for (Map.Entry<String, List<PlayerScoreboardData>> section : grouped.entrySet()) {
            rows++;
            rows += section.getValue().size();
        }
        return rows;
    }

    private void drawTeamColumn(GuiGraphics g, Font font, int x, int panelY, int colW, int panelH,
                                 Map<String, List<PlayerScoreboardData>> grouped, String teamName,
                                 int playerCount, int alpha, int teamColor) {
        int headerY = panelY;

        int headerBg = teamColor & 0x00FFFFFF | 0x44000000;
        RenderHelper.roundedRect(g, x, headerY, colW, TEAM_HEADER_H, 3,
            AnimationHelper.withAlpha(headerBg, Math.min(255, alpha)));

        g.fill(x, headerY + TEAM_HEADER_H - 2, x + colW, headerY + TEAM_HEADER_H,
            AnimationHelper.withAlpha(teamColor, alpha / 2));

        String headerText = teamName;
        int hw = font.width(headerText);
        g.drawString(font, headerText, x + PADDING, headerY + 5,
            AnimationHelper.withAlpha(teamColor, alpha));

        String countStr = "(" + playerCount + ")";
        g.drawString(font, countStr, x + PADDING + hw + 4, headerY + 5,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int) (alpha * 0.8f)));

        int colHeaderY = headerY + TEAM_HEADER_H;
        g.fill(x, colHeaderY, x + colW, colHeaderY + COL_HEADER_H,
            AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int) (alpha * 0.85f)));

        drawColumnHeaders(g, font, x, colHeaderY, colW, alpha);

        g.fill(x, colHeaderY + COL_HEADER_H - 1, x + colW, colHeaderY + COL_HEADER_H,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha / 3));

        if (panelH > 0) {
            g.enableScissor(x, colHeaderY + COL_HEADER_H, x + colW, colHeaderY + COL_HEADER_H + panelH);
        }

        int cy = colHeaderY + COL_HEADER_H - scrollOffset;
        int rowX = x + PADDING;

        for (Map.Entry<String, List<PlayerScoreboardData>> section : grouped.entrySet()) {
            if (cy + SECTION_H > colHeaderY + COL_HEADER_H - ROW_H &&
                cy < colHeaderY + COL_HEADER_H + panelH) {
                String sqLabel = section.getKey();
                g.drawString(font, sqLabel, rowX, cy + 3,
                    AnimationHelper.withAlpha(UITheme.SQUAD_LABEL, alpha));
                g.fill(x + PADDING, cy + SECTION_H - 1, x + colW - PADDING, cy + SECTION_H,
                    AnimationHelper.withAlpha(UITheme.SQUAD_LINE, alpha));
            }
            cy += SECTION_H;

            for (PlayerScoreboardData pd : section.getValue()) {
                if (cy + ROW_H > colHeaderY + COL_HEADER_H - ROW_H &&
                    cy < colHeaderY + COL_HEADER_H + panelH) {
                    drawPlayerRow(g, font, x, cy, colW, pd, alpha);
                }
                cy += ROW_H;
            }
        }

        if (panelH > 0) {
            g.disableScissor();
        }
    }

    private void drawColumnHeaders(GuiGraphics g, Font font, int x, int y, int colW, int alpha) {
        int cx = x + PADDING + ICON_SIZE + 4;
        int ty = y + 4;
        int hdrColor = AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int) (alpha * 0.8f));

        g.drawString(font, "Rank", cx, ty, hdrColor);
        cx += COL_RANK + COL_GAP;
        g.drawString(font, "Name", cx, ty, hdrColor);
        cx += COL_NAME + COL_GAP;
        g.drawString(font, "K", cx, ty, hdrColor);
        cx += COL_KILLS + COL_GAP;
        g.drawString(font, "D", cx, ty, hdrColor);
        cx += COL_DEATHS + COL_GAP;
        g.drawString(font, "K/D", cx, ty, hdrColor);
        cx += COL_KD + COL_GAP;
        g.drawString(font, "Ping", cx, ty, hdrColor);
    }

    private void drawPlayerRow(GuiGraphics g, Font font, int x, int rowY, int colW,
                                PlayerScoreboardData pd, int alpha) {
        int rowX = x + PADDING;

        if (pd.isSelf) {
            g.fill(x, rowY, x + colW, rowY + ROW_H,
                AnimationHelper.withAlpha(UITheme.SELF_BG, alpha));
            g.fill(x, rowY, x + 3, rowY + ROW_H,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int) (alpha * 0.8f)));
        }

        RankDefinition rankDef = RankDefinition.get(pd.rankId);
        int iconY = rowY + (ROW_H - ICON_SIZE) / 2;
        g.blit(rankDef.iconTexture, rowX + 2, iconY, 0, rankDef.getIconVOffset(),
            ICON_SIZE, ICON_SIZE, 16, 160);

        int cx = rowX + 2 + ICON_SIZE + 2;

        String rankStr = rankDef.shortName;
        g.drawString(font, rankStr, cx, rowY + 4, AnimationHelper.withAlpha(rankDef.color, alpha));
        cx += COL_RANK + COL_GAP;

        String nameStr = truncateText(font, pd.callsign, COL_NAME);
        g.drawString(font, nameStr, cx, rowY + 4, AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
        cx += COL_NAME + COL_GAP;

        String killsStr = String.valueOf(pd.kills);
        g.drawString(font, killsStr, cx, rowY + 4, AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
        cx += COL_KILLS + COL_GAP;

        String deathsStr = String.valueOf(pd.deaths);
        g.drawString(font, deathsStr, cx, rowY + 4, AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
        cx += COL_DEATHS + COL_GAP;

        float kd = pd.deaths > 0 ? (float) pd.kills / pd.deaths : pd.kills;
        String kdStr = String.format("%.1f", kd);
        int kdColor = kd >= 2.0f ? UITheme.STATUS_OK : kd >= 1.0f ? UITheme.STATUS_WARN : UITheme.STATUS_DANGER;
        g.drawString(font, kdStr, cx, rowY + 4, AnimationHelper.withAlpha(kdColor, alpha));
        cx += COL_KD + COL_GAP;

        String pingStr = formatPing(pd.pingMs);
        int pingColor;
        if (pd.pingMs < 80) pingColor = UITheme.PING_LOW;
        else if (pd.pingMs < 200) pingColor = UITheme.PING_MID;
        else pingColor = UITheme.PING_HIGH;
        g.drawString(font, pingStr, cx, rowY + 4, AnimationHelper.withAlpha(pingColor, alpha));
    }

    private String formatPing(int ms) {
        if (ms >= 1000) return (ms / 1000) + "s";
        return ms + "ms";
    }

    private String truncateText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String result = text;
        while (font.width(result + "...") > maxWidth && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }
}
