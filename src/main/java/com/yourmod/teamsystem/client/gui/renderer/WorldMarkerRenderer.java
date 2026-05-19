package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yourmod.teamsystem.client.ClientMarkerData;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.CapturePointData;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.VisualsConfig;
import com.yourmod.teamsystem.core.MarkerData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WorldMarkerRenderer {

    private static final double MAX_DIST = 240.0;
    private static final double BASE_MAX_DIST = 240.0;
    private static final double FULL_VIS_DIST = 40.0;

    // State colors: fillColor, borderColor, bgColor
    private static final int   NEUTRAL_BORDER = 0xFF888888;
    private static final int   NEUTRAL_BG     = 0xCC333333;

    private static final int   NATO_FILL     = 0xFF1C5FAD;
    private static final int   NATO_BORDER   = 0xFF3A7FCE;
    private static final int   NATO_BG       = 0xCC0A1E38;

    private static final int   RUSSIA_FILL   = 0xFFAD1C1C;
    private static final int   RUSSIA_BORDER = 0xFFCE3A3A;
    private static final int   RUSSIA_BG     = 0xCC380A0A;

    private static final int   CAPTURE_FILL  = 0xFFFFCC44;
    private static final int   CAPTURE_BORDER= 0xFFFFE066;
    private static final int   CAPTURE_BG    = 0xCC2A2000;

    private static final int   CONTEST_FILL  = 0xFFFF4444;
    private static final int   CONTEST_BG    = 0xCC2A0808;

    private long startTime = System.currentTimeMillis();

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = camera.getPosition();

        List<MarkerData> markers = ClientMarkerData.getMarkers();
        if (markers != null && !markers.isEmpty()) {
            for (MarkerData marker : markers) {
                renderMarkerLabel(poseStack, bufferSource, camera, camPos,
                    marker.getX(), marker.getY() + 2.5, marker.getZ(),
                    buildLabel(marker), 0xFFFFFFFF);
            }
        }

        List<CapturePointData> points = ClientTeamData.capturePoints;
        if (points != null && !points.isEmpty()) {
            List<CapturePointRender> renderList = new ArrayList<>();
            for (CapturePointData cp : points) {
                double dx = cp.x() - camPos.x;
                double dy = cp.y() - camPos.y;
                double dz = cp.z() - camPos.z;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > MAX_DIST) continue;
                renderList.add(new CapturePointRender(cp, dist));
            }
            renderList.sort(Comparator.comparingDouble(a -> -a.dist));
            for (CapturePointRender r : renderList) {
                renderCapturePoint(poseStack, bufferSource, camera, camPos, r.cp, r.dist);
            }
        }

        renderBases(poseStack, bufferSource, camera, camPos);
    }

    private static class CapturePointRender {
        final CapturePointData cp;
        final double dist;
        CapturePointRender(CapturePointData cp, double dist) { this.cp = cp; this.dist = dist; }
    }

    private void renderCapturePoint(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera,
                                     Vec3 camPos, CapturePointData cp, double dist) {
        Minecraft mc = Minecraft.getInstance();
        int ownerOrdinal = cp.ownerTeamOrdinal();
        int capturerOrdinal = cp.capturingTeamOrdinal();
        boolean isOwned = ownerOrdinal == 0 || ownerOrdinal == 1;
        double prog = cp.progress();
        boolean isContested = isOwned && capturerOrdinal == 2 && prog > 0.01f && prog < 1.0f;
        boolean isCapturing = !isContested && prog > 0.01f && prog < 1.0f
            && capturerOrdinal >= 0 && capturerOrdinal != ownerOrdinal && capturerOrdinal != 2;

        int fillColor, borderColor, bgColor;

        if (isContested) {
            fillColor = CONTEST_FILL;
            borderColor = CONTEST_FILL;
            bgColor = CONTEST_BG;
        } else if (isCapturing) {
            fillColor = CAPTURE_FILL;
            borderColor = CAPTURE_BORDER;
            bgColor = CAPTURE_BG;
        } else if (ownerOrdinal == 0) {
            fillColor = NATO_FILL;
            borderColor = NATO_BORDER;
            bgColor = NATO_BG;
        } else if (ownerOrdinal == 1) {
            fillColor = RUSSIA_FILL;
            borderColor = RUSSIA_BORDER;
            bgColor = RUSSIA_BG;
        } else {
            fillColor = 0;
            borderColor = NEUTRAL_BORDER;
            bgColor = NEUTRAL_BG;
        }

        VisualsConfig.PointVisual cfg = VisualsConfig.get().capturePoint;

        // Proximity fade: transparent when inside the capture zone
        float proxAlpha = 1.0f;
        if (dist < cfg.proximityFade) {
            proxAlpha = (float)(dist / cfg.proximityFade);
        }

        float alpha = calcAlpha(dist, MAX_DIST) * proxAlpha;
        if (alpha < 0.01) return;

        boolean lodFull = dist <= FULL_VIS_DIST;

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;
        float bob = (float)(Math.sin(elapsed * Math.PI * 2.0 / cfg.bobPeriod) * cfg.bobAmplitude / 16f);
        if (isContested) {
            bob = (float)(Math.sin(elapsed * Math.PI * 2.0 / 1.5) * 4f / 16f);
        }

        // Rise+scale with distance
        double t = Math.min(1.0, dist / MAX_DIST);
        double rise = t * cfg.riseHeight;
        float scale = 0.035f + (float)(t * cfg.extraScale);

        poseStack.pushPose();
        double wy = cp.y() + 3.0 + bob + rise;
        poseStack.translate(cp.x() - camPos.x, wy - camPos.y, cp.z() - camPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-scale, -scale, scale);

        Font font = mc.font;
        String letter = cp.name().length() > 0 ? cp.name().substring(0, 1).toUpperCase() : "?";
        float hs = (float)cfg.markerSize / 2f;
        float texAlpha = textAlpha(dist, MAX_DIST);

        // === Background ===
        drawRect(poseStack, bufferSource, -hs, -hs, hs, hs,
            applyAlpha(bgColor, alpha));

        // === Progress fill (full background, behind letter) ===
        if (prog > 0.01) {
            int pf;
            if (capturerOrdinal >= 0 && capturerOrdinal <= 1) {
                pf = capturerOrdinal == 0 ? NATO_FILL : RUSSIA_FILL;
            } else if (ownerOrdinal >= 0 && ownerOrdinal <= 1) {
                pf = ownerOrdinal == 0 ? NATO_FILL : RUSSIA_FILL;
            } else {
                pf = CAPTURE_FILL;
            }
            float fillW = (float)(cfg.markerSize * Math.max(0.0, Math.min(1.0, prog)));
            drawRect(poseStack, bufferSource, -hs, -hs, -hs + fillW, hs,
                applyAlpha(pf, alpha));
        }

        // === Border (thick, no pulse) ===
        float bd = (float)cfg.borderWidth;
        int borderA = (int)(alpha * 0xFF);
        int ba = Math.min(0xFF, Math.max(0, borderA * 2 / 3));
        int borderArgb = (borderA << 24) | (borderColor & 0x00FFFFFF);
        int borderOuterArgb = (ba << 24) | (borderColor & 0x00FFFFFF);
        drawRect(poseStack, bufferSource, -hs - bd, -hs - bd, hs + bd, -hs, borderOuterArgb);
        drawRect(poseStack, bufferSource, -hs - bd, hs, hs + bd, hs + bd, borderOuterArgb);
        drawRect(poseStack, bufferSource, -hs - bd, -hs, -hs, hs + bd, borderOuterArgb);
        drawRect(poseStack, bufferSource, hs, -hs, hs + bd, hs + bd, borderOuterArgb);
        drawRect(poseStack, bufferSource, -hs, -hs, hs, -hs + bd, borderArgb);
        drawRect(poseStack, bufferSource, -hs, hs - bd, hs, hs, borderArgb);
        drawRect(poseStack, bufferSource, -hs, -hs, -hs + bd, hs, borderArgb);
        drawRect(poseStack, bufferSource, hs - bd, -hs, hs, hs, borderArgb);

        // === Progress edge line (no pulse) ===
        if (prog > 0.01) {
            float fillW = (float)(cfg.markerSize * Math.max(0.0, Math.min(1.0, prog)));
            float edgeX = -hs + fillW;
            float ew = (float)cfg.progressEdgeWidth;
            int edgeColor = applyAlpha(0xFFFFFFFF, alpha * 0.6f);
            drawRect(poseStack, bufferSource, edgeX - ew, -hs, edgeX + ew, hs, edgeColor);
        }

        // === Letter (centered) ===
        int letterColor = (int)(0xFF * texAlpha) << 24 | 0xFFFFFF;
        int lw = font.width(letter);
        font.drawInBatch(letter,
            -lw / 2f, -4f,
            letterColor, true,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0, 0xF000F0);

        // === Distance suffix ===
        if (cfg.showDistance && !lodFull && dist > FULL_VIS_DIST + 10) {
            String distStr = (int)dist + "m";
            int dw = font.width(distStr);
            float ds = 0.5f;
            poseStack.pushPose();
            poseStack.translate(0, hs + 2, 0);
            poseStack.scale(ds, ds, 1);
            font.drawInBatch(distStr,
                -dw / 2f, 0f,
                (int)(0x88 * alpha) << 24 | 0xCCCCCC, true,
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                0, 0xF000F0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void drawRect(PoseStack poseStack, MultiBufferSource bufferSource,
                          float x1, float y1, float x2, float y2, int color) {
        VertexConsumer vc = bufferSource.getBuffer(RenderType.gui());
        Matrix4f mat = poseStack.last().pose();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        vc.vertex(mat, x1, y2, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x2, y2, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x2, y1, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, x1, y1, 0).color(r, g, b, a).endVertex();
    }

    private void renderBases(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, Vec3 camPos) {
        int[] nato = ClientTeamData.getNatoBasePos();
        int[] russia = ClientTeamData.getRussiaBasePos();
        if (nato == null && russia == null) return;
        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;

        if (nato != null) {
            renderBaseMarker(poseStack, bufferSource, camera, camPos,
                nato[0], nato[1], nato[2], "NATO", UITheme.TEAM_NATO, elapsed);
        }
        if (russia != null) {
            renderBaseMarker(poseStack, bufferSource, camera, camPos,
                russia[0], russia[1], russia[2], "RUSSIA", UITheme.TEAM_RUSSIA, elapsed);
        }
    }

    private void renderBaseMarker(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera,
                                   Vec3 camPos, double x, double y, double z,
                                   String label, int teamColor, float elapsed) {
        double dx = x - camPos.x;
        double dy = y - camPos.y;
        double dz = z - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > BASE_MAX_DIST) return;

        VisualsConfig.BaseVisual cfg = VisualsConfig.get().base;

        // Proximity fade using base radius
        int baseRadius = ClientTeamData.getBaseRadius();
        float proxAlpha = 1.0f;
        if (cfg.proximityFade > 0 && dist < baseRadius) {
            proxAlpha = (float)(dist / baseRadius);
        }

        float alpha = calcAlpha(dist, BASE_MAX_DIST) * proxAlpha;
        if (alpha < 0.01) return;

        float bob = (float)(Math.sin(elapsed * Math.PI * 2.0 / cfg.bobPeriod) * cfg.bobAmplitude / 16f);

        // Rise+scale with distance
        double t = Math.min(1.0, dist / BASE_MAX_DIST);
        double rise = t * cfg.riseHeight;
        float scale = 0.035f + (float)(t * cfg.extraScale);

        poseStack.pushPose();
        poseStack.translate(dx, y + 4.0 + bob + rise - camPos.y, dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.scale(-scale, -scale, scale);

        Font font = Minecraft.getInstance().font;
        float hw = (float)cfg.width / 2f;
        float hh = (float)cfg.height / 2f;

        // Background
        drawRect(poseStack, bufferSource, -hw, -hh, hw, hh,
            ((int)(0x99 * alpha) << 24) | 0x0A0A0A);
        // Team fill
        drawRect(poseStack, bufferSource, -hw, -hh, hw, hh,
            ((int)(0x77 * alpha) << 24) | (teamColor & 0x00FFFFFF));
        // Border
        int borderArgb = ((int)(0xFF * alpha) << 24) | (teamColor & 0x00FFFFFF);
        float bd = (float)cfg.borderWidth;
        drawRect(poseStack, bufferSource, -hw, -hh, hw, -hh + bd, borderArgb);
        drawRect(poseStack, bufferSource, -hw, hh - bd, hw, hh, borderArgb);
        drawRect(poseStack, bufferSource, -hw, -hh, -hw + bd, hh, borderArgb);
        drawRect(poseStack, bufferSource, hw - bd, -hh, hw, hh, borderArgb);

        // Label
        float texAlpha = textAlpha(dist, BASE_MAX_DIST);
        int labelColor = (int)(0xFF * texAlpha) << 24 | 0xFFFFFF;
        int lw = font.width(label);
        font.drawInBatch(label,
            -lw / 2f, -4f,
            labelColor, true,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0, 0xF000F0);

        // Distance suffix
        boolean lodFull = dist <= FULL_VIS_DIST;
        if (cfg.showDistance && !lodFull && dist > FULL_VIS_DIST + 10) {
            String distStr = (int)dist + "m";
            int dw = font.width(distStr);
            float ds = 0.5f;
            poseStack.pushPose();
            poseStack.translate(0, hh + 2, 0);
            poseStack.scale(ds, ds, 1);
            font.drawInBatch(distStr,
                -dw / 2f, 0f,
                (int)(0x88 * alpha) << 24 | 0xCCCCCC, true,
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                0, 0xF000F0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderMarkerLabel(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, Vec3 camPos,
                             double x, double y, double z, String label, int color) {
        double dx = x - camPos.x;
        double dy = y - camPos.y;
        double dz = z - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > MAX_DIST) return;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

        float scale = 0.025f;
        poseStack.scale(-scale, -scale, scale);

        Minecraft mc = Minecraft.getInstance();
        mc.font.drawInBatch(label,
            -mc.font.width(label) / 2f, -4f,
            color, true,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0, 0xF000F0);
        poseStack.popPose();
    }

    private String buildLabel(MarkerData m) {
        return m.getLabel() != null ? m.getLabel() : "";
    }

    private static float calcAlpha(double dist, double maxDist) {
        if (dist <= FULL_VIS_DIST) return 1.0f;
        if (dist >= maxDist) return 0.05f;
        return (float)Math.max(0.05, 1.0 - (dist - FULL_VIS_DIST) / (maxDist - FULL_VIS_DIST));
    }

    private static float textAlpha(double dist, double maxDist) {
        if (dist <= FULL_VIS_DIST) return 1.0f;
        if (dist >= maxDist) return 0.35f;
        return (float)Math.max(0.35, 1.0 - (dist - FULL_VIS_DIST) / (maxDist - FULL_VIS_DIST) * 0.65);
    }

    private static int applyAlpha(int color, float alpha) {
        int a = (int)(((color >> 24) & 0xFF) * alpha);
        return (Math.min(0xFF, a) << 24) | (color & 0x00FFFFFF);
    }
}
