package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Player;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ForgeVoicechatPlugin
public class TeamVoicePlugin implements VoicechatPlugin {

    private static VoiceGroupManager groupManager;
    static final Map<UUID, VoiceSpeakerState> activeSpeakers = new ConcurrentHashMap<>();
    static final long SERVER_SPEAKER_TIMEOUT_MS = 600;

    private static final Map<UUID, Map<UUID, StaticAudioChannel>> channelCache = new ConcurrentHashMap<>();
    private static final Map<UUID, OpusEncoder> senderEncoders = new ConcurrentHashMap<>();
    private static long lastEncoderCleanup = 0L;

    static final class VoiceSpeakerState {
        final String name;
        final int channel;
        long lastSeen;

        VoiceSpeakerState(String name, int channel) {
            this.name = name;
            this.channel = channel;
            this.lastSeen = System.currentTimeMillis();
        }
    }

    static void cleanupStaleSpeakers() {
        long now = System.currentTimeMillis();
        activeSpeakers.values().removeIf(s -> now - s.lastSeen > SERVER_SPEAKER_TIMEOUT_MS);
    }

    static void cleanupStaleEncoders() {
        long now = System.currentTimeMillis();
        if (now - lastEncoderCleanup < 300_000L) return;
        lastEncoderCleanup = now;
        if (!senderEncoders.isEmpty()) {
            PWP.LOGGER.debug("Voice encoder cleanup skipped during active match");
        }
    }

    @Override
    public String getPluginId() {
        return "pwp";
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi serverApi) {
            groupManager = new VoiceGroupManager();
            groupManager.init(serverApi);
        }
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection senderConn = event.getSenderConnection();
        if (senderConn == null) return;
        Player svPlayer = senderConn.getPlayer();
        if (svPlayer == null) return;
        UUID senderUUID = svPlayer.getUuid();
        if (senderUUID == null) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer senderPlayer = server.getPlayerList().getPlayer(senderUUID);
        if (senderPlayer == null) return;

        int channel = groupManager != null ? groupManager.getPlayerChannel(senderUUID) : VoiceGroupManager.CHANNEL_LOCAL;
        String senderName = senderPlayer.getName().getString();

        activeSpeakers.put(senderUUID, new VoiceSpeakerState(senderName, channel));

        if (channel == VoiceGroupManager.CHANNEL_LOCAL) return;

        event.cancel();

        VoicechatServerApi api = event.getVoicechat();
        byte[] opusData = event.getPacket().getOpusEncodedData();
        if (opusData == null || opusData.length == 0) return;

        Set<UUID> recipients = findRecipients(senderUUID, channel, server);
        if (recipients.isEmpty()) return;

        ServerLevel svLevel = api.fromServerLevel(senderPlayer.serverLevel());
        sendToRecipients(senderUUID, opusData, recipients, api, svLevel);
    }

    public static void forwardRawAudio(ServerPlayer senderPlayer, short[] pcmSamples, int packetChannel) {
        UUID senderUUID = senderPlayer.getUUID();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            PWP.LOGGER.warn("[Voice] forwardRawAudio: server == null");
            return;
        }

        if (groupManager == null) {
            PWP.LOGGER.warn("[Voice] forwardRawAudio: groupManager == null");
            return;
        }

        int channel = groupManager.getPlayerChannel(senderUUID);
        if (channel == VoiceGroupManager.CHANNEL_LOCAL) {
            channel = packetChannel;
            if (channel == VoiceGroupManager.CHANNEL_LOCAL) {
                PWP.LOGGER.warn("[Voice] forwardRawAudio: channel is LOCAL for {}", senderPlayer.getName().getString());
                return;
            }
        }

        VoicechatServerApi api = groupManager.getApi();
        if (api == null) {
            PWP.LOGGER.warn("[Voice] forwardRawAudio: api == null");
            return;
        }

        String senderName = senderPlayer.getName().getString();
        activeSpeakers.put(senderUUID, new VoiceSpeakerState(senderName, channel));

        Set<UUID> recipients = findRecipients(senderUUID, channel, server);
        if (recipients.isEmpty()) {
            PWP.LOGGER.warn("[Voice] forwardRawAudio: no recipients for {} on channel {}", senderName, channel);
            return;
        }

        OpusEncoder encoder = senderEncoders.computeIfAbsent(senderUUID, k -> api.createEncoder());
        if (encoder == null) {
            PWP.LOGGER.warn("[Voice] forwardRawAudio: encoder is null for {}", senderName);
            return;
        }

        ServerLevel svLevel = api.fromServerLevel(senderPlayer.serverLevel());

        PWP.LOGGER.info("[Voice] forwardRawAudio: {} channel={} recipients={} samples={}", senderName, channel, recipients.size(), pcmSamples.length);

        byte[] opusData = encoder.encode(pcmSamples);
        if (opusData != null && opusData.length > 0) {
            PWP.LOGGER.info("[Voice] forwardRawAudio: encoded {} -> {} bytes for {}", senderName, opusData.length, recipients.size());
            sendToRecipients(senderUUID, opusData, recipients, api, svLevel);
        } else {
            PWP.LOGGER.warn("[Voice] forwardRawAudio: encode returned empty for {}", senderName);
        }

        cleanupStaleEncoders();
    }

    static Set<UUID> findRecipients(UUID senderUUID, int channel, MinecraftServer server) {
        Set<UUID> recipients = new HashSet<>();
        if (channel == VoiceGroupManager.CHANNEL_TEAM) {
            Team senderTeam = PWP.getTeamManager().getOrCreatePlayerData(senderUUID).getTeam();
            if (!senderTeam.isPlayable()) return recipients;
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                UUID uid = sp.getUUID();
                if (!uid.equals(senderUUID) && PWP.getTeamManager().getOrCreatePlayerData(uid).getTeam() == senderTeam) {
                    recipients.add(uid);
                }
            }
        }
        return recipients;
    }

    static void sendToRecipients(UUID senderUUID, byte[] opusData, Set<UUID> recipients, VoicechatServerApi api, ServerLevel svLevel) {
        Map<UUID, StaticAudioChannel> perRecipient = channelCache.get(senderUUID);
        if (perRecipient == null) {
            perRecipient = new ConcurrentHashMap<>();
            channelCache.put(senderUUID, perRecipient);
        }
        for (UUID targetUUID : recipients) {
            StaticAudioChannel ch = perRecipient.get(targetUUID);
            if (ch == null || ch.isClosed()) {
                VoicechatConnection targetConn = api.getConnectionOf(targetUUID);
                if (targetConn == null) continue;
                ch = api.createStaticAudioChannel(UUID.randomUUID(), svLevel, targetConn);
                if (ch == null) continue;
                perRecipient.put(targetUUID, ch);
            }
            ch.send(opusData);
        }
    }

    public static VoiceGroupManager getGroupManager() {
        return groupManager;
    }
}
