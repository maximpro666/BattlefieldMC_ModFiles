package com.pigeostudios.pwp.bleeding;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.UUID;

public class BleedingState {
    public static final int BLEEDOUT_TICKS = 600;
    public static final int REVIVE_TICKS = 100;

    public final UUID playerUUID;
    public final UUID attackerUUID;
    public final String attackerName;
    public final String sourceMsgId;
    public int bleedTimeRemaining;
    public int downedTime;
    public int reviveProgress;
    public UUID reviverUUID;
    public int reviveThreshold = REVIVE_TICKS;

    public BleedingState(ServerPlayer player, ServerPlayer attacker, DamageSource source) {
        this.playerUUID = player.getUUID();
        this.attackerUUID = attacker != null ? attacker.getUUID() : null;
        this.attackerName = attacker != null ? attacker.getName().getString() : "unknown";
        this.sourceMsgId = source.getMsgId();
        this.bleedTimeRemaining = BLEEDOUT_TICKS;
        this.downedTime = 0;
        this.reviveProgress = 0;
        this.reviverUUID = null;
    }

    public boolean isBleeding() {
        return bleedTimeRemaining > 0 && reviveProgress < reviveThreshold;
    }

    public boolean isBeingRevived() {
        return reviverUUID != null;
    }

    public boolean hasBledOut() {
        return bleedTimeRemaining <= 0;
    }

    public boolean isRevived() {
        return reviveProgress >= reviveThreshold;
    }
}
