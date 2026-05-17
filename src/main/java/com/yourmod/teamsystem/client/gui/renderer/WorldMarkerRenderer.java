package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.yourmod.teamsystem.client.ClientMarkerData;
import com.yourmod.teamsystem.core.MarkerData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class WorldMarkerRenderer {
    private static final double RENDER_DISTANCE = 64.0;
    private static final ResourceLocation ICON_ATLAS = new ResourceLocation("teamsystem", "textures/gui/markers.png");

    public static void render(PoseStack poseStack, Camera camera, Matrix4f projectionMatrix) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Vec3 cameraPos = camera.getPosition();
        List<MarkerData> markers = ClientMarkerData.getMarkers();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        poseStack.pushPose();

        for (MarkerData marker : markers) {
            if (!marker.getDimension().equals(player.level().dimension().location())) continue;

            double dx = marker.getX() - cameraPos.x;
            double dy = marker.getY() - cameraPos.y;
            double dz = marker.getZ() - cameraPos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > RENDER_DISTANCE) continue;

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);

            poseStack.mulPose(camera.rotation());
            float scale = 0.025f;
            poseStack.scale(-scale, -scale, scale);

            int color = switch (marker.getType()) {
                case ATTACK -> 0xFFFF4444;
                case DEFEND -> 0xFF4444FF;
                case OBSERVE -> 0xFFFFAA00;
                default -> 0xFFFFFFFF;
            };

            float a = ((color >> 24) & 0xFF) / 255f;
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            float s = 8f;
            Matrix4f mat = poseStack.last().pose();

            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            builder.vertex(mat, -s, -s, 0).color(r, g, b, a).endVertex();
            builder.vertex(mat, s, -s, 0).color(r, g, b, a).endVertex();
            builder.vertex(mat, s, s, 0).color(r, g, b, a).endVertex();
            builder.vertex(mat, -s, s, 0).color(r, g, b, a).endVertex();
            tesselator.end();

            poseStack.popPose();
        }

        poseStack.popPose();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
