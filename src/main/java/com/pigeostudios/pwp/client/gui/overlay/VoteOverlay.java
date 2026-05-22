package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class VoteOverlay {

    private static final int BADGE_W = 220;
    private static final int CARD_H = 18;
    private static final int MAX_VISIBLE_MAPS = 6;

    private float fadeAlpha = 0f;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        if (Minecraft.getInstance().screen instanceof com.pigeostudios.pwp.client.gui.screen.VoteScreen) return;

        int phase = ClientTeamData.getGamePhase();
        if (phase != 1) {
            fadeAlpha = AnimationHelper.lerp(fadeAlpha, 0f, 0.1f);
            return;
        }

        List<String> mapNames = ClientTeamData.getVoteMapNames();
        if (mapNames.isEmpty()) return;

        fadeAlpha = AnimationHelper.lerp(fadeAlpha, 1f, 0.08f);
        int alpha = (int) (fadeAlpha * 0xFF);
        if (alpha < 5) return;

        Font font = Minecraft.getInstance().font;
        int remaining = ClientTeamData.getVoteRemainingSeconds();
        int[] voteCounts = ClientTeamData.getVoteCounts();

        int totalVotes = 0;
        for (int v : voteCounts) totalVotes += v;

        int panelX = screenWidth - BADGE_W - 4;

        int visibleCount = Math.min(mapNames.size(), MAX_VISIBLE_MAPS);
        int titleH = 20;
        int sepH = 6;
        int totalH = titleH + sepH + visibleCount * CARD_H + 4;

        int panelY = screenHeight - totalH - 4;

        // Background panel
        RenderHelper.dropShadow(g, panelX, panelY, BADGE_W, totalH, 4, (int) (fadeAlpha * 80));
        g.fill(panelX, panelY, panelX + BADGE_W, panelY + totalH,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int) (fadeAlpha * 0xE0)));

        // Accent line at top
        g.fill(panelX, panelY, panelX + BADGE_W, panelY + 2,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        // Title + Timer
        String title = "\u0413\u041e\u041b\u041e\u0421\u041e\u0412\u0410\u041d\u0418\u0415";
        int titleX = panelX + 8;
        int titleY = panelY + 4;

        boolean urgent = remaining <= 5;
        int timerColor = urgent ? UITheme.STATUS_DANGER : UITheme.TEXT_SECONDARY;
        String timeText = remaining + "\u0441";
        int timeW = font.width(timeText);

        // Pulse glow when urgent
        if (urgent) {
            float pulse = (float) Math.sin(System.currentTimeMillis() / 300.0 * Math.PI * 2) * 0.3f + 0.7f;
            RenderHelper.glow(g, panelX + BADGE_W - timeW - 14, titleY - 2, timeW + 12, font.lineHeight + 4,
                UITheme.STATUS_DANGER, 4, fadeAlpha * pulse * 0.3f);
        }

        g.drawString(font, title, titleX, titleY,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
        g.drawString(font, timeText, panelX + BADGE_W - timeW - 8, titleY,
            AnimationHelper.withAlpha(timerColor, alpha));

        // Separator
        int sepY = titleY + font.lineHeight + 3;
        g.fill(panelX + 4, sepY, panelX + BADGE_W - 4, sepY + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, (int) (alpha * 0.3f)));

        // Map rows
        for (int i = 0; i < visibleCount; i++) {
            int cardY = sepY + 4 + i * CARD_H;
            drawMapRow(g, font, panelX, cardY, i, mapNames, voteCounts, totalVotes, alpha);
        }

        // More indicator
        if (mapNames.size() > MAX_VISIBLE_MAPS) {
            String more = "+" + (mapNames.size() - MAX_VISIBLE_MAPS);
            int moreY = sepY + 4 + MAX_VISIBLE_MAPS * CARD_H + 2;
            g.drawString(font, more, panelX + BADGE_W / 2 - font.width(more) / 2, moreY,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int) (alpha * 0.6f)));
        }
    }

    private void drawMapRow(GuiGraphics g, Font font, int panelX, int cardY, int index,
                            List<String> mapNames, int[] voteCounts, int totalVotes, int alpha) {
        String name = mapNames.get(index);
        int votes = index < voteCounts.length ? voteCounts[index] : 0;
        String selectedMap = ClientTeamData.getVotedMap();
        boolean selected = name.equals(selectedMap);

        int rowX = panelX + 4;
        int rowW = BADGE_W - 8;

        // Selected indicator
        if (selected) {
            g.fill(rowX, cardY, rowX + 2, cardY + CARD_H - 2,
                AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
        }

        // Map name (truncated)
        int maxNameW = rowW - 50;
        String displayName = font.width(name) > maxNameW
            ? font.plainSubstrByWidth(name, maxNameW - 6) + "..."
            : name;
        int textColor = selected ? UITheme.ACCENT : UITheme.TEXT_SECONDARY;
        g.drawString(font, displayName, rowX + 6, cardY + 2,
            AnimationHelper.withAlpha(textColor, alpha));

        // Vote count
        String voteText = votes + "";
        int voteX = rowX + rowW - font.width(voteText) - 4;
        g.drawString(font, voteText, voteX, cardY + 2,
            AnimationHelper.withAlpha(selected ? UITheme.ACCENT : UITheme.TEXT_MUTED, alpha));

        // Bar
        int barY = cardY + CARD_H - 4;
        int barH = 2;
        g.fill(rowX + 4, barY, rowX + rowW - 4, barY + barH,
            AnimationHelper.withAlpha(UITheme.BG_SLOT, (int) (alpha * 0.6f)));

        if (totalVotes > 0) {
            float pct = (float) votes / totalVotes;
            int fillW = (int) ((rowW - 8) * pct);
            if (fillW > 0) {
                int barColor = selected ? UITheme.ACCENT : 0xFF4A90D9;
                g.fill(rowX + 4, barY, rowX + 4 + fillW, barY + barH,
                    AnimationHelper.withAlpha(barColor, alpha));
            }
        }
    }
}
