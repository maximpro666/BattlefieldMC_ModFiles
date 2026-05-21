package com.pigeostudios.pwp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pigeostudios.pwp.blockentity.RespawnBeaconBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;

public class RespawnBeaconBlockRenderer implements BlockEntityRenderer<RespawnBeaconBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("pwp", "textures/entity/fob_beacon.png");

    public RespawnBeaconBlockRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(RespawnBeaconBlockEntity beacon, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        pose.pushPose();

        // Центрируем и поднимаем модель чуть выше блока
        pose.translate(0.5, 0.0, 0.5);
        // Масштаб — чуть больше 1 блока
        float scale = 1.3f;
        pose.scale(scale, scale, scale);
        // Парим — лёгкое покачивание вверх-вниз
        double time = beacon.getLevel() != null ? beacon.getLevel().getGameTime() + partialTick : 0;
        float floatOffset = (float) Math.sin(time * 0.05) * 0.1f;
        pose.translate(0, floatOffset, 0);
        // Медленное вращение
        pose.mulPose(Axis.YP.rotationDegrees((float)(time * 0.8)));

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));

        // Рисуем красивый маяк — 8-гранная призма с основанием и пирамидкой сверху
        renderBeacon(pose, consumer, packedLight);

        pose.popPose();
    }

    private void renderBeacon(PoseStack pose, VertexConsumer consumer, int packedLight) {
        // Параметры: радиус основания, высота колонны, радиус вершины, высота пирамидки
        float baseR = 0.5f;
        float columnH = 0.8f;
        float topR = 0.2f;
        float pyramidH = 0.4f;

        // Восьмиугольная призма (колонна)
        int segments = 8;
        float[][] topVerts = new float[segments][3];
        float[][] botVerts = new float[segments][3];

        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float cx = (float) Math.cos(angle);
            float sz = (float) Math.sin(angle);
            botVerts[i] = new float[]{cx * baseR, 0, sz * baseR};
            topVerts[i] = new float[]{cx * topR, columnH, sz * topR};
        }

        // Бока колонны
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            addQuad(pose, consumer, packedLight,
                botVerts[i][0], botVerts[i][1], botVerts[i][2],
                botVerts[next][0], botVerts[next][1], botVerts[next][2],
                topVerts[next][0], topVerts[next][1], topVerts[next][2],
                topVerts[i][0], topVerts[i][1], topVerts[i][2],
                0, 0, 0, 1, 1, 0
            );
        }

        // Основание (дно)
        float baseY = 0;
        // Пирамидка сверху
        float tipX = 0, tipY = columnH + pyramidH, tipZ = 0;
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            addTri(pose, consumer, packedLight,
                topVerts[i][0], topVerts[i][1], topVerts[i][2],
                topVerts[next][0], topVerts[next][1], topVerts[next][2],
                tipX, tipY, tipZ
            );
        }

        // Кольцо-гало вокруг, чуть больше блока
        renderHalo(pose, consumer, packedLight, segments);
    }

    private void renderHalo(PoseStack pose, VertexConsumer consumer, int packedLight, int segments) {
        float haloR = 0.7f;
        float haloY = 0.3f;
        float[][] ring = new float[segments][3];
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            ring[i] = new float[]{
                (float) Math.cos(angle) * haloR, haloY, (float) Math.sin(angle) * haloR
            };
        }
        // Тонкое кольцо
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            float dy = 0.04f;
            addQuad(pose, consumer, packedLight,
                ring[i][0], ring[i][1], ring[i][2],
                ring[next][0], ring[next][1], ring[next][2],
                ring[next][0], ring[next][1] + dy, ring[next][2],
                ring[i][0], ring[i][1] + dy, ring[i][2],
                1, 1, 1, 1, 1, 1
            );
        }
    }

    private void addQuad(PoseStack pose, VertexConsumer consumer, int light,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float r, float g, float b, float u1, float v1, float u2) {
        Matrix4f mat = pose.last().pose();
        Vector3f normal = getNormal(x1, y1, z1, x2, y2, z2, x3, y3, z3);
        consumer.vertex(mat, x1, y1, z1).color(r, g, b, 1.0f).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal.x, normal.y, normal.z).endVertex();
        consumer.vertex(mat, x2, y2, z2).color(r, g, b, 1.0f).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal.x, normal.y, normal.z).endVertex();
        consumer.vertex(mat, x3, y3, z3).color(r, g, b, 1.0f).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal.x, normal.y, normal.z).endVertex();
        consumer.vertex(mat, x4, y4, z4).color(r, g, b, 1.0f).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal.x, normal.y, normal.z).endVertex();
    }

    private void addTri(PoseStack pose, VertexConsumer consumer, int light,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3) {
        Matrix4f mat = pose.last().pose();
        Vector3f normal = getNormal(x1, y1, z1, x2, y2, z2, x3, y3, z3);
        consumer.vertex(mat, x1, y1, z1).color(1, 1, 1, 1.0f).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal.x, normal.y, normal.z).endVertex();
        consumer.vertex(mat, x2, y2, z2).color(1, 1, 1, 1.0f).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal.x, normal.y, normal.z).endVertex();
        consumer.vertex(mat, x3, y3, z3).color(1, 1, 1, 1.0f).uv(0.5f, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal.x, normal.y, normal.z).endVertex();
    }

    private Vector3f getNormal(float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3) {
        float ax = x2 - x1, ay = y2 - y1, az = z2 - z1;
        float bx = x3 - x1, by = y3 - y1, bz = z3 - z1;
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0) len = 1;
        return new Vector3f(nx / len, ny / len, nz / len);
    }
}
