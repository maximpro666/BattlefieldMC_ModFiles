package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.DownedSyncPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;
import java.util.stream.Collectors;

public class DownedManager {
    private final Map<UUID, DownedData> downedPlayers;
    private static final double REVIVE_SPEED = 10.0;
    private static final double REVIVE_RANGE = 3.0;

    public DownedManager() {
        this.downedPlayers = new HashMap<>();
    }

    public boolean isDowned(UUID playerUUID) {
        return downedPlayers.containsKey(playerUUID);
    }

    public DownedData getDownedData(UUID playerUUID) {
        return downedPlayers.get(playerUUID);
    }

    public void setDowned(ServerPlayer player, ServerPlayer downer) {
        UUID uuid = player.getUUID();
        String dim = player.serverLevel().dimension().location().toString();
        DownedData data = new DownedData(uuid, player.getName().getString(),
            player.getX(), player.getY(), player.getZ(), dim,
            downer != null ? downer.getUUID() : null);
        downedPlayers.put(uuid, data);
        syncDownedToPlayer(player);
        syncDownedToPlayer(downer);
    }

    public void removeDowned(UUID playerUUID) {
        downedPlayers.remove(playerUUID);
    }

    public boolean tryRevive(ServerPlayer reviver, UUID downedUUID) {
        DownedData data = downedPlayers.get(downedUUID);
        if (data == null) return false;

        if (reviver.distanceToSqr(data.getX(), data.getY(), data.getZ()) > REVIVE_RANGE * REVIVE_RANGE) {
            return false;
        }

        data.addReviveProgress(REVIVE_SPEED);
        if (data.isRevived()) {
            downedPlayers.remove(downedUUID);
            ServerPlayer downedPlayer = reviver.server.getPlayerList().getPlayer(downedUUID);
            if (downedPlayer != null) {
                ContributionManager cm = TeamSystem.getContributionManager();
                if (cm != null) cm.addRevive(reviver.getUUID(), reviver.getName().getString());
            }
            return true;
        }
        return false;
    }

    private final Set<UUID> bleedoutKills = new HashSet<>();

    public boolean isBleedoutKill(UUID uuid) {
        return bleedoutKills.contains(uuid);
    }

    public void clearBleedoutKill(UUID uuid) {
        bleedoutKills.remove(uuid);
    }

    public void tickDowned() {
        if (downedPlayers.isEmpty()) return;
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, DownedData> entry : downedPlayers.entrySet()) {
            entry.getValue().tickBleedout();
            if (entry.getValue().isBleedoutDead()) {
                toRemove.add(entry.getKey());
            }
        }

        for (UUID uuid : toRemove) {
            downedPlayers.remove(uuid);
            ServerPlayer player = TeamSystem.getGameManager().getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                bleedoutKills.add(uuid);
                player.hurt(player.damageSources().generic(), 100.0F);
            }
        }
    }

    public List<DownedData> getDownedInRadius(double x, double y, double z, double radius) {
        double radiusSq = radius * radius;
        return downedPlayers.values().stream()
            .filter(d -> d.distanceToSqr(x, y, z) <= radiusSq)
            .collect(Collectors.toList());
    }

    public List<DownedData> getAllDowned() {
        return new ArrayList<>(downedPlayers.values());
    }

    public void syncDownedToPlayer(ServerPlayer player) {
        if (player == null) return;
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new DownedSyncPacket(new ArrayList<>(downedPlayers.values())));
    }

    public void syncDownedToAll() {
        List<DownedData> all = new ArrayList<>(downedPlayers.values());
        for (ServerPlayer player : TeamSystem.getGameManager().getServer().getPlayerList().getPlayers()) {
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new DownedSyncPacket(all));
        }
    }

    public void clearAll() {
        downedPlayers.clear();
    }
}
