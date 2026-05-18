package com.yourmod.teamsystem.data;

import java.util.List;

public final class LockChecker {

    private LockChecker() {}

    public static class Context {
        public int    playerRank;
        public String playerTeam;
        public String currentMap;
        public String selectedClass;
        public String selectedKit;
    }

    public static LockState checkKit(KitConfig.KitDef kit, Context ctx) {
        KitConfig.KitRequirements req = kit.requirements;
        if (req == null) return LockState.AVAILABLE;
        if (ctx.playerRank < req.rank) return LockState.LOCKED_RANK;
        if (req.team != null && !req.team.equalsIgnoreCase(ctx.playerTeam))
            return LockState.LOCKED_TEAM;
        return LockState.AVAILABLE;
    }

    public static LockState checkAttachment(
            String attachmentId,
            List<String> allowed,
            int requiredRank,
            int playerRank) {

        if (allowed != null && !allowed.isEmpty() && !allowed.contains(attachmentId))
            return LockState.INCOMPATIBLE;
        if (playerRank < requiredRank)
            return LockState.LOCKED_RANK;
        return LockState.AVAILABLE;
    }

    public static LockState checkVehicle(int requiredRank, int playerRank,
                                         String requiredRole, String playerRole) {
        if (playerRank < requiredRank) return LockState.LOCKED_RANK;
        if (requiredRole != null && !requiredRole.equalsIgnoreCase(playerRole))
            return LockState.LOCKED_KIT;
        return LockState.AVAILABLE;
    }
}
