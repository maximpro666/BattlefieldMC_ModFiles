package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.UITheme;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.core.Team;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.TeamSelectPacket;
import com.pigeostudios.pwp.client.gui.component.BButton;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TeamSelectionScreen extends Screen {

    private static final int COLOR_ORANGE    = UITheme.ACCENT;
    private static final int COLOR_BG        = UITheme.BG_SCREEN;
    private static final int COLOR_CARD_BG   = UITheme.BG_SURFACE;
    private static final int COLOR_NATO_ACCENT   = UITheme.TEAM_NATO;
    private static final int COLOR_RUSSIA_ACCENT = UITheme.TEAM_RUSSIA;
    private static final int COLOR_TEXT      = UITheme.TEXT_PRIMARY;
    private static final int COLOR_SUBTEXT   = UITheme.TEXT_SECONDARY;
    private static final int COLOR_BORDER    = UITheme.ACCENT;

    private float fadeAlpha = 0f;
    private long openTime;

    private BButton natoButton;
    private BButton russiaButton;
    private BButton spectatorButton;

    private float natoHover   = 0f;
    private float russiaHover = 0f;

    public TeamSelectionScreen() {
        super(Component.literal("Team Selection"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int cx = width / 2;
        int cy = height / 2;
        int cardW = 180;
        int cardH = 220;
        int gap   = 24;

        natoButton = addRenderableWidget(new BButton(
            cx - cardW - gap / 2, cy - cardH / 2, cardW, cardH,
            Component.literal(""), btn -> selectTeam(Team.NATO)
        ));

        russiaButton = addRenderableWidget(new BButton(
            cx + gap / 2, cy - cardH / 2, cardW, cardH,
            Component.literal(""), btn -> selectTeam(Team.RUSSIA)
        ));

        spectatorButton = addRenderableWidget(new BButton(
            cx - 60, cy + cardH / 2 + 20, 120, 20,
            Component.literal("Spectator"), btn -> selectTeam(Team.SPECTATOR)
        ));
    }

    private void selectTeam(Team team) {
        ClientTeamData.setLocalPlayerTeam(team);
        PacketHandler.CHANNEL.sendToServer(new TeamSelectPacket(team.ordinal()));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);

        int cx = width / 2;
        int cy = height / 2;
        int cardW = 180;
        int cardH = 220;
        int gap   = 24;

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xCC)));

        String title = "SELECT YOUR TEAM";
        int titleW = font.width(title);
        g.drawString(font, title, cx - titleW / 2, cy - cardH / 2 - 36, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        int natoX = cx - cardW - gap / 2;
        int natoY = cy - cardH / 2;
        boolean natoHovered = mx >= natoX && mx <= natoX + cardW && my >= natoY && my <= natoY + cardH;
        natoHover = AnimationHelper.lerp(natoHover, natoHovered ? 1f : 0f, 0.15f);
        drawTeamCard(g, natoX, natoY, cardW, cardH, "NATO", "North Atlantic\nTreaty Organization",
            COLOR_NATO_ACCENT, natoHover, fadeAlpha);

        int rusX = cx + gap / 2;
        int rusY = cy - cardH / 2;
        boolean rusHovered = mx >= rusX && mx <= rusX + cardW && my >= rusY && my <= rusY + cardH;
        russiaHover = AnimationHelper.lerp(russiaHover, rusHovered ? 1f : 0f, 0.15f);
        drawTeamCard(g, rusX, rusY, cardW, cardH, "RUSSIA", "Russian Armed\nForces",
            COLOR_RUSSIA_ACCENT, russiaHover, fadeAlpha);

        int natoTickets   = ClientTeamData.getNatoTickets();
        int russiaTickets = ClientTeamData.getRussiaTickets();
        g.drawString(font, "Tickets: " + natoTickets,
            natoX + 8, natoY + cardH - 30,
            AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(fadeAlpha * 200)));
        g.drawString(font, "Tickets: " + russiaTickets,
            rusX + 8, rusY + cardH - 30,
            AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(fadeAlpha * 200)));

        super.render(g, mx, my, pt);
    }

    private void drawTeamCard(GuiGraphics g, int x, int y, int w, int h,
                               String teamName, String subName,
                               int accentColor, float hover, float alpha) {
        int bgAlpha  = (int)(alpha * (0xDD + hover * 0x11));
        int brdAlpha = (int)(alpha * (0x88 + hover * 0x77));

        g.fill(x, y, x + w, y + h, AnimationHelper.withAlpha(COLOR_CARD_BG, bgAlpha));

        int barH = (int)(4 + hover * 2);
        g.fill(x, y, x + w, y + barH, AnimationHelper.withAlpha(accentColor, (int)(alpha * 255)));

        g.fill(x, y, x + 1, y + h, AnimationHelper.withAlpha(accentColor, brdAlpha));
        g.fill(x + w - 1, y, x + w, y + h, AnimationHelper.withAlpha(accentColor, brdAlpha));
        g.fill(x, y + h - 1, x + w, y + h, AnimationHelper.withAlpha(accentColor, brdAlpha));

        int nameW = font.width(teamName);
        g.drawString(font, teamName, x + w / 2 - nameW / 2, y + 20, AnimationHelper.withAlpha(COLOR_TEXT, (int)(alpha * 255)));

        String[] lines = subName.split("\n");
        for (int i = 0; i < lines.length; i++) {
            int lw = font.width(lines[i]);
            g.drawString(font, lines[i], x + w / 2 - lw / 2, y + 36 + i * 12,
                AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(alpha * 200)));
        }

        if (hover > 0.01f) {
            g.fill(x + 1, y + barH, x + w - 1, y + barH + 1,
                AnimationHelper.withAlpha(accentColor, (int)(hover * 80)));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
