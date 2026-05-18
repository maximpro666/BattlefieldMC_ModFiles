package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.core.Rank;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;
import java.util.UUID;

public class BattlefieldTabOverlay {

    private static final int COLOR_BG        = UITheme.BG_SCREEN;
    private static final int COLOR_HEADER    = UITheme.BG_PANEL;
    private static final int COLOR_ORANGE    = UITheme.ACCENT;
    private static final int COLOR_TEXT      = UITheme.TEXT_PRIMARY;
    private static final int COLOR_SUBTEXT   = UITheme.TEXT_SECONDARY;
    private static final int COLOR_ROW_ALT   = UITheme.BORDER_ALT;

    private static final int COL_RANK = 50;
    private static final int COL_CS   = 70;
    private static final int COL_NICK = 80;
    private static final int COL_SQ   = 40;
    private static final int COL_KD   = 50;
    private static final int COL_PING = 36;
    private static final int TOTAL_W  = COL_RANK + COL_CS + COL_NICK + COL_SQ + COL_KD + COL_PING + 12;
    private static final int ROW_H    = 14;

    private boolean visible = false;
    private float   alpha   = 0f;

    public void setVisible(boolean v) { this.visible = v; }

    public void tick() {
        alpha = AnimationHelper.lerp(alpha, visible ? 1f : 0f, 0.18f);
    }

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        if (alpha < 0.02f) return;
        int a = (int)(alpha * 255);

        int panelX = screenWidth / 2 - TOTAL_W / 2;
        int panelY = 20;

        g.fill(panelX - 4, panelY - 4,
               panelX + TOTAL_W + 4, panelY + countRows() * ROW_H + 32,
               AnimationHelper.withAlpha(COLOR_BG, (int)(alpha * 0xCC)));

        drawHeaderRow(g, panelX, panelY, a);

        int y = panelY + ROW_H + 4;
        int rowIdx = 0;
        Map<UUID, PlayerListEntry> map = ClientTeamData.playerDataMap;
        if (map == null) return;

        for (Map.Entry<UUID, PlayerListEntry> entry : map.entrySet()) {
            PlayerListEntry ple = entry.getValue();
            if (rowIdx % 2 == 0) {
                g.fill(panelX, y, panelX + TOTAL_W, y + ROW_H, AnimationHelper.withAlpha(COLOR_ROW_ALT, (int)(alpha * 0x0A)));
            }
            drawPlayerRow(g, panelX, y, ple, a);
            y += ROW_H;
            rowIdx++;
        }
    }

    private void drawHeaderRow(GuiGraphics g, int x, int y, int a) {
        g.fill(x, y, x + TOTAL_W, y + ROW_H, AnimationHelper.withAlpha(COLOR_HEADER, a));
        g.fill(x, y, x + TOTAL_W, y + 2, AnimationHelper.withAlpha(COLOR_ORANGE, a));

        int[] cols = { COL_RANK, COL_CS, COL_NICK, COL_SQ, COL_KD, COL_PING };
        String[] headers = { "Rank", "Callsign", "Nick", "Sq", "K/D", "Ping" };
        int cx = x + 4;
        for (int i = 0; i < headers.length; i++) {
            g.drawString(Minecraft.getInstance().font, headers[i], cx, y + 3,
                AnimationHelper.withAlpha(COLOR_ORANGE, a));
            cx += cols[i];
        }
    }

    private void drawPlayerRow(GuiGraphics g, int x, int y, PlayerListEntry ple, int a) {
        Minecraft mc = Minecraft.getInstance();
        int cx = x + 4;

        Rank rank = Rank.fromOrdinal(ple.rank());
        String rankStr = rank != null ? rank.getDisplayName(ple.teamOrdinal() == Team.RUSSIA.ordinal()) : "-";
        g.drawString(mc.font, rankStr, cx, y + 3, AnimationHelper.withAlpha(COLOR_TEXT, a));
        cx += COL_RANK;

        g.drawString(mc.font, ple.callsign() != null ? ple.callsign() : "-", cx, y + 3,
            AnimationHelper.withAlpha(COLOR_TEXT, a));
        cx += COL_CS;

        g.drawString(mc.font, "...", cx, y + 3, AnimationHelper.withAlpha(COLOR_SUBTEXT, a));
        cx += COL_NICK;

        g.drawString(mc.font, ple.squad() != null ? ple.squad() : "-", cx, y + 3,
            AnimationHelper.withAlpha(COLOR_SUBTEXT, a));
        cx += COL_SQ;

        g.drawString(mc.font, ple.kills() + "/" + ple.deaths(), cx, y + 3,
            AnimationHelper.withAlpha(COLOR_TEXT, a));
        cx += COL_KD;

        g.drawString(mc.font, "...", cx, y + 3, AnimationHelper.withAlpha(COLOR_SUBTEXT, a));
    }

    private int countRows() {
        if (ClientTeamData.playerDataMap == null) return 0;
        return Math.min(ClientTeamData.playerDataMap.size(), 32);
    }
}

