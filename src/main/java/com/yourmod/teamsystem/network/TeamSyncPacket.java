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

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(teamOrdinal);
    }

    public static TeamSyncPacket decode(FriendlyByteBuf buf) {
        int teamOrdinal = buf.readInt();
        return new TeamSyncPacket(teamOrdinal);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            Team team = Team.fromOrdinal(teamOrdinal);
            ClientTeamData.setLocalPlayerTeam(team);
        });
        ctx.setPacketHandled(true);
    }
}
