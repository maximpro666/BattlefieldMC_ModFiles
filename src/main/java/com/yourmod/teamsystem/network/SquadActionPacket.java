package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SquadActionPacket {
    private final String action;
    private final UUID target;

    public SquadActionPacket(String action, UUID target) {
        this.action = action != null ? action : "";
        this.target = target != null ? target : new UUID(0, 0);
    }

    public SquadActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readUtf(32);
        this.target = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(action);
        buf.writeUUID(target);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || action.isEmpty()) return;
            var squadManager = TeamSystem.getSquadManager();
            if (squadManager == null) return;
            switch (action.toUpperCase()) {
                case "CREATE" -> {
                    var team = TeamSystem.getTeamManager().getOrCreatePlayerData(player.getUUID()).getTeam();
                    squadManager.createSquad(player.getScoreboardName() + "'s Squad", team, player.getUUID());
                }
                case "LEAVE" -> squadManager.leaveSquad(player.getUUID());
                case "INVITE" -> {
                    if (target != null) {
                        var squad = squadManager.getPlayerSquad(player.getUUID());
                        if (squad != null) squadManager.invitePlayer(target, squad);
                    }
                }
                case "KICK" -> {
                    if (target != null) squadManager.kickMember(player.getUUID(), target);
                }
                case "PROMOTE" -> {
                    if (target != null) squadManager.promoteLeader(player.getUUID(), target);
                }
                default -> TeamSystem.LOGGER.warn("Unknown squad action: {}", action);
            }
        });
        return true;
    }
}
