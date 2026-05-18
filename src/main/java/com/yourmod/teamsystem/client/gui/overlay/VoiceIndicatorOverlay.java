package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.core.Rank;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import java.util.UUID;

public class VoiceIndicatorOverlay implements IGuiOverlay {
    private static final int ICON_SIZE = 16;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        var speakers = ClientTeamData.getSpeakingPlayers();
        if (speakers.isEmpty()) return;

        int x = 10;
        int y = screenHeight / 2 - (speakers.size() * (ICON_SIZE + 2)) / 2;

        for (String uuidStr : speakers) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (Exception e) {
                continue;
            }

            PlayerListEntry playerData = ClientTeamData.getPlayerData(uuid);
            if (playerData == null) continue;

            int rank = playerData.rank();
            String callsign = playerData.callsign();
            Rank rankObj = Rank.fromOrdinal(rank);
            String prefix = rankObj.getPrefix(ClientTeamData.getLocalPlayerTeam() == Team.RUSSIA);

            String text = prefix + " " + callsign;
            int textW = mc.font.width(text);
            int bgW = textW + ICON_SIZE + 8;

            g.fill(x, y, x + bgW, y + ICON_SIZE, 0x88000000);
            g.fill(x, y, x + 2, y + ICON_SIZE, 0xFF00FF00);
            g.drawString(mc.font, text, x + ICON_SIZE + 4, y + 4, 0xFFFFFFFF);
            y += ICON_SIZE + 2;
        }
    }
}
