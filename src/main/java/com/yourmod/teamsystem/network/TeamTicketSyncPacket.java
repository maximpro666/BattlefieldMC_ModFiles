package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TeamTicketSyncPacket {
    private final int natoTickets;
    private final int russiaTickets;
    private final int matchTimeSeconds;
    private final int maxTickets;

    public TeamTicketSyncPacket(int natoTickets, int russiaTickets) {
        this(natoTickets, russiaTickets, -1, 100);
    }

    public TeamTicketSyncPacket(int natoTickets, int russiaTickets, int matchTimeSeconds, int maxTickets) {
        this.natoTickets = natoTickets;
        this.russiaTickets = russiaTickets;
        this.matchTimeSeconds = matchTimeSeconds;
        this.maxTickets = maxTickets;
    }

    public static void encode(TeamTicketSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.natoTickets);
        buf.writeInt(msg.russiaTickets);
        buf.writeInt(msg.matchTimeSeconds);
        buf.writeInt(msg.maxTickets);
    }

    public static TeamTicketSyncPacket decode(FriendlyByteBuf buf) {
        return new TeamTicketSyncPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(TeamTicketSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientTeamData.setTickets(msg.natoTickets, msg.russiaTickets);
            if (msg.matchTimeSeconds >= 0) {
                ClientTeamData.matchTimeSeconds = msg.matchTimeSeconds;
            }
            ClientTeamData.maxTickets = msg.maxTickets;
        });
        ctx.get().setPacketHandled(true);
    }
}
