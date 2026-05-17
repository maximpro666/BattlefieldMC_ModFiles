package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TeamTicketSyncPacket {
    private final int natoTickets;
    private final int russiaTickets;

    public TeamTicketSyncPacket(int natoTickets, int russiaTickets) {
        this.natoTickets = natoTickets;
        this.russiaTickets = russiaTickets;
    }

    public static void encode(TeamTicketSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.natoTickets);
        buf.writeInt(msg.russiaTickets);
    }

    public static TeamTicketSyncPacket decode(FriendlyByteBuf buf) {
        return new TeamTicketSyncPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(TeamTicketSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientTeamData.setTickets(msg.natoTickets, msg.russiaTickets);
        });
        ctx.get().setPacketHandled(true);
    }
}
