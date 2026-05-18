package com.yourmod.teamsystem.client.gui;

import com.yourmod.teamsystem.client.gui.overlay.*;
import com.yourmod.teamsystem.client.gui.screen.BattlefieldPauseScreen;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.screen.VoteScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "teamsystem", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientGuiHandler {

    private static final TicketOverlay       ticketOverlay   = new TicketOverlay(0);

    private static final SquadOverlay        squadOverlay    = new SquadOverlay();
    private static final VitalsOverlay       vitalsOverlay   = new VitalsOverlay();
    private static final NotificationOverlay notifOverlay    = new NotificationOverlay();
    private static final HotbarOverlay       hotbarOverlay   = new HotbarOverlay();
    private static final BattlefieldTabOverlay tabOverlay    = new BattlefieldTabOverlay();
    private static final CaptureNotificationOverlay captureOverlay = new CaptureNotificationOverlay();
    private static final VoiceIndicatorOverlay voiceOverlay  = new VoiceIndicatorOverlay();

    private static boolean hudRenderedThisFrame = false;

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START)
            hudRenderedThisFrame = false;
    }

    @SubscribeEvent
    public static void onPreRenderHUD(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() ||
            event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type() ||
            event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type() ||
            event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type() ||
            event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type() ||
            event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPostRenderAnyOverlay(RenderGuiOverlayEvent.Post event) {
        if (hudRenderedThisFrame) return;
        hudRenderedThisFrame = true;
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        float scale = ClientTeamData.guiScale;
        if (Math.abs(scale - 1.0f) > 0.01f) {
            float ox = w / 2f * (1f - scale);
            float oy = h / 2f * (1f - scale);
            g.pose().pushPose();
            g.pose().translate(ox, oy, 0);
            g.pose().scale(scale, scale, 1f);
        }

        ticketOverlay.render(g, w, event.getPartialTick());
        squadOverlay.render(g, h);
        vitalsOverlay.render(g, w, h);
        notifOverlay.render(g, w);
        hotbarOverlay.render(g, w, h);
        tabOverlay.render(g, w, h);
        captureOverlay.render(g, w, h);
        voiceOverlay.render(g, w, h);

        if (Math.abs(scale - 1.0f) > 0.01f) {
            g.pose().popPose();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tabOverlay.tick();
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof PauseScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                event.setNewScreen(new BattlefieldPauseScreen());
            }
        }
    }

    public static void updateCaptureNotification(String pointName, float progress, int capturingTeamOrdinal, int ownerTeamOrdinal) {
        if (capturingTeamOrdinal >= 0 && capturingTeamOrdinal != ownerTeamOrdinal) {
            captureOverlay.startCapture(pointName);
            captureOverlay.updateProgress(progress);
        } else {
            captureOverlay.endCapture();
        }
    }
}
