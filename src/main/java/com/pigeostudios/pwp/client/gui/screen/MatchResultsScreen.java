package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import com.pigeostudios.pwp.network.OpenMatchResultsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class MatchResultsScreen extends Screen {

    private static final int PANEL_W = 640;
    private static final int PANEL_H = 460;
    private static final int HEADER_H = 80;
    private static final int ROW_H = 20;
    private static final int COL_NAME = 120;
    private static final int[] COL_WIDTHS = {60, 60, 60, 70, 70, 70, 60};
    private static final int COL_TOTAL;

    static {
        int sum = COL_NAME;
        for (int w : COL_WIDTHS) sum += w;
        COL_TOTAL = sum;
    }

    private final OpenMatchResultsPacket data;
    private float fadeAlpha = 0f;
    private long openTime;
    private float[] rowAnimProgress;
    private float winnerGlow = 0f;
    private boolean dismissed = false;

    public MatchResultsScreen(OpenMatchResultsPacket data) {
        super(Component.literal("Match Results"));
        this.data = data;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        List<OpenMatchResultsPacket.PlayerResultEntry> players = data.getPlayers();
        rowAnimProgress = new float[players.size()];
    }

    @Override
    public void tick() {
        int phase = ClientTeamData.getGamePhase();
        if (phase != 3 && phase != 2) {
            dismissed = true;
            onClose();
        }
        if (System.currentTimeMillis() - openTime > 15000) {
            dismissed = true;
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (dismissed) return;

        float elapsed = (System.currentTimeMillis() - openTime) / 300f;
        fadeAlpha = Math.min(1f, elapsed);
        winnerGlow = (float) Math.sin((System.currentTimeMillis() - openTime) / 800.0 * Math.PI * 2) * 0.15f + 0.85f;

        renderBackground(g);

        int cx = width / 2;
        int cy = height / 2;
        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        float anim = Math.min(1f, elapsed * 0.8f);
        int panelY = (int) (py + (1f - AnimationHelper.easeOutCubic(anim)) * 60);

        renderPanel(g, px, panelY, anim);
        renderHeader(g, px, panelY, anim);
        renderTeamScores(g, px, panelY, anim);
        renderStatsTable(g, px, panelY, anim);
        renderFooter(g, px, panelY, anim);

        super.render(g, mx, my, pt);
    }

    private void renderPanel(GuiGraphics g, int x, int y, float anim) {
        int alpha = (int) (fadeAlpha * 0xDD);
        RenderHelper.dropShadow(g, x, y, PANEL_W, PANEL_H, 6, (int) (fadeAlpha * 80));
        g.fill(x, y, x + PANEL_W, y + PANEL_H,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, alpha));
        g.fill(x, y, x + PANEL_W, y + 2,
            AnimationHelper.withAlpha(getWinnerColor(), (int) (fadeAlpha * 255 * winnerGlow)));
    }

    private void renderHeader(GuiGraphics g, int x, int y, float anim) {
        Font font = Minecraft.getInstance().font;
        int alpha = (int) (fadeAlpha * 255);
        int winnerColor = getWinnerColor();
        String winnerName = getWinnerName();

        String title = "\u2694 " + winnerName + " WINS!";
        int tw = font.width(title);
        int ty = y + 16;
        float glowInt = winnerGlow * 0.5f;
        RenderHelper.glow(g, x + PANEL_W / 2 - tw / 2 - 8, ty - 4, tw + 16, font.lineHeight + 8,
            winnerColor, 6, fadeAlpha * glowInt);
        g.drawString(font, title, x + PANEL_W / 2 - tw / 2, ty,
            AnimationHelper.withAlpha(winnerColor, alpha));

        String mapInfo = "\u041a\u0430\u0440\u0442\u0430: " + data.getMapName()
            + "   \u0414\u043b\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c: " + formatDuration(data.getMatchDurationSeconds());
        int miw = font.width(mapInfo);
        g.drawString(font, mapInfo, x + PANEL_W / 2 - miw / 2, ty + 18,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int) (alpha * 0.8f)));

        int sepY = ty + 44;
        g.fill(x + 20, sepY, x + PANEL_W - 20, sepY + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, (int) (fadeAlpha * 0x88)));
    }

    private void renderTeamScores(GuiGraphics g, int x, int y, float anim) {
        Font font = Minecraft.getInstance().font;
        int alpha = (int) (fadeAlpha * 255);
        int sy = y + HEADER_H - 10;

        int boxW = (PANEL_W - 60) / 2;
        int boxH = 28;

        int natoX = x + 20;
        int rusX = x + PANEL_W / 2 + 10;

        int natoScore = data.getNatoTotalScore();
        int rusScore = data.getRussiaTotalScore();
        boolean natoWins = data.getWinningTeamOrdinal() == 0;

        drawTeamScoreBox(g, font, natoX, sy, boxW, boxH, "NATO",
            UITheme.TEAM_NATO, data.getNatoTickets(), natoScore, natoWins, alpha);
        drawTeamScoreBox(g, font, rusX, sy, boxW, boxH, "RUSSIA",
            UITheme.TEAM_RUSSIA, data.getRussiaTickets(), rusScore, !natoWins && data.getWinningTeamOrdinal() >= 0, alpha);

        List<OpenMatchResultsPacket.PlayerResultEntry> players = data.getPlayers();
        OpenMatchResultsPacket.PlayerResultEntry mvp = null;
        for (OpenMatchResultsPacket.PlayerResultEntry e : players) {
            if (e.isMVP) { mvp = e; break; }
        }
        if (mvp != null) {
            String mvpText = "\u2B50 MVP: " + mvp.playerName + " (" + mvp.score + " \u043e\u0447\u043a\u043e\u0432"
                + ", K/D/A: " + mvp.kills + "/" + mvp.deaths + "/" + mvp.assists + ")";
            int mw = font.width(mvpText);
            g.drawString(font, mvpText, x + PANEL_W / 2 - mw / 2, sy + boxH + 6,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int) (alpha * 0.9f)));
        }
    }

    private void drawTeamScoreBox(GuiGraphics g, Font font, int x, int y, int w, int h,
                                   String name, int color, int tickets, int score, boolean winner, int alpha) {
        int bgAlpha = winner ? (int) (fadeAlpha * 0x22) : (int) (fadeAlpha * 0x12);
        g.fill(x, y, x + w, y + h, AnimationHelper.withAlpha(color, bgAlpha));
        g.fill(x, y, x + w, y + 2, AnimationHelper.withAlpha(color, (int) (fadeAlpha * (winner ? 200 : 80))));

        String label = name + " \u2014 " + score + " \u043e\u0447\u043a\u043e\u0432";
        g.drawString(font, label, x + 8, y + 5,
            AnimationHelper.withAlpha(winner ? color : UITheme.TEXT_SECONDARY, alpha));
        String ticketsStr = "\u0411\u0438\u043b\u0435\u0442\u044b: " + tickets;
        int tw = font.width(ticketsStr);
        g.drawString(font, ticketsStr, x + w - tw - 8, y + 5,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int) (alpha * 0.7f)));
    }

    private void renderStatsTable(GuiGraphics g, int x, int y, float anim) {
        Font font = Minecraft.getInstance().font;
        int alpha = (int) (fadeAlpha * 255);
        List<OpenMatchResultsPacket.PlayerResultEntry> players = data.getPlayers();

        int tableY = y + HEADER_H + 40;
        int tableX = x + 20 + (PANEL_W - 40 - COL_TOTAL) / 2;
        int tableW = COL_TOTAL;

        if (players.isEmpty()) return;

        String[] headers = {"\u0418\u0433\u0440\u043e\u043a", "K", "D", "A", "\u0417\u0430\u0445\u0432.", "\u041e\u0447\u043a\u0438", "BC", "WC"};
        int[] colWidths = {COL_NAME, COL_WIDTHS[0], COL_WIDTHS[1], COL_WIDTHS[2], COL_WIDTHS[3], COL_WIDTHS[4], COL_WIDTHS[5], COL_WIDTHS[6]};

        g.fill(tableX, tableY - 1, tableX + tableW, tableY, AnimationHelper.withAlpha(UITheme.BORDER, (int) (fadeAlpha * 0x44)));

        int colX = tableX;
        for (int i = 0; i < headers.length; i++) {
            int cw = colWidths[i];
            if (i == 0) {
                g.drawString(font, headers[i], colX + 4, tableY - 12,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int) (alpha * 0.7f)));
            } else {
                int hw = font.width(headers[i]);
                g.drawString(font, headers[i], colX + cw - hw - 6, tableY - 12,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int) (alpha * 0.7f)));
            }
            colX += cw;
        }

        int maxRows = Math.min(players.size(), 12);
        tableY += 4;

        for (int i = 0; i < maxRows; i++) {
            OpenMatchResultsPacket.PlayerResultEntry e = players.get(i);
            int ry = tableY + i * ROW_H;

            if (i < rowAnimProgress.length) {
                float rowDelay = i * 0.04f;
                float rowAnim = Math.min(1f, Math.max(0, (anim - rowDelay) / (1f - rowDelay)));
                rowAnimProgress[i] = AnimationHelper.lerp(rowAnimProgress[i], 1f, 0.15f);
                float rowAlpha = rowAnimProgress[i];

                int rowAlphaInt = (int) (alpha * rowAlpha);
                if (rowAlphaInt < 2) continue;

                boolean isMvp = e.isMVP;
                boolean isNato = e.teamOrdinal == 0;
                int teamColor = isNato ? UITheme.TEAM_NATO : UITheme.TEAM_RUSSIA;

                if (i % 2 == 0) {
                    g.fill(tableX, ry, tableX + tableW, ry + ROW_H,
                        AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int) (fadeAlpha * 0x33 * rowAlpha)));
                }

                if (isMvp) {
                    g.fill(tableX, ry, tableX + 3, ry + ROW_H,
                        AnimationHelper.withAlpha(UITheme.ACCENT, (int) (fadeAlpha * 200 * rowAlpha)));
                }

                int nameColor = isMvp ? UITheme.ACCENT : teamColor;
                float nameX = tableX + 4 + (1f - rowAnimProgress[i]) * 20;
                g.drawString(font, e.playerName, (int) nameX, ry + 4,
                    AnimationHelper.withAlpha(nameColor, rowAlphaInt));

                int[] vals = {e.kills, e.deaths, e.assists, e.captures, e.score, e.bcEarned, e.wcEarned};
                int cx = tableX + COL_NAME;
                for (int vi = 0; vi < vals.length; vi++) {
                    String vs = String.valueOf(vals[vi]);
                    int vw = font.width(vs);
                    int cw = colWidths[vi + 1];
                    g.drawString(font, vs, cx + cw - vw - 6, ry + 4,
                        AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, rowAlphaInt));
                    cx += cw;
                }
            }
        }
    }

    private void renderFooter(GuiGraphics g, int x, int y, float anim) {
        Font font = Minecraft.getInstance().font;
        String hint = "\u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u043b\u044e\u0431\u0443\u044e \u043a\u043b\u0430\u0432\u0438\u0448\u0443 \u0434\u043b\u044f \u043f\u0440\u043e\u0434\u043e\u043b\u0436\u0435\u043d\u0438\u044f";
        int hw = font.width(hint);
        g.drawString(font, hint, x + PANEL_W / 2 - hw / 2, y + PANEL_H - 20,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int) (fadeAlpha * 0x6f)));
    }

    private String getWinnerName() {
        if (data.getWinningTeamOrdinal() < 0 || data.getWinningTeamOrdinal() > 1) return "DRAW";
        return data.getWinningTeamOrdinal() == 0 ? "NATO" : "RUSSIA";
    }

    private int getWinnerColor() {
        if (data.getWinningTeamOrdinal() < 0 || data.getWinningTeamOrdinal() > 1) return UITheme.ACCENT;
        return data.getWinningTeamOrdinal() == 0 ? UITheme.TEAM_NATO : UITheme.TEAM_RUSSIA;
    }

    private String formatDuration(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return min + "\u043c " + sec + "\u0441";
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        dismissed = true;
        onClose();
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        dismissed = true;
        onClose();
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
}
