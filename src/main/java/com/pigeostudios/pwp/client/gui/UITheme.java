package com.pigeostudios.pwp.client.gui;

public final class UITheme {
    private UITheme() {}

    public static final int BG_SCREEN        = 0xCC0A0A0A;
    public static final int BG_PANEL         = 0xDD141414;
    public static final int BG_SURFACE       = 0xDD1C1C1C;
    public static final int BG_SLOT          = 0xDD242424;
    public static final int BG_HUD           = 0xD20F0F0F;
    public static final int BG_BLACK         = 0xFF000000;
    public static final int BG_TOOLTIP       = 0xDD0D0D0D;
    public static final int BG_OVERLAY       = 0xBB000000;

    public static final int TEXT_PRIMARY      = 0xFFEFEFEF;
    public static final int TEXT_SECONDARY    = 0xFFB0B0B0;
    public static final int TEXT_MUTED        = 0xFF808080;

    public static final int ACCENT            = 0xFFE07B00;
    public static final int ACCENT_DIM        = 0x80E07B00;
    public static final int ACCENT_GHOST      = 0x26E07B00;

    public static final int BORDER            = 0xFF2E2E2E;
    public static final int BORDER_ALT        = 0x0AFFFFFF;
    public static final int BORDER_ACTIVE     = ACCENT;

    public static final int TEAM_NATO         = 0xFF1C5FAD;
    public static final int TEAM_NATO_BG      = 0xAA0D2A55;
    public static final int TEAM_NATO_BAR     = 0xFF1C5FAD;
    public static final int NATO              = TEAM_NATO;

    public static final int TEAM_RUSSIA       = 0xFFAD1C1C;
    public static final int TEAM_RUSSIA_BG    = 0xAA550D0D;
    public static final int TEAM_RUSSIA_BAR   = 0xFFAD1C1C;

    public static final int HUD_HP_BG         = 0xDD1A1A1A;
    public static final int HUD_HP_FULL       = 0xFF50B050;
    public static final int HUD_HP_MID        = 0xFFCCA030;
    public static final int HUD_HP_LOW        = 0xFFCC3030;
    public static final int HUD_AMMO_BG       = 0xD2141414;
    public static final int HUD_AMMO_TEXT     = 0xFFEFEFEF;
    public static final int HUD_MINIMAP_BG    = 0xCC0A0A0A;
    public static final int HUD_MINIMAP_BORDER= 0xFF2E2E2E;

    public static final int MENU_BUTTON_BG    = 0xCC141414;
    public static final int MENU_BUTTON_HOVER = 0xCC1C1C1C;

    public static final int LOADING_BAR_BG    = 0xFF1A1A1A;
    public static final int LOADING_BAR_FILL  = ACCENT;

    public static final int MARKER_POINT      = 0xFFD4D4D4;
    public static final int MARKER_ATTACK     = 0xFFE05050;
    public static final int MARKER_DEFEND     = 0xFF5090E0;
    public static final int MARKER_OBSERVE    = 0xFF60C060;
    public static final int MARKER_BG         = 0xD20A0A0A;

    public static final int NAMETAG_RANK      = ACCENT;
    public static final int NAMETAG_SPECTATOR = 0xD23A3A3A;

    public static final int STATUS_DANGER     = 0xFFCC3030;
    public static final int STATUS_WARN       = 0xFFCCA030;
    public static final int STATUS_OK         = 0xFF50B050;

    public static final int COMPASS_CARDINAL  = TEXT_PRIMARY;
    public static final int COMPASS_TICK      = 0xFF484848;
    public static final int COMPASS_TEXT      = TEXT_SECONDARY;
    public static final int COMPASS_CURSOR    = ACCENT;

    public static final int KILLFEED_KILLER   = ACCENT;
    public static final int KILLFEED_VICTIM   = TEXT_SECONDARY;

    public static final int MAP_COOLDOWN     = 0xFFCC3030;
    public static final int MAP_COOLDOWN_BG  = 0xD2550000;

    public static final int ALPHA_FULL   = 255;
    public static final int ALPHA_HIGH   = 221;
    public static final int ALPHA_SCREEN = 204;
    public static final int ALPHA_HUD    = 210;
    public static final int ALPHA_MID    = 170;
    public static final int ALPHA_DIM    = 128;
    public static final int ALPHA_LOW    = 102;
    public static final int ALPHA_LINE   = 64;
    public static final int ALPHA_GHOST  = 38;

    public static final int RANK_GOLD = 0xFFE8750A;
    public static final int RANK_GOLD_DIM = 0xFFC8A050;
    public static final int RANK_SILVER = 0xFF8090A0;
    public static final int RANK_SILVER_DIM = 0xFF607080;
    public static final int DONATE_VIP = 0xFFE8750A;
    public static final int DONATE_ELITE_A = 0xFFA080E0;
    public static final int DONATE_ELITE_B = 0xFF3090D0;
    public static final int DONATE_GENERAL = 0xFFE07B00;
    public static final int SQUAD_LABEL = ACCENT;
    public static final int SQUAD_LINE = 0x40E07B00;
    public static final int SELF_BG = 0x26E07B00;
    public static final int SELF_BORDER = ACCENT;
    public static final int PING_LOW = 0xFF50B050;
    public static final int PING_MID = 0xFFCCA030;
    public static final int PING_HIGH = 0xFFCC3030;
    public static final int NAMETAG_BAR_BG = 0x40000000;
    public static final int NAMETAG_BAR_BASE = 0xFF808080;

    public static final int VOICE_LOCAL  = 0xFF50B050;
    public static final int VOICE_SQUAD  = 0xFF5090E0;
    public static final int VOICE_TEAM   = 0xFFE07B00;
    public static final int VOICE_INACTIVE = 0x66808080;

    public static int getTeamColor(int teamOrdinal) {
        if (teamOrdinal == 0) return TEAM_NATO;
        if (teamOrdinal == 1) return TEAM_RUSSIA;
        return TEXT_PRIMARY;
    }

    public static int getNotificationColor(String type) {
        if ("success".equals(type)) return STATUS_OK;
        if ("warning".equals(type)) return STATUS_WARN;
        if ("error".equals(type)) return STATUS_DANGER;
        if ("match".equals(type)) return ACCENT;
        if ("capture_nato".equals(type)) return TEAM_NATO;
        if ("capture_russia".equals(type)) return TEAM_RUSSIA;
        if ("capture_neutral".equals(type)) return TEXT_MUTED;
        return TEXT_PRIMARY;
    }

    public static String getNotificationIcon(String type) {
        if ("success".equals(type)) return "\u2713";
        if ("warning".equals(type)) return "\u26A0";
        if ("error".equals(type)) return "\u2716";
        if ("match".equals(type)) return "\u2694";
        if (type != null && type.startsWith("capture")) return "\u25C9";
        return "\u2139";
    }
}
