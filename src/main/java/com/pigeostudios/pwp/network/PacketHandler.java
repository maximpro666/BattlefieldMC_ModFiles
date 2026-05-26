package com.pigeostudios.pwp.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("pwp", "main"),
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

        CHANNEL.messageBuilder(TeamBaseSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(TeamBaseSyncPacket::new)
            .encoder(TeamBaseSyncPacket::toBytes)
            .consumerMainThread(TeamBaseSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(BorderSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(BorderSyncPacket::new)
            .encoder(BorderSyncPacket::toBytes)
            .consumerMainThread(BorderSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(ReloadVisualsPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(ReloadVisualsPacket::new)
            .encoder(ReloadVisualsPacket::toBytes)
            .consumerMainThread(ReloadVisualsPacket::handle)
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

        CHANNEL.messageBuilder(KitConfigSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(KitConfigSyncPacket::new)
            .encoder(KitConfigSyncPacket::toBytes)
            .consumerMainThread(KitConfigSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(TransferPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(TransferPacket::new)
            .encoder(TransferPacket::toBytes)
            .consumerMainThread(TransferPacket::handle)
            .add();

        CHANNEL.registerMessage(
            nextId++,
            VoiceChannelSwitchPacket.class,
            VoiceChannelSwitchPacket::encode,
            VoiceChannelSwitchPacket::decode,
            VoiceChannelSwitchPacket::handle
        );

        CHANNEL.messageBuilder(VoiceSpeakingStatePacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(VoiceSpeakingStatePacket::new)
            .encoder(VoiceSpeakingStatePacket::toBytes)
            .consumerMainThread(VoiceSpeakingStatePacket::handle)
            .add();

        CHANNEL.messageBuilder(WCSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(WCSyncPacket::new)
            .encoder(WCSyncPacket::toBytes)
            .consumerMainThread(WCSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(VehicleCreditsSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(VehicleCreditsSyncPacket::new)
            .encoder(VehicleCreditsSyncPacket::toBytes)
            .consumerMainThread(VehicleCreditsSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(ResupplyActionPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(ResupplyActionPacket::new)
            .encoder(ResupplyActionPacket::toBytes)
            .consumerMainThread(ResupplyActionPacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenMatchResultsPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenMatchResultsPacket::new)
            .encoder(OpenMatchResultsPacket::toBytes)
            .consumerMainThread(OpenMatchResultsPacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenVoteScreenPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenVoteScreenPacket::new)
            .encoder(OpenVoteScreenPacket::toBytes)
            .consumerMainThread(OpenVoteScreenPacket::handle)
            .add();

        CHANNEL.messageBuilder(VoteSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(VoteSyncPacket::new)
            .encoder(VoteSyncPacket::toBytes)
            .consumerMainThread(VoteSyncPacket::handle)
            .add();

        // ===== Punishment System Packets =====
        CHANNEL.messageBuilder(HWIDPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(HWIDPacket::new)
            .encoder(HWIDPacket::toBytes)
            .consumerMainThread(HWIDPacket::handle)
            .add();

        CHANNEL.messageBuilder(ReportSubmitPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(ReportSubmitPacket::new)
            .encoder(ReportSubmitPacket::toBytes)
            .consumerMainThread(ReportSubmitPacket::handle)
            .add();

        CHANNEL.messageBuilder(ReportCountSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(ReportCountSyncPacket::new)
            .encoder(ReportCountSyncPacket::toBytes)
            .consumerMainThread(ReportCountSyncPacket::handle)
            .add();

        // ===== Ticket System Packets =====
        CHANNEL.messageBuilder(TicketClaimPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(TicketClaimPacket::new)
            .encoder(TicketClaimPacket::toBytes)
            .consumerMainThread(TicketClaimPacket::handle)
            .add();

        CHANNEL.messageBuilder(TicketMessagePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(TicketMessagePacket::new)
            .encoder(TicketMessagePacket::toBytes)
            .consumerMainThread(TicketMessagePacket::handle)
            .add();

        CHANNEL.messageBuilder(TicketMessageSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(TicketMessageSyncPacket::new)
            .encoder(TicketMessageSyncPacket::toBytes)
            .consumerMainThread(TicketMessageSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(TicketListSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(TicketListSyncPacket::new)
            .encoder(TicketListSyncPacket::toBytes)
            .consumerMainThread(TicketListSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenReportScreenPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenReportScreenPacket::new)
            .encoder(OpenReportScreenPacket::toBytes)
            .consumerMainThread(OpenReportScreenPacket::handle)
            .add();

        // ===== Bleeding System Packets =====
        CHANNEL.messageBuilder(BleedingSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(BleedingSyncPacket::new)
            .encoder(BleedingSyncPacket::toBytes)
            .consumerMainThread(BleedingSyncPacket::handle)
            .add();

        CHANNEL.messageBuilder(ReviveActionPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(ReviveActionPacket::new)
            .encoder(ReviveActionPacket::toBytes)
            .consumerMainThread(ReviveActionPacket::handle)
            .add();

        // ===== ToS Agreement Packets =====
        CHANNEL.messageBuilder(OpenTOSScreenPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
            .decoder(OpenTOSScreenPacket::new)
            .encoder(OpenTOSScreenPacket::toBytes)
            .consumerMainThread(OpenTOSScreenPacket::handle)
            .add();

        CHANNEL.messageBuilder(TOSAcceptPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
            .decoder(TOSAcceptPacket::new)
            .encoder(TOSAcceptPacket::toBytes)
            .consumerMainThread(TOSAcceptPacket::handle)
            .add();
    }
}
