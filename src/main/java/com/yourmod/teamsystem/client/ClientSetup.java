package com.yourmod.teamsystem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourmod.teamsystem.TeamSystem;
import com.yourmod.teamsystem.client.gui.ClientGuiHandler;
import com.yourmod.teamsystem.client.gui.renderer.CaptureParticles;
import com.yourmod.teamsystem.client.gui.renderer.CustomNametagRenderer;
import com.yourmod.teamsystem.client.gui.renderer.WorldMarkerRenderer;
import com.yourmod.teamsystem.client.gui.screen.ClassSelectionScreen;
import com.yourmod.teamsystem.client.gui.screen.ResupplyScreen;
import com.yourmod.teamsystem.client.gui.screen.VehicleSelectionScreen;
import com.yourmod.teamsystem.client.journeymap.JourneyMapIntegration;
import com.yourmod.teamsystem.core.GameManager;
import com.yourmod.teamsystem.core.Team;
import com.yourmod.teamsystem.network.PacketHandler;
import net.minecraft.client.Camera;
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

    public static final KeyMapping OPEN_VEHICLE_KEY = new KeyMapping(
        "key.teamsystem.open_vehicle",
        GLFW.GLFW_KEY_H,
        "key.categories.teamsystem"
    );

    public static final KeyMapping SQUAD_VOICE_KEY = new KeyMapping(
        "key.teamsystem.squad_voice",
        GLFW.GLFW_KEY_V,
        "key.categories.teamsystem"
    );

    public static final KeyMapping TEAM_VOICE_KEY = new KeyMapping(
        "key.teamsystem.team_voice",
        GLFW.GLFW_KEY_B,
        "key.categories.teamsystem"
    );

    public static final KeyMapping REQUEST_AMMO_KEY = new KeyMapping(
        "key.teamsystem.request_ammo",
        GLFW.GLFW_KEY_R,
        "key.categories.teamsystem"
    );

    @Mod.EventBusSubscriber(modid = "teamsystem", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModBus {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_KIT_VEHICLE_KEY);
            event.register(OPEN_VEHICLE_KEY);
            event.register(SQUAD_VOICE_KEY);
            event.register(TEAM_VOICE_KEY);
            event.register(REQUEST_AMMO_KEY);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            JourneyMapIntegration.init();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        int key = event.getKey();
        int action = event.getAction();

        if (key == GLFW.GLFW_KEY_TAB) {
            if (action == GLFW.GLFW_PRESS) {
                ClientGuiHandler.getTabOverlay().setVisible(true);
            } else if (action == GLFW.GLFW_RELEASE) {
                ClientGuiHandler.getTabOverlay().setVisible(false);
            }
        }

        if (key == SQUAD_VOICE_KEY.getKey().getValue()) {
            if (action == GLFW.GLFW_PRESS) {
                ClientVoiceHandler.setSquadPtt(true);
            } else if (action == GLFW.GLFW_RELEASE) {
                ClientVoiceHandler.setSquadPtt(false);
            }
        }
        if (key == TEAM_VOICE_KEY.getKey().getValue()) {
            if (action == GLFW.GLFW_PRESS) {
                ClientVoiceHandler.setTeamPtt(true);
            } else if (action == GLFW.GLFW_RELEASE) {
                ClientVoiceHandler.setTeamPtt(false);
            }
        }

        if (OPEN_KIT_VEHICLE_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                GameManager gm = TeamSystem.getGameManager();
                if (gm != null && gm.isPlaying()) {
                    if (!isNearTeamBase(mc.player)) {
                        mc.player.displayClientMessage(
                            Component.literal("\u00a7e[\u00a76BF\u00a7e] \u00a7cYou must be at your team's base to change loadout!"),
                            true);
                        return;
                    }
                }
                mc.setScreen(new ClassSelectionScreen());
            }
        }

        if (OPEN_VEHICLE_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                mc.setScreen(new VehicleSelectionScreen());
            }
        }

        if (REQUEST_AMMO_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                mc.setScreen(new ResupplyScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (ClientGuiHandler.getTabOverlay().isVisible()) {
            double delta = event.getScrollDelta();
            ClientGuiHandler.getTabOverlay().scrollBy((int) -delta * 15);
            event.setCanceled(true);
        }
    }

    private static boolean isNearTeamBase(Player player) {
        Team team = ClientTeamData.getLocalPlayerTeam();
        if (team == Team.SPECTATOR) return false;
        int[] basePos = team == Team.NATO ? ClientTeamData.getNatoBasePos() : ClientTeamData.getRussiaBasePos();
        if (basePos == null) return false;
        double dx = player.getX() - (basePos[0] + 0.5);
        double dz = player.getZ() - (basePos[2] + 0.5);
        double radius = ClientTeamData.getBaseRadius();
        return dx * dx + dz * dz <= radius * radius;
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
