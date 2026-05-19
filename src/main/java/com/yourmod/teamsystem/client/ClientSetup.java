package com.yourmod.teamsystem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.client.gui.renderer.CaptureParticles;
import com.yourmod.teamsystem.client.gui.renderer.CustomNametagRenderer;
import com.yourmod.teamsystem.client.gui.renderer.WorldMarkerRenderer;
import com.yourmod.teamsystem.client.gui.screen.ClassSelectionScreen;
import com.yourmod.teamsystem.client.xaero.XaeroIntegration;
import com.yourmod.teamsystem.core.GameManager;
import net.minecraft.client.Camera;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "teamsystem", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientSetup {

    private static final WorldMarkerRenderer markerRenderer = new WorldMarkerRenderer();
    private static final CaptureParticles captureParticles = new CaptureParticles();
    private static final CustomNametagRenderer nametagRenderer = new CustomNametagRenderer();
    public static final KeyMapping OPEN_KIT_VEHICLE_KEY = new KeyMapping(
        "key.teamsystem.open_kit_vehicle",
        GLFW.GLFW_KEY_G,
        "key.categories.teamsystem"
    );

    @Mod.EventBusSubscriber(modid = "teamsystem", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModBus {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_KIT_VEHICLE_KEY);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            XaeroIntegration.init();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (OPEN_KIT_VEHICLE_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                GameManager gm = TeamSystem.getGameManager();
                if (gm != null && gm.isPlaying()) {
                    if (!isNearCapturePoint(mc.player)) {
                        mc.player.displayClientMessage(
                            Component.literal("\u00a7e[\u00a76BF\u00a7e] \u00a7cYou must be near a capture point to change loadout!"),
                            true);
                        return;
                    }
                }
                mc.setScreen(new ClassSelectionScreen());
            }
        }
    }

    private static boolean isNearCapturePoint(Player player) {
        List<CapturePointData> points = ClientTeamData.capturePoints;
        if (points == null || points.isEmpty()) return false;
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        for (CapturePointData cp : points) {
            double dx = px - cp.x();
            double dy = py - cp.y();
            double dz = pz - cp.z();
            if (dx * dx + dz * dz <= 4.5 * 4.5 && Math.abs(dy) <= 6.0) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onClientLevelRender(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            Camera camera = event.getCamera();
            float partialTick = event.getPartialTick();
            Vec3 camPos = camera.getPosition();

            markerRenderer.render(poseStack, bufferSource, camera, partialTick);

            captureParticles.render(poseStack, bufferSource,
                camPos.x, camPos.y, camPos.z, partialTick);
        }
    }

    @SubscribeEvent
    public static void onRenderNametag(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof Player player && !player.isSpectator()) {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            nametagRenderer.renderNametag(event.getPoseStack(), event.getMultiBufferSource(), player, camera);
        }
    }
}
