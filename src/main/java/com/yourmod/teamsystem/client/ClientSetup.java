package com.yourmod.teamsystem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourmod.teamsystem.client.gui.renderer.CaptureParticles;
import com.yourmod.teamsystem.client.gui.renderer.CustomNametagRenderer;
import com.yourmod.teamsystem.client.gui.renderer.WorldMarkerRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "teamsystem", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientSetup {

    private static final WorldMarkerRenderer markerRenderer = new WorldMarkerRenderer();
    private static final CaptureParticles captureParticles = new CaptureParticles();
    private static final CustomNametagRenderer nametagRenderer = new CustomNametagRenderer();

    @SubscribeEvent
    public static void onClientLevelRender(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            Camera camera = event.getCamera();
            float partialTick = event.getPartialTick();

            markerRenderer.render(poseStack, bufferSource, camera, partialTick);
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
