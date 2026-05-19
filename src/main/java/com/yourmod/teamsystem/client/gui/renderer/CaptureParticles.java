package com.yourmod.teamsystem.client.gui.renderer;

import com.yourmod.teamsystem.client.CapturePointData;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.VisualsConfig;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

import java.util.List;

public class CaptureParticles {

    private static final int RING_SEGMENTS = 48;
    private static final double FULL_VIS_DIST = 40.0;

    private long startTime = System.currentTimeMillis();

    public void render(PoseStack poseStack, MultiBufferSource bufferSource,
                       double camX, double camY, double camZ, float partialTick) {
        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;

        renderBaseZones(poseStack, bufferSource, camX, camY, camZ, elapsed);
        renderBorderWalls(poseStack, bufferSource, camX, camY, camZ, elapsed);

        VisualsConfig cfg = VisualsConfig.get();
        VisualsConfig.RingVisual ringCfg = cfg.rings;

        List<CapturePointData> points = ClientTeamData.capturePoints;
        if (points == null || points.isEmpty()) return;

        for (CapturePointData cp : points) {
            float zoneRad = (float)(cp.radius() > 0 ? cp.radius() : cfg.capturePoint.zoneRadius);
            double dx = cp.x() - camX;
            double dy = cp.y() - camY;
            double dz = cp.z() - camZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > ringCfg.maxBeamDist) continue;

            int ownerOrdinal = cp.ownerTeamOrdinal();
            int capturerOrdinal = cp.capturingTeamOrdinal();
            boolean isOwned = ownerOrdinal == 0 || ownerOrdinal == 1;
            double prog = cp.progress();
            boolean isContested = isOwned && capturerOrdinal == 2 && prog > 0.01f && prog < 1.0f;
            boolean isCapturing = !isContested && prog > 0.01f && prog < 1.0f
                && capturerOrdinal >= 0 && capturerOrdinal != ownerOrdinal && capturerOrdinal != 2;

            int ringColor, beamColor;
            float rotSpeed, extraPulse;

            if (isContested) {
                ringColor = 0x55FF4444;
                beamColor = 0x33FF4444;
                rotSpeed = 2.0f;
                extraPulse = (float)(Math.sin(elapsed * 4.0) * 0.3 + 0.7);
            } else if (isCapturing) {
                ringColor = 0x88E07B00;
                beamColor = 0x44E07B00;
                rotSpeed = 1.2f;
                extraPulse = 1.0f;
            } else {
                ringColor = switch (ownerOrdinal) {
                    case 0 -> 0x551C5FAD;
                    case 1 -> 0x55AD1C1C;
                    default -> 0x55AAAAAA;
                };
                beamColor = switch (ownerOrdinal) {
                    case 0 -> 0x331C5FAD;
                    case 1 -> 0x33AD1C1C;
                    default -> 0x33AAAAAA;
                };
                rotSpeed = 0.4f;
                extraPulse = 0.6f;
            }

            float alpha = calcAlpha(dist, ringCfg.maxBeamDist);

            // === Outer ring (dashed) ===
            renderDashedRing(poseStack, bufferSource,
                cp.x() - camX, cp.y() - camY + 0.06, cp.z() - camZ,
                zoneRad, (float)ringCfg.outerThickness, elapsed * 0.125f, ringColor, extraPulse * alpha);

            // === Inner ring (solid) ===
            renderSolidRing(poseStack, bufferSource,
                cp.x() - camX, cp.y() - camY + 0.05, cp.z() - camZ,
                zoneRad * 0.7f, (float)ringCfg.innerThickness, -elapsed * 0.083f, ringColor, extraPulse * alpha * 1.4f);

            // === Dots on outer ring ===
            renderRingDots(poseStack, bufferSource,
                cp.x() - camX, cp.y() - camY + 0.07, cp.z() - camZ,
                zoneRad, ringCfg.ringDotsCount, elapsed * 0.125f, ringColor, extraPulse * alpha);

            // === Beam removed per request ===
        }
    }

    private void renderDashedRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                  double ox, double oy, double oz,
                                  float radius, float thickness, float rotation,
                                  int color, float alpha) {
        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f mat = poseStack.last().pose();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f * alpha;

        float dashLen = 0.6f;
        float gapLen = 0.3f;
        float arcLen = dashLen + gapLen;
        float circumference = (float)(Math.PI * 2.0 * radius);
        int segments = (int)(circumference / arcLen);
        float segAngle = (float)(Math.PI * 2.0 / segments);
        float halfT = thickness / 2f;

        for (int i = 0; i < segments; i += 2) {
            float a1 = i * segAngle + rotation;
            float a2 = (i + 1) * segAngle + rotation;
            float cos1 = (float)Math.cos(a1), sin1 = (float)Math.sin(a1);
            float cos2 = (float)Math.cos(a2), sin2 = (float)Math.sin(a2);

            float x1i = cos1 * (radius - halfT), z1i = sin1 * (radius - halfT);
            float x1o = cos1 * (radius + halfT), z1o = sin1 * (radius + halfT);
            float x2i = cos2 * (radius - halfT), z2i = sin2 * (radius - halfT);
            float x2o = cos2 * (radius + halfT), z2o = sin2 * (radius + halfT);

            vc.vertex(mat, x1i, 0, z1i).color(r, g, b, a).endVertex();
            vc.vertex(mat, x1o, 0, z1o).color(r, g, b, a).endVertex();
            vc.vertex(mat, x2o, 0, z2o).color(r, g, b, a).endVertex();
            vc.vertex(mat, x1i, 0, z1i).color(r, g, b, a).endVertex();
            vc.vertex(mat, x2o, 0, z2o).color(r, g, b, a).endVertex();
            vc.vertex(mat, x2i, 0, z2i).color(r, g, b, a).endVertex();
        }

        poseStack.popPose();
    }

    private void renderSolidRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                 double ox, double oy, double oz,
                                 float radius, float thickness, float rotation,
                                 int color, float alpha) {
        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f mat = poseStack.last().pose();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f * alpha;

        int segments = 48;
        float halfT = thickness / 2f;

        for (int i = 0; i < segments; i++) {
            float a1 = (float)(i / (double) segments * Math.PI * 2.0) + rotation;
            float a2 = (float)((i + 1) / (double) segments * Math.PI * 2.0) + rotation;
            float cos1 = (float)Math.cos(a1), sin1 = (float)Math.sin(a1);
            float cos2 = (float)Math.cos(a2), sin2 = (float)Math.sin(a2);

            float x1i = cos1 * (radius - halfT), z1i = sin1 * (radius - halfT);
            float x1o = cos1 * (radius + halfT), z1o = sin1 * (radius + halfT);
            float x2i = cos2 * (radius - halfT), z2i = sin2 * (radius - halfT);
            float x2o = cos2 * (radius + halfT), z2o = sin2 * (radius + halfT);

            vc.vertex(mat, x1i, 0, z1i).color(r, g, b, a).endVertex();
            vc.vertex(mat, x1o, 0, z1o).color(r, g, b, a).endVertex();
            vc.vertex(mat, x2o, 0, z2o).color(r, g, b, a).endVertex();
            vc.vertex(mat, x1i, 0, z1i).color(r, g, b, a).endVertex();
            vc.vertex(mat, x2o, 0, z2o).color(r, g, b, a).endVertex();
            vc.vertex(mat, x2i, 0, z2i).color(r, g, b, a).endVertex();
        }

        poseStack.popPose();
    }

    private void renderRingDots(PoseStack poseStack, MultiBufferSource bufferSource,
                                double ox, double oy, double oz,
                                float radius, int count, float rotation,
                                int color, float alpha) {
        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.LINES);
        Matrix4f mat = poseStack.last().pose();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f * alpha * 1.5f;
        float dotSize = 0.12f;

        for (int i = 0; i < count; i++) {
            float angle = (float)(i / (double) count * Math.PI * 2.0) + rotation;
            float cx = (float)(Math.cos(angle) * radius);
            float cz = (float)(Math.sin(angle) * radius);
            vc.vertex(mat, cx, -dotSize, cz).color(r, g, b, a).normal(0, 1, 0).endVertex();
            vc.vertex(mat, cx, dotSize, cz).color(r, g, b, a).normal(0, 1, 0).endVertex();
        }

        poseStack.popPose();
    }

    private void renderBeamQuad(PoseStack poseStack, MultiBufferSource bufferSource,
                                double ox, double oy, double oz,
                                float beamWidth, float beamHeight,
                                int color, float alpha, float elapsed) {
        float beamPulse = (float)(Math.sin(elapsed * 2.0 * Math.PI / 3.0) * 0.15 + 0.3);

        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.LINES);
        Matrix4f mat = poseStack.last().pose();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float baseA = ((color >> 24) & 0xFF) / 255f * alpha;

        float hw = beamWidth / 2f;
        for (int i = 0; i < 8; i++) {
            float angle = (float)(i / 8.0 * Math.PI * 2.0) + elapsed * 0.5f;
            float nx = (float)(Math.cos(angle) * hw);
            float nz = (float)(Math.sin(angle) * hw);
            float bottomAlpha = baseA * beamPulse;
            float topAlpha = baseA * 0.1f;
            vc.vertex(mat, nx, 0.06f, nz).color(r, g, b, bottomAlpha).normal(0, 1, 0).endVertex();
            vc.vertex(mat, nx, beamHeight, nz).color(r, g, b, topAlpha).normal(0, 1, 0).endVertex();
        }

        poseStack.popPose();
    }

    private void renderBaseZones(PoseStack poseStack, MultiBufferSource bufferSource,
                                 double camX, double camY, double camZ, float elapsed) {
        VisualsConfig cfg = VisualsConfig.get();
        VisualsConfig.BaseRingVisual brCfg = cfg.baseRings;

        int[] nato = ClientTeamData.getNatoBasePos();
        int[] russia = ClientTeamData.getRussiaBasePos();
        int radius = ClientTeamData.getBaseRadius();

        double maxDist = brCfg.maxDist;

        if (nato != null) {
            double dx = nato[0] - camX;
            double dy = nato[1] - camY;
            double dz = nato[2] - camZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= maxDist) {
                renderBaseRing(poseStack, bufferSource, dx, nato[1] - camY, dz,
                    radius, 0x661C5FAD, elapsed, dist, brCfg);
            }
        }
        if (russia != null) {
            double dx = russia[0] - camX;
            double dy = russia[1] - camY;
            double dz = russia[2] - camZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= maxDist) {
                renderBaseRing(poseStack, bufferSource, dx, russia[1] - camY, dz,
                    radius, 0x66AD1C1C, elapsed, dist, brCfg);
            }
        }
    }

    private void renderBaseRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                double ox, double oy, double oz,
                                int baseRadius, int color,
                                float elapsed, double dist,
                                VisualsConfig.BaseRingVisual brCfg) {
        float alpha = calcAlpha(dist, brCfg.maxDist);
        float rot = elapsed * 0.05f;

        float ringRad = (float)(baseRadius * brCfg.radiusMultiplier);
        renderBaseDashedRing(poseStack, bufferSource, ox, oy + 0.06, oz,
            ringRad, (float)brCfg.outerThickness, rot, color, alpha);

        renderBaseDashedRing(poseStack, bufferSource, ox, oy + 0.05, oz,
            ringRad * (float)brCfg.innerRingScale, (float)brCfg.innerThickness, rot + 0.5f, color, alpha * 0.35f);
    }

    private void renderBaseDashedRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                      double ox, double oy, double oz,
                                      float radius, float thickness, float rotation,
                                      int color, float alpha) {
        poseStack.pushPose();
        poseStack.translate(ox, oy, oz);
        VertexConsumer vc = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f mat = poseStack.last().pose();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f * alpha;

        float dashLen = 0.8f;
        float gapLen = 0.4f;
        float arcLen = dashLen + gapLen;
        float circumference = (float)(Math.PI * 2.0 * radius);
        int segments = (int)(circumference / arcLen);
        float segAngle = (float)(Math.PI * 2.0 / segments);
        float halfT = thickness / 2f;

        for (int i = 0; i < segments; i += 2) {
            float a1 = i * segAngle + rotation;
            float a2 = (i + 1) * segAngle + rotation;
            float cos1 = (float)Math.cos(a1), sin1 = (float)Math.sin(a1);
            float cos2 = (float)Math.cos(a2), sin2 = (float)Math.sin(a2);

            float x1i = cos1 * (radius - halfT), z1i = sin1 * (radius - halfT);
            float x1o = cos1 * (radius + halfT), z1o = sin1 * (radius + halfT);
            float x2i = cos2 * (radius - halfT), z2i = sin2 * (radius - halfT);
            float x2o = cos2 * (radius + halfT), z2o = sin2 * (radius + halfT);

            vc.vertex(mat, x1i, 0, z1i).color(r, g, b, a).endVertex();
            vc.vertex(mat, x1o, 0, z1o).color(r, g, b, a).endVertex();
            vc.vertex(mat, x2o, 0, z2o).color(r, g, b, a).endVertex();
            vc.vertex(mat, x1i, 0, z1i).color(r, g, b, a).endVertex();
            vc.vertex(mat, x2o, 0, z2o).color(r, g, b, a).endVertex();
            vc.vertex(mat, x2i, 0, z2i).color(r, g, b, a).endVertex();
        }

        poseStack.popPose();
    }

    private void renderBorderWalls(PoseStack poseStack, MultiBufferSource bufferSource,
                                    double camX, double camY, double camZ, float elapsed) {
        List<byte[]> types = ClientTeamData.borderZoneTypes;
        List<double[]> data = ClientTeamData.borderZoneData;
        if (types == null || data == null || types.isEmpty()) return;

        for (int zi = 0; zi < types.size(); zi++) {
            byte type = types.get(zi)[0];
            double[] d = data.get(zi);
            if (type == 0) {
                double minX = d[0], minZ = d[1], maxX = d[2], maxZ = d[3];
                emitEdge(poseStack, bufferSource, camX, camY, camZ, minX, minZ, maxX, minZ, elapsed);
                emitEdge(poseStack, bufferSource, camX, camY, camZ, maxX, minZ, maxX, maxZ, elapsed);
                emitEdge(poseStack, bufferSource, camX, camY, camZ, maxX, maxZ, minX, maxZ, elapsed);
                emitEdge(poseStack, bufferSource, camX, camY, camZ, minX, maxZ, minX, minZ, elapsed);
            } else {
                int verts = d.length / 2;
                if (verts < 3) continue;
                for (int i = 0; i < verts; i++) {
                    double x1 = d[i * 2], z1 = d[i * 2 + 1];
                    double x2 = d[(i + 1) % verts * 2], z2 = d[(i + 1) % verts * 2 + 1];
                    emitEdge(poseStack, bufferSource, camX, camY, camZ, x1, z1, x2, z2, elapsed);
                }
            }
        }
    }

    private void emitEdge(PoseStack poseStack, MultiBufferSource bufferSource,
                           double camX, double camY, double camZ,
                           double x1, double z1, double x2, double z2, float elapsed) {
        double dx = x2 - x1, dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.5) return;

        VisualsConfig.BorderVisual cfg = VisualsConfig.get().border;
        double maxDist = cfg.maxDistance;
        double spacing = cfg.beamSpacing;
        double bw = cfg.beamWidth;
        double bh = cfg.beamHeight;

        double mx = (x1 + x2) / 2, mz = (z1 + z2) / 2;
        double midDist = Math.sqrt((mx - camX) * (mx - camX) + (mz - camZ) * (mz - camZ));
        if (midDist > maxDist) return;

        if (midDist > 60) spacing *= 1.5;
        if (midDist > 90) spacing *= 2.0;
        double edgeDist = distanceToSegment(camX, camZ, x1, z1, x2, z2);
        if (edgeDist > maxDist) return;
        float alpha = (float)Math.max(0.05, 1.0 - edgeDist / maxDist);

        double ux = dx / len, uz = dz / len;
        int count = Math.max(1, (int)(len / spacing));

        float r = (float)cfg.beamRed;
        float g = (float)cfg.beamGreen;
        float b = (float)cfg.beamBlue;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.LINES);
        Matrix4f mat = poseStack.last().pose();

        double yBase = 0.06;

        for (int i = 0; i <= count; i++) {
            double t = i / (double) count;
            double px = x1 + ux * t * len;
            double pz = z1 + uz * t * len;

            float wx = (float)(px - camX);
            float wz = (float)(pz - camZ);

            double perpX = -uz * bw, perpZ = ux * bw;
            float wx1 = (float)(wx + perpX), wz1 = (float)(wz + perpZ);
            float wx2 = (float)(wx - perpX), wz2 = (float)(wz - perpZ);

            float edgeFade = (float)(1.0 - Math.abs(t - 0.5) * 0.2);
            float a = Math.max(0.08f, alpha * edgeFade);

            vc.vertex(mat, wx1, (float)(yBase - camY), wz1).color(r, g, b, a * 0.8f).normal(0, 1, 0).endVertex();
            vc.vertex(mat, wx1, (float)(yBase + bh - camY), wz1).color(r, g, b, a * 0.04f).normal(0, 1, 0).endVertex();
            vc.vertex(mat, wx2, (float)(yBase - camY), wz2).color(r, g, b, a * 0.8f).normal(0, 1, 0).endVertex();
            vc.vertex(mat, wx2, (float)(yBase + bh - camY), wz2).color(r, g, b, a * 0.04f).normal(0, 1, 0).endVertex();
        }
    }

    private static double distanceToSegment(double px, double pz, double x1, double z1, double x2, double z2) {
        double abx = x2 - x1, abz = z2 - z1;
        double lenSq = abx * abx + abz * abz;
        if (lenSq == 0) return Math.sqrt((px - x1) * (px - x1) + (pz - z1) * (pz - z1));
        double t = ((px - x1) * abx + (pz - z1) * abz) / lenSq;
        t = Math.max(0, Math.min(1, t));
        double cx = x1 + t * abx, cz = z1 + t * abz;
        return Math.sqrt((px - cx) * (px - cx) + (pz - cz) * (pz - cz));
    }

    private static float calcAlpha(double dist, double maxDist) {
        if (dist <= FULL_VIS_DIST) return 1.0f;
        if (dist >= maxDist) return 0.05f;
        return (float)Math.max(0.05, 1.0 - (dist - FULL_VIS_DIST) / (maxDist - FULL_VIS_DIST));
    }
}
