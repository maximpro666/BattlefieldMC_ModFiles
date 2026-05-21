package com.pigeostudios.pwp.client.gui.overlay;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.PlayerListEntry;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.RenderHelper;
import com.pigeostudios.pwp.core.Rank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerLabelOverlay {

    private static final int PANEL_W = 140;
    private static final int ENTRY_H = 13;
    private static final int PADDING = 5;
    private static final int MAX_PLAYERS = 8;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        if (font == null || mc.player == null) return;

        boolean russian = "ru".equals(ClientTeamData.language);
        String mySquad = ClientTeamData.localPlayerSquad;

        List<PlayerLabel> labels = buildPlayerLabels(mc, mySquad, russian);

        if (labels.isEmpty()) return;

        int contentH = labels.size() * ENTRY_H;
        int panelX = screenWidth - PANEL_W - 6;
        int panelY = screenHeight - contentH - 8;

        RenderHelper.dropShadow(g, panelX, panelY, PANEL_W, contentH, 3, 80);
        RenderHelper.roundedRect(g, panelX, panelY, PANEL_W, contentH, 3,
            AnimationHelper.withAlpha(UITheme.BG_HUD, UITheme.ALPHA_HUD));

        int cy = panelY;

        for (PlayerLabel pl : labels) {
            int color;
            if (pl.isLeader) color = UITheme.ACCENT;
            else if (pl.isSquad) color = UITheme.VOICE_SQUAD;
            else color = UITheme.TEXT_PRIMARY;

            g.drawString(font, pl.text, panelX + PADDING + 2, cy + 3,
                AnimationHelper.withAlpha(color, 230));
            cy += ENTRY_H;
        }
    }

    private List<PlayerLabel> buildPlayerLabels(Minecraft mc, String mySquad, boolean russian) {
        List<PlayerLabel> result = new ArrayList<>();
        if (mc.level == null) return result;

        Player local = mc.player;
        if (local == null) return result;

        Map<UUID, PlayerListEntry> map = ClientTeamData.playerDataMap;
        if (map == null) return result;

        int myTeam = -1;
        PlayerListEntry myEntry = map.get(local.getUUID());
        if (myEntry != null) myTeam = myEntry.teamOrdinal();

        for (Player player : mc.level.players()) {
            if (player == local) continue;
            if (player.isSpectator()) continue;

            UUID uuid = player.getUUID();
            PlayerListEntry ple = map.get(uuid);
            if (ple == null) continue;
            if (ple.teamOrdinal() != myTeam) continue;

            String squadName = ple.squadName() != null && !ple.squadName().isEmpty() ? ple.squadName() : ple.squad();
            boolean isSquad = mySquad != null && !mySquad.isEmpty() && mySquad.equals(squadName);
            boolean isLeader = ple.isSquadLeader();

            Rank rank = Rank.fromOrdinal(ple.rank());
            String rankStr = rank.getPrefix(russian);
            String callsign = ple.callsign();
            if (callsign == null || callsign.isEmpty()) callsign = player.getName().getString();

            String leaderMark = isLeader ? "\u2605 " : "";
            String text = leaderMark + rankStr + " " + callsign;
            result.add(new PlayerLabel(text, isSquad, isLeader));
        }

        result.sort((a, b) -> {
            if (a.isLeader != b.isLeader) return a.isLeader ? -1 : 1;
            if (a.isSquad != b.isSquad) return a.isSquad ? -1 : 1;
            return 0;
        });

        if (result.size() > MAX_PLAYERS) {
            result = result.subList(0, MAX_PLAYERS);
        }

        return result;
    }

    private record PlayerLabel(String text, boolean isSquad, boolean isLeader) {}
}
