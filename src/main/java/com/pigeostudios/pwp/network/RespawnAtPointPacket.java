package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.core.*;
import com.pigeostudios.pwp.data.KitConfigServerHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

import static com.pigeostudios.pwp.core.ChatHelper.*;

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

            GameManager game = PWP.getGameManager();
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
        TeamManager teamManager = PWP.getTeamManager();
        if (teamManager == null) return;
        Team team = teamManager.getOrCreatePlayerData(player.getUUID()).getTeam();
        GameManager gameManager = PWP.getGameManager();
        if (gameManager == null) return;
        player.setGameMode(GameType.SURVIVAL);
        gameManager.teleportPlayerToMapAtTeamSpawn(player, team);
        applySelectedKit(player);
        closeScreen(player);
    }

    private void handleSquadmate(ServerPlayer player, ServerLevel level) {
        ServerPlayer squadmate = level.getServer().getPlayerList().getPlayer(targetUUID);
        if (squadmate == null) {
            player.sendSystemMessage(error("Squadmate is no longer available."));
            return;
        }
        if (!SquadmateRespawnCooldownManager.canSpawnOnPlayer(squadmate.serverLevel(), targetUUID)) {
            int cdTicks = SquadmateRespawnCooldownManager.getSquadmateCooldownTicks(squadmate.serverLevel(), targetUUID);
            int cdSecs  = (cdTicks + 19) / 20;
            player.sendSystemMessage(error("This squadmate is under fire! Wait " + cdSecs + "s"));
            return;
        }
        player.setGameMode(GameType.SURVIVAL);
        player.teleportTo(squadmate.serverLevel(),
                squadmate.getX(), squadmate.getY() + 1, squadmate.getZ(),
                squadmate.getYRot(), squadmate.getXRot());
        applySelectedKit(player);
        closeScreen(player);
    }

    private void handleFOB(ServerPlayer player, ServerLevel level) {
        FOBManager fobManager = PWP.getFOBManager();
        if (fobManager == null) return;

        Team playerTeam = PWP.getTeamManager().getOrCreatePlayerData(player.getUUID()).getTeam();

        FOBManager.SavedFOB target = null;
        for (FOBManager.SavedFOB fob : fobManager.getFOBs()) {
            if (fob.name.equals(targetName) && fob.teamOrdinal == playerTeam.ordinal()) {
                target = fob;
                break;
            }
        }
        if (target == null) {
            player.sendSystemMessage(error("FOB not found: " + targetName));
            return;
        }

        ServerLevel dest = level.getServer().getLevel(
                ResourceKey.create(Registries.DIMENSION, new ResourceLocation(target.dimension)));
        if (dest == null) {
            player.sendSystemMessage(error("Cannot reach FOB dimension"));
            return;
        }

        if (!SquadmateRespawnCooldownManager.canSpawnOnFOB(dest, target.fobId)) {
            int cdTicks = SquadmateRespawnCooldownManager.getFobCooldownTicks(dest, target.fobId);
            int cdSecs  = (cdTicks + 19) / 20;
            player.sendSystemMessage(error("This FOB is under attack! Wait " + cdSecs + "s"));
            return;
        }

        player.setGameMode(GameType.SURVIVAL);
        player.teleportTo(dest, target.x + 0.5, target.y + 1, target.z + 0.5,
                player.getYRot(), player.getXRot());
        applySelectedKit(player);
        closeScreen(player);
    }

    private void handleBeacon(ServerPlayer player) {
        RespawnManager respawnManager = PWP.getRespawnManager();
        if (respawnManager == null) return;
        player.setGameMode(GameType.SURVIVAL);
        respawnManager.respawnPlayerAtBeacon(player, targetName);
        applySelectedKit(player);
        closeScreen(player);
    }

    private static void applySelectedKit(ServerPlayer player) {
        TeamManager tm = PWP.getTeamManager();
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
