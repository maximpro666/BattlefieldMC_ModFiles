package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CombatDataSyncPacket {
    private final UUID playerId;
    private final int teamOrdinal;
    private final int kills;
    private final int deaths;
    private final String prefix;
    private final String suffix;
    private final String displayName;
    private final String callsign;
    private final int rankOrdinal;

    public CombatDataSyncPacket(UUID playerId, int teamOrdinal, int kills, int deaths, String prefix, String suffix, String displayName, String callsign, int rankOrdinal) {
        this.playerId = playerId;
        this.teamOrdinal = teamOrdinal;
        this.kills = kills;
        this.deaths = deaths;
        this.prefix = prefix != null ? prefix : "";
        this.suffix = suffix != null ? suffix : "";
        this.displayName = displayName != null ? displayName : "";
        this.callsign = callsign != null ? callsign : "";
        this.rankOrdinal = rankOrdinal;
    }

    public static void encode(CombatDataSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeInt(msg.teamOrdinal);
        buf.writeInt(msg.kills);
        buf.writeInt(msg.deaths);
        buf.writeUtf(msg.prefix);
        buf.writeUtf(msg.suffix);
        buf.writeUtf(msg.displayName);
        buf.writeUtf(msg.callsign);
        buf.writeInt(msg.rankOrdinal);
    }

    public static CombatDataSyncPacket decode(FriendlyByteBuf buf) {
        return new CombatDataSyncPacket(
            buf.readUUID(), buf.readInt(), buf.readInt(), buf.readInt(),
            buf.readUtf(), buf.readUtf(), buf.readUtf(),
            buf.readUtf(), buf.readInt()
        );
    }

    public static void handle(CombatDataSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            UUID localId = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getUUID() : null;
            if (msg.playerId.equals(localId)) {
                ClientTeamData.setLocalPlayerData(
                    Team.fromOrdinal(msg.teamOrdinal), msg.kills, msg.deaths,
                    msg.prefix, msg.suffix, msg.displayName
                );
            }
            ClientTeamData.playerDataMap.put(msg.playerId, new PlayerListEntry(
                msg.rankOrdinal, msg.callsign, "", msg.kills, msg.deaths, msg.teamOrdinal
            ));
        });
        ctx.get().setPacketHandled(true);
    }
}
