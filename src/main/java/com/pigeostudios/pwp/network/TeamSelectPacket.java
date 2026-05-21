package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class TeamSelectPacket {
    private final int teamOrdinal;

    public TeamSelectPacket(int teamOrdinal) {
        this.teamOrdinal = teamOrdinal;
    }

    public TeamSelectPacket(FriendlyByteBuf buf) {
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
            Team team = Team.fromOrdinal(teamOrdinal);
            if (team == Team.SPECTATOR) return;
            PWP.getTeamManager().setPlayerTeam(player, team);
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenLoadoutScreenPacket());
        });
        return true;
    }
}
