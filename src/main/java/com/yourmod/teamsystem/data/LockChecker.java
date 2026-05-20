package com.yourmod.teamsystem.data;

import java.util.List;

public final class LockChecker {

    private LockChecker() {}

    public static class Context {
        public int    playerRank;
        public String playerTeam;
        public int    playerSP;
        public int    playerBC;
        public String currentMap;
        public String selectedClass;
        public String selectedKit;
    }

    private static final String NO_TEAM = "SPECTATOR";

    public static LockState checkKit(KitConfig.KitDef kit, Context ctx) {
        KitConfig.KitRequirements req = kit.requirements;
        if (req == null) return LockState.AVAILABLE;
        if (ctx.playerRank < req.rank) return LockState.LOCKED_RANK;
        // Skip team filter if player hasn't been assigned a team yet
        if (req.team != null && !NO_TEAM.equalsIgnoreCase(ctx.playerTeam)
                && !req.team.equalsIgnoreCase(ctx.playerTeam))
            return LockState.LOCKED_TEAM;
        if (req.sp_cost > 0 && ctx.playerSP < req.sp_cost)
            return LockState.LOCKED_COST;
        if (req.bc_cost > 0 && ctx.playerBC < req.bc_cost)
            return LockState.LOCKED_COST;
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
