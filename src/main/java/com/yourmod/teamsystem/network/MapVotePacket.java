package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MapVotePacket {
    private final String mapName;

    public MapVotePacket(String mapName) {
        this.mapName = mapName != null ? mapName : "";
    }

    public MapVotePacket(FriendlyByteBuf buf) {
        this.mapName = buf.readUtf(128);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(mapName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!RateLimiter.checkAndThrottle(player)) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.checkStringLength(mapName, 128))) return;
            if (mapName.isEmpty()) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requireVoting(TeamSystem.getGameManager()))) return;

            TeamSystem.getGameManager().voteMap(player, mapName);
        });
        return true;
    }
}
