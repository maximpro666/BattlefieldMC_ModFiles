package com.yourmod.teamsystem.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("teamsystem", "main"),
        () -> PROTOCOL_VERSION,
        NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION::equals),
        NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION::equals)
    );

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(
            nextId++,
            CombatDataSyncPacket.class,
            CombatDataSyncPacket::encode,
            CombatDataSyncPacket::decode,
            CombatDataSyncPacket::handle
        );

        CHANNEL.registerMessage(
            nextId++,
            TeamSyncPacket.class,
            TeamSyncPacket::encode,
            TeamSyncPacket::decode,
            TeamSyncPacket::handle
        );
    }
}
