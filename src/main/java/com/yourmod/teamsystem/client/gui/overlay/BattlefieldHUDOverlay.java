package com.yourmod.teamsystem.client.gui.overlay;

import com.yourmod.teamsystem.client.ClientMarkerData;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.core.MarkerData;
import com.yourmod.teamsystem.core.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "teamsystem", value = Dist.CLIENT)
public class BattlefieldHUDOverlay {
    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().getPath().equals("hotbar")) return;
        LocalPlayer player = mc.player;
        if (player == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        renderCompass(gui, player, w);
        renderTeamInfo(gui, w);
        renderTickets(gui, w);
        renderGamePhase(gui, w, h);
        renderHealthArmorFood(gui, player, w, h);
        renderAmmo(gui, player, w, h);
        renderPlayerStats(gui, w);
    }

    private static void renderCompass(GuiGraphics gui, LocalPlayer player, int w) {
        float yaw = player.getYRot();
        int cw = 182, ch = 13, sy = 2;
        int sx = (w - cw) / 2;
        float cx = w / 2f;

        gui.fill(sx - 4, sy, sx + cw + 4, sy + ch, 0x88000000);

        String[] labels = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        float[] angles = {0, 45, 90, 135, 180, 225, 270, 315};

        for (int i = 0; i < 8; i++) {
            float diff = angles[i] - yaw;
            while (diff > 180) diff -= 360;
            while (diff < -180) diff += 360;
            float x = cx + diff * cw / 360f;
            if (x >= sx && x <= sx + cw) {
                gui.drawString(mc.font, labels[i], (int) x - 2, sy + 2, labels[i].equals("N") ? 0xFFFF4444 : 0xFFFFFFFF);
            }
        }

        List<MarkerData> markers = ClientMarkerData.getMarkers();
        if (markers != null) {
            for (MarkerData m : markers) {
                double dx = m.getX() - player.getX();
                double dz = m.getZ() - player.getZ();
                double ma = Math.toDegrees(Math.atan2(dz, dx)) - 90;
                if (ma < 0) ma += 360;
                float diff = (float) ma - yaw;
                while (diff > 180) diff -= 360;
                while (diff < -180) diff += 360;
                float x = cx + diff * cw / 360f;
                if (x >= sx && x <= sx + cw) {
                    gui.drawString(mc.font, "\u2666", (int) x - 2, sy + 2, m.getTeamOrdinal() == Team.NATO.ordinal() ? 0xFF5555FF : 0xFFFF5555);
                }
            }
        }
    }

    private static void renderTeamInfo(GuiGraphics gui, int w) {
        Team team = ClientTeamData.getLocalPlayerTeam();
        String name = team != null ? team.getName() : "NONE";
        int color = team == Team.NATO ? 0xFF5555FF : (team == Team.RUSSIA ? 0xFFFF5555 : 0xFF888888);
        gui.drawString(mc.font, name, 4, 2, color);
        gui.drawString(mc.font, "Map: " + ClientTeamData.getCurrentMapName(), 4, 12, 0xFFFFFFFF);
    }

    private static void renderTickets(GuiGraphics gui, int w) {
        int nx = w - 90;
        gui.drawString(mc.font, "NATO: " + ClientTeamData.getNatoTickets(), nx, 20, 0xFF5555FF);
        gui.drawString(mc.font, "RUS: " + ClientTeamData.getRussiaTickets(), nx, 30, 0xFFFF5555);
    }

    private static void renderGamePhase(GuiGraphics gui, int w, int h) {
        int phase = ClientTeamData.getGamePhase();
        String text;
        switch (phase) {
            case 0: text = "LOBBY"; break;
            case 1: text = "PREPARATION"; break;
            case 2: text = "BATTLE"; break;
            case 3: text = "GAME OVER"; break;
            default: return;
        }
        gui.drawString(mc.font, text, w / 2 - 20, h - 70, 0xFFFFFF00);
    }

    private static void renderHealthArmorFood(GuiGraphics gui, LocalPlayer player, int w, int h) {
        int bw = 100, bh = 5, y = h - 35, x = w / 2 - bw / 2;

        float hp = player.getHealth(), maxHp = player.getMaxHealth();
        gui.fill(x, y, x + bw, y + bh, 0x88000000);
        gui.fill(x, y, x + (int) ((hp / maxHp) * bw), y + bh, 0xFFFF0000);
        gui.drawString(mc.font, String.format("%.0f/%d", hp, (int) maxHp), x + bw + 4, y - 2, 0xFFFFFFFF);

        int armor = player.getArmorValue();
        if (armor > 0) {
            y -= 9;
            gui.fill(x, y, x + bw, y + bh, 0x88000000);
            gui.fill(x, y, x + (int) ((armor / 20f) * bw), y + bh, 0xFF5555FF);
            gui.drawString(mc.font, String.valueOf(armor), x + bw + 4, y - 2, 0xFFFFFFFF);
        }

        int food = player.getFoodData().getFoodLevel();
        y -= 9;
        gui.fill(x, y, x + bw, y + bh, 0x88000000);
        gui.fill(x, y, x + (int) ((food / 20f) * bw), y + bh, 0xFF8B4513);
    }

    private static void renderAmmo(GuiGraphics gui, LocalPlayer player, int w, int h) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.isDamageableItem()) return;

        int maxDmg = held.getMaxDamage(), dmg = held.getDamageValue(), ammo = maxDmg - dmg;
        int bw = 60, bh = 4, x = w / 2 - bw / 2, y = h - 18;

        gui.fill(x, y, x + bw, y + bh, 0x88000000);
        float r = (float) ammo / maxDmg;
        int c = r > 0.3f ? 0xFFFFFF00 : 0xFFFF0000;
        gui.fill(x, y, x + (int) (r * bw), y + bh, c);
        gui.drawString(mc.font, ammo + "/" + maxDmg, x + bw + 4, y - 2, 0xFFFFFFFF);
    }

    private static void renderPlayerStats(GuiGraphics gui, int w) {
        int k = ClientTeamData.getLocalPlayerKills();
        int d = ClientTeamData.getLocalPlayerDeaths();
        String sq = ClientTeamData.localPlayerSquad;
        int bc = ClientTeamData.localPlayerBC;
        int sp = ClientTeamData.localPlayerSP;
        gui.drawString(mc.font, "K: " + k + " D: " + d, 4, 24, 0xFFFFFFFF);
        gui.drawString(mc.font, sq.isEmpty() ? "No Squad" : sq, 4, 34, 0xFFFFFFFF);
        gui.drawString(mc.font, "BC: " + bc + " SP: " + sp, 4, 44, 0xFFFFFFFF);
    }
}
