package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yourmod.teamsystem.client.ClientMarkerData;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.CapturePointData;
import com.yourmod.teamsystem.core.MarkerData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WorldMarkerRenderer {

    private static final int TEXT_COLOR_NEUTRAL   = 0xFFCCCCCC;
    private static final int TEXT_COLOR_NATO      = 0xFF4A90D9;
    private static final int TEXT_COLOR_RUSSIA    = 0xFFD94A4A;
    private static final int TEXT_COLOR_CAPTURING = 0xFFFFCC44;
    private static final int TEXT_COLOR_CONTESTED = 0xFFFF4444;
    private static final double MAX_DIST          = 128.0;

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
            for (CapturePointData cp : points) {
                renderCapturePoint(poseStack, bufferSource, camera, camPos, cp, partialTick);
            }
        }
    }

    private void renderCapturePoint(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera,
                                     Vec3 camPos, CapturePointData cp, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        double dx = cp.x() - camPos.x;
        double dy = cp.y() - camPos.y;
        double dz = cp.z() - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > MAX_DIST) return;

        int ownerOrdinal = cp.ownerTeamOrdinal();
        int capturerOrdinal = cp.capturingTeamOrdinal();
        boolean isOwned = ownerOrdinal == 0 || ownerOrdinal == 1;
        double prog = cp.progress();
        boolean isContested = isOwned && capturerOrdinal == 2 && prog > 0.01f && prog < 1.0f;
        boolean isCapturing = prog > 0.01f && prog < 1.0f
            && capturerOrdinal >= 0 && capturerOrdinal != ownerOrdinal && capturerOrdinal != 2;

        int textColor;
        String icon;
        String label;

        if (isContested) {
            textColor = TEXT_COLOR_CONTESTED;
            icon = "\u26A1";
            label = icon + " " + cp.name() + " \u26A1";
        } else if (isCapturing) {
            textColor = TEXT_COLOR_CAPTURING;
            icon = "\u25B6";
            label = icon + " " + cp.name() + " " + (int)(cp.progress() * 100) + "%";
        } else {
            textColor = switch (ownerOrdinal) {
                case 0 -> TEXT_COLOR_NATO;
                case 1 -> TEXT_COLOR_RUSSIA;
                default -> TEXT_COLOR_NEUTRAL;
            };
            icon = switch (ownerOrdinal) {
                case 0 -> "\u2691";
                case 1 -> "\u2691";
                default -> "\u25CB";
            };
            label = icon + " " + cp.name();
        }

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;
        float bob = (float)(Math.sin(elapsed * 0.8) * 0.3);

        // Draw floating label at Y+3 with bob animation
        poseStack.pushPose();
        double wy = cp.y() + 3.0 + bob;
        poseStack.translate(cp.x() - camPos.x, wy - camPos.y, cp.z() - camPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

        float scale = 0.035f;
        poseStack.scale(-scale, -scale, scale);

        // Background box
        int tw = mc.font.width(label);
        mc.font.drawInBatch(label,
            -tw / 2f, -4f,
            textColor, false,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0, 0xF000F0);

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
            color, false,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0, 0xF000F0);

        poseStack.popPose();
    }

    private String buildLabel(MarkerData m) {
        return m.getLabel() != null ? m.getLabel() : "";
    }
}
