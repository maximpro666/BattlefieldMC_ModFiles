package com.yourmod.teamsystem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourmod.teamsystem.client.gui.renderer.CaptureParticles;
import com.yourmod.teamsystem.client.gui.renderer.CustomNametagRenderer;
import com.yourmod.teamsystem.client.gui.renderer.WorldMarkerRenderer;
import com.yourmod.teamsystem.client.gui.screen.KitSelectionScreen;
import com.yourmod.teamsystem.client.gui.screen.VehicleSelectionScreen;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

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
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (OPEN_KIT_VEHICLE_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                mc.setScreen(new KitSelectionScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onClientLevelRender(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            Camera camera = event.getCamera();
            float partialTick = event.getPartialTick();

            markerRenderer.render(poseStack, bufferSource, camera, partialTick);

            List<CapturePointData> points = ClientTeamData.capturePoints;
            if (points != null) {
                for (CapturePointData cp : points) {
                    captureParticles.render(poseStack, bufferSource,
                        cp.x(), cp.y() + 1.0, cp.z(),
                        camera.getPosition().x, camera.getPosition().y, camera.getPosition().z,
                        partialTick);
                }
            }
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
