package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.data.KitConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KitAdminRequestPacket {

    public KitAdminRequestPacket() {
    }

    public KitAdminRequestPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!player.hasPermissions(2)) return;

            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            KitConfig cfg = KitConfig.get();
            if (cfg == null) {
                java.nio.file.Path worldDir = player.server.getWorldPath(LevelResource.ROOT);
                cfg = KitConfig.loadOrCreate(worldDir);
            }
            String json = gson.toJson(cfg);
            PacketHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new KitAdminConfigSyncPacket(json));
        });
        return true;
    }
}
