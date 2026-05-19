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

        // ===== GUI bridge C2S packets =====
        CHANNEL.messageBuilder(TeamSelectPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(TeamSelectPacket::new)
            .encoder(TeamSelectPacket::toBytes)
            .consumerMainThread(TeamSelectPacket::handle)
            .add();

        CHANNEL.messageBuilder(VehicleSpawnPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(VehicleSpawnPacket::new)
            .encoder(VehicleSpawnPacket::toBytes)
            .consumerMainThread(VehicleSpawnPacket::handle)
            .add();

        CHANNEL.messageBuilder(MapVotePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(MapVotePacket::new)
            .encoder(MapVotePacket::toBytes)
            .consumerMainThread(MapVotePacket::handle)
            .add();

        CHANNEL.messageBuilder(SquadActionPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(SquadActionPacket::new)
            .encoder(SquadActionPacket::toBytes)
            .consumerMainThread(SquadActionPacket::handle)
            .add();

        CHANNEL.messageBuilder(KitSavePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(KitSavePacket::new)
            .encoder(KitSavePacket::toBytes)
            .consumerMainThread(KitSavePacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenTeamSelectionScreenPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenTeamSelectionScreenPacket::new)
            .encoder(OpenTeamSelectionScreenPacket::toBytes)
            .consumerMainThread(OpenTeamSelectionScreenPacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenLoadoutScreenPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenLoadoutScreenPacket::new)
            .encoder(OpenLoadoutScreenPacket::toBytes)
            .consumerMainThread(OpenLoadoutScreenPacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenAdminPanelPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenAdminPanelPacket::new)
            .encoder(OpenAdminPanelPacket::toBytes)
            .consumerMainThread(OpenAdminPanelPacket::handle)
            .add();

        CHANNEL.messageBuilder(KitAdminConfigSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(KitAdminConfigSyncPacket::new)
            .encoder(KitAdminConfigSyncPacket::toBytes)
            .consumerMainThread(KitAdminConfigSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(KitAdminSavePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(KitAdminSavePacket::new)
            .encoder(KitAdminSavePacket::toBytes)
            .consumerMainThread(KitAdminSavePacket::handle)
            .add();

        CHANNEL.messageBuilder(KitAdminRequestPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(KitAdminRequestPacket::new)
            .encoder(KitAdminRequestPacket::toBytes)
            .consumerMainThread(KitAdminRequestPacket::handle)
            .add();

        // ===== Spawn System Packets =====
        CHANNEL.messageBuilder(RespawnBeaconSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(RespawnBeaconSyncPacket::new)
            .encoder(RespawnBeaconSyncPacket::toBytes)
            .consumerMainThread(RespawnBeaconSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(SquadmateStatusSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SquadmateStatusSyncPacket::new)
            .encoder(SquadmateStatusSyncPacket::toBytes)
            .consumerMainThread(SquadmateStatusSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenSpawnSelectionScreenPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenSpawnSelectionScreenPacket::new)
            .encoder(OpenSpawnSelectionScreenPacket::toBytes)
            .consumerMainThread(OpenSpawnSelectionScreenPacket::handle)
            .add();

        CHANNEL.messageBuilder(CloseSpawnScreenPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(CloseSpawnScreenPacket::new)
            .encoder(CloseSpawnScreenPacket::toBytes)
            .consumerMainThread(CloseSpawnScreenPacket::handle)
            .add();

        CHANNEL.messageBuilder(RespawnAtPointPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(RespawnAtPointPacket::new)
            .encoder(RespawnAtPointPacket::toBytes)
            .consumerMainThread(RespawnAtPointPacket::handle)
            .add();
    }
}
