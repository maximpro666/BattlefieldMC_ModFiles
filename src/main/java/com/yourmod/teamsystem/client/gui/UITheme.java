package com.yourmod.teamsystem.client.gui;

public final class UITheme {
    private UITheme() {}

    public static final int BG_SCREEN        = 0xCC0A0A0A;
    public static final int BG_PANEL         = 0xDD141414;
    public static final int BG_SURFACE       = 0xDD1C1C1C;
    public static final int BG_SLOT          = 0xFF242424;
    public static final int BG_HUD           = 0xAA0F0F0F;
    public static final int BG_BLACK         = 0xFF000000;

    public static final int TEXT_PRIMARY      = 0xFFEFEFEF;
    public static final int TEXT_SECONDARY    = 0xFF909090;
    public static final int TEXT_MUTED        = 0xFF606060;

    public static final int ACCENT            = 0xFFE07B00;
    public static final int ACCENT_DIM        = 0x80E07B00;
    public static final int ACCENT_GHOST      = 0x26E07B00;

    public static final int BORDER            = 0xFF2E2E2E;
    public static final int BORDER_ALT        = 0x0AFFFFFF;
    public static final int BORDER_ACTIVE     = ACCENT;

    public static final int TEAM_NATO         = 0xFF1C5FAD;
    public static final int TEAM_NATO_BG      = 0xAA0D2A55;
    public static final int TEAM_NATO_BAR     = 0xFF1C5FAD;

    public static final int TEAM_RUSSIA       = 0xFFAD1C1C;
    public static final int TEAM_RUSSIA_BG    = 0xAA550D0D;
    public static final int TEAM_RUSSIA_BAR   = 0xFFAD1C1C;

    public static final int MARKER_POINT      = 0xFFD4D4D4;
    public static final int MARKER_ATTACK     = 0xFFE05050;
    public static final int MARKER_DEFEND     = 0xFF5090E0;
    public static final int MARKER_OBSERVE    = 0xFF60C060;
    public static final int MARKER_BG         = 0xAA0A0A0A;

    public static final int NAMETAG_RANK      = ACCENT;
    public static final int NAMETAG_SPECTATOR = 0xAA3A3A3A;

    public static final int STATUS_DANGER     = 0xFFCC3030;
    public static final int STATUS_WARN       = 0xFFCCA030;
    public static final int STATUS_OK         = 0xFF50B050;

    public static final int COMPASS_CARDINAL  = TEXT_PRIMARY;
    public static final int COMPASS_TICK      = 0xFF484848;
    public static final int COMPASS_TEXT      = TEXT_SECONDARY;
    public static final int COMPASS_CURSOR    = ACCENT;

    public static final int KILLFEED_KILLER   = ACCENT;
    public static final int KILLFEED_VICTIM   = TEXT_SECONDARY;

    public static final int ALPHA_FULL        = 0xFF;
    public static final int ALPHA_HIGH        = 0xE0;
    public static final int ALPHA_MID         = 0xAA;
    public static final int ALPHA_LOW         = 0x66;
    public static final int ALPHA_GHOST       = 0x26;
}
