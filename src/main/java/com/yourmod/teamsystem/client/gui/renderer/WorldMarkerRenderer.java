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

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = camera.getPosition();

        List<MarkerData> markers = ClientMarkerData.getMarkers();
        if (markers != null && !markers.isEmpty()) {
            for (MarkerData marker : markers) {
                renderLabel(poseStack, bufferSource, camera, camPos, marker.getX(), marker.getY() + 2.5, marker.getZ(), buildLabel(marker), 0xFFFFFFFF, false);
            }
        }

        List<CapturePointData> points = ClientTeamData.capturePoints;
        if (points != null && !points.isEmpty()) {
            for (CapturePointData cp : points) {
                String pointLabel = "[" + cp.name() + "]";
                renderLabel(poseStack, bufferSource, camera, camPos, cp.x(), cp.y() + 2.0, cp.z(), pointLabel, 0xFF50E050, true);
            }
        }
    }

    private void renderLabel(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, Vec3 camPos,
                             double x, double y, double z, String label, int color, boolean distanceScale) {
        double dx = x - camPos.x;
        double dy = y - camPos.y;
        double dz = z - camPos.z;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 256) return;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

        float scale = distanceScale ? (float)(0.02 + 0.03 * (dist / 40.0)) : 0.025f;
        if (scale > 0.15f) scale = 0.15f;
        poseStack.scale(scale, scale, scale);

        Minecraft mc = Minecraft.getInstance();
        mc.font.drawInBatch(label,
            -mc.font.width(label) / 2f, -4f,
            color, false,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0,
            0xF000F0);

        poseStack.popPose();
    }

    private String buildLabel(MarkerData m) {
        String prefix = m.getType() != null ? "[" + m.getType().name() + "] " : "";
        String lbl    = m.getLabel() != null ? m.getLabel() : "";
        return prefix + lbl;
    }
}
