package com.yourmod.teamsystem.network;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.data.KitConfig.KitDef;
import com.yourmod.teamsystem.data.KitConfig.ClassConfig;
import com.yourmod.teamsystem.data.KitConfig.AttachmentLimit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KitAdminSavePacket {
    private final String configJson;

    public KitAdminSavePacket(String configJson) {
        this.configJson = configJson != null ? configJson : "{}";
    }

    public KitAdminSavePacket(FriendlyByteBuf buf) {
        this.configJson = buf.readUtf(65535);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(configJson);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§cNo permission"));
                return;
            }

            try {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();

                KitConfig parsed = gson.fromJson(configJson, KitConfig.class);
                if (parsed == null) {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§cInvalid kit config JSON"));
                    return;
                }

                java.nio.file.Path worldDir = player.server.getWorldPath(LevelResource.ROOT);
                java.nio.file.Path dir = worldDir.resolve("teamsystem");
                java.nio.file.Files.createDirectories(dir);
                java.nio.file.Files.writeString(dir.resolve("kits.json"), gson.toJson(parsed));

                KitConfig.set(parsed);

                TeamSystem.LOGGER.info("Kit config saved by {} ({} bytes)", player.getName().getString(), configJson.length());

                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§aKit config saved successfully"));

                KitAdminConfigSyncPacket syncBack = new KitAdminConfigSyncPacket(configJson);
                com.yourmod.teamsystem.network.PacketHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), syncBack);

            } catch (Exception e) {
                TeamSystem.LOGGER.error("Failed to save kit config from {}: {}", player.getName().getString(), e.getMessage());
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§cFailed to save kit config: " + e.getMessage()));
            }
        });
        return true;
    }
}
