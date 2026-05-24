package com.pigeostudios.pwp.punishment;

import com.pigeostudios.pwp.PWP;

import java.util.UUID;

public class AutoEscalationManager {

    private AutoEscalationManager() {}

    public static void onWarn(UUID uuid, WarnCategory category, UUID staffUuid) {
        PunishmentManager.incrementWarnCount(uuid, category);
        int count = PunishmentManager.getActiveWarnCount(uuid, category);

        switch (category) {
            case CHAT -> {
                if (count == 3) {
                    PunishmentManager.issuePunishment(uuid, staffUuid, PunishmentType.MUTE,
                        WarnCategory.CHAT, "Auto-mute: 3 chat warnings", 86400);
                    PWP.LOGGER.info("Auto-escalation: {} muted for 24h (3x CHAT warn)", uuid);
                } else if (count == 5) {
                    PunishmentManager.issuePunishment(uuid, staffUuid, PunishmentType.MUTE,
                        WarnCategory.CHAT, "Auto-mute: 5 chat warnings", 604800);
                    PWP.LOGGER.info("Auto-escalation: {} muted for 7d (5x CHAT warn)", uuid);
                }
            }
            case GAME -> {
                if (count == 2) {
                    PunishmentManager.issuePunishment(uuid, staffUuid, PunishmentType.TEMP_BAN,
                        WarnCategory.GAME, "Auto-ban: 2 game warnings", 172800);
                    PWP.LOGGER.info("Auto-escalation: {} banned for 48h (2x GAME warn)", uuid);
                } else if (count == 3) {
                    PunishmentManager.issuePunishment(uuid, staffUuid, PunishmentType.TEMP_BAN,
                        WarnCategory.GAME, "Auto-ban: 3 game warnings", 604800);
                    PWP.LOGGER.info("Auto-escalation: {} banned for 7d (3x GAME warn)", uuid);
                }
            }
            case VOICE -> {
                if (count == 2) {
                    PunishmentManager.issuePunishment(uuid, staffUuid, PunishmentType.VOICE_MUTE,
                        WarnCategory.VOICE, "Auto-voicemute: 2 voice warnings", 86400);
                    PWP.LOGGER.info("Auto-escalation: {} voice muted for 24h (2x VOICE warn)", uuid);
                } else if (count == 4) {
                    PunishmentManager.issuePunishment(uuid, staffUuid, PunishmentType.VOICE_MUTE,
                        WarnCategory.VOICE, "Auto-voicemute: 4 voice warnings", 604800);
                    PWP.LOGGER.info("Auto-escalation: {} voice muted for 7d (4x VOICE warn)", uuid);
                }
            }
            case GENERAL -> {
                if (count == 3) {
                    PunishmentManager.issuePunishment(uuid, staffUuid, PunishmentType.TEMP_BAN,
                        WarnCategory.GENERAL, "Auto-ban: 3 general warnings", 86400);
                    PWP.LOGGER.info("Auto-escalation: {} banned for 24h (3x GENERAL warn)", uuid);
                }
            }
        }
    }
}
