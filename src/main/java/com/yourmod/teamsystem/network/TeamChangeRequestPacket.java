package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.yourmod.teamsystem.core.ChatHelper.*;

public class TeamChangeRequestPacket {
    private final int teamOrdinal;

    public TeamChangeRequestPacket(int teamOrdinal) {
        this.teamOrdinal = teamOrdinal;
    }

    public TeamChangeRequestPacket(FriendlyByteBuf buf) {
        this.teamOrdinal = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(teamOrdinal);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!RateLimiter.checkAndThrottle(player)) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.checkStringLength(String.valueOf(teamOrdinal), 4))) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requireValidTeamOrdinal(teamOrdinal))) return;

            GameManager game = TeamSystem.getGameManager();
            if (game != null && game.isPlaying()) {
                PacketValidator.reject(player, "Cannot change team while game is running");
                return;
            }

            Team team = Team.fromOrdinal(teamOrdinal);
            if (team == Team.SPECTATOR) return;
            TeamSystem.getTeamManager().setPlayerTeam(player, team);
        });
        return true;
    }
}
