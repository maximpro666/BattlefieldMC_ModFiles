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

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!RateLimiter.checkAndThrottle(player)) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.checkStringLength(kitName, 128))) return;
            if (kitName.isEmpty()) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requireTeamPlayable(player))) return;

            // Check if modern format "classId:kitId" or old format "kitName"
            if (kitName.contains(":")) {
                String[] parts = kitName.split(":", 2);
                String classId = parts[0];
                String kitId = parts[1];
                String result = KitConfigServerHelper.applyKit(player, classId, kitId);
                if (result != null) {
                    player.displayClientMessage(error(result), false);
                } else {
                    TeamManager tm = TeamSystem.getTeamManager();
                    if (tm != null) {
                        PlayerCombatData pcd = tm.getOrCreatePlayerData(player.getUUID());
                        pcd.setSelectedKit(kitName);
                        tm.setDirty();
                    }
                }
            } else {
                // Old system: plain kit name
                String result = TeamSystem.getKitManager().claimKit(player, kitName, TeamSystem.getTeamManager());
                if (result != null) {
                    player.displayClientMessage(error(result), false);
                }
            }
        });
        return true;
    }
}
