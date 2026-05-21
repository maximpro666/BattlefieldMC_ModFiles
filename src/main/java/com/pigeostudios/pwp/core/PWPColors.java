package com.pigeostudios.pwp.core;

import net.minecraft.ChatFormatting;

public final class PWPColors {

    private PWPColors() {}

    // ========== ARGB Color Ints (for immediate-mode GUI rendering) ==========

    // --- Accent ---
    public static final int ACCENT           = 0xFF00AAFF;

    // --- Backgrounds ---
    public static final int BG_DARK          = 0xCC111111;
    public static final int BG_MEDIUM        = 0xCC222222;
    public static final int BG_CARD          = 0xCC222222;
    public static final int BG_LIGHT         = 0x88000000;
    public static final int BG_BUTTON        = 0xAA222222;
    public static final int BG_BUTTON_HOVER  = 0xAA444444;
    public static final int BG_PROGRESS      = 0x80000000;
    public static final int BG_MAIN_MENU     = 0xFF111111;
    public static final int BG_SLOT          = 0x33000000;
    public static final int BG_PAUSE         = 0x88000000;

    // --- Borders ---
    public static final int BORDER           = 0xFF555555;
    public static final int BORDER_HOVER     = 0xFFAAAAAA;
    public static final int BORDER_ACCENT    = 0xFF00AAFF;

    // --- Text ---
    public static final int TEXT_PRIMARY     = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY   = 0xFFAAAAAA;
    public static final int TEXT_TERTIARY    = 0xFF888888;
    public static final int TEXT_DISABLED    = 0xFF888888;
    public static final int TEXT_BUTTON      = 0xFFCCCCCC;
    public static final int TEXT_BUTTON_HOVER = 0xFFFFFFFF;
    public static final int TEXT_SELECTED    = 0x66FFFFFF;
    public static final int TEXT_ACCENT      = 0xFF00AAFF;

    // --- Team colors ---
    public static final int TEAM_NATO        = 0xFF4488FF;
    public static final int TEAM_RUSSIA      = 0xFFFF4444;
    public static final int TEAM_SPECTATOR   = 0xFF888888;

    // --- Team RGB (without alpha, for Team.java enum) ---
    public static final int TEAM_NATO_RGB      = 0x5555FF;
    public static final int TEAM_RUSSIA_RGB    = 0xFF5555;
    public static final int TEAM_SPECTATOR_RGB = 0x888888;

    // --- Team colors with alpha (for world markers, particles) ---
    public static final int TEAM_NATO_ALPHA  = 0x4444FFFF;
    public static final int TEAM_RUSSIA_ALPHA = 0xFFFF4444;

    // --- Status ---
    public static final int HEALTH           = 0xFF44FF44;
    public static final int ARMOR            = 0xFF4488FF;
    public static final int DANGER           = 0xFFFF4444;
    public static final int DOWNED           = 0xFFFF4444;
    public static final int WARNING          = 0xFFFFAA00;
    public static final int SUCCESS          = 0xFF44FF44;
    public static final int ACTIVE           = 0xFF00FF00;

    // ========== ChatFormatting constants (for Component.withStyle) ==========

    public static final ChatFormatting CHAT_ACCENT       = ChatFormatting.GOLD;
    public static final ChatFormatting CHAT_SUCCESS      = ChatFormatting.GREEN;
    public static final ChatFormatting CHAT_ERROR        = ChatFormatting.RED;
    public static final ChatFormatting CHAT_INFO         = ChatFormatting.AQUA;
    public static final ChatFormatting CHAT_WARNING      = ChatFormatting.YELLOW;
    public static final ChatFormatting CHAT_HIGHLIGHT    = ChatFormatting.GOLD;
    public static final ChatFormatting CHAT_NEUTRAL      = ChatFormatting.GRAY;
    public static final ChatFormatting CHAT_BRIGHT       = ChatFormatting.WHITE;
    public static final ChatFormatting CHAT_DIM          = ChatFormatting.DARK_GRAY;
    public static final ChatFormatting CHAT_HEADER       = ChatFormatting.GOLD;
    public static final ChatFormatting CHAT_EMPHASIS     = ChatFormatting.BOLD;

    // ========== Minecraft section-sign color codes ==========
    // For legacy string formatting where Component.literal is used

    public static final String MC_ACCENT      = "§6";
    public static final String MC_SUCCESS     = "§a";
    public static final String MC_ERROR       = "§c";
    public static final String MC_INFO        = "§b";
    public static final String MC_WARNING     = "§e";
    public static final String MC_HIGHLIGHT   = "§6";
    public static final String MC_NEUTRAL     = "§7";
    public static final String MC_BRIGHT      = "§f";
    public static final String MC_DIM         = "§8";
    public static final String MC_EMPHASIS    = "§l";
    public static final String MC_RESET       = "§r";
}
