package com.pigeostudios.pwp.client.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.PlayerListEntry;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.VisualsConfig;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SquadMarkerRenderer {

    private static final int LEADER_COLOR_DEFAULT = 0xFFFFD700;

    private static final ResourceLocation TEX_DIAMOND = new ResourceLocation(PWP.MODID, "textures/squad/diamond.png");
    private static final ResourceLocation TEX_CHEVRON = new ResourceLocation(PWP.MODID, "textures/squad/chevron.png");
    private static final ResourceLocation TEX_SQUARE  = new ResourceLocation(PWP.MODID, "textures/squad/square.png");

    public enum Shape {
        DIAMOND(TEX_DIAMOND),
        CHEVRON(TEX_CHEVRON),
        SQUARE(TEX_SQUARE);

        public final ResourceLocation texture;
        Shape(ResourceLocation tex) { this.texture = tex; }
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player localPlayer = mc.player;
        if (localPlayer == null) return;

        VisualsConfig cfg = VisualsConfig.get();
        if (cfg.squadMarker == null) return;
        if (!cfg.squadMarker.enabled) return;

        Team localTeam = ClientTeamData.getLocalPlayerTeam();
        if (localTeam == Team.SPECTATOR) return;
        if (mc.level == null) return;

        double fadeDist = cfg.squadMarker.proximityFadeDist;
        double angleDeg = cfg.squadMarker.crosshairAngle;
        double angleCos = angleDeg > 0 ? Math.cos(Math.toRadians(angleDeg)) : -1;
        Vec3 camPos = camera.getPosition();

        for (Player player : mc.level.players()) {
            if (player == localPlayer) continue;
            if (player.isSpectator()) continue;

            PlayerListEntry entry = ClientTeamData.playerDataMap.get(player.getUUID());
            if (entry == null) continue;
            if (entry.teamOrdinal() != localTeam.ordinal()) continue;

            double dist = player.distanceTo(localPlayer);

            float alpha = (float) cfg.squadMarker.opacity;
            if (fadeDist > 0 && dist < fadeDist) alpha *= (float) (dist / fadeDist);
            if (alpha < 0.01f) continue;

            Vec3 targetPos = player.getEyePosition(partialTick);
            Vec3 toTarget = targetPos.subtract(camPos).normalize();
            Vector3f lv = camera.getLookVector();
            Vec3 lookVec = new Vec3(lv.x, lv.y, lv.z);
            double dot = toTarget.dot(lookVec);
            if (dot > angleCos) continue;

            Team playerTeam = Team.fromOrdinal(entry.teamOrdinal());
            int teamColor = playerTeam == Team.NATO ? UITheme.TEAM_NATO : UITheme.TEAM_RUSSIA;

            String mySquad = ClientTeamData.localPlayerSquad;
            boolean inSquad = mySquad != null && !mySquad.isEmpty();
            boolean isLeaderInMySquad = inSquad && isPlayerInMySquad(entry, mySquad) && entry.isSquadLeader();
            int color = isLeaderInMySquad
                ? (cfg.squadMarker.leaderColor != 0 ? cfg.squadMarker.leaderColor : LEADER_COLOR_DEFAULT)
                : (cfg.squadMarker.color != 0 ? cfg.squadMarker.color : teamColor);

            Shape shape = parseShape(cfg.squadMarker.shape);
            renderMarker(poseStack, bufferSource, player, camPos, camera, partialTick,
                cfg.squadMarker.size, color, alpha, shape);
        }
    }

    private Shape parseShape(String name) {
        if (name == null) return Shape.DIAMOND;
        try { return Shape.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return Shape.DIAMOND; }
    }

    private boolean isPlayerInMySquad(PlayerListEntry entry, String mySquad) {
        String ps = entry.squadName() != null && !entry.squadName().isEmpty() ? entry.squadName() : entry.squad();
        return ps != null && mySquad.equals(ps);
    }

    private void renderMarker(PoseStack poseStack, MultiBufferSource bufferSource, Player player,
                               Vec3 camPos, Camera camera, float partialTick,
                               double size, int color, float alpha, Shape shape) {
        double wy = player.yo + (player.getY() - player.yo) * partialTick;
        double headY = wy + player.getBbHeight() + 0.55;
        double px = player.xo + (player.getX() - player.xo) * partialTick;
        double pz = player.zo + (player.getZ() - player.zo) * partialTick;
        double dx = px - camPos.x;
        double dz = pz - camPos.z;
        double dy = headY - camPos.y;
        double actualDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (actualDist < 0.01) return;

        float scale = (float) Math.min(actualDist * 0.008 * size * 8f, 4.0f);
        float half = 0.5f;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.translate(0, 0, (float) (-actualDist * 0.0001));
        poseStack.scale(-scale, -scale, scale);

        // Glow pass (additive blend, larger size)
        float glowScale = 3.0f;
        VertexConsumer vcGlow = bufferSource.getBuffer(RenderTypes.eyesNoDepth(shape.texture));
        Matrix4f matGlow = poseStack.last().pose();
        drawTexturedQuad(vcGlow, matGlow, -half * glowScale, half * glowScale,
            -half * glowScale, half * glowScale, r, g, b, alpha * 0.15f);

        // Main pass (cutout)
        VertexConsumer vc = bufferSource.getBuffer(RenderTypes.entityCutoutNoDepth(shape.texture));
        Matrix4f mat = poseStack.last().pose();
        drawTexturedQuad(vc, mat, -half, half, -half, half, r, g, b, alpha);

        poseStack.popPose();
    }

    private void drawTexturedQuad(VertexConsumer vc, Matrix4f mat,
                                   float x1, float x2, float y1, float y2,
                                   float r, float g, float b, float a) {
        vc.vertex(mat, x1, y2, 0).color(r, g, b, a).uv(0, 1).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();
        vc.vertex(mat, x2, y2, 0).color(r, g, b, a).uv(1, 1).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();
        vc.vertex(mat, x2, y1, 0).color(r, g, b, a).uv(1, 0).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();
        vc.vertex(mat, x1, y1, 0).color(r, g, b, a).uv(0, 0).overlayCoords(0, 10).uv2(0xF000F0).normal(0, 0, 1).endVertex();
    }
}
