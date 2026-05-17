package com.yourmod.teamsystem.client;

import com.yourmod.teamsystem.client.gui.screen.BattlefieldPauseScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "teamsystem", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientGuiHandler {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        String path = event.getOverlay().id().getPath();
        if (!path.equals("hotbar")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof PauseScreen) {
            event.setNewScreen(new BattlefieldPauseScreen());
        }
    }
}
