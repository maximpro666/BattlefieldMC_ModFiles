package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

import static com.pigeostudios.pwp.core.ChatHelper.*;

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
            if (player == null) return;

            if (!RateLimiter.checkAndThrottle(player)) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.checkStringLength(action, 32))) return;
            if (action.isEmpty()) return;

            var squadManager = PWP.getSquadManager();
            if (squadManager == null) return;

            String normalized = action.toUpperCase();

            switch (normalized) {
                case "CREATE" -> {
                    if (!PacketValidator.checkAndReject(player, PacketValidator.requirePlaying(PWP.getGameManager()))) return;
                    if (!PacketValidator.checkAndReject(player, PacketValidator.requireTeamPlayable(player))) return;
                    var team = PWP.getTeamManager().getOrCreatePlayerData(player.getUUID()).getTeam();
                    squadManager.createSquad(player.getScoreboardName() + "'s Squad", team, player.getUUID());
                    squadManager.syncToAll(player.server);
                }
                case "LEAVE" -> {
                    if (!PacketValidator.checkAndReject(player, PacketValidator.requireSquadMember(player))) return;
                    squadManager.leaveSquad(player.getUUID());
                    squadManager.syncToAll(player.server);
                }
                case "INVITE" -> {
                    if (!PacketValidator.checkAndReject(player, PacketValidator.requireSquadMember(player))) return;
                    if (target == null) return;
                    var squad = squadManager.getPlayerSquad(player.getUUID());
                    if (squad != null) squadManager.invitePlayer(target, squad);
                }
                case "KICK" -> {
                    if (!PacketValidator.checkAndReject(player, PacketValidator.requireSquadLeader(player))) return;
                    if (target == null) return;
                    squadManager.kickMember(player.getUUID(), target);
                }
                case "PROMOTE" -> {
                    if (!PacketValidator.checkAndReject(player, PacketValidator.requireSquadLeader(player))) return;
                    if (target == null) return;
                    squadManager.promoteLeader(player.getUUID(), target);
                }
                default -> player.sendSystemMessage(error("Unknown squad action"));
            }
        });
        return true;
    }
}
