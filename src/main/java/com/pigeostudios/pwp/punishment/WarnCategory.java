package com.pigeostudios.pwp.punishment;

public enum WarnCategory {
    CHAT,
    GAME,
    VOICE,
    GENERAL;

    public String getDisplayName() {
        return switch (this) {
            case CHAT -> "Chat";
            case GAME -> "Game";
            case VOICE -> "Voice";
            case GENERAL -> "General";
        };
    }
}
