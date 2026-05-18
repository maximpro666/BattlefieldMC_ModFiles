package com.yourmod.teamsystem.client.gui.renderer;

import com.yourmod.teamsystem.client.CapturePointData;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.UITheme;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.List;

public class CaptureParticles {

    private static final int RING_SEGMENTS = 32;
    private static final float INNER_RADIUS = 3.2f;
    private static final float OUTER_RADIUS = 3.8f;
    private static final int COLOR_NEUTRAL = 0x55AAAAAA;
    private static final int COLOR_NATO = (UITheme.TEAM_NATO & 0x00FFFFFF) | 0x55000000;
    private static final int COLOR_RUSSIA = (UITheme.TEAM_RUSSIA & 0x00FFFFFF) | 0x55000000;
    private static final int COLOR_CAPTURING = (UITheme.ACCENT & 0x00FFFFFF) | 0x88000000;
    private static final int COLOR_CONTESTED = 0x55FF4444;

    private long startTime = System.currentTimeMillis();

    public void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       double camX, double camY, double camZ, float partialTick) {
        List<CapturePointData> points = ClientTeamData.capturePoints;
        if (points == null || points.isEmpty()) return;

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;

        for (CapturePointData cp : points) {
            double dx = cp.x() - camX;
            double dy = cp.y() - camY;
            double dz = cp.z() - camZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 128) continue;

            int ownerOrdinal = cp.ownerTeamOrdinal();
            int capturerOrdinal = cp.capturingTeamOrdinal();
            boolean isOwned = ownerOrdinal == 0 || ownerOrdinal == 1;
            double prog = cp.progress();
            boolean isContested = isOwned && capturerOrdinal == 2 && prog > 0.01f && prog < 1.0f;
            boolean isCapturing = prog > 0.01f && prog < 1.0f
                && capturerOrdinal >= 0 && capturerOrdinal != ownerOrdinal && capturerOrdinal != 2;

            int ringColor;
            float rotSpeed;
            float alphaMul;
            if (isContested) {
                ringColor = COLOR_CONTESTED;
                rotSpeed = 2.0f;
                alphaMul = (float)(Math.sin(elapsed * 4.0) * 0.4 + 0.6);
            } else if (isCapturing) {
                ringColor = COLOR_CAPTURING;
                rotSpeed = 1.2f;
                alphaMul = 1.0f;
            } else {
                ringColor = switch (ownerOrdinal) {
                    case 0 -> COLOR_NATO;
                    case 1 -> COLOR_RUSSIA;
                    default -> COLOR_NEUTRAL;
                };
                rotSpeed = 0.4f;
                alphaMul = 0.6f;
            }

            float rotation = elapsed * rotSpeed;

            // Ground ring (flat on terrain) — subtle animated glow
            poseStack.pushPose();
            poseStack.translate(cp.x() - camX, cp.y() - camY + 0.06, cp.z() - camZ);
            VertexConsumer vc = bufferSource.getBuffer(RenderType.debugLineStrip(1.5));
            Matrix4f mat = poseStack.last().pose();

            for (int i = 0; i <= RING_SEGMENTS; i++) {
                float angle = (float)(i / (double) RING_SEGMENTS * Math.PI * 2.0) + rotation;
                float nx = (float)(Math.cos(angle) * OUTER_RADIUS);
                float nz = (float)(Math.sin(angle) * OUTER_RADIUS);
                int col = ringColor;
                float a = ((col >> 24) & 0xFF) / 255f * alphaMul;
                float r = ((col >> 16) & 0xFF) / 255f;
                float g = ((col >> 8) & 0xFF) / 255f;
                float b = (col & 0xFF) / 255f;
                vc.vertex(mat, nx, 0, nz).color(r, g, b, a).endVertex();

                float ix = (float)(Math.cos(angle) * INNER_RADIUS);
                float iz = (float)(Math.sin(angle) * INNER_RADIUS);
                vc.vertex(mat, ix, 0, iz).color(r, g, b, a * 0.6f).endVertex();
            }
            poseStack.popPose();

            // Vertical glow columns
            poseStack.pushPose();
            poseStack.translate(cp.x() - camX, cp.y() - camY + 0.06, cp.z() - camZ);
            vc = bufferSource.getBuffer(RenderType.debugLineStrip(1.0));
            mat = poseStack.last().pose();

            int col = ringColor;
            float a = ((col >> 24) & 0xFF) / 255f * alphaMul;
            float r = ((col >> 16) & 0xFF) / 255f;
            float g = ((col >> 8) & 0xFF) / 255f;
            float b = (col & 0xFF) / 255f;

            for (int i = 0; i < 8; i++) {
                float angle = (float)(i / 8.0 * Math.PI * 2.0) + rotation;
                float nx = (float)(Math.cos(angle) * INNER_RADIUS);
                float nz = (float)(Math.sin(angle) * INNER_RADIUS);
                float height = (float)(Math.sin(elapsed * 1.5 + i) * 0.5 + 0.8);
                vc.vertex(mat, nx, 0, nz).color(r, g, b, a).endVertex();
                vc.vertex(mat, nx, height, nz).color(r, g, b, a * 0.3f).endVertex();
            }
            poseStack.popPose();
        }
    }
}
