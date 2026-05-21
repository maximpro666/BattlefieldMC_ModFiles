package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.ClientVoiceHandler;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.RenderHelper;
import com.yourmod.teamsystem.core.Rank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerLabelOverlay {

    private static final int PANEL_W = 130;
    private static final int CHANNEL_H = 14;
    private static final int ENTRY_H = 12;
    private static final int PADDING = 4;
    private static final int MAX_PLAYERS = 8;
    private static final int ALLY_RANGE = 64;

    public void render(GuiGraphics g, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        if (font == null || mc.player == null) return;

        boolean russian = "ru".equals(ClientTeamData.language);

        int ch = ClientVoiceHandler.getActiveChannel();
        String channelLabel;
        int channelColor;
        switch (ch) {
            case 1:
                channelLabel = "\u266B " + (russian ? "\u0421\u041A\u0412\u0410\u0414" : "SQUAD");
                channelColor = UITheme.VOICE_SQUAD;
                break;
            case 2:
                channelLabel = "\u266B " + (russian ? "\u041A\u041E\u041C\u0410\u041D\u0414\u0410" : "TEAM");
                channelColor = UITheme.VOICE_TEAM;
                break;
            default:
                channelLabel = "\u266B " + (russian ? "\u041B\u041E\u041A\u0410\u041B" : "LOCAL");
                channelColor = UITheme.VOICE_LOCAL;
                break;
        }

        String mySquad = ClientTeamData.localPlayerSquad;

        List<PlayerLabel> labels = buildPlayerLabels(mc, mySquad, russian);

        boolean hasSpeaking = !ClientVoiceHandler.getActiveSpeakingPlayers().isEmpty();
        boolean hasPtt = ClientVoiceHandler.isPttActive();

        if (labels.isEmpty() && !hasSpeaking && !hasPtt) return;

        int contentH = CHANNEL_H + labels.size() * ENTRY_H;
        int panelX = screenWidth - PANEL_W - 6;
        int panelY = screenHeight - contentH - 6;

        RenderHelper.roundedRect(g, panelX, panelY, PANEL_W, contentH, 3,
            AnimationHelper.withAlpha(UITheme.BG_HUD, 200));

        int cy = panelY;
        int channelBg = channelColor & 0xFFFFFF | 0x33000000;
        RenderHelper.roundedRect(g, panelX, cy, PANEL_W, CHANNEL_H, 2,
            AnimationHelper.withAlpha(channelBg, 180));
        g.drawString(font, channelLabel, panelX + PADDING, cy + 3,
            AnimationHelper.withAlpha(channelColor, 240));

        cy += CHANNEL_H;

        for (PlayerLabel pl : labels) {
            int color;
            if (pl.isLeader) {
                color = UITheme.ACCENT;
            } else if (pl.isSquad) {
                color = UITheme.VOICE_SQUAD;
            } else {
                color = UITheme.TEXT_PRIMARY;
            }
            g.drawString(font, pl.text, panelX + PADDING, cy + 2,
                AnimationHelper.withAlpha(color, 220));
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

            double dist = player.distanceTo(local);
            if (dist > ALLY_RANGE) continue;

            String squadName = ple.squadName() != null && !ple.squadName().isEmpty() ? ple.squadName() : ple.squad();
            boolean isSquad = mySquad != null && !mySquad.isEmpty() && mySquad.equals(squadName);
            boolean isLeader = ple.isSquadLeader();

            Rank rank = Rank.fromOrdinal(ple.rank());
            String rankStr = rank.getPrefix(russian);
            String callsign = ple.callsign();
            if (callsign == null || callsign.isEmpty()) callsign = player.getName().getString();

            String leaderMark = isLeader ? "\u2605 " : "";
            String text = leaderMark + rankStr + " " + callsign;
            result.add(new PlayerLabel(text, isSquad, isLeader, dist));

            if (result.size() >= MAX_PLAYERS) break;
        }

        result.sort((a, b) -> {
            if (a.isLeader != b.isLeader) return a.isLeader ? -1 : 1;
            if (a.isSquad != b.isSquad) return a.isSquad ? -1 : 1;
            return Double.compare(a.dist, b.dist);
        });

        return result;
    }

    private record PlayerLabel(String text, boolean isSquad, boolean isLeader, double dist) {}
}
