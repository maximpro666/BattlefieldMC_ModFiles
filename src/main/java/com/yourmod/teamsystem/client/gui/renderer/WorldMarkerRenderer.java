package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yourmod.teamsystem.client.ClientMarkerData;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.core.MarkerData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class WorldMarkerRenderer {
    private static final double RENDER_DIST = 64.0;

    public static void render(PoseStack poseStack, Camera camera, Matrix4f projectionMatrix) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3 camPos = camera.getPosition();
        var markers = ClientMarkerData.getMarkers();
        if (markers.isEmpty()) return;

        var bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer builder = bufferSource.getBuffer(RenderType.LINES);

        poseStack.pushPose();
        Vec3 markerPos;
        for (MarkerData marker : markers) {
            markerPos = new Vec3(marker.getX(), marker.getY(), marker.getZ());
            double dist = markerPos.distanceTo(camPos);
            if (dist > RENDER_DIST) continue;

            double dx = marker.getX() - camPos.x;
            double dy = marker.getY() - camPos.y;
            double dz = marker.getZ() - camPos.z;

            int teamColor = marker.getTeamOrdinal() == 0 ? 0x4444FFFF : 0xFF4444FF;
            float r = ((teamColor >> 24) & 0xFF) / 255f;
            float g = ((teamColor >> 16) & 0xFF) / 255f;
            float b = ((teamColor >> 8) & 0xFF) / 255f;
            float a = (teamColor & 0xFF) / 255f;

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            Matrix4f mat = poseStack.last().pose();
            builder.vertex(mat, 0, 0, 0).color(r, g, b, a).endVertex();
            builder.vertex(mat, 0, 2, 0).color(r, g, b, a).endVertex();
            poseStack.popPose();
        }
        poseStack.popPose();
        bufferSource.endBatch(RenderType.LINES);
    }
}
