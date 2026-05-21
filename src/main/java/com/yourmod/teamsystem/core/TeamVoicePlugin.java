package com.yourmod.teamsystem.core;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.VoiceSpeakingStatePacket;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamVoicePlugin implements VoicechatPlugin {

    private static VoiceGroupManager groupManager;
    private static final Map<UUID, Long> lastSpeakingBroadcast = new ConcurrentHashMap<>();
    private static final long SPEAKING_BROADCAST_INTERVAL_MS = 200;
    private static final double MAX_VOICE_DISTANCE_SQ = 48.0 * 48.0;

    @Override
    public String getPluginId() {
        return "teamsystem";
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi serverApi) {
            groupManager = new VoiceGroupManager();
            groupManager.init(serverApi);
            TeamSystem.LOGGER.info("TeamVoicePlugin initialized with SV API");
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return;
        Player svPlayer = sender.getPlayer();
        if (svPlayer == null) return;
        UUID senderUUID = svPlayer.getUuid();
        if (senderUUID == null) return;

        long now = System.currentTimeMillis();
        Long last = lastSpeakingBroadcast.get(senderUUID);
        if (last != null && now - last < SPEAKING_BROADCAST_INTERVAL_MS) return;
        lastSpeakingBroadcast.put(senderUUID, now);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerPlayer senderPlayer = server.getPlayerList().getPlayer(senderUUID);
        if (senderPlayer == null) return;
        String senderName = senderPlayer.getName().getString();

        Group senderGroup = sender.getGroup();

        int channel = VoiceGroupManager.CHANNEL_LOCAL;
        if (senderGroup != null) {
            String gname = senderGroup.getName();
            channel = gname.startsWith("ts_squad") ? VoiceGroupManager.CHANNEL_SQUAD : VoiceGroupManager.CHANNEL_TEAM;
        }

        VoiceSpeakingStatePacket packet = new VoiceSpeakingStatePacket(senderUUID, senderName, channel);

        // Send packet on main thread to avoid blocking SVC keepalive
        server.execute(() -> {
            VoicechatServerApi serverApi = groupManager != null ? groupManager.getApi() : null;
            if (serverApi == null) return;
            for (ServerPlayer listener : server.getPlayerList().getPlayers()) {
                if (listener.getUUID().equals(senderUUID)) continue;

                VoicechatConnection conn = serverApi.getConnectionOf(listener.getUUID());
                if (conn == null || conn.isDisabled()) continue;

                boolean canHear;
                if (senderGroup == null) {
                    Group listenerGroup = conn.getGroup();
                    canHear = listenerGroup == null
                        && listener.distanceToSqr(senderPlayer) <= MAX_VOICE_DISTANCE_SQ;
                } else {
                    Group listenerGroup = conn.getGroup();
                    canHear = listenerGroup != null
                        && listenerGroup.getId().equals(senderGroup.getId());
                }

                if (canHear) {
                    PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> listener), packet);
                }
            }
        });
    }

    public static VoiceGroupManager getGroupManager() {
        return groupManager;
    }
}
