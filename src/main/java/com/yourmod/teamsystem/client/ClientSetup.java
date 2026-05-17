package com.yourmod.teamsystem.client;

import com.yourmod.teamsystem.client.gui.renderer.CaptureParticles;
import com.yourmod.teamsystem.client.gui.renderer.CustomNametagRenderer;
import com.yourmod.teamsystem.client.gui.renderer.WorldMarkerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "teamsystem", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientLevelRender(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            WorldMarkerRenderer.render(event.getPoseStack(), event.getCamera(), event.getProjectionMatrix());
            CaptureParticles.render(event);
        }
    }

    @SubscribeEvent
    public static void onRenderNametag(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof Player player && !player.isSpectator()) {
            CustomNametagRenderer.render(event.getPoseStack(), event.getMultiBufferSource(), player, event.getPackedLight());
        }
    }
}
