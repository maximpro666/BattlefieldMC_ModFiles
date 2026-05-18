package com.yourmod.teamsystem.client.gui;

import com.yourmod.teamsystem.client.gui.overlay.*;
import com.yourmod.teamsystem.client.gui.screen.BattlefieldPauseScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "teamsystem", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientGuiHandler {

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.PLAYER_LIST.id(), "bf_tab", new BattlefieldTabOverlay());
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "bf_compass", new CompassOverlay());
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "bf_tickets", new TicketOverlay());
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "bf_squad", new SquadOverlay());
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "bf_vitals", new VitalsOverlay());
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "bf_notifications", new NotificationOverlay());
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "bf_killfeed", new KillFeedOverlay());
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "bf_capture_notification", new CaptureNotificationOverlay());
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "bf_hotbar", new HotbarOverlay());
    }

    @Mod.EventBusSubscriber(modid = "teamsystem", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientGuiForgeEvents {
        @SubscribeEvent
        public static void onScreenOpen(ScreenEvent.Opening event) {
            if (event.getScreen() instanceof PauseScreen) {
                event.setNewScreen(new BattlefieldPauseScreen());
            }
        }
    }
}
