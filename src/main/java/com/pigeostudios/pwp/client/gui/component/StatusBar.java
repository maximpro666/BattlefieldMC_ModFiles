package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.I18n;
import com.pigeostudios.pwp.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class StatusBar {

    public static final int STATUS_H = 18;

    private boolean connected   = true;
    private String  playerName  = "";
    private String  kitName     = "";

    private float fadeAlpha = 0f;

    public void setConnected(boolean v)  { this.connected  = v; }
    public void setPlayerName(String n)  { this.playerName = n; }
    public void setKitName(String k)     { this.kitName    = k; }

    public void render(GuiGraphics g, int screenW, int screenH) {
        fadeAlpha = AnimationHelper.lerp(fadeAlpha, 1f, 0.10f);
        int alpha = (int)(fadeAlpha * 0xFF);

        int y0 = screenH - STATUS_H;

        g.fill(0, y0, screenW, screenH,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xEE)));
        g.fill(0, y0, screenW, y0 + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));

        var font = Minecraft.getInstance().font;
        int ty  = y0 + (STATUS_H - font.lineHeight) / 2;
        int x   = 10;

        int dotColor = connected ? UITheme.STATUS_OK : UITheme.STATUS_DANGER;
        g.fill(x, ty + 2, x + 6, ty + 8,
            AnimationHelper.withAlpha(dotColor, alpha));
        x += 10;

        String connText = connected ? I18n.get("pwp.ui.status_bar.connected") : I18n.get("pwp.ui.status_bar.offline");
        g.drawString(font, connText, x, ty,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha));
        x += font.width(connText) + 8;

        g.fill(x, y0 + 4, x + 1, screenH - 4,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        x += 8;

        String team      = ClientTeamData.getLocalPlayerTeam().name();
        int    teamColor = team.equals("NATO") ? UITheme.TEAM_NATO
                         : team.equals("RUSSIA") ? UITheme.TEAM_RUSSIA
                         : UITheme.TEXT_MUTED;
        String teamStr = I18n.get("pwp.ui.status_bar.team");
        g.drawString(font, teamStr, x, ty,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
        x += font.width(teamStr);
        g.drawString(font, team, x, ty,
            AnimationHelper.withAlpha(teamColor, alpha));
        x += font.width(team) + 8;

        g.fill(x, y0 + 4, x + 1, screenH - 4,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        x += 8;

        if (!playerName.isEmpty()) {
            g.drawString(font, playerName, x, ty,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
            x += font.width(playerName) + 8;

            g.fill(x, y0 + 4, x + 1, screenH - 4,
                AnimationHelper.withAlpha(UITheme.BORDER, alpha));
            x += 8;
        }

        if (!kitName.isEmpty()) {
            g.drawString(font, I18n.get("pwp.ui.status_bar.kit"), x, ty,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
            x += font.width(I18n.get("pwp.ui.status_bar.kit"));
            g.drawString(font, kitName, x, ty,
                AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
        }

        String ver = "PWP v2.0 \u00B7 MC 1.20.1 Forge";
        int vw = font.width(ver);
        g.drawString(font, ver, screenW - vw - 8, ty,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 0x66)));
    }
}
