package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TeamSyncPacket {
    private final int teamOrdinal;

    public TeamSyncPacket(int teamOrdinal) {
        this.teamOrdinal = teamOrdinal;
    }

    public static void encode(TeamSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.teamOrdinal);
    }

    public static TeamSyncPacket decode(FriendlyByteBuf buf) {
        return new TeamSyncPacket(buf.readInt());
    }

    public static void handle(TeamSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientTeamData.setLocalPlayerTeam(Team.fromOrdinal(msg.teamOrdinal));
        });
        ctx.get().setPacketHandled(true);
    }
}
