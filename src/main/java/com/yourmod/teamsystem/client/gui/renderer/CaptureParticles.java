package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.CapturePointData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Random;

public class CaptureParticles {
    private static final Random RANDOM = new Random();

    public static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();

        long time = System.currentTimeMillis();
        double baseAngle = (time % 4000) / 4000.0 * Math.PI * 2;

        for (CapturePointData cp : ClientTeamData.capturePoints) {
            double px = cp.id() * 50 + 0.5;
            double py = 70;
            double pz = cp.id() * 50 + 0.5;

            poseStack.pushPose();
            double dx = px - cameraPos.x;
            double dy = py - cameraPos.y;
            double dz = pz - cameraPos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 64.0) {
                poseStack.popPose();
                continue;
            }
            poseStack.translate(dx, dy, dz);

            int teamColor = switch (cp.ownerTeamOrdinal()) {
                case 1 -> 0x55FF5555;
                case 2 -> 0x555555FF;
                default -> 0x55FFFFFF;
            };

            float a = ((teamColor >> 24) & 0xFF) / 255f;
            float r = ((teamColor >> 16) & 0xFF) / 255f;
            float g = ((teamColor >> 8) & 0xFF) / 255f;
            float b = (teamColor & 0xFF) / 255f;

            Matrix4f mat = poseStack.last().pose();
            float particleSize = 0.3f;

            for (int i = 0; i < 8; i++) {
                double angle = baseAngle + i * Math.PI / 4;
                float ox = (float) (Math.cos(angle) * 2.0);
                float oz = (float) (Math.sin(angle) * 2.0);
                float oy = (float) (Math.sin(time * 0.002 + i) * 1.0);

                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                builder.vertex(mat, ox - particleSize, oy - particleSize, oz).color(r, g, b, a).endVertex();
                builder.vertex(mat, ox + particleSize, oy - particleSize, oz).color(r, g, b, a).endVertex();
                builder.vertex(mat, ox + particleSize, oy + particleSize, oz).color(r, g, b, a).endVertex();
                builder.vertex(mat, ox - particleSize, oy + particleSize, oz).color(r, g, b, a).endVertex();
                tesselator.end();
            }

            poseStack.popPose();
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
