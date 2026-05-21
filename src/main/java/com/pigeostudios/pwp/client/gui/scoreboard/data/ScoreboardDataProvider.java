package com.pigeostudios.pwp.client.gui.scoreboard.data;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.PlayerListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardDataProvider {

    public static List<PlayerScoreboardData> buildPlayerList() {
        List<PlayerScoreboardData> result = new ArrayList<>();
        Map<UUID, PlayerListEntry> map = ClientTeamData.playerDataMap;
        if (map == null || map.isEmpty()) return result;

        Minecraft mc = Minecraft.getInstance();
        UUID localUuid = mc.player != null ? mc.player.getUUID() : null;

        for (Map.Entry<UUID, PlayerListEntry> entry : map.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerListEntry ple = entry.getValue();

            PlayerScoreboardData data = new PlayerScoreboardData();
            data.rankId = ple.rank();
            data.callsign = ple.callsign() != null ? ple.callsign() : "";
            data.squad = ple.squadName() != null && !ple.squadName().isEmpty() ? ple.squadName() : ple.squad();
            data.teamOrdinal = ple.teamOrdinal();
            data.kills = ple.kills();
            data.deaths = ple.deaths();
            data.isSelf = uuid.equals(localUuid);

            PlayerInfo playerInfo = mc.getConnection() != null ? mc.getConnection().getPlayerInfo(uuid) : null;
            if (playerInfo != null) {
                data.nick = playerInfo.getProfile().getName();
                data.pingMs = playerInfo.getLatency();
            } else {
                data.nick = "";
                data.pingMs = 0;
            }

            data.donateLevel = switch (ple.donatTier()) {
                case 1 -> PlayerScoreboardData.DonateLevel.VIP;
                case 2 -> PlayerScoreboardData.DonateLevel.ELITE;
                case 3 -> PlayerScoreboardData.DonateLevel.GENERAL;
                default -> PlayerScoreboardData.DonateLevel.NONE;
            };
            data.donateBarProgress = 0f;

            result.add(data);
        }

        return result;
    }
}
