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

    public static void register() {
        // ===== Original packets =====
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

        // ===== Builder-style packets (original) =====
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

        CHANNEL.messageBuilder(BCSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(BCSyncPacket::new)
            .encoder(BCSyncPacket::toBytes)
            .consumerMainThread(BCSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(SPSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SPSyncPacket::new)
            .encoder(SPSyncPacket::toBytes)
            .consumerMainThread(SPSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(DownedSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(DownedSyncPacket::new)
            .encoder(DownedSyncPacket::toBytes)
            .consumerMainThread(DownedSyncPacket::handle)
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

        // ===== New packets =====
        CHANNEL.messageBuilder(ConfigSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(ConfigSyncPacket::new)
            .encoder(ConfigSyncPacket::toBytes)
            .consumerMainThread(ConfigSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(FOBSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(FOBSyncPacket::new)
            .encoder(FOBSyncPacket::toBytes)
            .consumerMainThread(FOBSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(SoundPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SoundPacket::new)
            .encoder(SoundPacket::toBytes)
            .consumerMainThread(SoundPacket::handle)
            .add();

        CHANNEL.messageBuilder(KitDataSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(KitDataSyncPacket::new)
            .encoder(KitDataSyncPacket::toBytes)
            .consumerMainThread(KitDataSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(VehicleDataSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(VehicleDataSyncPacket::new)
            .encoder(VehicleDataSyncPacket::toBytes)
            .consumerMainThread(VehicleDataSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(NotificationPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(NotificationPacket::new)
            .encoder(NotificationPacket::toBytes)
            .consumerMainThread(NotificationPacket::handle)
            .add();

        // ===== C2S packets =====
        CHANNEL.messageBuilder(TeamChangeRequestPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(TeamChangeRequestPacket::new)
            .encoder(TeamChangeRequestPacket::toBytes)
            .consumerMainThread(TeamChangeRequestPacket::handle)
            .add();

        CHANNEL.messageBuilder(KitSelectPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(KitSelectPacket::new)
            .encoder(KitSelectPacket::toBytes)
            .consumerMainThread(KitSelectPacket::handle)
            .add();

        CHANNEL.messageBuilder(VehicleDeployPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(VehicleDeployPacket::new)
            .encoder(VehicleDeployPacket::toBytes)
            .consumerMainThread(VehicleDeployPacket::handle)
            .add();

        CHANNEL.messageBuilder(FOBPlacePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(FOBPlacePacket::new)
            .encoder(FOBPlacePacket::toBytes)
            .consumerMainThread(FOBPlacePacket::handle)
            .add();
    }
}
