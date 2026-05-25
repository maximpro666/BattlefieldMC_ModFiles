package com.pigeostudios.pwp.client;

import com.pigeostudios.pwp.client.gui.screen.BattlefieldLoadingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "pwp", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LoadingHandler {

    @SubscribeEvent
    public static void onClientTickLoading(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        boolean hasWorld = mc.level != null && mc.player != null;

        if (mc.getOverlay() instanceof LoadingOverlay) {
            BattlefieldLoadingScreen.show();
        }

        BattlefieldLoadingScreen loading = BattlefieldLoadingScreen.getInstance();
        if (loading == null) return;

        boolean hasConnection = mc.getConnection() != null;

        if (!hasWorld && !hasConnection && System.currentTimeMillis() - loading.getOpenTime() > 3000) {
            loading.dismiss();
            return;
        }

        int connStage = hasConnection ? 2 : (hasWorld ? 1 : 0);
        int cfgStage = !ClientTeamData.receivedKitConfigJson.isEmpty() ? 2 : (hasWorld && hasConnection ? 1 : 0);
        int teamStage = hasWorld && hasConnection ? 2 : 0;

        BattlefieldLoadingScreen.updateProgress(connStage, cfgStage, teamStage);
    }

    @SubscribeEvent
    public static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        BattlefieldLoadingScreen.show();
        ClientHWID.onPlayerJoin();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        com.pigeostudios.pwp.client.gui.screen.SpawnScreenHelper.clear();
        ClientTeamData.resetVoteData();
        BattlefieldLoadingScreen.show();
        ClientHWID.reset();
    }
}
