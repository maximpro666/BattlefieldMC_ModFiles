package com.pigeostudios.pwp.punishment;

public enum PunishmentType {
    WARN,
    KICK,
    MUTE,
    VOICE_MUTE,
    TEMP_BAN,
    PERM_BAN;

    public boolean isBan() {
        return this == TEMP_BAN || this == PERM_BAN;
    }

    public boolean isMute() {
        return this == MUTE || this == VOICE_MUTE;
    }

    public boolean isPermanent() {
        return this == PERM_BAN;
    }
}
