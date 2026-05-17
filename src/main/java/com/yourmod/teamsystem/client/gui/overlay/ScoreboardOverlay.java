package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.ClientTeamData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "teamsystem", value = Dist.CLIENT)
public class ScoreboardOverlay {
    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().getPath().equals("hotbar")) return;
        GuiGraphics gui = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        int pw = 110;
        int px = w - pw - 4;
        int py = h / 2 - 75;

        gui.fill(px, py, px + pw, py + 150, 0xAA111111);
        gui.fill(px, py, px + pw, py + 1, 0xFF444444);

        int y = py + 6;

        gui.drawString(mc.font, "\u00a7lSCOREBOARD", px + 8, y, 0xFFFFFF00);
        y += 12;

        int secs = ClientTeamData.matchTimeSeconds;
        String time = String.format("%02d:%02d", secs / 60, secs % 60);
        gui.drawString(mc.font, "Time: " + time, px + 8, y, 0xFFFFFFFF);
        y += 10;

        gui.drawString(mc.font, "NATO: " + ClientTeamData.getNatoTickets(), px + 8, y, 0xFF5555FF);
        y += 10;
        gui.drawString(mc.font, "RUSSIA: " + ClientTeamData.getRussiaTickets(), px + 8, y, 0xFFFF5555);
        y += 12;

        gui.drawString(mc.font, "\u00a7lPLAYER", px + 8, y, 0xFFFFFFAA);
        y += 10;
        gui.drawString(mc.font, "Kills: " + ClientTeamData.getLocalPlayerKills(), px + 8, y, 0xFFFFFFFF);
        y += 10;
        gui.drawString(mc.font, "Deaths: " + ClientTeamData.getLocalPlayerDeaths(), px + 8, y, 0xFFFFFFFF);
        y += 10;
        gui.drawString(mc.font, "BC: " + ClientTeamData.localPlayerBC, px + 8, y, 0xFFFFFFFF);
        y += 10;
        gui.drawString(mc.font, "SP: " + ClientTeamData.localPlayerSP, px + 8, y, 0xFFFFFFFF);
        y += 12;

        String map = ClientTeamData.getCurrentMapName();
        if (!map.isEmpty()) {
            gui.drawString(mc.font, "\u00a77" + map, px + 8, y, 0xFF888888);
        }
    }
}
