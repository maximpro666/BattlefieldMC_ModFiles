package com.yourmod.teamsystem.client.gui.component;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.UITheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TopBar {

    public static final int TOP_H = 24;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private float fadeAlpha = 0f;

    public void render(GuiGraphics g, int screenW, String screenName,
                       int sp, int bc, int rank) {
        fadeAlpha = AnimationHelper.lerp(fadeAlpha, 1f, 0.10f);
        int alpha = (int)(fadeAlpha * 0xFF);

        g.fill(0, 0, screenW, TOP_H,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xEE)));
        g.fill(0, TOP_H - 1, screenW, TOP_H,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        g.fill(0, 0, 3, TOP_H,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        var font = Minecraft.getInstance().font;
        int y = (TOP_H - font.lineHeight) / 2;

        g.drawString(font, "BATTLEFIELD", 8, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        int dotX = 8 + font.width("BATTLEFIELD") + 4;
        g.drawString(font, "\u00B7", dotX, y,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        int nameX = dotX + font.width("\u00B7") + 4;
        g.drawString(font, screenName.toUpperCase(), nameX, y,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha));

        String time = LocalTime.now().format(TIME_FMT);
        String team = getTeamLabel();
        int teamColor = getTeamColor();

        String rankStr  = "Rank " + rank;
        String spStr    = "SP " + sp;
        String bcStr    = "BC " + bc;

        int rx = screenW - 8;

        rx -= font.width(time);
        g.drawString(font, time, rx, y,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
        rx -= 8;

        g.fill(rx, 4, rx + 1, TOP_H - 4,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        rx -= 8;

        rx -= font.width(team);
        g.drawString(font, team, rx, y,
            AnimationHelper.withAlpha(teamColor, alpha));
        rx -= 12;

        rx -= font.width(rankStr);
        g.drawString(font, rankStr, rx, y,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
        rx -= 8;

        rx -= font.width(spStr);
        g.drawString(font, spStr, rx, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
        rx -= 8;

        rx -= font.width(bcStr);
        g.drawString(font, bcStr, rx, y,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha));
    }

    private static String getTeamLabel() {
        return switch (ClientTeamData.getLocalPlayerTeam().name()) {
            case "NATO"     -> "NATO";
            case "RUSSIA"   -> "RUSSIA";
            default         -> "SPEC";
        };
    }

    private static int getTeamColor() {
        return switch (ClientTeamData.getLocalPlayerTeam().name()) {
            case "NATO"   -> UITheme.TEAM_NATO;
            case "RUSSIA" -> UITheme.TEAM_RUSSIA;
            default       -> UITheme.TEXT_MUTED;
        };
    }
}
