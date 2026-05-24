package com.pigeostudios.pwp.report;

public enum ReportType {
    TEAM_KILL("Team Kill", "Убийство союзника"),
    CHEATING("Cheating", "Использование читов"),
    TOXIC_CHAT("Toxic Chat", "Токсичность в чате"),
    VOICE_ABUSE("Voice Abuse", "Нарушение в голосовом чате"),
    GRIEFING("Griefing", "Гриферство / уничтожение своей техники"),
    EXPLOITING("Exploiting", "Использование эксплойтов"),
    AFK("AFK", "Бездействие во время матча"),
    INAPPROPRIATE_NAME("Inappropriate Name", "Неподходящий никнейм"),
    INAPPROPRIATE_SKIN("Inappropriate Skin", "Неподходящий скин"),
    TEAM_STACKING("Team Stacking", "Намеренный дисбаланс команд"),
    COALITION("Coalition", "Союз сторон / сговор между командами"),
    OTHER("Other", "Другое");

    private final String displayName;
    private final String description;

    ReportType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
