package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.UITheme;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.PlayerListEntry;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.SquadActionPacket;
import com.pigeostudios.pwp.client.gui.component.BButton;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.SlotBadge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.UUID;

public class SquadScreen extends Screen {

    private static final int COLOR_BG      = UITheme.BG_SCREEN;
    private static final int COLOR_PANEL   = UITheme.BG_PANEL;
    private static final int COLOR_ORANGE  = UITheme.ACCENT;
    private static final int COLOR_BORDER  = UITheme.BORDER;
    private static final int COLOR_TEXT    = UITheme.TEXT_PRIMARY;
    private static final int COLOR_SUBTEXT = UITheme.TEXT_MUTED;

    private float fadeAlpha = 0f;
    private long openTime;
    private float[] memberHover;

    private static final int PANEL_W = 300;

    public SquadScreen() {
        super(Component.literal("Squad Management"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int cx = width / 2;
        int panelH = height - 100;
        int panelX = cx - PANEL_W / 2;
        int panelY = 50;

        int players = ClientTeamData.playerDataMap != null ? ClientTeamData.playerDataMap.size() : 0;
        memberHover = new float[players];

        addRenderableWidget(new BButton(
            panelX, panelY + panelH + 10, PANEL_W / 2 - 4, 20,
            Component.literal("Join Squad"), btn -> {
                PacketHandler.CHANNEL.sendToServer(new SquadActionPacket("create", null));
                onClose();
            }
        ));

        addRenderableWidget(new BButton(
            panelX + PANEL_W / 2 + 4, panelY + panelH + 10, PANEL_W / 2 - 4, 20,
            Component.literal("Leave Squad"), btn -> {
                PacketHandler.CHANNEL.sendToServer(new SquadActionPacket("leave", null));
                onClose();
            }
        ));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);

        int cx = width / 2;
        int panelX = cx - PANEL_W / 2;
        int panelY = 50;
        int panelH = height - 100;

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xCC)));

        g.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, AnimationHelper.withAlpha(COLOR_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 180)));
        g.fill(panelX, panelY, panelX + 1, panelY + panelH, AnimationHelper.withAlpha(COLOR_BORDER, (int)(fadeAlpha * 180)));
        g.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + panelH, AnimationHelper.withAlpha(COLOR_BORDER, (int)(fadeAlpha * 180)));

        String title = "SQUAD MANAGEMENT";
        int tw = font.width(title);
        g.drawString(font, title, cx - tw / 2, panelY + 10, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 255)));

        String squad = ClientTeamData.localPlayerSquad;
        boolean inSquad = squad != null && !squad.isEmpty();
        String squadText = "Current Squad: " + (inSquad ? squad : "None");
        int sw = font.width(squadText);
        g.drawString(font, squadText, cx - sw / 2, panelY + 24, AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(fadeAlpha * 200)));

        if (inSquad && ClientTeamData.playerDataMap != null) {
            int memberCount = 0;
            for (var entry : ClientTeamData.playerDataMap.entrySet()) {
                String ps = entry.getValue().squadName();
                if (ps == null || ps.isEmpty()) ps = entry.getValue().squad();
                if (squad.equals(ps)) memberCount++;
            }
            int bx = cx + sw / 2 + 6;
            int by = panelY + 23;
            SlotBadge.draw(g, bx, by, memberCount, 4, fadeAlpha);
        }

        g.fill(panelX + 8, panelY + 38, panelX + PANEL_W - 8, panelY + 39,
            AnimationHelper.withAlpha(COLOR_BORDER, (int)(fadeAlpha * 150)));

        if (ClientTeamData.playerDataMap != null) {
            int idx = 0;
            int rowH = 24;
            int listY = panelY + 46;

            for (Map.Entry<UUID, PlayerListEntry> entry : ClientTeamData.playerDataMap.entrySet()) {
                if (listY + rowH > panelY + panelH - 10) break;
                UUID uuid = entry.getKey();
                PlayerListEntry ple = entry.getValue();

                boolean hov = mx >= panelX + 8 && mx <= panelX + PANEL_W - 8
                    && my >= listY && my <= listY + rowH - 2;

                if (idx < memberHover.length)
                    memberHover[idx] = AnimationHelper.lerp(memberHover[idx], hov ? 1f : 0f, 0.15f);

                drawPlayerRow(g, panelX + 8, listY, PANEL_W - 16, ple, idx < memberHover.length ? memberHover[idx] : 0f);
                listY += rowH;
                idx++;
            }
        } else {
            String empty = "No players in session";
            int ew = font.width(empty);
            g.drawString(font, empty, cx - ew / 2, panelY + panelH / 2,
                AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(fadeAlpha * 180)));
        }

        super.render(g, mx, my, pt);
    }

    private void drawPlayerRow(GuiGraphics g, int x, int y, int w,
                                PlayerListEntry ple, float hover) {
        if (hover > 0.01f) {
            g.fill(x, y, x + w, y + 22, AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(hover * 0x0A)));
        }

        String sq = ple.squadName() != null && !ple.squadName().isEmpty() ? ple.squadName() : ple.squad();
        String badge = "[" + (sq != null ? sq : "-") + "]";
        g.drawString(font, badge, x + 2, y + 6, AnimationHelper.withAlpha(COLOR_ORANGE, (int)(fadeAlpha * 200)));

        String name = ple.callsign() != null ? ple.callsign() : "Unknown";
        g.drawString(font, name, x + 32, y + 6, AnimationHelper.withAlpha(COLOR_TEXT, (int)(fadeAlpha * 255)));

        String kd = ple.kills() + "/" + ple.deaths();
        int kdW = font.width(kd);
        g.drawString(font, kd, x + w - kdW - 2, y + 6, AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(fadeAlpha * 200)));

        g.fill(x, y + 22, x + w, y + 23, AnimationHelper.withAlpha(COLOR_BORDER, (int)(fadeAlpha * 80)));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

