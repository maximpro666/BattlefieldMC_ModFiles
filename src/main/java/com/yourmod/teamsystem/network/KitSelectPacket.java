package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.*;
import com.yourmod.teamsystem.data.KitConfigServerHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.yourmod.teamsystem.core.ChatHelper.*;

public class KitSelectPacket {
    private final String kitName;

    public KitSelectPacket(String kitName) {
        this.kitName = kitName != null ? kitName : "";
    }

    public KitSelectPacket(FriendlyByteBuf buf) {
        this.kitName = buf.readUtf(128);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(kitName);
    }

    private static boolean isAtTeamBase(ServerPlayer player, Team team) {
        GameManager game = TeamSystem.getGameManager();
        if (game == null) return false;
        MapConfig map = game.getCurrentMap();
        if (map == null || !map.hasTeamSpawns()) return false;
        int[] spawn = team == Team.NATO ? map.getNatoSpawn() : map.getRussiaSpawn();
        if (spawn == null) return false;
        double dx = player.getX() - (spawn[0] + 0.5);
        double dz = player.getZ() - (spawn[2] + 0.5);
        double radius = map.getBaseRadius();
        return dx * dx + dz * dz <= radius * radius;
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!RateLimiter.checkAndThrottle(player)) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.checkStringLength(kitName, 128))) return;
            if (kitName.isEmpty()) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requireTeamPlayable(player))) return;

            TeamManager tm = TeamSystem.getTeamManager();
            if (tm == null) return;
            Team playerTeam = tm.getOrCreatePlayerData(player.getUUID()).getTeam();
            if (!isAtTeamBase(player, playerTeam)) {
                player.displayClientMessage(error("You must be at your team's base to change loadout!"), false);
                return;
            }

            // Check if modern format "classId:kitId" or old format "kitName"
            if (kitName.contains(":")) {
                String[] parts = kitName.split(":", 2);
                String classId = parts[0];
                String kitId = parts[1];

                // Save kit selection FIRST so it persists through death/respawn
                PlayerCombatData pcd = tm.getOrCreatePlayerData(player.getUUID());
                pcd.setSelectedKit(kitName);
                tm.setDirty();

                String result = KitConfigServerHelper.applyKit(player, classId, kitId);
                if (result != null) {
                    player.displayClientMessage(error(result), false);
                }
            } else {
                // Old system: plain kit name
                String result = TeamSystem.getKitManager().claimKit(player, kitName, tm);
                if (result != null) {
                    player.displayClientMessage(error(result), false);
                }
            }
        });
        return true;
    }
}
