package com.yourmod.teamsystem.core;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.minecraft.ChatFormatting;

public final class ChatHelper {

    private ChatHelper() {}

    public static MutableComponent success(String msg) {
        return Component.literal(msg).withStyle(TeamSystemColors.CHAT_SUCCESS);
    }

    public static MutableComponent error(String msg) {
        return Component.literal(msg).withStyle(TeamSystemColors.CHAT_ERROR);
    }

    public static MutableComponent info(String msg) {
        return Component.literal(msg).withStyle(TeamSystemColors.CHAT_INFO);
    }

    public static MutableComponent warning(String msg) {
        return Component.literal(msg).withStyle(TeamSystemColors.CHAT_WARNING);
    }

    public static MutableComponent accent(String msg) {
        return Component.literal(msg).withStyle(TeamSystemColors.CHAT_ACCENT);
    }

    public static MutableComponent neutral(String msg) {
        return Component.literal(msg).withStyle(TeamSystemColors.CHAT_NEUTRAL);
    }

    public static MutableComponent bright(String msg) {
        return Component.literal(msg).withStyle(TeamSystemColors.CHAT_BRIGHT);
    }

    public static MutableComponent header(String msg) {
        return Component.literal(msg).withStyle(TeamSystemColors.CHAT_HEADER, TeamSystemColors.CHAT_EMPHASIS);
    }

    public static MutableComponent highlight(String msg) {
        return Component.literal(msg).withStyle(TeamSystemColors.CHAT_HIGHLIGHT);
    }

    public static MutableComponent styled(String msg, ChatFormatting... styles) {
        return Component.literal(msg).withStyle(styles);
    }

    public static MutableComponent join(MutableComponent... parts) {
        MutableComponent result = Component.literal("");
        for (int i = 0; i < parts.length; i++) {
            result = result.append(parts[i]);
        }
        return result;
    }
}
