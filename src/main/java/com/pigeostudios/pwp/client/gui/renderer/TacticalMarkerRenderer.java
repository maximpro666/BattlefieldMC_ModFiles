package com.pigeostudios.pwp.client.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.client.ClientMarkerData;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.core.MarkerData;
import com.pigeostudios.pwp.core.MarkerData.MarkerType;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TacticalMarkerRenderer {

    private static final double MAX_DIST = 5000.0;
    private static final double FULL_VIS_DIST = 100.0;

    private static final Map<MarkerType, ResourceLocation> TEXTURES = new HashMap<>();
    static {
        TEXTURES.put(MarkerType.POINT, new ResourceLocation(PWP.MODID, "textures/markers/point.png"));
        TEXTURES.put(MarkerType.ATTACK, new ResourceLocation(PWP.MODID, "textures/markers/attack.png"));
        TEXTURES.put(MarkerType.DEFEND, new ResourceLocation(PWP.MODID, "textures/markers/defend.png"));
        TEXTURES.put(MarkerType.OBSERVE, new ResourceLocation(PWP.MODID, "textures/markers/observe.png"));
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Team localTeam = ClientTeamData.getLocalPlayerTeam();
        if (localTeam == Team.SPECTATOR) return;

        List<MarkerData> markers = ClientMarkerData.getMarkers();
        if (markers.isEmpty()) return;

        Vec3 camPos = camera.getPosition();
        RenderSystem.disableDepthTest();

        for (MarkerData marker : markers) {
            if (marker.getTeamOrdinal() != localTeam.ordinal()) continue;

            double dx = marker.getX() - camPos.x;
            double dy = marker.getY() - camPos.y;
            double dz = marker.getZ() - camPos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > MAX_DIST) continue;

            RenderType renderType = getRenderType(marker.getType());
            if (renderType == null) continue;

            float alpha = calcAlpha(dist);
            if (alpha < 0.01) continue;

            renderMarker(poseStack, bufferSource, mc, camera,
                dx, dy, dz, dist, marker, renderType, alpha);
        }

        RenderSystem.enableDepthTest();
    }

    private RenderType getRenderType(MarkerType type) {
        ResourceLocation tex = TEXTURES.get(type);
        if (tex == null) return null;
        return RenderTypes.entityCutoutNoDepth(tex);
    }

    private void renderMarker(PoseStack poseStack, MultiBufferSource bufferSource,
                               Minecraft mc, Camera camera,
                               double dx, double dy, double dz, double dist,
                               MarkerData marker, RenderType renderType, float alpha) {
        Font font = mc.font;
        String label = marker.getLabel();
        int lw = font.width(label);
        float scale = (float) Math.min(Math.max(0.5, dist * 0.006), 3.0f);
        float iconSize = 4f * scale;

        poseStack.pushPose();
        poseStack.translate(dx, dy + 2.0, dz);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.translate(0, 0, (float) (-dist * 0.0001));
        poseStack.scale(-scale, -scale, scale);

        // Icon quad
        int teamColor = getTeamColor(marker.getTeamOrdinal());
        float r = ((teamColor >> 16) & 0xFF) / 255f;
        float g = ((teamColor >> 8) & 0xFF) / 255f;
        float b = (teamColor & 0xFF) / 255f;

        VertexConsumer vc = bufferSource.getBuffer(renderType);
        Matrix4f mat = poseStack.last().pose();
        vc.vertex(mat, -iconSize, iconSize, 0).color(r, g, b, alpha).uv(0, 1).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();
        vc.vertex(mat, iconSize, iconSize, 0).color(r, g, b, alpha).uv(1, 1).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();
        vc.vertex(mat, iconSize, -iconSize, 0).color(r, g, b, alpha).uv(1, 0).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();
        vc.vertex(mat, -iconSize, -iconSize, 0).color(r, g, b, alpha).uv(0, 0).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();

        // Label text
        float textY = iconSize + 2f;
        float textScale = 0.6f;
        poseStack.pushPose();
        poseStack.translate(0, textY, 0.02f);
        poseStack.scale(textScale, textScale, 1f);
        font.drawInBatch(label, -lw / 2f, 0f,
            ((int)(0xDD * alpha) << 24) | 0xFFFFFF, true,
            poseStack.last().pose(), bufferSource,
            Font.DisplayMode.SEE_THROUGH, 0, 0xF000F0);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static int getTeamColor(int teamOrdinal) {
        return teamOrdinal == 0 ? UITheme.TEAM_NATO : UITheme.TEAM_RUSSIA;
    }

    private static float calcAlpha(double dist) {
        if (dist <= FULL_VIS_DIST) return 1.0f;
        if (dist >= MAX_DIST) return 0.15f;
        return (float) Math.max(0.15, 1.0 - (dist - FULL_VIS_DIST) / (MAX_DIST - FULL_VIS_DIST));
    }
}
