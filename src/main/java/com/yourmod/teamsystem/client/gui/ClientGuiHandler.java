package com.yourmod.teamsystem.client.gui;

import com.yourmod.teamsystem.client.ClientVoiceHandler;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.overlay.*;
import com.yourmod.teamsystem.client.gui.screen.BattlefieldLoadingScreen;
import com.yourmod.teamsystem.client.gui.screen.BattlefieldMainMenuScreen;
import com.yourmod.teamsystem.client.gui.screen.SpawnScreenHelper;
import com.yourmod.teamsystem.client.gui.screen.BattlefieldPauseScreen;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.screen.VoteScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
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
    private static final PlayerLabelOverlay  playerLabelOverlay = new PlayerLabelOverlay();
    private static final KillFeedOverlay     killFeedOverlay = new KillFeedOverlay();
    private static final CaptureNotificationOverlay captureNotifOverlay = new CaptureNotificationOverlay();
    private static final CompassOverlay             compassOverlay      = new CompassOverlay();

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
            compassOverlay.render(g, w);
            ticketOverlay.render(g, w, event.getPartialTick());
            squadOverlay.render(g, h);
            vitalsOverlay.render(g, w, h);
            notifOverlay.render(g, w);
            hotbarOverlay.render(g, w, h);
            playerLabelOverlay.render(g, w, h);
            killFeedOverlay.render(g, w);
            captureNotifOverlay.render(g, w, h);
        }

        tabOverlay.render(g, w, h);

        if (Math.abs(scale - 1.0f) > 0.01f) {
            g.pose().popPose();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tabOverlay.tick();
            ClientVoiceHandler.tick();
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
        if (event.getScreen() instanceof TitleScreen) {
            event.setNewScreen(new BattlefieldMainMenuScreen());
        }
    }

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

        // Auto-dismiss if on main menu with no loading happening for 3+ seconds
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
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SpawnScreenHelper.clear();
        BattlefieldLoadingScreen.show();
    }
}
