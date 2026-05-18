package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yourmod.teamsystem.client.ClientMarkerData;
import com.yourmod.teamsystem.core.MarkerData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WorldMarkerRenderer {

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTick) {
        List<MarkerData> markers = ClientMarkerData.getMarkers();
        if (markers == null || markers.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = camera.getPosition();

        for (MarkerData marker : markers) {
            double dx = marker.getX() - camPos.x;
            double dy = marker.getY() + 2.5 - camPos.y;
            double dz = marker.getZ() - camPos.z;

            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 128) continue;

            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
            poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

            float scale = 0.025f;
            poseStack.scale(scale, scale, scale);

            String label = buildLabel(marker);
            mc.font.draw(poseStack, label, -mc.font.width(label) / 2f, -4f, 0xFFFFFFFF);

            poseStack.popPose();
        }
    }

    private String buildLabel(MarkerData m) {
        String prefix = m.getType() != null ? "[" + m.getType().name() + "] " : "";
        String lbl    = m.getLabel() != null ? m.getLabel() : "";
        return prefix + lbl;
    }
}
