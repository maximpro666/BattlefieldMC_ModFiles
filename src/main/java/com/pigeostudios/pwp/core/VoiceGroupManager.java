package com.pigeostudios.pwp.core;

import com.pigeostudios.pwp.PWP;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoiceGroupManager {

    public static final int CHANNEL_LOCAL = 0;
    public static final int CHANNEL_TEAM  = 2;

    private VoicechatServerApi api;
    private final Map<UUID, Integer> playerChannels = new HashMap<>();
    private boolean available = false;

    public VoiceGroupManager() {
    }

    public void init(VoicechatServerApi voicechatApi) {
        this.api = voicechatApi;
        available = true;
        PWP.LOGGER.info("Voice Group Manager initialized");
    }

    @Nullable
    private VoicechatConnection getConnection(UUID playerUUID) {
        if (api == null) return null;
        return api.getConnectionOf(playerUUID);
    }

    public void setPlayerChannel(UUID playerUUID, int channel) {
        if (!available) return;
        playerChannels.put(playerUUID, channel);
    }

    public int getPlayerChannel(UUID playerUUID) {
        return playerChannels.getOrDefault(playerUUID, CHANNEL_LOCAL);
    }

    public void joinChannel(ServerPlayer player, int channel) {
        if (!available) return;
        playerChannels.put(player.getUUID(), channel);
    }

    public void leaveChannel(UUID playerUUID) {
        if (!available) return;
        playerChannels.remove(playerUUID);
        VoicechatConnection conn = getConnection(playerUUID);
        if (conn == null) return;
        conn.setGroup(null);
    }

    public void cleanup() {
        playerChannels.clear();
        available = false;
        api = null;
    }

    public boolean isAvailable() {
        return available;
    }

    public VoicechatServerApi getApi() {
        return api;
    }
}
