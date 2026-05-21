package com.yourmod.teamsystem.client;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.VoiceChannelSwitchPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class ClientVoiceHandler {

    private static boolean voiceChatDetected = false;
    private static int activeChannel = 0;
    private static boolean squadPttDown = false;
    private static boolean teamPttDown = false;
    private static long lastSpeakingUpdate = 0L;

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

    private static void updateChannel() {
        int newChannel;
        if (squadPttDown) {
            newChannel = CHANNEL_SQUAD;
        } else if (teamPttDown) {
            newChannel = CHANNEL_TEAM;
        } else {
            newChannel = CHANNEL_LOCAL;
        }

        if (newChannel == activeChannel) return;
        activeChannel = newChannel;

        if (Minecraft.getInstance().getConnection() != null) {
            PacketHandler.CHANNEL.sendToServer(new VoiceChannelSwitchPacket(activeChannel));
        }
        ClientTeamData.currentVoiceChannel = activeChannel;
    }

    public static void onPlayerSpeaking(UUID playerUUID, String playerName) {
        if (Minecraft.getInstance().level == null) return;
        long now = System.currentTimeMillis();
        String entry = playerName + ":" + now;
        ClientTeamData.speakingPlayers.removeIf(s -> s.startsWith(playerName + ":"));
        ClientTeamData.speakingPlayers.add(entry);
        lastSpeakingUpdate = now;
        cleanupExpired();
    }

    public static List<String> getActiveSpeakingPlayers() {
        List<String> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (String entry : ClientTeamData.speakingPlayers) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                try {
                    long time = Long.parseLong(parts[1]);
                    if (now - time < ClientTeamData.SPEAKING_TIMEOUT_MS) {
                        result.add(parts[0]);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    public static boolean isPttActive() {
        return squadPttDown || teamPttDown;
    }

    public static void tick() {
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
        ClientTeamData.speakingPlayers.removeIf(s -> {
            String[] parts = s.split(":", 2);
            if (parts.length == 2) {
                try {
                    return now - Long.parseLong(parts[1]) >= ClientTeamData.SPEAKING_TIMEOUT_MS;
                } catch (NumberFormatException ignored) {}
            }
            return true;
        });
    }
}
