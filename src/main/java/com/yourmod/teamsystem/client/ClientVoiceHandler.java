package com.yourmod.teamsystem.client;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.VoiceChannelSwitchPacket;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientVoiceHandler {

    public record SpeakingEntry(String name, int channel, long timestamp) {}

    private static boolean voiceChatDetected = false;
    private static int activeChannel = 0;
    private static boolean squadPttDown = false;
    private static boolean teamPttDown = false;
    private static final Map<UUID, SpeakingEntry> speakingPlayers = new ConcurrentHashMap<>();

    private static int releaseTimer = 0;
    private static boolean fadingOut = false;
    private static float voiceAnim = 0f;
    private static final int RELEASE_DELAY_TICKS = 10;

    public static final int CHANNEL_LOCAL = 0;
    public static final int CHANNEL_SQUAD = 1;
    public static final int CHANNEL_TEAM  = 2;

    static {
        try {
            Class.forName("de.maxhenkel.voicechat.api.VoicechatApi");
            voiceChatDetected = true;
            TeamSystem.LOGGER.info("Simple Voice Chat detected");
        } catch (ClassNotFoundException e) {
            voiceChatDetected = false;
        }
    }

    public static boolean isVoiceChatDetected() {
        return voiceChatDetected;
    }

    public static int getActiveChannel() {
        return activeChannel;
    }

    public static float getVoiceAnim() {
        return voiceAnim;
    }

    public static boolean isFadingOut() {
        return fadingOut;
    }

    public static void setSquadPtt(boolean pressed) {
        if (squadPttDown == pressed) return;
        squadPttDown = pressed;
        updateChannel();
    }

    public static void setTeamPtt(boolean pressed) {
        if (teamPttDown == pressed) return;
        teamPttDown = pressed;
        updateChannel();
    }

    private static void sendChannelSwitch(int channel) {
        if (Minecraft.getInstance().getConnection() != null) {
            PacketHandler.CHANNEL.sendToServer(new VoiceChannelSwitchPacket(channel));
        }
        ClientTeamData.currentVoiceChannel = channel;
    }

    private static void updateChannel() {
        boolean pttActive = squadPttDown || teamPttDown;

        if (pttActive) {
            int newChannel;
            if (squadPttDown) newChannel = CHANNEL_SQUAD;
            else newChannel = CHANNEL_TEAM;

            if (newChannel != activeChannel || releaseTimer > 0) {
                activeChannel = newChannel;
                releaseTimer = 0;
                fadingOut = false;
                voiceAnim = 1f;
                sendChannelSwitch(activeChannel);
            }
            return;
        }

        if (activeChannel != CHANNEL_LOCAL && !fadingOut) {
            releaseTimer = RELEASE_DELAY_TICKS;
            fadingOut = true;
        }
    }

    public static void onPlayerSpeaking(UUID playerUUID, String playerName, int channel) {
        if (Minecraft.getInstance().level == null) return;
        long now = System.currentTimeMillis();
        speakingPlayers.put(playerUUID, new SpeakingEntry(playerName, channel, now));
        cleanupExpired();
    }

    public static List<SpeakingEntry> getActiveGroupSpeakers() {
        long now = System.currentTimeMillis();
        return speakingPlayers.values().stream()
            .filter(e -> now - e.timestamp < ClientTeamData.SPEAKING_TIMEOUT_MS)
            .filter(e -> e.channel == CHANNEL_SQUAD || e.channel == CHANNEL_TEAM)
            .sorted(Comparator.comparingLong(e -> e.timestamp))
            .collect(Collectors.toList());
    }

    public static boolean isPttActive() {
        return squadPttDown || teamPttDown || fadingOut;
    }

    public static int getActiveChannelGroup() {
        if (squadPttDown || (fadingOut && activeChannel == CHANNEL_SQUAD)) return CHANNEL_SQUAD;
        if (teamPttDown || (fadingOut && activeChannel == CHANNEL_TEAM)) return CHANNEL_TEAM;
        return CHANNEL_LOCAL;
    }

    public static void tick() {
        if (fadingOut) {
            releaseTimer--;
            voiceAnim = Math.max(0f, (float) releaseTimer / RELEASE_DELAY_TICKS);

            if (ClientSetup.SQUAD_VOICE_KEY.isDown() || ClientSetup.TEAM_VOICE_KEY.isDown()) {
                releaseTimer = 0;
                fadingOut = false;
                voiceAnim = 1f;
            } else if (releaseTimer <= 0) {
                fadingOut = false;
                voiceAnim = 0f;
                activeChannel = CHANNEL_LOCAL;
                sendChannelSwitch(CHANNEL_LOCAL);
            }
        } else if (activeChannel == CHANNEL_LOCAL && voiceAnim > 0f) {
            voiceAnim = Math.max(0f, voiceAnim - 0.05f);
        }

        cleanupExpired();
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            boolean squadDown = ClientSetup.SQUAD_VOICE_KEY.isDown();
            boolean teamDown = ClientSetup.TEAM_VOICE_KEY.isDown();
            if (squadPttDown != squadDown || teamPttDown != teamDown) {
                squadPttDown = squadDown;
                teamPttDown = teamDown;
                updateChannel();
            }
        }
    }

    private static void cleanupExpired() {
        long now = System.currentTimeMillis();
        speakingPlayers.values().removeIf(e -> now - e.timestamp >= ClientTeamData.SPEAKING_TIMEOUT_MS);
    }
}
