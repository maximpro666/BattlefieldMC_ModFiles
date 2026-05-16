package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CombatDataSyncPacket {
    private final int teamOrdinal;
    private final int kills;
    private final int deaths;

    public CombatDataSyncPacket(int teamOrdinal, int kills, int deaths) {
        this.teamOrdinal = teamOrdinal;
        this.kills = kills;
        this.deaths = deaths;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(teamOrdinal);
        buf.writeInt(kills);
        buf.writeInt(deaths);
    }

    public static CombatDataSyncPacket decode(FriendlyByteBuf buf) {
        int teamOrdinal = buf.readInt();
        int kills = buf.readInt();
        int deaths = buf.readInt();
        return new CombatDataSyncPacket(teamOrdinal, kills, deaths);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            Team team = Team.fromOrdinal(teamOrdinal);
            ClientTeamData.setLocalPlayerData(team, kills, deaths);
        });
        ctx.setPacketHandled(true);
    }
}
