package com.pigeostudios.pwp.client.gui;

import com.pigeostudios.pwp.client.ClientVoiceHandler;
import com.pigeostudios.pwp.client.VehicleCameraHandler;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.overlay.*;
import com.pigeostudios.pwp.client.gui.screen.BattlefieldMainMenuScreen;
import com.pigeostudios.pwp.client.gui.screen.BattlefieldPauseScreen;
import com.pigeostudios.pwp.client.ClientTeamData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "pwp", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientGuiHandler {

    private static final TicketOverlay       ticketOverlay   = new TicketOverlay(0);

    private static final SquadOverlay        squadOverlay    = new SquadOverlay();
    private static final VitalsOverlay       vitalsOverlay   = new VitalsOverlay();
    private static final NotificationOverlay notifOverlay    = new NotificationOverlay();
    private static final HotbarOverlay       hotbarOverlay   = new HotbarOverlay();
    private static final BattlefieldTabOverlay tabOverlay    = new BattlefieldTabOverlay();
    private static final VoiceChatHudOverlay voiceChatHudOverlay = new VoiceChatHudOverlay();
    private static final KillFeedOverlay     killFeedOverlay = new KillFeedOverlay();
    //private static final CaptureNotificationOverlay captureNotifOverlay = new CaptureNotificationOverlay();
    private static final CompassOverlay             compassOverlay      = new CompassOverlay();
    private static final VoteOverlay                voteOverlay         = new VoteOverlay();
    private static final ReportHudOverlay           reportHudOverlay     = new ReportHudOverlay();
    private static final BleedingOverlay            bleedingOverlay      = new BleedingOverlay();

    public static BattlefieldTabOverlay getTabOverlay() {
        return tabOverlay;
    }

    private static boolean hudRenderedThisFrame = false;

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            AnimationHelper.updateOpacityCache();
            hudRenderedThisFrame = false;
        }
    }

    @SubscribeEvent
    public static void onPreRenderHUD(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() ||
            event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type() ||
            event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type() ||
            event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type() ||
            event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type() ||
            event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type() ||
            event.getOverlay() == VanillaGuiOverlay.PLAYER_LIST.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPostRenderAnyOverlay(RenderGuiOverlayEvent.Post event) {
        if (hudRenderedThisFrame) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.screen != null) return;
        hudRenderedThisFrame = true;
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        float scale = ClientTeamData.guiScale;
        if (Math.abs(scale - 1.0f) > 0.01f) {
            float ox = w / 2f * (1f - scale);
            float oy = h / 2f * (1f - scale);
            g.pose().pushPose();
            g.pose().translate(ox, oy, 0);
            g.pose().scale(scale, scale, 1f);
        }

        boolean tabVisible = tabOverlay.isVisible();

        if (!tabVisible) {
            if (ClientTeamData.showCompass)    compassOverlay.render(g, w);
            if (ClientTeamData.showTicketBar)  ticketOverlay.render(g, w, event.getPartialTick());
            if (ClientTeamData.showSquad)      squadOverlay.render(g, h);
            if (ClientTeamData.showVitals && ClientTeamData.getGamePhase() >= 2) vitalsOverlay.render(g, w, h);
            notifOverlay.render(g, w);
            if (ClientTeamData.showHotbar)     hotbarOverlay.render(g, w, h);
            voiceChatHudOverlay.render(g, w, h);
            if (ClientTeamData.showKillFeed)   killFeedOverlay.render(g, w);
            //captureNotifOverlay.render(g, w, h);
            voteOverlay.render(g, w, h);
            reportHudOverlay.render(g, w);
        }

        tabOverlay.render(g, w, h);

        bleedingOverlay.render(g, w, h);

        if (Math.abs(scale - 1.0f) > 0.01f) {
            g.pose().popPose();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            VehicleCameraHandler.handlePerspectiveKey();
        } else {
            tabOverlay.tick();
            ClientVoiceHandler.tick();
            //captureNotifOverlay.tickFromCapturePoints(ClientTeamData.capturePoints);
            reportHudOverlay.tick();
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!ClientTeamData.useCustomMenu) return;
        if (event.getScreen() instanceof PauseScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                event.setNewScreen(new BattlefieldPauseScreen());
            }
        }
        if (event.getScreen() instanceof TitleScreen) {
            event.setNewScreen(new BattlefieldMainMenuScreen());
        }
    }

}
