package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "teamsystem", value = Dist.CLIENT)
public class TabOverlay {
    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().getPath().equals("hotbar")) return;
        if (mc.screen != null) return;
        if (!mc.options.keyPlayerList.isDown()) return;

        GuiGraphics gui = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        int panelW = Math.min(w - 40, 400);
        int panelH = Math.min(h - 40, 300);
        int px = (w - panelW) / 2;
        int py = (h - panelH) / 2;

        gui.fill(px, py, px + panelW, py + panelH, 0xCC111111);
        gui.fill(px, py, px + panelW, py + 1, 0xFF444444);

        int yOff = py + 6;
        int colX = px + 8;
        int lineH = 10;

        List<PlayerListEntry> nato = new ArrayList<>();
        List<PlayerListEntry> russia = new ArrayList<>();
        List<PlayerListEntry> spectators = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : ClientTeamData.playerTeamMap.entrySet()) {
            PlayerListEntry p = ClientTeamData.playerDataMap.get(entry.getKey());
            if (p == null) continue;
            int teamOrd = entry.getValue() != null ? entry.getValue() : Team.SPECTATOR.ordinal();
            if (teamOrd == Team.NATO.ordinal()) nato.add(p);
            else if (teamOrd == Team.RUSSIA.ordinal()) russia.add(p);
            else spectators.add(p);
        }

        yOff = renderTeamColumn(gui, "NATO", 0xFF5555FF, nato, colX, yOff, lineH);
        yOff = renderTeamColumn(gui, "RUSSIA", 0xFFFF5555, russia, colX + panelW / 3, py + 6, lineH);
        renderTeamColumn(gui, "SPECTATORS", 0xFF888888, spectators, colX + 2 * panelW / 3, py + 6, lineH);
    }

    private static int renderTeamColumn(GuiGraphics gui, String title, int color, List<PlayerListEntry> players, int x, int y, int lh) {
        gui.drawString(mc.font, title + " (" + players.size() + ")", x, y, color);
        y += 12;
        for (PlayerListEntry p : players) {
            String kd = p.kills() + "/" + p.deaths();
            String line = "[" + p.rank() + "] " + p.callsign();
            if (!p.squad().isEmpty()) line += " [" + p.squad() + "]";
            gui.drawString(mc.font, line, x, y, 0xFFFFFFFF);
            gui.drawString(mc.font, kd, x + 120, y, 0xFFAAAAAA);
            y += lh;
            if (y > 280) break;
        }
        return y;
    }
}
