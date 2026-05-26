package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TOSAcceptPacket {
    public TOSAcceptPacket() {}

    public TOSAcceptPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            var teamManager = PWP.getTeamManager();
            if (teamManager == null) return;
            var pcd = teamManager.getOrCreatePlayerData(player.getUUID());
            if (pcd.hasAcceptedTOS()) return;

            pcd.setHasAcceptedTOS(true);
            teamManager.setDirty();
            PWP.LOGGER.info("Player {} accepted ToS/Privacy Policy", player.getName().getString());

            var game = PWP.getGameManager();
            if (game == null) return;

            teamManager.fullSyncPlayer(player);
            game.syncPhaseToPlayer(player);

            var cp = PWP.getCapturePointManager();
            if (cp != null) cp.syncToPlayer(player);

            var mm = PWP.getMarkerManager();
            if (mm != null) mm.syncToPlayer(player);

            var team = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
            if (game.isPlaying() && game.isMapReady() && team != null && team.isPlayable()) {
                game.teleportPlayerToMapAtTeamSpawn(player, team);
                game.setMapRespawn(player, team);
            } else {
                game.teleportPlayerToLobby(player);
                game.setLobbyRespawn(player);
            }
        });
        return true;
    }
}
