package com.yourmod.teamsystem.core;

import net.minecraft.server.level.ServerPlayer;

import com.yourmod.teamsystem.TeamSystem;
import java.util.UUID;

import static com.yourmod.teamsystem.core.ChatHelper.*;

public final class PacketValidator {
    private PacketValidator() {}

    public static String requirePhase(GameManager.GamePhase current, GameManager.GamePhase required) {
        if (current != required) {
            return "Wrong game phase: " + current;
        }
        return null;
    }

    public static String requirePlaying(GameManager game) {
        if (game == null || !game.isPlaying()) {
            return "Game is not running";
        }
        return null;
    }

    public static String requireNotPlaying(GameManager game) {
        if (game != null && game.isPlaying()) {
            return "Cannot do this while game is running";
        }
        return null;
    }

    public static String requireVoting(GameManager game) {
        if (game == null || !game.isVoting()) {
            return "Not in voting phase";
        }
        return null;
    }

    public static String requireLobby(GameManager game) {
        if (game == null || !game.isLobby()) {
            return "Not in lobby phase";
        }
        return null;
    }

    public static String requireTeamPlayable(ServerPlayer player) {
        PlayerCombatData data = TeamSystem.getTeamManager().getOrCreatePlayerData(player.getUUID());
        if (data == null || !data.getTeam().isPlayable()) {
            return "You must be on a team";
        }
        return null;
    }

    public static String requireSquadLeader(ServerPlayer player) {
        SquadManager sm = TeamSystem.getSquadManager();
        if (sm == null) return "Squad system unavailable";
        Squad squad = sm.getPlayerSquad(player.getUUID());
        if (squad == null || !squad.isLeader(player.getUUID())) {
            return "You must be a squad leader";
        }
        return null;
    }

    public static String requireSquadMember(ServerPlayer player) {
        SquadManager sm = TeamSystem.getSquadManager();
        if (sm == null) return "Squad system unavailable";
        Squad squad = sm.getPlayerSquad(player.getUUID());
        if (squad == null) {
            return "You are not in a squad";
        }
        return null;
    }

    public static String requireValidTeamOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal > 2) {
            return "Invalid team";
        }
        return null;
    }

    public static String checkStringLength(String value, int maxLen) {
        if (value == null || value.length() > maxLen) {
            return "Value too long (max " + maxLen + ")";
        }
        return null;
    }

    public static String checkNotNull(Object value, String name) {
        if (value == null) {
            return name + " is required";
        }
        return null;
    }

    public static void reject(ServerPlayer player, String error) {
        if (error != null) {
            player.sendSystemMessage(error(error));
        }
    }

    public static boolean checkAndReject(ServerPlayer player, String error) {
        if (error != null) {
            player.sendSystemMessage(error(error));
            return false;
        }
        return true;
    }
}
