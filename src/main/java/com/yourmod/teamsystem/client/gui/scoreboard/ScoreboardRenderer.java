package com.yourmod.teamsystem.client.gui.scoreboard;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.scoreboard.data.PlayerScoreboardData;
import com.yourmod.teamsystem.client.gui.scoreboard.data.RankDefinition;
import com.yourmod.teamsystem.client.gui.scoreboard.data.ScoreboardDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.*;

public class ScoreboardRenderer {

    private static final int ROW_H = 13;
    private static final int SECTION_H = 15;
    private static final int PADDING = 4;
    private static final int ICON_SIZE = 10;
    private static final int TEAM_HEADER_H = 13;
    private static final int COLUMNS_GAP = 16;
    private static final int PANEL_TOP = 18;

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

    public boolean isVisible() {
        return visible;
    }

    public float getAlpha() {
        return prevAlpha;
    }

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

        int rankW = ICON_SIZE + 2 + font.width("Gen.") + 2;
        int baseDataW = rankW + COL_CALLSIGN + COL_NICK + COL_KD + COL_PING + COL_GAP * 4;
        int minColW = baseDataW + PADDING * 2;
        int availableW = screenWidth - PADDING * 2 - COLUMNS_GAP;
        int colW = Math.max(minColW, Math.min(availableW / 2, baseDataW + 40));

        if (colW * 2 + COLUMNS_GAP + PADDING * 2 > screenWidth) {
            colW = (screenWidth - COLUMNS_GAP - PADDING * 2) / 2;
        }

        int totalW = colW * 2 + COLUMNS_GAP;
        int panelX = screenWidth / 2 - totalW / 2;
        int panelY = PANEL_TOP;

        int natoRows = countRows(natoGrouped);
        int russiaRows = countRows(russiaGrouped);
        int maxRows = Math.max(natoRows, russiaRows);
        int contentH = maxRows * ROW_H + PADDING * 2;
        int maxH = screenHeight - panelY - PADDING - TEAM_HEADER_H;
        if (maxH <= 0) return;
        int panelH = Math.min(contentH, maxH);

        maxScroll = Math.max(0, contentH - maxH);

        g.fill(panelX, panelY, panelX + totalW, panelY + panelH + TEAM_HEADER_H,
                AnimationHelper.withAlpha(UITheme.BG_SCREEN, alpha));

        drawTeamColumn(g, font, panelX, panelY, colW, panelH, natoGrouped, "NATO", nato.size(), alpha, 0xFF1C5FAD);
        drawTeamColumn(g, font, panelX + colW + COLUMNS_GAP, panelY, colW, panelH, russiaGrouped, "RUSSIA", russia.size(), alpha, 0xFFAD1C1C);

