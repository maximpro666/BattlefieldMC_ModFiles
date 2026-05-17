package com.yourmod.teamsystem.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
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

    // Protocol version "2": TeamSyncPacket (team ordinal), CombatDataSyncPacket (team ordinal, kills, deaths, prefix, suffix, displayName), TeamTicketSyncPacket
    // Do not increment version unless packet format changes (breaking compatibility).
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

        CHANNEL.registerMessage(
            nextId++,
            TeamTicketSyncPacket.class,
            TeamTicketSyncPacket::encode,
            TeamTicketSyncPacket::decode,
            TeamTicketSyncPacket::handle
        );

        CHANNEL.registerMessage(
            nextId++,
            GameStateSyncPacket.class,
            GameStateSyncPacket::encode,
            GameStateSyncPacket::decode,
            GameStateSyncPacket::handle
        );

        CHANNEL.registerMessage(
            nextId++,
            MarkerSyncPacket.class,
            MarkerSyncPacket::encode,
            MarkerSyncPacket::decode,
            MarkerSyncPacket::handle
        );

        CHANNEL.messageBuilder(RankSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(RankSyncPacket::new)
            .encoder(RankSyncPacket::toBytes)
            .consumerMainThread(RankSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(SquadSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SquadSyncPacket::new)
            .encoder(SquadSyncPacket::toBytes)
            .consumerMainThread(SquadSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(KitSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(KitSyncPacket::new)
            .encoder(KitSyncPacket::toBytes)
            .consumerMainThread(KitSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(VehicleSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(VehicleSyncPacket::new)
            .encoder(VehicleSyncPacket::toBytes)
            .consumerMainThread(VehicleSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(CapturePointSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(CapturePointSyncPacket::new)
            .encoder(CapturePointSyncPacket::toBytes)
            .consumerMainThread(CapturePointSyncPacket::handle)
            .add();
    }
}
