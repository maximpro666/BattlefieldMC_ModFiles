package com.yourmod.teamsystem.client.gui;

import com.yourmod.teamsystem.client.gui.overlay.*;
import com.yourmod.teamsystem.client.gui.screen.BattlefieldPauseScreen;
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
    private static final CompassOverlay      compassOverlay  = new CompassOverlay();
    private static final SquadOverlay        squadOverlay    = new SquadOverlay();
    private static final VitalsOverlay       vitalsOverlay   = new VitalsOverlay();
    private static final NotificationOverlay notifOverlay    = new NotificationOverlay();
    private static final KillFeedOverlay     killFeedOverlay = new KillFeedOverlay();
    private static final HotbarOverlay       hotbarOverlay   = new HotbarOverlay();
    private static final BattlefieldTabOverlay tabOverlay    = new BattlefieldTabOverlay();
    private static final CaptureNotificationOverlay captureOverlay = new CaptureNotificationOverlay();
    private static final VoiceIndicatorOverlay voiceOverlay  = new VoiceIndicatorOverlay();

    @SubscribeEvent
    public static void onRenderHUD(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        float yaw = mc.player.getYRot();

        ticketOverlay.render(g, w, event.getPartialTick());
        compassOverlay.render(g, w, yaw);
        squadOverlay.render(g, h);
        vitalsOverlay.render(g, w, h);
        notifOverlay.render(g, w);
        killFeedOverlay.render(g, w);
        hotbarOverlay.render(g, w, h);
        tabOverlay.render(g, w, h);
        captureOverlay.render(g, w, h);
        voiceOverlay.render(g, w, h);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tabOverlay.tick();
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
}