        if (maxScroll > 0) {
            String scrollHint = (scrollOffset > 0 ? "\u25B2 " : "") + "\u25BC";
            int hintW = font.width(scrollHint);
            g.drawString(font, scrollHint, screenWidth / 2 - hintW / 2, panelY + panelH + TEAM_HEADER_H + 2,
                    AnimationHelper.withAlpha(0xFF808080, alpha));
        }
    }

    private static final int COL_CALLSIGN = 60;
    private static final int COL_NICK = 60;
    private static final int COL_KD = 36;
    private static final int COL_PING = 30;
    private static final int COL_GAP = 2;

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
        int alphaTeam = teamColor & 0x00FFFFFF | (alpha << 24);
        g.drawString(font, teamName + " (" + playerCount + ")", x + PADDING, headerY + 3, alphaTeam);
        g.fill(x, headerY + TEAM_HEADER_H - 1, x + colW, headerY + TEAM_HEADER_H,
                AnimationHelper.withAlpha(teamColor, alpha / 3));

        if (panelH > 0) {
            g.enableScissor(x, headerY + TEAM_HEADER_H, x + colW, headerY + TEAM_HEADER_H + panelH);
        }

        int cy = headerY + TEAM_HEADER_H - scrollOffset;
        int rowX = x + PADDING;

        for (Map.Entry<String, List<PlayerScoreboardData>> section : grouped.entrySet()) {
            if (cy + SECTION_H > headerY + TEAM_HEADER_H - ROW_H &&
                cy < headerY + TEAM_HEADER_H + panelH) {
                g.drawString(font, section.getKey(), rowX, cy + 2,
                        AnimationHelper.withAlpha(UITheme.SQUAD_LABEL, alpha));
                g.fill(x + PADDING, cy + SECTION_H - 1, x + colW - PADDING, cy + SECTION_H,
                        AnimationHelper.withAlpha(UITheme.SQUAD_LINE, alpha));
            }
            cy += SECTION_H;

            for (PlayerScoreboardData pd : section.getValue()) {
                if (cy + ROW_H > headerY + TEAM_HEADER_H - ROW_H &&
                    cy < headerY + TEAM_HEADER_H + panelH) {
                    int rowY = cy;

                    if (pd.isSelf) {
                        g.fill(x, rowY, x + colW, rowY + ROW_H, AnimationHelper.withAlpha(UITheme.SELF_BG, alpha));
                        g.fill(x, rowY, x + 2, rowY + ROW_H, AnimationHelper.withAlpha(UITheme.SELF_BORDER, alpha));
                    }

                    RankDefinition rankDef = RankDefinition.get(pd.rankId);
                    int iconY = rowY + (ROW_H - ICON_SIZE) / 2;
                    g.blit(rankDef.iconTexture, rowX, iconY, 0, rankDef.getIconVOffset(), ICON_SIZE, ICON_SIZE, 16, 160);
                    int cx = rowX + ICON_SIZE + 2;

                    String rankStr = rankDef.shortName;
                    g.drawString(font, rankStr, cx, rowY + 3, AnimationHelper.withAlpha(rankDef.color, alpha));
                    cx += font.width(rankStr) + 2;

                    int dataW = colW - (cx - x) - PADDING;
                    if (dataW <= 0) dataW = 10;

                    int callsignW = Math.min(COL_CALLSIGN, dataW * 35 / 100);
                    int nickW = Math.min(COL_NICK, dataW * 30 / 100);
                    int kdW = Math.min(COL_KD, dataW * 15 / 100);
                    int donateW = Math.min(50, dataW * 10 / 100);
                    int pingW = Math.min(COL_PING, dataW * 10 / 100);

                    String callsign = pd.callsign;
                    if (font.width(callsign) > callsignW) {
                        while (font.width(callsign + "...") > callsignW && callsign.length() > 1)
                            callsign = callsign.substring(0, callsign.length() - 1);
                        callsign += "...";
                    }
                    g.drawString(font, callsign, cx, rowY + 3, AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
                    cx += callsignW + COL_GAP;

                    String nickStr = pd.nick != null && !pd.nick.isEmpty() ? pd.nick : "-";
                    if (font.width(nickStr) > nickW) {
                        while (font.width(nickStr + "...") > nickW && nickStr.length() > 1)
                            nickStr = nickStr.substring(0, nickStr.length() - 1);
                        nickStr += "...";
                    }
                    g.drawString(font, nickStr, cx, rowY + 3, AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha));
                    cx += nickW + COL_GAP;

                    String kdStr = pd.kills + "/" + pd.deaths;
                    g.drawString(font, kdStr, cx, rowY + 3, AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
                    cx += kdW + COL_GAP;

                    if (pd.donateLevel != PlayerScoreboardData.DonateLevel.NONE) {
                        String donStr = switch (pd.donateLevel) {
                            case VIP -> "VIP";
                            case ELITE -> "ELT";
                            case GENERAL -> "GEN";
                            default -> "";
                        };
                        int donCol = switch (pd.donateLevel) {
                            case VIP -> UITheme.DONATE_VIP;
                            case ELITE -> UITheme.DONATE_ELITE_A;
                            case GENERAL -> UITheme.DONATE_GENERAL;
                            default -> 0;
                        };
                        g.drawString(font, donStr, cx, rowY + 3, AnimationHelper.withAlpha(donCol, alpha));
                    }
                    cx += donateW + COL_GAP;

                    int pingColor;
                    if (pd.pingMs < 80) pingColor = UITheme.PING_LOW;
                    else if (pd.pingMs < 200) pingColor = UITheme.PING_MID;
                    else pingColor = UITheme.PING_HIGH;
                    g.drawString(font, pd.pingMs + "ms", cx, rowY + 3, AnimationHelper.withAlpha(pingColor, alpha));
                }
                cy += ROW_H;
            }
        }

        if (panelH > 0) {
            g.disableScissor();
        }
    }
}
