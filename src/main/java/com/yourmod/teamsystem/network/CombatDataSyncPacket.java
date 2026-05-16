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

    public static void encode(CombatDataSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.teamOrdinal);
        buf.writeInt(msg.kills);
        buf.writeInt(msg.deaths);
    }

    public static CombatDataSyncPacket decode(FriendlyByteBuf buf) {
        return new CombatDataSyncPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(CombatDataSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientTeamData.setLocalPlayerData(Team.fromOrdinal(msg.teamOrdinal), msg.kills, msg.deaths);
        });
        ctx.get().setPacketHandled(true);
    }
}
