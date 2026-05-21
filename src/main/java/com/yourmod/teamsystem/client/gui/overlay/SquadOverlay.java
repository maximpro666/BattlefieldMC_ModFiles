package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.RenderHelper;
import com.yourmod.teamsystem.core.Rank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.util.Map;
import java.util.UUID;

public class SquadOverlay {

    private static final int ROW_H = 26;
    private static final int PANEL_W = 170;
    private static final int PADDING = 6;
    private static final int AVATAR_SIZE = 18;

    public void render(GuiGraphics g, int screenHeight) {
        Map<UUID, PlayerListEntry> map = ClientTeamData.playerDataMap;
        String mySquad = ClientTeamData.localPlayerSquad;
        if (map == null || mySquad == null || mySquad.isEmpty()) return;

        int x = 8;
        int startY = screenHeight / 2 - 70;
        int idx = 0;

        Font font = Minecraft.getInstance().font;
        UUID localUuid = Minecraft.getInstance().player != null ?
            Minecraft.getInstance().player.getUUID() : null;

        int count = 0;
        for (Map.Entry<UUID, PlayerListEntry> entry : map.entrySet()) {
            PlayerListEntry ple = entry.getValue();
            String ps = ple.squadName() != null && !ple.squadName().isEmpty() ?
                ple.squadName() : ple.squad();
            if (mySquad.equals(ps)) count++;
        }

        int totalH = count * (ROW_H + 2) + 14;

        RenderHelper.dropShadow(g, x, startY - 14, PANEL_W, totalH, 3, 80);
        RenderHelper.roundedRect(g, x, startY - 14, PANEL_W, totalH, 3,
            AnimationHelper.withAlpha(UITheme.BG_HUD, 200));

        g.fill(x, startY - 14, x + PANEL_W, startY - 12,
            AnimationHelper.withAlpha(UITheme.ACCENT_DIM, 120));

        String squadLabel = "\u2694 " + mySquad;
        g.drawString(font, squadLabel, x + PADDING, startY - 11,
            AnimationHelper.withAlpha(UITheme.ACCENT, 255));

        for (Map.Entry<UUID, PlayerListEntry> entry : map.entrySet()) {
            PlayerListEntry ple = entry.getValue();
            String ps = ple.squadName() != null && !ple.squadName().isEmpty() ?
                ple.squadName() : ple.squad();
            if (!mySquad.equals(ps)) continue;

            int y = startY + 1 + idx * (ROW_H + 2);
            boolean isSelf = localUuid != null && localUuid.equals(entry.getKey());

            int bgColor = isSelf ?
                AnimationHelper.withAlpha(UITheme.SELF_BG, 220) :
                AnimationHelper.withAlpha(0x00000000, 0);

            if (isSelf) {
                g.fill(x + PADDING, y, x + PANEL_W - PADDING, y + ROW_H,
                    AnimationHelper.withAlpha(UITheme.SELF_BG, 200));
                g.fill(x, y, x + 3, y + ROW_H,
                    AnimationHelper.withAlpha(UITheme.ACCENT, 255));
            }

            Rank rank = Rank.fromOrdinal(ple.rank());
            String callsign = ple.callsign();
            if (callsign == null) callsign = "Unknown";

            String rankPrefix = (rank != null ? rank.getPrefix(false) : "");
            String label = rankPrefix + " " + callsign;

            if (font.width(label) > PANEL_W - PADDING * 2 - AVATAR_SIZE - 8) {
                while (font.width(label + "...") > PANEL_W - PADDING * 2 - AVATAR_SIZE - 8 && label.length() > 1) {
                    label = label.substring(0, label.length() - 1);
                }
                label += "...";
            }

            int iconY = y + (ROW_H - AVATAR_SIZE) / 2;
            g.fill(x + PADDING + 1, iconY, x + PADDING + AVATAR_SIZE - 1, iconY + AVATAR_SIZE,
                AnimationHelper.withAlpha(0xFF1A1A1A, 200));

            g.drawString(font, rankPrefix, x + PADDING + 4, iconY + 5,
                AnimationHelper.withAlpha(rank != null && rank.ordinal() >= 6 ? UITheme.ACCENT : UITheme.TEXT_PRIMARY, 220));

            int nameColor = isSelf ? UITheme.ACCENT : UITheme.TEXT_PRIMARY;
            g.drawString(font, label, x + PADDING + AVATAR_SIZE + 6, y + 4,
                AnimationHelper.withAlpha(nameColor, 240));

            ple.isSquadLeader();
            if (ple.isSquadLeader()) {
                String star = "\u2605";
                g.drawString(font, star, x + PANEL_W - PADDING - font.width(star), y + 4,
                    AnimationHelper.withAlpha(UITheme.ACCENT, 255));
            }

            Integer status = ClientTeamData.squadmateStatuses.get(entry.getKey());
            if (status != null) {
                float healthFrac = Math.max(0, Math.min(1, status / 100f));
                int barW = (int) ((PANEL_W - PADDING * 2 - AVATAR_SIZE - 10) * healthFrac);
                int barY = y + ROW_H - 5;
                int barStartX = x + PADDING + AVATAR_SIZE + 6;

                g.fill(barStartX, barY, barStartX + (PANEL_W - PADDING * 2 - AVATAR_SIZE - 10), barY + 3,
                    AnimationHelper.withAlpha(UITheme.HUD_HP_BG, 180));

                if (barW > 0) {
                    int healthColor = AnimationHelper.hpColor(healthFrac);
                    RenderHelper.gradientRectH(g, barStartX, barY, barW, 3,
                        healthColor, brightenColor(healthColor, 1.2f));
                }
            }

            idx++;
            if (idx >= 6) break;
        }
    }

    private int brightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
