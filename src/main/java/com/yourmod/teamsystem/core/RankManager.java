package com.yourmod.teamsystem.core;

import net.minecraft.server.level.ServerPlayer;

public class RankManager {

    public static Rank getRankByKills(int kills) {
        return Rank.fromKills(kills);
    }

    public static String getRankPrefix(int rankOrdinal) {
        Rank rank = Rank.fromOrdinal(rankOrdinal);
        return rank.getPrefix();
    }

    public static String getRankName(int rankOrdinal) {
        Rank rank = Rank.fromOrdinal(rankOrdinal);
        return rank.getDisplayName();
    }

    public static Rank getPlayerRank(ServerPlayer player, int kills) {
        return Rank.fromKills(kills);
    }

    public static Rank checkPromotion(int oldKills, int newKills) {
        Rank oldRank = Rank.fromKills(oldKills);
        Rank newRank = Rank.fromKills(newKills);

        if (oldRank.ordinal() < newRank.ordinal()) {
            return newRank;
        }
        return null;
    }
}
