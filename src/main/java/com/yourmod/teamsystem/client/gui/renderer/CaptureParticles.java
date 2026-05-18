package com.yourmod.teamsystem.client.gui.renderer;

import com.yourmod.teamsystem.client.gui.UITheme;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public class CaptureParticles {

    private static final int   PARTICLE_COUNT = 32;
    private static final float RING_RADIUS    = 3.5f;
    private static final float PARTICLE_SIZE  = 0.08f;
    private static final int   COLOR_ORANGE   = UITheme.ACCENT;
    private static final int   COLOR_WHITE    = UITheme.TEXT_PRIMARY;

    private long startTime = System.currentTimeMillis();

    public void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       double worldX, double worldY, double worldZ,
                       double camX, double camY, double camZ,
                       float partialTick) {

        float elapsed  = (System.currentTimeMillis() - startTime) / 1000f;
        float rotation = elapsed * 0.8f;

        poseStack.pushPose();
        poseStack.translate(worldX - camX, worldY - camY + 0.1, worldZ - camZ);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.debugLineStrip(2.0));
        Matrix4f mat = poseStack.last().pose();

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float angle = (float)(i / (double) PARTICLE_COUNT * Math.PI * 2.0) + rotation;
            float nx    = (float)(Math.cos(angle) * RING_RADIUS);
            float nz    = (float)(Math.sin(angle) * RING_RADIUS);
            int col = (i % 2 == 0) ? COLOR_ORANGE : COLOR_WHITE;
            float a = ((col >> 24) & 0xFF) / 255f;
            float r = ((col >> 16) & 0xFF) / 255f;
            float g = ((col >>  8) & 0xFF) / 255f;
            float b = ( col        & 0xFF) / 255f;

            float hs = PARTICLE_SIZE;
            vc.vertex(mat, nx - hs, hs, nz - hs).color(r, g, b, a).endVertex();
            vc.vertex(mat, nx + hs, hs, nz + hs).color(r, g, b, a).endVertex();
        }

        poseStack.popPose();
    }
}
