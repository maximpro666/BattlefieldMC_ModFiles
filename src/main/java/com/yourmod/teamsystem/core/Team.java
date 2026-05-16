package com.yourmod.teamsystem.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum Team {
    NATO("NATO", ChatFormatting.BLUE, 0x5555FF),
    RUSSIA("RUSSIA", ChatFormatting.RED, 0xFF5555),
    SPECTATOR("SPECTATOR", ChatFormatting.GRAY, 0x888888);

    private final String name;
    private final ChatFormatting chatColor;
    private final int rgbColor;

    Team(String name, ChatFormatting chatColor, int rgbColor) {
        this.name = name;
        this.chatColor = chatColor;
        this.rgbColor = rgbColor;
    }

    public String getName() {
        return name;
    }

    public ChatFormatting getChatColor() {
        return chatColor;
    }

    public int getRgbColor() {
        return rgbColor;
    }

    public String getScoreboardName() {
        return "ts_" + name.toLowerCase();
    }

    public MutableComponent getColoredName() {
        return Component.literal(name).withStyle(chatColor);
    }

    public boolean isPlayable() {
        return this != SPECTATOR;
    }

    public boolean isFriendly(Team other) {
        if (this == SPECTATOR || other == SPECTATOR) {
            return true;
        }
        return this == other;
    }

    public static Team fromString(String name) {
        for (Team team : values()) {
            if (team.name.equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

    public static Team fromOrdinal(int ordinal) {
        if (ordinal >= 0 && ordinal < values().length) {
            return values()[ordinal];
        }
        return SPECTATOR;
    }
}
