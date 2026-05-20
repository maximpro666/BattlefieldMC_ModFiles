package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.*;
import com.yourmod.teamsystem.data.KitConfigServerHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

import static com.yourmod.teamsystem.core.ChatHelper.*;

public class RespawnAtPointPacket {

    public static final String TYPE_BASE      = "BASE";
    public static final String TYPE_SQUADMATE = "SQUADMATE";
    public static final String TYPE_FOB       = "FOB";
    public static final String TYPE_BEACON    = "BEACON";

    private final String type;
    private final UUID   targetUUID;
    private final String targetName;

    public RespawnAtPointPacket(String type, UUID targetUUID, String targetName) {
        this.type       = type;
        this.targetUUID = targetUUID != null ? targetUUID : new UUID(0, 0);
        this.targetName = targetName != null ? targetName : "";
    }

    public RespawnAtPointPacket(FriendlyByteBuf buf) {
        this.type       = buf.readUtf(32);
        this.targetUUID = buf.readUUID();
        this.targetName = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(type, 32);
        buf.writeUUID(targetUUID);
        buf.writeUtf(targetName, 64);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!RateLimiter.checkAndThrottle(player)) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.checkStringLength(type, 32))) return;

            GameManager game = TeamSystem.getGameManager();
            if (!PacketValidator.checkAndReject(player, PacketValidator.requirePlaying(game))) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requireTeamPlayable(player))) return;

            ServerLevel level = player.serverLevel();

            switch (type) {
                case TYPE_BASE -> handleBase(player);
                case TYPE_SQUADMATE -> handleSquadmate(player, level);
                case TYPE_FOB -> handleFOB(player, level);
                case TYPE_BEACON -> handleBeacon(player);
                default -> player.sendSystemMessage(error("Unknown spawn type: " + type));
            }
        });
        context.setPacketHandled(true);
        return true;
    }

    private void handleBase(ServerPlayer player) {
        TeamManager teamManager = TeamSystem.getTeamManager();
        if (teamManager == null) return;
        Team team = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        GameManager gameManager = TeamSystem.getGameManager();
        if (gameManager == null) return;
        player.setGameMode(GameType.SURVIVAL);
        gameManager.teleportPlayerToMapAtTeamSpawn(player, team);
        applySelectedKit(player);
        closeScreen(player);
    }

    private void handleSquadmate(ServerPlayer player, ServerLevel level) {
        if (!SquadmateRespawnCooldownManager.canSpawnOnPlayer(level, targetUUID)) {
            int cdTicks = SquadmateRespawnCooldownManager.getSquadmateCooldownTicks(level, targetUUID);
            int cdSecs  = (cdTicks + 19) / 20;
            player.sendSystemMessage(error("This squadmate is under fire! Wait " + cdSecs + "s"));
            return;
        }
        ServerPlayer squadmate = level.getServer().getPlayerList().getPlayer(targetUUID);
        if (squadmate == null) {
            player.sendSystemMessage(error("Squadmate is no longer available."));
            return;
        }
        player.setGameMode(GameType.SURVIVAL);
        player.teleportTo(level,
                squadmate.getX(), squadmate.getY(), squadmate.getZ(),
                squadmate.getYRot(), squadmate.getXRot());
        applySelectedKit(player);
        closeScreen(player);
    }

    private void handleFOB(ServerPlayer player, ServerLevel level) {
        FOBManager fobManager = TeamSystem.getFOBManager();
        if (fobManager == null) return;

        FOBManager.SavedFOB target = null;
        for (FOBManager.SavedFOB fob : fobManager.getFOBs()) {
            if (fob.name.equals(targetName)) {
                target = fob;
                break;
            }
        }
        if (target == null) {
            player.sendSystemMessage(error("FOB not found: " + targetName));
            return;
        }

        if (!SquadmateRespawnCooldownManager.canSpawnOnFOB(level, target.fobId)) {
            int cdTicks = SquadmateRespawnCooldownManager.getFobCooldownTicks(level, target.fobId);
            int cdSecs  = (cdTicks + 19) / 20;
            player.sendSystemMessage(error("This FOB is under attack! Wait " + cdSecs + "s"));
            return;
        }

        player.setGameMode(GameType.SURVIVAL);
        player.teleportTo(level, target.x + 0.5, target.y + 1, target.z + 0.5,
                player.getYRot(), player.getXRot());
        applySelectedKit(player);
        closeScreen(player);
    }

    private void handleBeacon(ServerPlayer player) {
        RespawnManager respawnManager = TeamSystem.getRespawnManager();
        if (respawnManager == null) return;
        player.setGameMode(GameType.SURVIVAL);
        respawnManager.respawnPlayerAtBeacon(player, targetName);
        applySelectedKit(player);
        closeScreen(player);
    }

    private static void applySelectedKit(ServerPlayer player) {
        TeamManager tm = TeamSystem.getTeamManager();
        if (tm == null) return;
        PlayerCombatData pcd = tm.getOrCreatePlayerData(player.getUUID());
        String kitName = pcd.getSelectedKit();
        if (kitName == null || kitName.isEmpty() || !kitName.contains(":")) return;
        player.setHealth(player.getMaxHealth());
        String[] parts = kitName.split(":", 2);
        KitConfigServerHelper.applyKit(player, parts[0], parts[1]);
    }

    private static void closeScreen(ServerPlayer player) {
        PacketHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new CloseSpawnScreenPacket());
    }
}
