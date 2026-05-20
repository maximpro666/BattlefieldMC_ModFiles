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

    private static final int COL_EMPTY = 48;
    private static final int COL_RANK = 80;
    private static final int COL_CALLSIGN = 90;
    private static final int COL_NICK = 100;
    private static final int COL_KD = 60;
    private static final int COL_RATIO = 50;
    private static final int COL_PING = 40;
    private static final int COL_GAP = 4;
    private static final int ROW_H = 20;
    private static final int SECTION_H = 24;
    private static final int PADDING = 6;
    private static final int ICON_SIZE = 16;

    private boolean visible = false;
    private float prevAlpha = 0f;
    private float targetAlpha = 0f;

    public void setVisible(boolean v) {
        this.visible = v;
        this.targetAlpha = v ? 1f : 0f;
    }

    public boolean isVisible() {
        return visible;
    }

    public float getAlpha() {
        return prevAlpha;
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

        players.sort((a, b) -> {
            int sqCmp = a.squad.compareTo(b.squad);
            if (sqCmp != 0) return sqCmp;
            int rCmp = Integer.compare(b.rankId, a.rankId);
            if (rCmp != 0) return rCmp;
            return Integer.compare(b.kills, a.kills);
        });

        Map<String, List<PlayerScoreboardData>> grouped = new LinkedHashMap<>();
        for (PlayerScoreboardData p : players) {
            String squad = p.squad != null && !p.squad.isEmpty() ? p.squad : "No Squad";
            grouped.computeIfAbsent(squad, k -> new ArrayList<>()).add(p);
        }

        int totalW = COL_EMPTY + COL_RANK + COL_CALLSIGN + COL_NICK + COL_KD + COL_RATIO + COL_PING + PADDING * 2 + COL_GAP * 6;
        int panelX = screenWidth / 2 - totalW / 2;
        int panelY = 16;

        int rowCount = 0;
        for (Map.Entry<String, List<PlayerScoreboardData>> section : grouped.entrySet()) {
            rowCount++;
            rowCount += section.getValue().size();
        }
        int panelH = rowCount * ROW_H + (grouped.size() > 0 ? grouped.size() * (SECTION_H - ROW_H) : 0) + PADDING * 2;

        g.fill(panelX, panelY, panelX + totalW, panelY + panelH, AnimationHelper.withAlpha(UITheme.BG_SCREEN, alpha));

        int cy = panelY + PADDING;
        for (Map.Entry<String, List<PlayerScoreboardData>> section : grouped.entrySet()) {
            String squadName = section.getKey();
            List<PlayerScoreboardData> members = section.getValue();

            g.drawString(font, squadName, panelX + PADDING, cy + 4, AnimationHelper.withAlpha(UITheme.SQUAD_LABEL, alpha));
            g.fill(panelX + PADDING, cy + SECTION_H - 1, panelX + totalW - PADDING, cy + SECTION_H, AnimationHelper.withAlpha(UITheme.SQUAD_LINE, alpha));
            cy += SECTION_H;

            for (PlayerScoreboardData pd : members) {
                int rowY = cy;

                if (pd.isSelf) {
                    g.fill(panelX, rowY, panelX + totalW, rowY + ROW_H, AnimationHelper.withAlpha(UITheme.SELF_BG, alpha));
                    g.fill(panelX, rowY, panelX + 3, rowY + ROW_H, AnimationHelper.withAlpha(UITheme.SELF_BORDER, alpha));
                }

                int cx = panelX + PADDING;
                cx += COL_EMPTY;

                RankDefinition rankDef = RankDefinition.get(pd.rankId);
                int iconY = rowY + (ROW_H - ICON_SIZE) / 2;
                g.blit(rankDef.iconTexture, cx, iconY, 0, rankDef.getIconVOffset(), ICON_SIZE, ICON_SIZE, 16, 160);
                cx += ICON_SIZE + 2;
                g.drawString(font, rankDef.shortName, cx, rowY + 4, AnimationHelper.withAlpha(rankDef.color, alpha));
                int abbrW = font.width(rankDef.shortName);
                cx += abbrW + 2;
                g.drawString(font, rankDef.fullName, cx, rowY + 4, AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha));
                cx = panelX + PADDING + COL_EMPTY + COL_RANK;

                g.drawString(font, pd.callsign, cx, rowY + 4, AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
                cx += COL_CALLSIGN;

                String nickStr = pd.nick != null && !pd.nick.isEmpty() ? pd.nick : "-";
                g.drawString(font, nickStr, cx, rowY + 2, AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha));
                DonateBadgeRenderer.renderBadge(g, cx, rowY + 12, pd.donateLevel, alpha);
                cx += COL_NICK;

                String kdStr = pd.kills + "/" + pd.deaths;
                g.drawString(font, kdStr, cx, rowY + 4, AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
                cx += COL_KD;

                String ratioStr = String.format("%.2f", pd.getKD());
                g.drawString(font, ratioStr, cx, rowY + 4, AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
                cx += COL_RATIO;

                int pingColor;
                if (pd.pingMs < 80) {
                    pingColor = UITheme.PING_LOW;
                } else if (pd.pingMs < 200) {
                    pingColor = UITheme.PING_MID;
                } else {
                    pingColor = UITheme.PING_HIGH;
                }
                String pingStr = pd.pingMs + "ms";
                g.drawString(font, pingStr, cx, rowY + 4, AnimationHelper.withAlpha(pingColor, alpha));

                cy += ROW_H;
            }
        }
    }
}
