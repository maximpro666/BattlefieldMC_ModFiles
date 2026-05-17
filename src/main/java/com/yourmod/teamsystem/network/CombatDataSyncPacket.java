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
    private final String prefix;
    private final String suffix;
    private final String displayName;

    public CombatDataSyncPacket(int teamOrdinal, int kills, int deaths) {
        this(teamOrdinal, kills, deaths, "", "", "");
    }

    public CombatDataSyncPacket(int teamOrdinal, int kills, int deaths, String prefix, String suffix, String displayName) {
        this.teamOrdinal = teamOrdinal;
        this.kills = kills;
        this.deaths = deaths;
        this.prefix = prefix != null ? prefix : "";
        this.suffix = suffix != null ? suffix : "";
        this.displayName = displayName != null ? displayName : "";
    }

    public static void encode(CombatDataSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.teamOrdinal);
        buf.writeInt(msg.kills);
        buf.writeInt(msg.deaths);
        buf.writeUtf(msg.prefix);
        buf.writeUtf(msg.suffix);
        buf.writeUtf(msg.displayName);
    }

    public static CombatDataSyncPacket decode(FriendlyByteBuf buf) {
        return new CombatDataSyncPacket(
            buf.readInt(), buf.readInt(), buf.readInt(),
            buf.readUtf(), buf.readUtf(), buf.readUtf()
        );
    }

    public static void handle(CombatDataSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientTeamData.setLocalPlayerData(
                Team.fromOrdinal(msg.teamOrdinal), msg.kills, msg.deaths,
                msg.prefix, msg.suffix, msg.displayName
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
