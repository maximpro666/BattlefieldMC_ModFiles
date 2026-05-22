package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.VoiceSpeakingStatePacket;
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
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamVoicePlugin implements VoicechatPlugin {

    private static VoiceGroupManager groupManager;
    static final Map<UUID, VoiceSpeakerState> activeSpeakers = new ConcurrentHashMap<>();
    static final double MAX_VOICE_DISTANCE_SQ = 48.0 * 48.0;

    static final class VoiceSpeakerState {
        final String name;
        final int channel;
        @Nullable final String groupName;
        long lastSeen;

        VoiceSpeakerState(String name, int channel, @Nullable String groupName) {
            this.name = name;
            this.channel = channel;
            this.groupName = groupName;
            this.lastSeen = System.currentTimeMillis();
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
            PWP.LOGGER.info("TeamVoicePlugin initialized with SV API");
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

        Group senderGroup = sender.getGroup();
        int channel = VoiceGroupManager.CHANNEL_LOCAL;
        String groupName = null;
        if (senderGroup != null) {
            groupName = senderGroup.getName();
            channel = groupName.startsWith("ts_squad") ? VoiceGroupManager.CHANNEL_SQUAD : VoiceGroupManager.CHANNEL_TEAM;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer senderPlayer = server.getPlayerList().getPlayer(senderUUID);
        if (senderPlayer == null) return;

        String senderName = senderPlayer.getName().getString();
        activeSpeakers.put(senderUUID, new VoiceSpeakerState(senderName, channel, groupName));
    }

    public static VoiceGroupManager getGroupManager() {
        return groupManager;
    }
}
