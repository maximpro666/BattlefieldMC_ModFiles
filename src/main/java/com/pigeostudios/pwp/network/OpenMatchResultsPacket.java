package com.pigeostudios.pwp.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenMatchResultsPacket {

    public static class PlayerResultEntry {
        public final String playerName;
        public final int teamOrdinal;
        public final int kills;
        public final int deaths;
        public final int assists;
        public final int captures;
        public final int score;
        public final int bcEarned;
        public final int wcEarned;
        public final boolean isMVP;

        public PlayerResultEntry(String playerName, int teamOrdinal, int kills, int deaths,
                                  int assists, int captures, int score,
                                  int bcEarned, int wcEarned, boolean isMVP) {
            this.playerName = playerName;
            this.teamOrdinal = teamOrdinal;
            this.kills = kills;
            this.deaths = deaths;
            this.assists = assists;
            this.captures = captures;
            this.score = score;
            this.bcEarned = bcEarned;
            this.wcEarned = wcEarned;
            this.isMVP = isMVP;
        }
    }

    private final int winningTeamOrdinal;
    private final String mapName;
    private final int matchDurationSeconds;
    private final int natoTickets;
    private final int russiaTickets;
    private final int natoTotalScore;
    private final int russiaTotalScore;
    private final List<PlayerResultEntry> players;

    public OpenMatchResultsPacket(int winningTeamOrdinal, String mapName, int matchDurationSeconds,
                                   int natoTickets, int russiaTickets,
                                   int natoTotalScore, int russiaTotalScore,
                                   List<PlayerResultEntry> players) {
        this.winningTeamOrdinal = winningTeamOrdinal;
        this.mapName = mapName;
        this.matchDurationSeconds = matchDurationSeconds;
        this.natoTickets = natoTickets;
        this.russiaTickets = russiaTickets;
        this.natoTotalScore = natoTotalScore;
        this.russiaTotalScore = russiaTotalScore;
        this.players = players;
    }

    public OpenMatchResultsPacket(FriendlyByteBuf buf) {
        this.winningTeamOrdinal = buf.readInt();
        this.mapName = buf.readUtf(128);
        this.matchDurationSeconds = buf.readInt();
        this.natoTickets = buf.readInt();
        this.russiaTickets = buf.readInt();
        this.natoTotalScore = buf.readInt();
        this.russiaTotalScore = buf.readInt();
        int count = buf.readInt();
        this.players = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf(64);
            int team = buf.readInt();
            int kills = buf.readInt();
            int deaths = buf.readInt();
            int assists = buf.readInt();
            int captures = buf.readInt();
            int score = buf.readInt();
            int bc = buf.readInt();
            int wc = buf.readInt();
            boolean mvp = buf.readBoolean();
            players.add(new PlayerResultEntry(name, team, kills, deaths, assists, captures, score, bc, wc, mvp));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(winningTeamOrdinal);
        buf.writeUtf(mapName);
        buf.writeInt(matchDurationSeconds);
        buf.writeInt(natoTickets);
        buf.writeInt(russiaTickets);
        buf.writeInt(natoTotalScore);
        buf.writeInt(russiaTotalScore);
        buf.writeInt(players.size());
        for (PlayerResultEntry e : players) {
            buf.writeUtf(e.playerName);
            buf.writeInt(e.teamOrdinal);
            buf.writeInt(e.kills);
            buf.writeInt(e.deaths);
            buf.writeInt(e.assists);
            buf.writeInt(e.captures);
            buf.writeInt(e.score);
            buf.writeInt(e.bcEarned);
            buf.writeInt(e.wcEarned);
            buf.writeBoolean(e.isMVP);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientScreenAccessor.openMatchResults(this)
            );
        });
        return true;
    }

    public int getWinningTeamOrdinal() { return winningTeamOrdinal; }
    public String getMapName() { return mapName; }
    public int getMatchDurationSeconds() { return matchDurationSeconds; }
    public int getNatoTickets() { return natoTickets; }
    public int getRussiaTickets() { return russiaTickets; }
    public int getNatoTotalScore() { return natoTotalScore; }
    public int getRussiaTotalScore() { return russiaTotalScore; }
    public List<PlayerResultEntry> getPlayers() { return players; }
}
