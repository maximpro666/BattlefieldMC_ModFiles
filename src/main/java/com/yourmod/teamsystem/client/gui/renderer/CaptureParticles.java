package com.yourmod.teamsystem.client.gui.renderer;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.CapturePointData;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.RenderLevelStageEvent;

public class CaptureParticles {

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        var points = ClientTeamData.capturePoints;
        if (points.isEmpty()) return;

        var poseStack = event.getPoseStack();
        var bufferSource = mc.renderBuffers().bufferSource();
        var builder = bufferSource.getBuffer(RenderType.LINES);
        var camPos = event.getCamera().getPosition();

        for (CapturePointData point : points) {
            double progress = point.progress();
            if (progress <= 0) continue;

            int teamOrdinal = point.capturingTeamOrdinal();
            int argb = teamOrdinal == 0 ? 0x4444FFFF : 0xFFFF4444;
            float r = ((argb >> 24) & 0xFF) / 255f;
            float g = ((argb >> 16) & 0xFF) / 255f;
            float b = ((argb >> 8) & 0xFF) / 255f;

            poseStack.pushPose();
            poseStack.translate(
                -(camPos.x - (point.id() * 10)),
                -(camPos.y - 2.5 - progress * 2),
                -(camPos.z)
            );
            var mat = poseStack.last().pose();

            float radius = 1.5f + (float) progress * 0.5f;
            for (int i = 0; i < 8; i++) {
                float angle = (float) (i / 8.0 * Math.PI * 2);
                float px = (float) Math.cos(angle) * radius;
                float pz = (float) Math.sin(angle) * radius;
                float nx = (float) Math.cos(angle + 0.1) * radius * 0.9f;
                float nz = (float) Math.sin(angle + 0.1) * radius * 0.9f;
                builder.vertex(mat, px, 0, pz).color(r, g, b, 0.7f).endVertex();
                builder.vertex(mat, nx, (float) (Math.sin(event.getPartialTick() + i) * 0.3), nz).color(r, g, b, 0.7f).endVertex();
            }
            poseStack.popPose();
        }
        bufferSource.endBatch(RenderType.LINES);
    }
}
