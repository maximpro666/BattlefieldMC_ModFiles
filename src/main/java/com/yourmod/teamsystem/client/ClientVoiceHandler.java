package com.yourmod.teamsystem.client;

import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.core.Rank;
import net.minecraft.client.Minecraft;

import java.util.UUID;

/**
 * Handles Simple Voice Chat integration for speaker indicators.
 * Uses soft dependency - catches linkage errors if VoiceChat is not installed.
 */
public class ClientVoiceHandler {
    private static boolean voiceChatDetected = false;

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

    /**
     * Called when a player starts talking in VoiceChat.
     * @param playerUUID UUID of the talking player
     * @param playerName Name of the talking player
     */
    public static void onPlayerSpeaking(UUID playerUUID, String playerName) {
        if (Minecraft.getInstance().level == null) return;

        String rankPrefix = "";
        if (com.yourmod.teamsystem.client.ClientTeamData.localPlayerRank >= 0) {
            boolean russian = "ru".equals(ClientTeamData.language);
            rankPrefix = Rank.fromOrdinal(ClientTeamData.localPlayerRank).getPrefix(russian);
        }

        // Remove existing entry for this player
        ClientTeamData.speakingPlayers.removeIf(sp -> sp.getName().equals(playerName));

        // Add new speaking indicator
        ClientTeamData.speakingPlayers.add(new SpeakingPlayer(playerName, rankPrefix));

        // Keep list clean
        ClientTeamData.speakingPlayers.removeIf(SpeakingPlayer::isExpired);
    }

    /**
     * Called every tick to clean up expired speaking indicators.
     */
    public static void tick() {
        if (!ClientTeamData.speakingPlayers.isEmpty()) {
            ClientTeamData.speakingPlayers.removeIf(SpeakingPlayer::isExpired);
        }
    }
}
